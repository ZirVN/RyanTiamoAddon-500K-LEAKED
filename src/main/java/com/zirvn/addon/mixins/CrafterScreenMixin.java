package com.zirvn.addon.mixins;

import com.zirvn.addon.modules.CrafterSetup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.ingame.CrafterScreen;
import net.minecraft.screen.CrafterScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrafterScreen.class)
public class CrafterScreenMixin {
    @Inject(
        method = "init",
        at = @At("TAIL")
    )
    private void onInit(CallbackInfo ci) {
        CrafterSetup setup = Modules.get().get(CrafterSetup.class);
        if (setup != null && setup.isActive()) {
            CrafterScreenHandler handler = ((CrafterScreen) (Object) this).getScreenHandler();
            HandledScreenAccessor accessor = (HandledScreenAccessor) this;

            for (int i = 0; i < 9; ++i) {
                boolean disabled = setup.isSlotDisabled(i);
                handler.setSlotEnabled(i, !disabled);
                accessor.invokeOnSlotChangedState(i, handler.syncId, !disabled);
            }
        }
    }
}

