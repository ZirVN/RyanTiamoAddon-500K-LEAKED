package com.zirvn.addon.mixins;

import com.zirvn.addon.config.ModConfig;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    public HitResult crosshairTarget;

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUseHead(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (ModConfig.get().modEnabled) {
            if (client.player != null && client.world != null) {
                if (this.crosshairTarget == null || this.crosshairTarget.getType() == HitResult.Type.MISS) {
                    BlockPos schematicPos = rayTraceSchematic(client);
                    if (schematicPos != null) {
                        Vec3d hitVec = new Vec3d((double) schematicPos.getX() + 0.5D, (double) schematicPos.getY() + 0.5D, (double) schematicPos.getZ() + 0.5D);
                        this.crosshairTarget = new BlockHitResult(hitVec, Direction.UP, schematicPos, false);
                    }
                }
            }
        }
    }

    @Unique
    private static BlockPos rayTraceSchematic(MinecraftClient client) {
        if (client.player == null) {
            return null;
        }
        Vec3d cameraPos = client.player.getCameraPosVec(1.0F);
        Vec3d rotation = client.player.getRotationVec(1.0F);
        double reach = 5.0D;

        for (double d = 0.0D; d <= reach; d += 0.1) {
            Vec3d currentPos = cameraPos.add(rotation.multiply(d));
            BlockPos pos = BlockPos.ofFloored(currentPos.x, currentPos.y, currentPos.z);
            
            BlockState schematicState = null;
            try {
                if (SchematicWorldHandler.getSchematicWorld() != null) {
                    schematicState = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
            } catch (Throwable ignored) {}

            if (schematicState != null && !schematicState.isAir()) {
                if (client.world != null && client.world.getBlockState(pos).isAir()) {
                    return pos;
                }
            }
        }

        return null;
    }
}
