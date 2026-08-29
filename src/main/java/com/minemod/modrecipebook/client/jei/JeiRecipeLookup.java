package com.minemod.modrecipebook.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.drawable.IScalableDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JeiRecipeLookup {
    private static final Map<ResourceLocation, JeiRecipeBinding> CACHE = new HashMap<>();
    private static final IScalableDrawable NO_BACKGROUND = (graphics, x, y, width, height) -> {};

    private JeiRecipeLookup() {}

    public static void clearCache() {
        CACHE.clear();
    }

    public static boolean canResolve(RecipeHolder<?> holder) {
        return resolve(holder).isPresent();
    }

    public record CategoryPage(IRecipeCategory<?> category, List<JeiRecipeBinding> recipes) {}

    public static List<CategoryPage> lookupOutput(ItemStack result) {
        if (result == null || result.isEmpty()) {
            return List.of();
        }
        return lookup(VanillaTypes.ITEM_STACK, result, RecipeIngredientRole.OUTPUT, null);
    }

    public static List<CategoryPage> lookupFluidOutput(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return List.of();
        }
        List<CategoryPage> pages = lookup(NeoForgeTypes.FLUID_STACK, fluid, RecipeIngredientRole.OUTPUT, null);
        if (pages.isEmpty() && fluid.getAmount() != FluidType.BUCKET_VOLUME) {
            FluidStack bucket = fluid.copy();
            bucket.setAmount(FluidType.BUCKET_VOLUME);
            pages = lookup(NeoForgeTypes.FLUID_STACK, bucket, RecipeIngredientRole.OUTPUT, null);
        }
        if (pages.isEmpty()) {
            pages = lookupEmptying(fluid.getFluid());
        }
        return pages;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> List<CategoryPage> lookup(IIngredientType<T> type, T ingredient,
                                                 RecipeIngredientRole role, Fluid requiredOutput) {
        if (!ModJeiPlugin.isAvailable() || ingredient == null) {
            return List.of();
        }
        IRecipeManager recipeManager = ModJeiPlugin.runtime().getRecipeManager();
        IFocusFactory focusFactory = ModJeiPlugin.runtime().getJeiHelpers().getFocusFactory();
        IFocus<T> focus = focusFactory.createFocus(role, type, ingredient);
        List<IFocus<?>> focuses = List.of(focus);
        IFocusGroup emptyFocus = focusFactory.getEmptyFocusGroup();

        List<CategoryPage> pages = new ArrayList<>();
        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup()
                .limitFocus(focuses)
                .get()
                .toList();
        for (IRecipeCategory category : categories) {
            List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType())
                    .limitFocus(focuses)
                    .get()
                    .toList();
            List<JeiRecipeBinding> bindings = new ArrayList<>();
            for (Object recipe : recipes) {
                Optional<? extends IRecipeLayoutDrawable<?>> drawable =
                        createDrawable(recipeManager, category, recipe, emptyFocus);
                if (drawable.isEmpty()) {
                    continue;
                }
                if (requiredOutput != null && !hasFluid(drawable.get(), requiredOutput)) {
                    continue;
                }
                bindings.add(new JeiRecipeBinding(category, recipe, drawable.get()));
            }
            if (!bindings.isEmpty()) {
                pages.add(new CategoryPage(category, List.copyOf(bindings)));
            }
        }
        return pages;
    }

    private static List<CategoryPage> lookupEmptying(Fluid fluid) {
        Item bucket = fluid.getBucket();
        if (bucket != Items.AIR) {
            List<CategoryPage> fromBucket = lookup(
                    VanillaTypes.ITEM_STACK, new ItemStack(bucket), RecipeIngredientRole.INPUT, fluid);
            if (!fromBucket.isEmpty()) {
                return fromBucket;
            }
        }
        if (!ModJeiPlugin.isAvailable()) {
            return List.of();
        }
        IRecipeManager recipeManager = ModJeiPlugin.runtime().getRecipeManager();
        IFocusGroup emptyFocus = ModJeiPlugin.runtime().getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        List<CategoryPage> pages = new ArrayList<>();
        for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().get().toList()) {
            String path = category.getRecipeType().getUid().getPath();
            if (!path.contains("drain") && !path.contains("emptying")) {
                continue;
            }
            List<JeiRecipeBinding> bindings = new ArrayList<>();
            for (Object recipe : recipeManager.createRecipeLookup(category.getRecipeType()).get().toList()) {
                Optional<? extends IRecipeLayoutDrawable<?>> drawable =
                        createDrawable(recipeManager, category, recipe, emptyFocus);
                if (drawable.isPresent() && hasFluid(drawable.get(), fluid)) {
                    bindings.add(new JeiRecipeBinding(category, recipe, drawable.get()));
                }
            }
            if (!bindings.isEmpty()) {
                pages.add(new CategoryPage(category, List.copyOf(bindings)));
            }
        }
        return pages;
    }

    private static boolean hasFluid(IRecipeLayoutDrawable<?> drawable, Fluid fluid) {
        for (IRecipeSlotView slot : drawable.getRecipeSlotsView().getSlotViews()) {
            Optional<FluidStack> displayed = slot.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK);
            if (displayed.isPresent() && sameFluid(displayed.get(), fluid)) {
                return true;
            }
            if (slot.getAllIngredients().anyMatch(ingredient ->
                    ingredient.getIngredient() instanceof FluidStack stack && sameFluid(stack, fluid))) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameFluid(FluidStack stack, Fluid fluid) {
        return stack != null && !stack.isEmpty() && stack.getFluid().isSame(fluid);
    }

    public static Optional<JeiRecipeBinding> resolve(RecipeHolder<?> holder) {
        if (!ModJeiPlugin.isAvailable()) {
            return Optional.empty();
        }
        JeiRecipeBinding cached = CACHE.get(holder.id());
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<JeiRecipeBinding> found = findBinding(holder);
        found.ifPresent(binding -> CACHE.put(holder.id(), binding));
        return found;
    }

    private static Optional<JeiRecipeBinding> findBinding(RecipeHolder<?> holder) {
        IRecipeManager recipeManager = ModJeiPlugin.runtime().getRecipeManager();
        IFocusGroup focusGroup = ModJeiPlugin.runtime().getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup().get().toList();
        for (IRecipeCategory<?> category : categories) {
            RecipeType<?> type = category.getRecipeType();
            List<?> recipes = recipeManager.createRecipeLookup(type).get().toList();
            for (Object recipe : recipes) {
                if (!matches(holder, category, recipe)) {
                    continue;
                }
                Optional<? extends IRecipeLayoutDrawable<?>> drawable =
                        createDrawable(recipeManager, category, recipe, focusGroup);
                if (drawable.isPresent()) {
                    return Optional.of(new JeiRecipeBinding(category, recipe, drawable.get()));
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<? extends IRecipeLayoutDrawable<?>> createDrawable(
            IRecipeManager recipeManager,
            IRecipeCategory category,
            Object recipe,
            IFocusGroup focusGroup
    ) {
        return recipeManager.createRecipeLayoutDrawable(category, recipe, focusGroup, NO_BACKGROUND, 0);
    }

    private static boolean matches(RecipeHolder<?> holder, IRecipeCategory<?> category, Object recipe) {
        ResourceLocation id = holder.id();
        ResourceLocation registryName = registryName(category, recipe);
        if (registryName != null && registryName.equals(id)) {
            return true;
        }
        if (recipe instanceof RecipeHolder<?> recipeHolder) {
            return recipeHolder.id().equals(id);
        }
        if (recipe instanceof Recipe vanillaRecipe && holder.value() == vanillaRecipe) {
            return true;
        }
        return holder.value().equals(recipe);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResourceLocation registryName(IRecipeCategory category, Object recipe) {
        return category.getRegistryName(recipe);
    }
}
