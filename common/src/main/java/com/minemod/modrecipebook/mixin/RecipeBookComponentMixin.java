package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.client.RecipeCategoryConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
    @Invoker("renderGhostRecipeTooltip")
    protected abstract void modrecipebook$renderGhostTooltip(GuiGraphics graphics, int leftPos, int topPos, int mouseX, int mouseY);

    // Ghosts are still drawn while the vanilla book is hidden, but its tooltip is not.
    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void modrecipebook$ghostTooltip(GuiGraphics graphics, int leftPos, int topPos, int mouseX, int mouseY, CallbackInfo ci) {
        if (!((RecipeBookComponent) (Object) this).isVisible()) {
            this.modrecipebook$renderGhostTooltip(graphics, leftPos, topPos, mouseX, mouseY);
        }
    }

    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true)
    private void modrecipebook$blockOpen(CallbackInfo ci) {
        if (!RecipeCategoryConfig.hideVanillaBook()) {
            return;
        }
        if (!((RecipeBookComponent) (Object) this).isVisible()) {
            ci.cancel();
        }
    }
}
