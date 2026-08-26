package com.minemod.modrecipebook.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class BrewingMixRecipe implements Recipe<RecipeInput> {
    public static final RecipeType<BrewingMixRecipe> TYPE = RecipeType.simple(
            ResourceLocation.fromNamespaceAndPath("modrecipebook", "brewing"));

    private final ItemStack input;
    private final Ingredient reagent;
    private final ItemStack result;

    public BrewingMixRecipe(ItemStack input, Ingredient reagent, ItemStack result) {
        this.input = input.copyWithCount(1);
        this.reagent = reagent;
        this.result = result.copyWithCount(1);
    }

    public ItemStack input() {
        return input;
    }

    public Ingredient reagent() {
        return reagent;
    }

    public boolean presentIn(Inventory inventory) {
        return hasStack(inventory, input) && hasIngredient(inventory, reagent);
    }

    private static boolean hasStack(Inventory inventory, ItemStack needed) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, needed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasIngredient(Inventory inventory, Ingredient ingredient) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (ingredient.test(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(input.getItem()), reagent);
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        throw new UnsupportedOperationException("synthetic brewing recipe");
    }
}
