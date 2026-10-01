package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.client.ClientUnlockedRecipes;
import com.minemod.modrecipebook.client.RecipeCategoryConfig;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeToast.class)
public class RecipeToastMixin {
    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void modrecipebook$modBookToasts(
            ToastComponent toastComponent, RecipeHolder<?> recipe, CallbackInfo ci) {
        if (RecipeCategoryConfig.hideVanillaBook() && !ClientUnlockedRecipes.showingOwnToast()) {
            ci.cancel();
        }
    }
}
