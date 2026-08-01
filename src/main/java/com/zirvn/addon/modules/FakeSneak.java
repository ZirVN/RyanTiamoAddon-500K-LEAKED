package com.zirvn.addon.modules;

import com.zirvn.addon.ZirAddon;
import java.util.List;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class FakeSneak extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<List<Block>> blocks;
    private static FakeSneak instance;

    public FakeSneak() {
        super(ZirAddon.CATEGORY, "fake-sneak", "Auto sneaks when interacting with specified blocks so you can place blocks on them without opening their GUI");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.blocks = this.sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to auto-sneak when interacting with")
            .defaultValue(List.of(
                Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
                Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.BARREL, Blocks.HOPPER,
                Blocks.DISPENSER, Blocks.DROPPER, Blocks.BREWING_STAND, Blocks.BEACON, Blocks.ENCHANTING_TABLE, Blocks.LOOM,
                Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE, Blocks.STONECUTTER, Blocks.SMITHING_TABLE, Blocks.SHULKER_BOX,
                Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX,
                Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX,
                Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX,
                Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX
            ))
            .build());
        instance = this;
    }

    public static boolean shouldSneak(Block block) {
        return instance != null && instance.isActive() && instance.blocks.get().contains(block);
    }

}

