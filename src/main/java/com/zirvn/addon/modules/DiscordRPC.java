package com.zirvn.addon.modules;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

public class DiscordRPC {
    private static DiscordRPC instance;
    private final RichPresence presence = new RichPresence();
    private boolean active;
    private int reconnectTicks;

    public static void init() {
        if (instance == null) {
            instance = new DiscordRPC();
            MeteorClient.EVENT_BUS.subscribe(instance);
            instance.startRpc();
        }
    }

    private void startRpc() {
        System.out.println("[DiscordRPC] Initializing...");

        try {
            if (!DiscordIPC.isConnected()) {
                DiscordIPC.start(1507719147457609868L, null);
            }
            System.out.println("[DiscordRPC] Connected: " + DiscordIPC.isConnected());
        } catch (Exception e) {
            System.out.println("[DiscordRPC] Start error: " + e.getMessage());
        }

        this.presence.setLargeImage("meteor_client", "ZirAddon v1.3.6");
        this.presence.setStart(System.currentTimeMillis() / 1000L);
        this.updatePresence();
        this.active = true;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (this.active) {
            this.updatePresence();
        }
    }

    private void updatePresence() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!DiscordIPC.isConnected()) {
                ++this.reconnectTicks;
                if (this.reconnectTicks < 100) {
                    return;
                }

                try {
                    DiscordIPC.start(1507719147457609868L, null);
                    this.reconnectTicks = 0;
                } catch (Exception e) {
                    return;
                }
            }

            this.reconnectTicks = 0;
            this.presence.setDetails("ZirAddon");
            String serverIp;
            if (mc.getCurrentServerEntry() != null) {
                serverIp = mc.getCurrentServerEntry().address;
                if (serverIp.contains(":")) {
                    serverIp = serverIp.split(":")[0];
                }
            } else {
                serverIp = "Singleplayer";
            }

            String playerName = mc.player != null ? mc.player.getName().getString() : "Not in game";
            this.presence.setState("Playing on " + serverIp + " | " + playerName);
            DiscordIPC.setActivity(this.presence);
        } catch (Exception e) {
            System.out.println("[DiscordRPC] Update error: " + e.getMessage());
        }
    }
}

