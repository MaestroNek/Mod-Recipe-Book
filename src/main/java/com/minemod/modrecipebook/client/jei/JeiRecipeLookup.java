package com.minemod.modrecipebook.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.drawable.IScalableDrawable;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<CategoryPage> lookupOutput(ItemStack result) {
        if (!ModJeiPlugin.isAvailable() || result == null || result.isEmpty()) {
            return List.of();
        }
        IRecipeManager recipeManager = ModJeiPlugin.runtime().getRecipeManager();
        IFocusFactory focusFactory = ModJeiPlugin.runtime().getJeiHelpers().getFocusFactory();
        IFocus<ItemStack> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, result);
        List<IFocus<?>> focuses = List.of(focus);
        IFocusGroup focusGroup = focusFactory.createFocusGroup(focuses);

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
                        createDrawable(recipeManager, category, recipe, focusGroup);
                drawable.ifPresent(d -> bindings.add(new JeiRecipeBinding(category, recipe, d)));
            }
            if (!bindings.isEmpty()) {
                pages.add(new CategoryPage(category, List.copyOf(bindings)));
            }
        }
        return pages;
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
