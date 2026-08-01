package com.zirvn.addon.modules;

import com.zirvn.addon.ZirAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class GuiClose extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<Boolean> inventoryOnly;
    private final Setting<Integer> delay;
    private int timer;

    public GuiClose() {
        super(ZirAddon.CATEGORY, "gui-close", "Auto closes any open GUI screens.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.inventoryOnly = this.sgGeneral.add(new BoolSetting.Builder()
            .name("inventory-only")
            .description("Only close inventory-type GUIs, keep chat/command open.")
            .defaultValue(false)
            .build());
        this.delay = this.sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay in ticks before closing GUI.")
            .defaultValue(5)
            .min(1)
            .sliderMax(40)
            .build());
        this.timer = 0;
    }

    @Override
    public void onActivate() {
        this.timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.mc.player != null) {
            if (this.timer > 0) {
                --this.timer;
            } else {
                this.closeGui();
                this.timer = this.delay.get();
            }
        }
    }

    private void closeGui() {
        if (this.mc.currentScreen != null) {
            String name = this.mc.currentScreen.getClass().getName().toLowerCase();
            if (name.contains("meteor") || name.contains("clickgui") || name.contains("gui$theme")) {
                return;
            }
            if (this.inventoryOnly.get() && !name.contains("handled") && !name.contains("container") && !name.contains("inventory") && !name.contains("generic")) {
                return;
            }
            if (this.mc.player != null) {
                this.mc.player.closeHandledScreen();
            }
        }
    }
}

