package com.zirvn.addon.util;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RotationSpoofer {
    public static boolean shouldPlaceBlockLater = false;
    public static boolean isPlacingLater = false;

    public static float originalYaw = 0.0f;
    public static float originalPitch = 0.0f;
    public static float spoofedYaw = 0.0f;
    public static float spoofedPitch = 0.0f;

    public static Hand savedHand;
    public static BlockHitResult savedHitResult;
    public static BlockPos savedPlacedPos;
    
    public static long lastPlaceTime = 0;

    // Smooth Rotation State
    public static boolean isQueued = false;
    public static boolean isSpoofing = false;
    public static float targetYaw = 0.0f;
    public static float targetPitch = 0.0f;
    public static float startYaw = 0.0f;
    public static float startPitch = 0.0f;
    public static float currentSpoofedYaw = 0.0f;
    public static float currentSpoofedPitch = 0.0f;
    public static int currentStep = 0;
    public static int totalSteps = 2;
    
    // Safety delay flags
    public static boolean spoofSent = false;
    public static int pendingTicks = 0;

    public static class PendingAdjustment {
        public final BlockPos pos;
        public final Hand hand;
        public final BlockHitResult hitResult;
        public final long timeCreated;
        public int clicksLeft = -1;

        public PendingAdjustment(BlockPos pos, Hand hand, BlockHitResult hitResult) {
            this.pos = pos;
            this.hand = hand;
            this.hitResult = hitResult;
            this.timeCreated = System.currentTimeMillis();
        }
    }

    public static final List<PendingAdjustment> pendingAdjustments = new ArrayList<>();
}
