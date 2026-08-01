package com.zirvn.addon.util;

import net.minecraft.block.*;
import net.minecraft.block.enums.Orientation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class RotationCalculator {

    public static class TargetRotation {
        public final Float yaw;
        public final Float pitch;

        public TargetRotation(Float yaw, Float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static TargetRotation getRequiredRotation(BlockState state, ClientPlayerEntity player) {
        Block block = state.getBlock();

        // 1. Crafter Block (Minecraft 1.21) using Properties.ORIENTATION
        if (state.contains(Properties.ORIENTATION)) {
            Orientation orientation = state.get(Properties.ORIENTATION);
            Direction facing = orientation.getFacing();
            Direction rotation = orientation.getRotation();
            
            if (facing.getAxis() == Direction.Axis.Y) {
                // Vertical crafter: output facing determines pitch, rotation determines yaw.
                // If facing is UP, we click the top face of the block below, meaning we look DOWN (pitch = 90.0f).
                // If facing is DOWN, we click the bottom face of the block above, meaning we look UP (pitch = -90.0f).
                float pitch = facing == Direction.UP ? 90.0f : -90.0f;
                // player horizontal facing must be opposite of rotation
                TargetRotation yawRot = getRotationForLookDirection(rotation.getOpposite(), false);
                return new TargetRotation(yawRot.yaw, pitch);
            } else {
                // Horizontal crafter: to place it facing the target direction, we must look at the opposite direction.
                // (since placed crafter faces the player).
                return getRotationForLookDirection(facing.getOpposite(), true);
            }
        }

        // 2. Blocks with AXIS property (Logs, Wood, Pillars, Basalt, Nether Portal, Nether Wart Block/etc.)
        if (state.contains(Properties.AXIS)) {
            Direction.Axis axis = state.get(Properties.AXIS);
            if (axis == Direction.Axis.Y) {
                return new TargetRotation(null, 90.0f); // Look DOWN to place Y-axis blocks
            } else if (axis == Direction.Axis.X) {
                if (player != null) {
                    float yaw = MathHelper.wrapDegrees(player.getYaw());
                    float targetYaw = (yaw < 0) ? -90.0f : 90.0f;
                    return new TargetRotation(targetYaw, null);
                } else {
                    return new TargetRotation(90.0f, null);
                }
            } else if (axis == Direction.Axis.Z) {
                if (player != null) {
                    float yaw = MathHelper.wrapDegrees(player.getYaw());
                    float targetYaw = (Math.abs(yaw) > 90.0f) ? 180.0f : 0.0f;
                    return new TargetRotation(targetYaw, null);
                } else {
                    return new TargetRotation(0.0f, null);
                }
            }
        }

        // 3. Blocks with HORIZONTAL_FACING property
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            if (block instanceof StairsBlock || block instanceof TrapdoorBlock || block instanceof FenceGateBlock || 
                block instanceof AnvilBlock || block instanceof DoorBlock || block instanceof HopperBlock) {
                return getRotationForLookDirection(facing, false);
            } else {
                return getRotationForLookDirection(facing.getOpposite(), false);
            }
        }

        // 4. Blocks with FACING property (Piston, Dispenser, Dropper, Observer, etc.)
        if (state.contains(Properties.FACING)) {
            Direction facing = state.get(Properties.FACING);
            if (block instanceof ObserverBlock) {
                return getRotationForLookDirection(facing, true);
            } else if (block instanceof HopperBlock) {
                if (facing != Direction.DOWN) {
                    return getRotationForLookDirection(facing, false);
                }
            } else {
                return getRotationForLookDirection(facing.getOpposite(), true);
            }
        }

        return null;
    }

    private static TargetRotation getRotationForLookDirection(Direction dir, boolean is6Directional) {
        switch (dir) {
            case UP: return new TargetRotation(null, -90.0f); // Look UP -> pitch -90
            case DOWN: return new TargetRotation(null, 90.0f); // Look DOWN -> pitch 90
            case SOUTH: return new TargetRotation(0.0f, is6Directional ? 0.0f : null); // Look SOUTH -> yaw 0
            case WEST: return new TargetRotation(90.0f, is6Directional ? 0.0f : null); // Look WEST -> yaw 90
            case NORTH: return new TargetRotation(180.0f, is6Directional ? 0.0f : null); // Look NORTH -> yaw 180
            case EAST: return new TargetRotation(-90.0f, is6Directional ? 0.0f : null); // Look EAST -> yaw -90
        }
        return null;
    }

    public static boolean isAlreadyCorrectRotation(ClientPlayerEntity player, TargetRotation rot) {
        if (rot == null) return true;

        if (rot.yaw != null) {
            Direction targetDir = getDirectionFromYaw(rot.yaw);
            if (targetDir != null && player.getHorizontalFacing() != targetDir) {
                return false;
            }
        }

        if (rot.pitch != null) {
            float currentPitch = player.getPitch();
            if (rot.pitch == -90.0f) { // UP
                if (currentPitch > -45.0f) return false;
            } else if (rot.pitch == 90.0f) { // DOWN
                if (currentPitch < 45.0f) return false;
            } else {
                if (currentPitch < -45.0f || currentPitch > 45.0f) return false;
            }
        }

        return true;
    }

    private static Direction getDirectionFromYaw(float yaw) {
        float normalizedYaw = MathHelper.wrapDegrees(yaw);
        if (normalizedYaw >= -45.0f && normalizedYaw < 45.0f) {
            return Direction.SOUTH;
        } else if (normalizedYaw >= 45.0f && normalizedYaw < 135.0f) {
            return Direction.WEST;
        } else if (normalizedYaw >= -135.0f && normalizedYaw < -45.0f) {
            return Direction.EAST;
        } else {
            return Direction.NORTH;
        }
    }

    public static float alignYawToSensitivity(float targetYaw, float currentYaw, MinecraftClient client) {
        double sensitivity = client.options.getMouseSensitivity().getValue();
        float f = (float) (sensitivity * 0.6F + 0.2F);
        float g = f * f * f * 8.0F;
        float gcd = g * 0.15F;

        float yawDiff = targetYaw - currentYaw;
        yawDiff = MathHelper.wrapDegrees(yawDiff);
        int steps = Math.round(yawDiff / gcd);
        return currentYaw + steps * gcd;
    }

    public static float alignPitchToSensitivity(float targetPitch, float currentPitch, MinecraftClient client) {
        double sensitivity = client.options.getMouseSensitivity().getValue();
        float f = (float) (sensitivity * 0.6F + 0.2F);
        float g = f * f * f * 8.0F;
        float gcd = g * 0.15F;

        float pitchDiff = targetPitch - currentPitch;
        int steps = Math.round(pitchDiff / gcd);
        float alignedPitch = currentPitch + steps * gcd;
        return MathHelper.clamp(alignedPitch, -90.0f, 90.0f);
    }
}
