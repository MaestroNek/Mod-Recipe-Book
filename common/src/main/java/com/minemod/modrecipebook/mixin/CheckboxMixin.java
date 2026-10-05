package com.minemod.modrecipebook.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Checkbox.class)
public abstract class CheckboxMixin {
    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void modrecipebook$resetColor(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
