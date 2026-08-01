package com.zirvn.addon.config;

import net.minecraft.client.gui.screen.Screen;

public class ModMenuIntegration {
    public static Screen createConfigScreen(Screen parent) {
        return new ModConfigScreen(parent);
    }
}

