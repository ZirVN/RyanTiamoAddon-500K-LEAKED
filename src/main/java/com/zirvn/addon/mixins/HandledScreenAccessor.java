package com.zirvn.addon.mixins;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Invoker("onSlotChangedState")
    void invokeOnSlotChangedState(int slotId, int containerId, boolean newState);
}

