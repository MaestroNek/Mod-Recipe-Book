package com.minemod.modrecipebook.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.Registry;

public final class RecipeTypes {
    public static <T extends Recipe<?>> RecipeType<T> register(ResourceLocation id) {
        if (BuiltInRegistries.RECIPE_TYPE.containsKey(id)) {
            @SuppressWarnings("unchecked")
            RecipeType<T> existing = (RecipeType<T>) BuiltInRegistries.RECIPE_TYPE.get(id);
            return existing;
        }
        RecipeType<T> type = new RecipeType<>() {
            @Override
            public String toString() {
                return id.toString();
            }
        };
        try {
            return Registry.register(BuiltInRegistries.RECIPE_TYPE, id, type);
        } catch (IllegalStateException frozen) {
            return type;
        }
    }

    private RecipeTypes() {}
}
