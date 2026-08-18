package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.recipe.ModRecipeBookAttachments;
import com.minemod.modrecipebook.recipe.PlaceRecipeContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBook.class)
public class RecipeBookMixin {
    @Inject(method = "contains(Lnet/minecraft/world/item/crafting/RecipeHolder;)Z", at = @At("RETURN"), cancellable = true)
    private void modrecipebook$orUnlocked(RecipeHolder<?> recipe, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || recipe == null) {
            return;
        }
        ServerPlayer player = PlaceRecipeContext.current();
        if (player != null && player.getData(ModRecipeBookAttachments.UNLOCKED).contains(recipe.id())) {
            cir.setReturnValue(true);
        }
    }
}
