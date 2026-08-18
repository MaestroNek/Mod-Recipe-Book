package com.minemod.modrecipebook.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public record RecipeGroup(ItemStack result, List<RecipeHolder<?>> recipes) {
    public RecipeHolder<?> primary() {
        return recipes.getFirst();
    }
}
