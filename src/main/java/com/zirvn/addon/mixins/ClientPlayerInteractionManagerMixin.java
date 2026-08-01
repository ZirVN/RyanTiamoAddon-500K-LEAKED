package com.zirvn.addon.mixins;

import com.zirvn.addon.modules.AutoDirection;
import com.zirvn.addon.modules.AutoPlace;
import com.zirvn.addon.util.RotationCalculator;
import com.zirvn.addon.util.RotationSpoofer;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.LayerRange;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    private boolean spoofedSneak = false;

    private boolean shouldSneakPlaceAgainst(ClientPlayerEntity player, BlockHitResult hitResult) {
        BlockState state = player.getEntityWorld().getBlockState(hitResult.getBlockPos());
        Block block = state.getBlock();
        return block instanceof BlockWithEntity
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof GrindstoneBlock
                || block instanceof StonecutterBlock
                || block instanceof SmithingTableBlock
                || block instanceof LecternBlock
                || block instanceof EnchantingTableBlock
                || block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof FenceGateBlock
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof NoteBlock
                || block instanceof JukeboxBlock
                || block instanceof RepeaterBlock
                || block instanceof ComparatorBlock
                || block instanceof BedBlock
                || block instanceof BellBlock
                || block instanceof ComposterBlock
                || block instanceof ChiseledBookshelfBlock
                || block instanceof FlowerPotBlock
                || block instanceof CakeBlock;
    }

    private static long lastInvSwapTime = 0;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlockHead(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (player == null) {
            return;
        }

        AutoPlace autoPlace = Modules.get().get(AutoPlace.class);
        AutoDirection autoDir = Modules.get().get(AutoDirection.class);

        boolean isAutoPlaceActive = autoPlace != null && autoPlace.isActive();
        boolean isAutoDirActive = autoDir != null && autoDir.isActive();

        if (!isAutoPlaceActive && !isAutoDirActive) {
            return;
        }

        if (isAutoPlaceActive && autoPlace.isAdjustRedstone() && !RotationSpoofer.isPlacingLater) {
            Block clickedBlock = MinecraftClient.getInstance().world.getBlockState(hitResult.getBlockPos()).getBlock();
            if (clickedBlock instanceof RepeaterBlock || clickedBlock instanceof ComparatorBlock) {
                cir.setReturnValue(ActionResult.FAIL);
                return;
            }
        }

        boolean preventGui = isAutoPlaceActive && autoPlace.isPreventGui();
        if (preventGui && !player.isSneaking() && shouldSneakPlaceAgainst(player, hitResult)) {
            this.spoofedSneak = true;
            player.networkHandler.sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, true, false)));
        }

        if (RotationSpoofer.isPlacingLater) {
            return;
        }

        if (AutoDirection.applyDirection(player.getStackInHand(hand).getItem(), hand, hitResult)) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        if (!isAutoPlaceActive) {
            return;
        }

        if (RotationSpoofer.isQueued) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        if (System.currentTimeMillis() - RotationSpoofer.lastPlaceTime < autoPlace.getPlaceDelayMs()) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        World schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            return;
        }

        ItemStack currentStack = player.getStackInHand(hand);

        BlockPos targetPos;
        if (currentStack.getItem() instanceof BucketItem || currentStack.getItem() == Items.POWDER_SNOW_BUCKET) {
            BlockState clickedState = MinecraftClient.getInstance().world.getBlockState(hitResult.getBlockPos());
            if (clickedState.isReplaceable()) {
                targetPos = hitResult.getBlockPos();
            } else {
                targetPos = hitResult.getBlockPos().offset(hitResult.getSide());
            }
        } else {
            ItemPlacementContext placementCtx = new ItemPlacementContext(player, hand, currentStack, hitResult);
            targetPos = placementCtx.getBlockPos();
        }

        if (autoPlace.isLockLayer()) {
            LayerRange range = DataManager.getRenderLayerRange();
            if (range != null && !range.isPositionWithinRange(targetPos)) {
                cir.setReturnValue(ActionResult.FAIL);
                return;
            }
        }

        BlockState stateSchematic = schematicWorld.getBlockState(targetPos);

        if (autoPlace.isLimitToSchematic() || autoPlace.isLockToSchematic()) {
            if (stateSchematic == null || stateSchematic.isAir()) {
                if (currentStack.getItem() instanceof BlockItem || currentStack.getItem() instanceof BucketItem) {
                    cir.setReturnValue(ActionResult.FAIL);
                    return;
                }
                return;
            }
        }

        if (stateSchematic == null || stateSchematic.isAir()) {
            return;
        }

        Item requiredItem = stateSchematic.getBlock().asItem();
        if (stateSchematic.getBlock() == Blocks.WATER) {
            requiredItem = Items.WATER_BUCKET;
        } else if (stateSchematic.getBlock() == Blocks.LAVA) {
            requiredItem = Items.LAVA_BUCKET;
        } else if (stateSchematic.getBlock() == Blocks.POWDER_SNOW) {
            requiredItem = Items.POWDER_SNOW_BUCKET;
        }

        if (autoPlace.isAutoPick() && currentStack.getItem() != requiredItem && hand == Hand.MAIN_HAND) {
            int foundSlot = -1;
            for (int i = 0; i < 36; i++) {
                if (player.getInventory().getStack(i).getItem() == requiredItem) {
                    foundSlot = i;
                    break;
                }
            }

            if (foundSlot != -1) {
                if (foundSlot < 9) {
                    ((PlayerInventoryAccessor) player.getInventory()).setSelectedSlot(foundSlot);
                    MinecraftClient.getInstance().getNetworkHandler().sendPacket(
                            new UpdateSelectedSlotC2SPacket(foundSlot)
                    );
                } else {
                    int activeHotbarSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
                    MinecraftClient.getInstance().interactionManager.clickSlot(
                            player.playerScreenHandler.syncId,
                            foundSlot,
                            activeHotbarSlot,
                            SlotActionType.SWAP,
                            player
                    );
                    cir.setReturnValue(ActionResult.FAIL);
                    return;
                }
            }
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem) && !(stack.getItem() instanceof BucketItem)) {
            return;
        }
        if (stack.getItem() != requiredItem) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        if (!autoPlace.shouldRotate(stack.getItem())) {
            queueAdjustmentIfRequired(targetPos, hand, hitResult, stateSchematic);
            return;
        }

        RotationCalculator.TargetRotation rot = RotationCalculator.getRequiredRotation(stateSchematic, player);

        if (rot != null) {
            if (RotationCalculator.isAlreadyCorrectRotation(player, rot)) {
                queueAdjustmentIfRequired(targetPos, hand, hitResult, stateSchematic);
                return;
            }

            float rawSpoofedYaw = rot.yaw != null ? rot.yaw : player.getYaw();
            float rawSpoofedPitch = rot.pitch != null ? rot.pitch : player.getPitch();

            float spoofedYaw = RotationCalculator.alignYawToSensitivity(rawSpoofedYaw, player.getYaw(), MinecraftClient.getInstance());
            float spoofedPitch = RotationCalculator.alignPitchToSensitivity(rawSpoofedPitch, player.getPitch(), MinecraftClient.getInstance());

            if (RotationSpoofer.isQueued) {
                cir.setReturnValue(ActionResult.FAIL);
                return;
            }

            RotationSpoofer.isQueued = true;
            RotationSpoofer.targetYaw = spoofedYaw;
            RotationSpoofer.targetPitch = spoofedPitch;
            RotationSpoofer.savedHand = hand;
            RotationSpoofer.savedHitResult = hitResult;
            RotationSpoofer.savedPlacedPos = targetPos;

            RotationSpoofer.currentSpoofedYaw = spoofedYaw;
            RotationSpoofer.currentSpoofedPitch = spoofedPitch;
            RotationSpoofer.isSpoofing = true;

            cir.setReturnValue(ActionResult.FAIL);
        } else {
            queueAdjustmentIfRequired(targetPos, hand, hitResult, stateSchematic);
        }
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void onInteractBlockReturn(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (this.spoofedSneak) {
            this.spoofedSneak = false;
            if (player != null && !player.isSneaking()) {
                player.networkHandler.sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
            }
        }
    }

    private void queueAdjustmentIfRequired(BlockPos targetPos, Hand hand, BlockHitResult hitResult, BlockState stateSchematic) {
        AutoPlace autoPlace = Modules.get().get(AutoPlace.class);
        if (autoPlace != null && autoPlace.isActive() && autoPlace.isAdjustRedstone()) {
            if (stateSchematic != null && (stateSchematic.getBlock() instanceof RepeaterBlock || stateSchematic.getBlock() instanceof ComparatorBlock)) {
                RotationSpoofer.pendingAdjustments.add(new RotationSpoofer.PendingAdjustment(
                        targetPos,
                        hand,
                        hitResult
                ));
            }
        }
    }
}
