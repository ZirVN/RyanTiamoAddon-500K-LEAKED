package com.zirvn.addon.mixins;

import com.zirvn.addon.config.ModConfig;
import com.zirvn.addon.modules.AutoDirection;
import com.zirvn.addon.util.RotationSpoofer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    private static boolean didAutoDirSpoof = false;
    private static float savedYaw = 0.0F;
    private static float savedPitch = 0.0F;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (AutoDirection.ticksToOverride > 0) {
            didAutoDirSpoof = true;
            --AutoDirection.ticksToOverride;
            savedYaw = player.getYaw();
            savedPitch = player.getPitch();
            if (!Float.isNaN(AutoDirection.overrideYaw)) {
                player.setYaw(AutoDirection.overrideYaw);
            }
            if (!Float.isNaN(AutoDirection.overridePitch)) {
                player.setPitch(AutoDirection.overridePitch);
            }
        } else if (RotationSpoofer.isSpoofing) {
            RotationSpoofer.originalYaw = player.getYaw();
            RotationSpoofer.originalPitch = player.getPitch();

            player.setYaw(RotationSpoofer.currentSpoofedYaw);
            player.setPitch(RotationSpoofer.currentSpoofedPitch);
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onSendMovementPacketsTail(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (didAutoDirSpoof) {
            didAutoDirSpoof = false;
            player.setYaw(savedYaw);
            player.setPitch(savedPitch);
        } else if (RotationSpoofer.isSpoofing) {
            player.setYaw(RotationSpoofer.originalYaw);
            player.setPitch(RotationSpoofer.originalPitch);
            RotationSpoofer.spoofSent = true;
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        if (ModConfig.get().modEnabled && ModConfig.get().pauseMovementDuringDelay) {
            long elapsed = System.currentTimeMillis() - RotationSpoofer.lastPlaceTime;
            if (elapsed < ModConfig.get().placeDelayMs) {
                ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
                if (player.input != null && player.input.playerInput != null) {
                    PlayerInput oldInput = player.input.playerInput;
                    player.input.playerInput = new PlayerInput(
                        false, false, false, false,
                        oldInput.jump(),
                        oldInput.sneak(),
                        oldInput.sprint()
                    );
                }
            }
        }
    }
}
