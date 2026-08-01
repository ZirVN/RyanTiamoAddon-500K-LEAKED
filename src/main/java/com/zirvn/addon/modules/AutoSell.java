package com.zirvn.addon.modules;

import com.zirvn.addon.ZirAddon;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class AutoSell extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgStats;
    private final SettingGroup sgWebhook;

    private final Setting<SellMode> mode;
    private final Setting<List<Item>> items;
    private final Setting<Integer> delay;
    private final Setting<Boolean> statsEnabled;
    private final Setting<Double> valuePerSale;
    private final Setting<Boolean> useChatParsing;
    private final Setting<Boolean> webhookEnabled;
    private final Setting<String> webhookName;
    private final Setting<String> webhookAvatar;
    private final Setting<String> webhookUrl;
    private final Setting<Integer> webhookIntervalMin;

    private int timer;
    private boolean inChest;
    private long startTime;
    private long lastWebhookTime;
    private long lastMessageTime;
    private int totalSales;
    private double totalEarned;
    private String lastMessageId;

    private static AutoSell instance;

    public AutoSell() {
        super(ZirAddon.CATEGORY, "auto-sell", "Automatically sells items with stats and webhook reporting.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgStats = this.settings.createGroup("Stats");
        this.sgWebhook = this.settings.createGroup("Webhook");

        this.mode = this.sgGeneral.add(new EnumSetting.Builder<SellMode>()
            .name("mode")
            .description("Whether to whitelist or blacklist the selected items.")
            .defaultValue(SellMode.Whitelist)
            .build());
        this.items = this.sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to sell.")
            .defaultValue(List.of(Items.CHEST))
            .build());
        this.delay = this.sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay in ticks between actions.")
            .defaultValue(5)
            .min(1)
            .sliderMax(40)
            .build());

        this.statsEnabled = this.sgStats.add(new BoolSetting.Builder()
            .name("stats-enabled")
            .description("Track AutoSell statistics.")
            .defaultValue(true)
            .build());
        this.valuePerSale = this.sgStats.add(new DoubleSetting.Builder()
            .name("value-per-sale")
            .description("Estimated money value added per sale for stats.")
            .defaultValue(1.0)
            .min(0.0)
            .sliderRange(0.0, 10000.0)
            .build());
        this.useChatParsing = this.sgStats.add(new BoolSetting.Builder()
            .name("use-chat-parsing")
            .description("Parse server chat for sale amounts instead of the fixed value-per-sale setting.")
            .defaultValue(true)
            .build());

        this.webhookEnabled = this.sgWebhook.add(new BoolSetting.Builder()
            .name("webhook-enabled")
            .description("Send periodic AutoSell reports to a webhook.")
            .defaultValue(false)
            .build());
        this.webhookName = this.sgWebhook.add(new StringSetting.Builder()
            .name("webhook-name")
            .description("Display name for the webhook message (optional)")
            .defaultValue("AutoSell Report")
            .build());
        this.webhookAvatar = this.sgWebhook.add(new StringSetting.Builder()
            .name("webhook-avatar")
            .description("Avatar URL for the webhook (optional)")
            .defaultValue("")
            .build());
        this.webhookUrl = this.sgWebhook.add(new StringSetting.Builder()
            .name("webhook-url")
            .description("Webhook URL for periodic AutoSell reports.")
            .defaultValue("")
            .build());
        this.webhookIntervalMin = this.sgWebhook.add(new IntSetting.Builder()
            .name("webhook-interval-min")
            .description("How often to send a webhook report.")
            .defaultValue(10)
            .min(1)
            .max(180)
            .build());

        this.timer = 0;
        this.inChest = false;
        instance = this;
    }

    public static AutoSell getInstance() {
        return instance;
    }

    public static void onChatMessage(String text) {
        try {
            if (text != null && instance != null && instance.isActive()) {
                if (instance.useChatParsing.get()) {
                    double val = instance.parseEarnings(text);
                    if (val > 0) {
                        instance.totalEarned += val;
                        instance.triggerWebhookUpdate();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private double parseEarnings(String text) {
        if (text == null) return 0.0;
        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.contains("sell") && !lower.contains("sold") && !lower.contains("earn") && !lower.contains("receive") && !lower.contains("reward") && !lower.contains("bán") && !lower.contains("nhận") && !lower.contains("kiếm") && !lower.contains("thưởng") && !text.contains("$") && !text.contains("+$") && !text.contains("+ $")) {
            return 0.0;
        }
        Matcher matcher = Pattern.compile("(?i)\\$?([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]+)?|[0-9]+(?:\\.[0-9]+)?)\\s*([kmb]?)").matcher(text);
        if (matcher.find()) {
            String numStr = matcher.group(1).replace(",", "");
            String unit = matcher.group(2).trim().toLowerCase(Locale.ROOT);
            try {
                double val = Double.parseDouble(numStr);
                if (unit.startsWith("k")) val *= 1000.0;
                else if (unit.startsWith("m")) val *= 1000000.0;
                else if (unit.startsWith("b")) val *= 1.0E9;
                return val;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    @Override
    public void onActivate() {
        this.timer = 20;
        this.inChest = false;
        this.resetStats();
        this.startTime = System.currentTimeMillis();
        this.lastWebhookTime = this.startTime;
        if (this.webhookEnabled.get() && isWebhookValid()) {
            sendWebhook("Running");
        }
    }

    @Override
    public void onDeactivate() {
        if (this.webhookEnabled.get() && isWebhookValid()) {
            sendFinalWebhook("Stopped");
        }
        this.resetStats();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (this.mc.player == null || this.mc.interactionManager == null) return;

        if (this.timer > 0) {
            --this.timer;
        } else {
            this.processAutoSell();
        }

        long now = System.currentTimeMillis();
        if (this.webhookEnabled.get() && this.lastMessageId != null && (now - this.lastWebhookTime >= (long) this.webhookIntervalMin.get() * 60000L)) {
            sendWebhook("Running");
            this.lastWebhookTime = now;
        }
    }

    private void processAutoSell() {
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            if (this.inChest) {
                this.inChest = false;
                this.timer = 20;
            } else {
                if (this.mc.player.networkHandler != null) {
                    this.mc.player.networkHandler.sendChatCommand("sell all");
                }
                this.onSaleCompleted();
                this.timer = 20;
            }
        } else {
            this.inChest = true;
            if (!sellChestItems(handler)) {
                boolean emptyInventory = true;
                for (int i = 0; i <= 44 && i < handler.slots.size(); i++) {
                    if (handler.getSlot(i).getStack().isEmpty()) {
                        emptyInventory = false;
                        break;
                    }
                }
                if (!emptyInventory) {
                    this.mc.interactionManager.clickSlot(handler.syncId, 53, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                } else {
                    for (Slot slot : handler.slots) {
                        if (slot.inventory == this.mc.player.getInventory() && !slot.getStack().isEmpty() && isItemTargeted(slot.getStack().getItem())) {
                            this.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, this.mc.player);
                        }
                    }
                }
            }
            this.timer = this.delay.get();
        }
    }

    private boolean sellChestItems(ScreenHandler handler) {
        boolean clicked = false;
        for (int i = 45; i <= 52 && i < handler.slots.size(); i++) {
            if (!handler.getSlot(i).getStack().isEmpty()) {
                this.mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, this.mc.player);
                clicked = true;
            }
        }
        return clicked;
    }

    private boolean isItemTargeted(Item item) {
        List<Item> targetList = this.items.get();
        if (this.mode.get() == SellMode.Whitelist) {
            return targetList.contains(item);
        } else {
            return !targetList.contains(item);
        }
    }

    public void resetStats() {
        this.startTime = 0L;
        this.lastWebhookTime = 0L;
        this.lastMessageTime = 0L;
        this.totalSales = 0;
        this.totalEarned = 0.0;
        this.lastMessageId = null;
    }

    private void onSaleCompleted() {
        if (this.statsEnabled.get()) {
            ++this.totalSales;
            if (!this.useChatParsing.get()) {
                this.totalEarned += this.valuePerSale.get();
            }
            this.triggerWebhookUpdate();
        }
    }

    private void triggerWebhookUpdate() {
        if (this.webhookEnabled.get() && isWebhookValid()) {
            if (this.lastMessageId != null) {
                long now = System.currentTimeMillis();
                if (now - this.lastMessageTime >= 5000L) {
                    this.lastMessageTime = now;
                    sendWebhook("Running");
                }
            }
        }
    }

    private boolean isWebhookValid() {
        return this.webhookUrl.get() != null && !this.webhookUrl.get().isBlank();
    }

    private String buildWebhookPayload(String status) {
        String playerName = this.mc.player != null ? this.mc.player.getName().getString() : "Unknown";
        String serverIp = this.mc.getCurrentServerEntry() != null ? this.mc.getCurrentServerEntry().address : "Singleplayer";
        String title = "AutoSell Report";
        int color = this.isActive() ? 1752220 : 15158332;

        long durationSec = (System.currentTimeMillis() - this.startTime) / 1000L;
        long h = durationSec / 3600L;
        long m = (durationSec % 3600L) / 60L;
        long s = durationSec % 60L;

        StringBuilder sb = new StringBuilder();
        sb.append("{\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"color\":").append(color).append(",");
        sb.append("\"fields\":[");
        sb.append("{\"name\":\"Status\",\"value\":\"").append(escapeJson(status)).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Player\",\"value\":\"").append(escapeJson(playerName)).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Server\",\"value\":\"").append(escapeJson(serverIp)).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Duration\",\"value\":\"").append(String.format(Locale.US, "%dh %dm %ds", h, m, s)).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Earned\",\"value\":\"").append(formatMoney(this.totalEarned)).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Sales\",\"value\":\"").append(this.totalSales).append("\",\"inline\":true}");
        sb.append("],\"footer\":{\"text\":\"AutoSell Utility\"}}");

        StringBuilder json = new StringBuilder("{");
        if (this.webhookName.get() != null && !this.webhookName.get().isBlank()) {
            json.append("\"username\":\"").append(escapeJson(this.webhookName.get())).append("\",");
        }
        if (this.webhookAvatar.get() != null && !this.webhookAvatar.get().isBlank()) {
            json.append("\"avatar_url\":\"").append(escapeJson(this.webhookAvatar.get())).append("\",");
        }
        json.append("\"embeds\":[").append(sb).append("]}");
        return json.toString();
    }

    private void sendWebhook(String status) {
        if (this.webhookEnabled.get() && isWebhookValid()) {
            String payload = buildWebhookPayload(status);
            String url = this.webhookUrl.get().trim();
            boolean isPatch = this.lastMessageId != null;

            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest.Builder req = HttpRequest.newBuilder()
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(8L));

                    if (isPatch) {
                        req.uri(URI.create(url + "/messages/" + this.lastMessageId))
                           .method("PATCH", BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
                    } else {
                        req.uri(URI.create(url + "?wait=true"))
                           .POST(BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
                    }

                    HttpResponse<String> res = client.send(req.build(), BodyHandlers.ofString());
                    if (res.statusCode() < 400) {
                        if (!isPatch && res.body() != null && res.body().contains("\"id\"")) {
                            int idx = res.body().indexOf("\"id\"");
                            idx = res.body().indexOf(':', idx);
                            idx = res.body().indexOf('"', idx);
                            int endIdx = res.body().indexOf('"', idx + 1);
                            if (idx > 0 && endIdx > idx) {
                                this.lastMessageId = res.body().substring(idx + 1, endIdx);
                                this.lastWebhookTime = System.currentTimeMillis();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }, "AutoSell-Webhook").start();
        }
    }

    private void sendFinalWebhook(String status) {
        if (this.webhookEnabled.get() && isWebhookValid()) {
            String payload = buildWebhookPayload(status);
            String url = this.webhookUrl.get().trim();
            boolean isPatch = this.lastMessageId != null;

            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.Builder req = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5L));

                if (isPatch) {
                    req.uri(URI.create(url + "/messages/" + this.lastMessageId))
                       .method("PATCH", BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
                } else {
                    req.uri(URI.create(url + "?wait=true"))
                       .POST(BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
                }
                client.send(req.build(), BodyHandlers.ofString());
            } catch (Exception ignored) {}
        }
    }

    private String escapeJson(String str) {
        return str == null ? "" : str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String formatMoney(double val) {
        double abs = Math.abs(val);
        String prefix = val < 0 ? "-$" : "$";
        if (abs >= 1.0E9) return String.format(Locale.US, "%s%.2fB", prefix, abs / 1.0E9);
        if (abs >= 1000000.0) return String.format(Locale.US, "%s%.2fM", prefix, abs / 1000000.0);
        if (abs >= 1000.0) return String.format(Locale.US, "%s%.2fK", prefix, abs / 1000.0);
        return String.format(Locale.US, "%s%.2f", prefix, abs);
    }

    public enum SellMode {
        Whitelist,
        Blacklist
    }
}

