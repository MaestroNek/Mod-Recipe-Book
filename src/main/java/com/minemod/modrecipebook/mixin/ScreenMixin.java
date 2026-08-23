package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.client.ModRecipeBookScreens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void modrecipebook$renderBook(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ModRecipeBookScreens.render((Screen) (Object) this, graphics, mouseX, mouseY, partialTick);
    }
}
