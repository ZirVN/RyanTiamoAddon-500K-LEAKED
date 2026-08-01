package com.zirvn.addon.modules;

import com.zirvn.addon.ZirAddon;
import com.zirvn.addon.config.ModConfig;
import com.zirvn.addon.mixins.PlayerInventoryAccessor;
import com.zirvn.addon.util.RotationSpoofer;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AutoPlace extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<Integer> placeDelayMs;
    private final Setting<Boolean> autoPick;
    private final Setting<Boolean> limitToSchematic;
    private final Setting<Boolean> preventGui;
    private final Setting<Boolean> adjustRedstone;
    private final Setting<Boolean> pauseMovement;
    private final Setting<Boolean> lockLayer;
    private final Setting<Boolean> lockToSchematic;

    private final SettingGroup sgRotationFilter;
    private final Setting<Boolean> rotationFilterEnabled;
    private final Setting<List<Item>> rotationItems;

    private final SettingGroup sgAirPlace;
    private final Setting<Boolean> airPlace;
    private final Setting<List<Item>> supportBlocks;

    private final SettingGroup sgSafety;
    private final Setting<Boolean> safetyMode;
    private final Setting<Integer> delayVariance;

    private final Random random;

    public AutoPlace() {
        super(ZirAddon.CATEGORY, "auto-place", "Auto places blocks from Litematica schematic with correct rotation, item swap, and air place support");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.placeDelayMs = this.sgGeneral.add(new IntSetting.Builder().name("place-delay-ms").description("Delay between placements in ms").defaultValue(0).min(0).max(500).sliderRange(0, 300).build());

        this.autoPick = this.sgGeneral.add(new BoolSetting.Builder().name("auto-pick").description("Auto pick items from inventory").defaultValue(true).build());
        this.limitToSchematic = this.sgGeneral.add(new BoolSetting.Builder().name("limit-to-schematic").description("Only place blocks matching the schematic").defaultValue(true).build());
        this.preventGui = this.sgGeneral.add(new BoolSetting.Builder().name("prevent-gui").description("Auto sneak to prevent opening containers").defaultValue(true).build());
        this.adjustRedstone = this.sgGeneral.add(new BoolSetting.Builder().name("adjust-redstone").description("Auto adjust repeater delays and comparator modes").defaultValue(true).build());
        this.pauseMovement = this.sgGeneral.add(new BoolSetting.Builder().name("pause-movement").description("Pause movement during place delay").defaultValue(true).build());
        this.lockLayer = this.sgGeneral.add(new BoolSetting.Builder().name("lock-layer").description("Only place blocks within the current Litematica layer range").defaultValue(false).build());
        this.lockToSchematic = this.sgGeneral.add(new BoolSetting.Builder().name("lock-to-schematic").description("Block placement at positions where schematic has no block (air).").defaultValue(false).build());

        this.sgRotationFilter = this.settings.createGroup("Rotation Filter");
        this.rotationFilterEnabled = this.sgRotationFilter.add(new BoolSetting.Builder().name("filter-enabled").description("Only apply rotation to selected blocks").defaultValue(false).build());
        this.rotationItems = this.sgRotationFilter.add(new ItemListSetting.Builder().name("rotation-items").description("Blocks to apply rotation spoofing to").defaultValue(List.of()).build());

        this.sgAirPlace = this.settings.createGroup("Air Place");
        this.airPlace = this.sgAirPlace.add(new BoolSetting.Builder().name("air-place").description("Auto place support blocks when target has no solid block to place against").defaultValue(false).build());
        this.supportBlocks = this.sgAirPlace.add(new ItemListSetting.Builder().name("support-blocks").description("Blocks to use as temporary support for air placement").defaultValue(List.of()).build());

        this.sgSafety = this.settings.createGroup("Safety");
        this.safetyMode = this.sgSafety.add(new BoolSetting.Builder().name("safety-mode").description("Enable anti-suspicious features (random delays, human-like rotation)").defaultValue(true).build());
        this.delayVariance = this.sgSafety.add(new IntSetting.Builder().name("delay-variance").description("Random delay variance in ms to avoid pattern detection").defaultValue(30).min(0).max(200).build());

        this.random = new Random();
    }

    @Override
    public void onActivate() {
        ModConfig.load();
        ModConfig config = ModConfig.get();
        config.modEnabled = true;
        this.syncConfig();
        ModConfig.save();
    }

    @Override
    public void onDeactivate() {
        ModConfig config = ModConfig.get();
        config.modEnabled = false;
        ModConfig.save();
    }

    private void syncConfig() {
        ModConfig config = ModConfig.get();
        config.placeDelayMs = this.placeDelayMs.get();
        config.autoPickFromInventory = this.autoPick.get();
        config.limitToSchematic = this.limitToSchematic.get();
        config.preventContainerInteraction = this.preventGui.get();
        config.pauseMovementDuringDelay = this.pauseMovement.get();
    }

    public boolean isAutoPick() { return this.autoPick.get(); }
    public boolean isLimitToSchematic() { return this.limitToSchematic.get(); }
    public boolean isPreventGui() { return this.preventGui.get(); }
    public boolean isAdjustRedstone() { return this.adjustRedstone.get(); }
    public boolean isPauseMovement() { return this.pauseMovement.get(); }
    public boolean isLockLayer() { return this.lockLayer.get(); }
    public boolean isLockToSchematic() { return this.lockToSchematic.get(); }
    public int getPlaceDelayMs() { return this.placeDelayMs.get(); }

    public boolean shouldRotate(Item item) {
        return !this.rotationFilterEnabled.get() || this.rotationItems.get().contains(item);
    }

    public boolean isSafetyMode() {
        return this.safetyMode.get();
    }

    public int getDelayVariance() {
        return this.delayVariance.get();
    }

    public int getRandomDelayOffset() {
        int v = this.delayVariance.get();
        return v > 0 ? this.random.nextInt(v + 1) - v / 2 : 0;
    }

    public boolean isAirPlaceEnabled() {
        return this.airPlace.get();
    }

    public List<Item> getSupportBlocks() {
        return this.supportBlocks.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.mc.player == null || this.mc.world == null) return;

        this.syncConfig();

        // 1. Process pending block adjustments (Repeater delay / Comparator mode)
        if (this.isAdjustRedstone() && !RotationSpoofer.pendingAdjustments.isEmpty()) {
            long now = System.currentTimeMillis();
            Iterator<RotationSpoofer.PendingAdjustment> iterator = RotationSpoofer.pendingAdjustments.iterator();
            boolean clickSentThisTick = false;
            while (iterator.hasNext()) {
                RotationSpoofer.PendingAdjustment adj = iterator.next();
                if (now - adj.timeCreated > 2000) {
                    iterator.remove();
                    continue;
                }
                
                World schematicWorld = SchematicWorldHandler.getSchematicWorld();
                if (schematicWorld == null) {
                    iterator.remove();
                    continue;
                }

                if (adj.clicksLeft == -1 && now - adj.timeCreated > 50) {
                    BlockState worldState = this.mc.world.getBlockState(adj.pos);
                    BlockState schemState = schematicWorld.getBlockState(adj.pos);

                    if (worldState.getBlock() instanceof RepeaterBlock && schemState.getBlock() instanceof RepeaterBlock) {
                        int worldDelay = worldState.get(Properties.DELAY);
                        int schemDelay = schemState.get(Properties.DELAY);
                        adj.clicksLeft = (schemDelay - worldDelay + 4) % 4;
                    } else if (worldState.getBlock() instanceof ComparatorBlock && schemState.getBlock() instanceof ComparatorBlock) {
                        ComparatorMode worldMode = worldState.get(Properties.COMPARATOR_MODE);
                        ComparatorMode schemMode = schemState.get(Properties.COMPARATOR_MODE);
                        adj.clicksLeft = (worldMode != schemMode) ? 1 : 0;
                    } else if (!worldState.isAir()) {
                        iterator.remove();
                        continue;
                    }
                }

                if (adj.clicksLeft > 0) {
                    if (!clickSentThisTick && this.mc.interactionManager != null) {
                        BlockHitResult adjustHitResult = new BlockHitResult(
                            new Vec3d(adj.pos.getX() + 0.5, adj.pos.getY() + 0.5, adj.pos.getZ() + 0.5),
                            Direction.UP,
                            adj.pos,
                            false
                        );
                        this.mc.interactionManager.interactBlock(this.mc.player, adj.hand, adjustHitResult);
                        adj.clicksLeft--;
                        clickSentThisTick = true;
                    }
                    if (adj.clicksLeft == 0) {
                        iterator.remove();
                    }
                } else if (adj.clicksLeft == 0) {
                    iterator.remove();
                }
            }
        }

        // 2. Execute queued placement with spoofed rotation
        if (RotationSpoofer.isQueued) {
            RotationSpoofer.pendingTicks++;
            if (RotationSpoofer.spoofSent || RotationSpoofer.pendingTicks >= 2) {
                RotationSpoofer.spoofSent = false;
                RotationSpoofer.pendingTicks = 0;

                float oldYaw = this.mc.player.getYaw();
                float oldPitch = this.mc.player.getPitch();
                this.mc.player.setYaw(RotationSpoofer.targetYaw);
                this.mc.player.setPitch(RotationSpoofer.targetPitch);
                
                RotationSpoofer.isPlacingLater = true;
                if (RotationSpoofer.savedHand != null && RotationSpoofer.savedHitResult != null && this.mc.interactionManager != null) {
                    boolean shouldSneak = this.isPreventGui() && !this.mc.player.isSneaking();
                    PlayerInput normalInput = null;
                    
                    if (shouldSneak && this.mc.getNetworkHandler() != null) {
                        normalInput = this.mc.player.input.playerInput;
                        PlayerInput sneakInput = new PlayerInput(
                            normalInput.forward(),
                            normalInput.backward(),
                            normalInput.left(),
                            normalInput.right(),
                            normalInput.jump(),
                            true,
                            normalInput.sprint()
                        );
                        this.mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(sneakInput));
                    }
                    
                    this.mc.interactionManager.interactBlock(this.mc.player, RotationSpoofer.savedHand, RotationSpoofer.savedHitResult);
                    RotationSpoofer.lastPlaceTime = System.currentTimeMillis();
                    
                    if (shouldSneak && normalInput != null && this.mc.getNetworkHandler() != null) {
                        this.mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(normalInput));
                    }
                    
                    try {
                        if (RotationSpoofer.savedPlacedPos != null && this.isAdjustRedstone()) {
                            RotationSpoofer.pendingAdjustments.add(new RotationSpoofer.PendingAdjustment(
                                    RotationSpoofer.savedPlacedPos,
                                    RotationSpoofer.savedHand,
                                    RotationSpoofer.savedHitResult
                            ));
                        }
                    } catch (Exception ignored) {}
                }
                
                RotationSpoofer.isPlacingLater = false;
                this.mc.player.setYaw(oldYaw);
                this.mc.player.setPitch(oldPitch);
                
                RotationSpoofer.isQueued = false;
                RotationSpoofer.isSpoofing = false;
            }
        }
    }
}
