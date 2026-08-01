package com.zirvn.addon.mixins.litematica;

import com.zirvn.addon.config.ModConfig;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {WorldUtils.class},
   remap = false
)
public class WorldUtilsMixin {
   @Inject(
      method = {"placementRestrictionInEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onPlacementRestrictionInEffect(MinecraftClient client, CallbackInfoReturnable<Boolean> cir) {
      if (ModConfig.get().modEnabled) {
         cir.setReturnValue(false);
      }
   }
}

