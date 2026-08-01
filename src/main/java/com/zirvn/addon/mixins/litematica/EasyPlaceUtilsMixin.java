package com.zirvn.addon.mixins.litematica;

import com.zirvn.addon.config.ModConfig;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {EasyPlaceUtils.class},
   remap = false
)
public class EasyPlaceUtilsMixin {
   @Inject(
      method = {"placementRestrictionInEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onPlacementRestrictionInEffect(CallbackInfoReturnable<Boolean> var0) {
      if (ModConfig.get().modEnabled) {
         var0.setReturnValue(Boolean.FALSE);
      }

   }
}
