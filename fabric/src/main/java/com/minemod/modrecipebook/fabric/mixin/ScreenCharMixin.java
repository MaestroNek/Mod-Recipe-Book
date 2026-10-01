package com.minemod.modrecipebook.fabric.mixin;

import com.minemod.modrecipebook.client.ClientGuiEvents;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ScreenCharMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void modrecipebook$charTyped(char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Screen screen && ClientGuiEvents.charTyped(screen, codePoint, modifiers)) {
            cir.setReturnValue(true);
        }
    }
}
