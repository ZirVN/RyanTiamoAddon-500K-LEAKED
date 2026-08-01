package com.zirvn.addon.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private BlockItemListWidget list;
    private ButtonWidget modEnabledBtn;
    private ButtonWidget autoSneakBtn;
    private ButtonWidget delayBtn;
    private ButtonWidget forcedDirBtn;
    private ButtonWidget limitToSchematicBtn;
    private ButtonWidget preventInteractionBtn;
    private ButtonWidget globalToggleBtn;

    private boolean localModEnabled;
    private boolean localAutoSneak;
    private int localPlaceDelayMs;
    private boolean localAllowAllBlockItems;
    private boolean localLimitToSchematic;
    private boolean localPreventInteraction;
    private ModConfig.RotationState localForcedRotationState;
    private final Set<Item> localEnabledItems = new HashSet<>();
    private final List<Item> allBlockItems = new ArrayList<>();

    public ModConfigScreen(Screen parent) {
        super(Text.literal("ZirAddon Config"));
        this.parent = parent;
        ModConfig config = ModConfig.get();
        this.localModEnabled = config.modEnabled;
        this.localAutoSneak = config.autoSneak;
        this.localPlaceDelayMs = config.placeDelayMs;
        this.localAllowAllBlockItems = !config.autoPickFromInventory;
        this.localLimitToSchematic = config.limitToSchematic;
        this.localPreventInteraction = config.preventContainerInteraction;
        this.localForcedRotationState = config.forcedRotationState;

        for (String idStr : config.targetItems) {
            try {
                Item item = Registries.ITEM.get(Identifier.of(idStr));
                if (item != null && item != Items.AIR) {
                    this.localEnabledItems.add(item);
                }
            } catch (Exception ignored) {}
        }

        for (Item item : Registries.ITEM) {
            if (item instanceof BlockItem) {
                this.allBlockItems.add(item);
            }
        }
        this.allBlockItems.sort(Comparator.comparing(item -> item.getName().getString()));
    }

    @Override
    protected void init() {
        int width = Math.min(300, this.width);
        int x = (this.width - width) / 2;

        this.modEnabledBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("AutoPlace: " + (this.localModEnabled ? "§aON" : "§cOFF")),
            btn -> {
                this.localModEnabled = !this.localModEnabled;
                btn.setMessage(Text.literal("AutoPlace: " + (this.localModEnabled ? "§aON" : "§cOFF")));
            }
        ).dimensions(x - 20, 10, 95, 20).build());

        this.autoSneakBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Sneak: " + (this.localAutoSneak ? "§aON" : "§cOFF")),
            btn -> {
                this.localAutoSneak = !this.localAutoSneak;
                btn.setMessage(Text.literal("Sneak: " + (this.localAutoSneak ? "§aON" : "§cOFF")));
            }
        ).dimensions(x + 80, 10, 75, 20).build());

        this.delayBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Delay: §b" + this.localPlaceDelayMs + "ms"),
            btn -> {
                this.localPlaceDelayMs = this.localPlaceDelayMs + 50 > 300 ? 0 : this.localPlaceDelayMs + 50;
                btn.setMessage(Text.literal("Delay: §b" + this.localPlaceDelayMs + "ms"));
            }
        ).dimensions(x + 160, 10, 85, 20).build());

        this.forcedDirBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Dir: §b" + this.localForcedRotationState.name()),
            btn -> {
                int nextIdx = (this.localForcedRotationState.ordinal() + 1) % ModConfig.RotationState.values().length;
                this.localForcedRotationState = ModConfig.RotationState.values()[nextIdx];
                btn.setMessage(Text.literal("Dir: §b" + this.localForcedRotationState.name()));
            }
        ).dimensions(x + 330, 10, 90, 20).build());

        this.searchField = new TextFieldWidget(this.textRenderer, x + 20, 36, 140, 20, Text.literal("Search..."));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        this.globalToggleBtn = this.addDrawableChild(ButtonWidget.builder(
            getGlobalToggleText(),
            btn -> {
                this.localAllowAllBlockItems = !this.localAllowAllBlockItems;
                btn.setMessage(getGlobalToggleText());
            }
        ).dimensions(x + 175, 36, 105, 20).build());

        this.limitToSchematicBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Lock blocks: " + (this.localLimitToSchematic ? "§aON" : "§cOFF")),
            btn -> {
                this.localLimitToSchematic = !this.localLimitToSchematic;
                btn.setMessage(Text.literal("Lock blocks: " + (this.localLimitToSchematic ? "§aON" : "§cOFF")));
            }
        ).dimensions(x + 285, 36, 140, 20).build());

        this.preventInteractionBtn = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Block GUI: " + (this.localPreventInteraction ? "§aON" : "§cOFF")),
            btn -> {
                this.localPreventInteraction = !this.localPreventInteraction;
                btn.setMessage(Text.literal("Block GUI: " + (this.localPreventInteraction ? "§aON" : "§cOFF")));
            }
        ).dimensions(x + 50, 62, 200, 20).build());

        this.list = new BlockItemListWidget(this.client, width, this.height - 122, 88, 24);
        this.addDrawableChild(this.list);
        this.onSearchChanged(this.searchField.getText());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), btn -> saveAndClose()).dimensions(x + 40, this.height - 28, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> close()).dimensions(x + 160, this.height - 28, 100, 20).build());
    }

    private Text getGlobalToggleText() {
        return Text.literal("All: " + (this.localAllowAllBlockItems ? "§aON" : "§cOFF"));
    }

    private void onSearchChanged(String text) {
        if (this.list != null) {
            this.list.refresh(text);
        }
    }

    private void saveAndClose() {
        ModConfig config = ModConfig.get();
        config.modEnabled = this.localModEnabled;
        config.autoSneak = this.localAutoSneak;
        config.placeDelayMs = this.localPlaceDelayMs;
        config.limitToSchematic = this.localLimitToSchematic;
        config.preventContainerInteraction = this.localPreventInteraction;
        config.forcedRotationState = this.localForcedRotationState;

        List<String> list = new ArrayList<>();
        for (Item item : this.localEnabledItems) {
            Identifier id = Registries.ITEM.getId(item);
            if (id != null) {
                list.add(id.toString());
            }
        }
        config.targetItems = list;
        ModConfig.save();
        close();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 4, 0xFFFFFF);
    }

    class BlockItemListWidget extends ElementListWidget<BlockItemListWidget.Entry> {
        public BlockItemListWidget(MinecraftClient client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }

        public void refresh(String filter) {
            this.clearEntries();
            String query = filter.toLowerCase(Locale.ROOT);
            for (Item item : ModConfigScreen.this.allBlockItems) {
                String name = item.getName().getString();
                String idStr = Registries.ITEM.getId(item).toString();
                if (name.toLowerCase(Locale.ROOT).contains(query) || idStr.contains(query)) {
                    this.addEntry(new Entry(item));
                }
            }
        }

        class Entry extends ElementListWidget.Entry<Entry> {
            private final Item item;
            private final ButtonWidget toggleButton;

            public Entry(Item item) {
                this.item = item;
                this.toggleButton = ButtonWidget.builder(getToggleText(), btn -> {
                    if (ModConfigScreen.this.localEnabledItems.contains(item)) {
                        ModConfigScreen.this.localEnabledItems.remove(item);
                    } else {
                        ModConfigScreen.this.localEnabledItems.add(item);
                    }
                    btn.setMessage(getToggleText());
                }).dimensions(0, 0, 50, 20).build();
            }

            private Text getToggleText() {
                return Text.literal(ModConfigScreen.this.localEnabledItems.contains(this.item) ? "§aON" : "§cOFF");
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawItem(new ItemStack(this.item), 4, 2);
                String name = this.item.getName().getString();
                if (name.length() > 22) {
                    name = name.substring(0, 20) + "...";
                }
                context.drawTextWithShadow(ModConfigScreen.this.textRenderer, name, 26, 6, 0xFFFFFF);
                this.toggleButton.render(context, mouseX, mouseY, tickDelta);
            }



            @Override
            public List<? extends Element> children() {
                return Collections.singletonList(this.toggleButton);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.singletonList(this.toggleButton);
            }
        }
    }
}

