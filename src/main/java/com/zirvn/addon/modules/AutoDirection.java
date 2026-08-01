package com.zirvn.addon.modules;

import com.zirvn.addon.ZirAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

import java.util.List;

public class AutoDirection extends Module {
    private final SettingGroup sgGeneral;
    public final Setting<Direction> direction;
    public final Setting<List<Item>> blocks;

    private static AutoDirection instance;
    private static boolean activeState = false;
    private static Hand pendingHand = null;
    private static BlockHitResult pendingHitResult = null;
    private static int stepState = 0;
    public static volatile int ticksToOverride = 0;
    public static float overrideYaw = 0.0F;
    public static float overridePitch = 0.0F;

    public AutoDirection() {
        super(ZirAddon.CATEGORY, "auto-direction", "Auto sets block direction for selected blocks.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.direction = this.sgGeneral.add(new EnumSetting.Builder<Direction>()
            .name("direction")
            .description("Block facing direction.")
            .defaultValue(Direction.NORTH)
            .build());
        this.blocks = this.sgGeneral.add(new ItemListSetting.Builder()
            .name("blocks")
            .description("Blocks to apply direction to.")
            .defaultValue(Items.PISTON, Items.STICKY_PISTON, Items.DISPENSER, Items.DROPPER, Items.HOPPER, Items.OBSERVER)
            .build());
        instance = this;
    }

    public static boolean applyDirection(Item item, Hand hand, BlockHitResult hitResult) {
        if (instance != null && instance.isActive()) {
            if (!(item instanceof BlockItem)) {
                return false;
            } else if (!instance.blocks.get().contains(item)) {
                return false;
            } else if (activeState) {
                return false;
            } else {
                activeState = true;
                pendingHand = hand;
                pendingHitResult = hitResult;
                stepState = 0;
                return true;
            }
        } else {
            return false;
        }
    }


    static float getYawForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> Float.NaN;
        };
    }

    static float getPitchForDirection(Direction dir) {
        return switch (dir) {
            case UP -> 90.0F;
            case DOWN -> -90.0F;
            default -> 0.0F;
        };
    }

    @Override
    public void onActivate() {
        activeState = false;
    }

    @Override
    public void onDeactivate() {
        activeState = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (activeState && this.mc.player != null) {
            if (stepState == 0) {
                Direction dir = this.direction.get();
                overrideYaw = getYawForDirection(dir);
                overridePitch = getPitchForDirection(dir);
                ticksToOverride = 2;
                stepState = 1;
            } else if (stepState == 1) {
                if (pendingHand != null && pendingHitResult != null && this.mc.interactionManager != null) {
                    this.mc.interactionManager.interactBlock(this.mc.player, pendingHand, pendingHitResult);
                }
                activeState = false;
                stepState = 0;
            }
        }
    }

    public enum Direction {
        UP,
        DOWN,
        NORTH,
        SOUTH,
        EAST,
        WEST
    }
}
