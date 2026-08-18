package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.client.RecipeCategoryConfig;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
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
