package com.minemod.modrecipebook.client.layout;

import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.HashMap;
import java.util.Map;

public final class RecipeLayouts {
    private static final RecipeLayout CRAFTING = new CraftingLayout();
    private static final RecipeLayout COOKING = new CookingLayout();
    private static final RecipeLayout GENERIC = new GenericLayout();
    private static final RecipeLayout BREWING = new BrewingLayout();
    private static final RecipeLayout SMITHING = new ProcessingLayout(
            new ItemStack(Items.SMITHING_TABLE), ItemStack.EMPTY, "Smithing", false);
    private static final RecipeLayout STONECUTTER = new ProcessingLayout(
            new ItemStack(Items.STONECUTTER), ItemStack.EMPTY, "Stonecutting", false);
    private static final Map<ResourceLocation, RecipeLayout> BY_TYPE = new HashMap<>();

    private RecipeLayouts() {}

    public static RecipeLayout of(RecipeHolder<?> recipe) {
        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.value().getType());
        RecipeLayout mapped = typeId == null ? null : BY_TYPE.get(typeId);
        if (mapped != null) {
            return mapped;
        }
        if (recipe.value() instanceof BrewingMixRecipe) {
            return BREWING;
        }
        if (recipe.value() instanceof CraftingRecipe) {
            return CRAFTING;
        }
        if (recipe.value() instanceof AbstractCookingRecipe) {
            return COOKING;
        }
        if (recipe.value() instanceof StonecutterRecipe) {
            return STONECUTTER;
        }
        if (recipe.value() instanceof SmithingRecipe) {
            return SMITHING;
        }
        return GENERIC;
    }

    public static void register(ResourceLocation type, RecipeLayout layout) {
        BY_TYPE.put(type, layout);
    }
}
