package com.zirvn.addon;

import com.zirvn.addon.config.ModConfig;
import com.zirvn.addon.modules.AutoDirection;
import com.zirvn.addon.modules.AutoPlace;
import com.zirvn.addon.modules.AutoSell;
import com.zirvn.addon.modules.CrackerModule;
import com.zirvn.addon.modules.CrafterSetup;
import com.zirvn.addon.modules.DiscordRPC;
import com.zirvn.addon.modules.FakeSneak;
import com.zirvn.addon.modules.GuiClose;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;

public class ZirAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("ZirVN", Items.NETHERITE_SWORD.getDefaultStack());
    private static boolean initialized = false;

    public static void registerModules() {
        if (!initialized) {
            initialized = true;
            System.out.println("[ZirAddon] Registering ZirVN modules...");
            Modules.get().add(new AutoDirection());
            Modules.get().add(new AutoPlace());
            Modules.get().add(new AutoSell());
            Modules.get().add(new CrafterSetup());
            Modules.get().add(new CrackerModule());
            Modules.get().add(new FakeSneak());
            Modules.get().add(new GuiClose());
            DiscordRPC.init();
        }
    }

    @Override
    public void onInitialize() {
        System.out.println("[ZirAddon] Initializing ZirVN Addon...");
        ModConfig.load();
        registerModules();
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.zirvn.addon";
    }
}
