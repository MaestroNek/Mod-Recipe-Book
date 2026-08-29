package com.minemod.modrecipebook.recipe;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

public final class ContainerFillRecipe implements Recipe<RecipeInput> {
    public static final RecipeType<ContainerFillRecipe> TYPE = RecipeType.simple(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "container_fill"));

    private final ItemStack empty;
    private final Fluid fluid;
    private final int amount;
    private final ItemStack result;

    public ContainerFillRecipe(ItemStack empty, Fluid fluid, int amount, ItemStack result) {
        this.empty = empty.copyWithCount(1);
        this.fluid = fluid;
        this.amount = Math.max(1, amount);
        this.result = result.copyWithCount(1);
    }

    public List<SizedFluidIngredient> getFluidIngredients() {
        return List.of(SizedFluidIngredient.of(fluid, amount));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(empty.getItem()));
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
        throw new UnsupportedOperationException("synthetic container fill recipe");
    }
}
