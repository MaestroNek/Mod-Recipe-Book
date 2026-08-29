package com.minemod.modrecipebook.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IngredientExtractor {
    private IngredientExtractor() {}

    public static List<Ingredient> items(Recipe<?> recipe) {
        List<Ingredient> list = new ArrayList<>();
        try {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    list.add(ingredient);
                }
            }
        } catch (Exception ignored) {
            // ponytail: some custom recipes throw from getIngredients()
        }
        // Exposure FilmDevelopingRecipe keeps the film in sourceIngredient, not getIngredients().
        reflectIngredients(recipe, "getSourceIngredient", list);
        if (list.isEmpty()) {
            reflectIngredients(recipe, "getItemIngredients", list);
            reflectIngredients(recipe, "itemIngredients", list);
            reflectIngredients(recipe, "ingredients", list);
            reflectIngredients(recipe, "input", list);
        }
        addSequencedIngredients(recipe, list);
        return list;
    }

    private static boolean hasDeclaredIngredients(Recipe<?> recipe) {
        try {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean hasInputs(Recipe<?> recipe) {
        return !items(recipe).isEmpty() || !fluidInputs(recipe).isEmpty();
    }

    public static Set<Item> itemIngredients(Recipe<?> recipe) {
        Set<Item> items = new HashSet<>();
        for (Ingredient ingredient : items(recipe)) {
            for (ItemStack stack : ingredient.getItems()) {
                if (!stack.isEmpty()) {
                    items.add(stack.getItem());
                }
            }
        }
        return items;
    }

    public static Set<ResourceLocation> fluidInputs(Recipe<?> recipe) {
        Set<ResourceLocation> fluids = reflectFluids(recipe, "getFluidIngredients", "fluidIngredients");
        addSequencedFluids(recipe, fluids);
        return fluids;
    }

    public static Set<ResourceLocation> fluidOutputs(Recipe<?> recipe) {
        return reflectFluids(recipe, "getFluidResults", "fluidResults");
    }

    public static boolean canCraft(Recipe<?> recipe, Inventory inventory, StackedContents stacked) {
        if (inventory == null) {
            return false;
        }
        if (recipe instanceof BrewingMixRecipe mix) {
            return mix.presentIn(inventory);
        }
        // CustomRecipe often omits inputs from getIngredients() (fireworks: none;
        // Exposure film developing: only potions, film is sourceIngredient).
        // StackedContents.canCraft also ignores potion components.
        if (recipe instanceof CraftingRecipe crafting && stacked != null
                && hasDeclaredIngredients(recipe) && !crafting.isSpecial()) {
            return stacked.canCraft(recipe, null);
        }
        List<Ingredient> ingredients = items(recipe);
        if (ingredients.isEmpty()) {
            return false;
        }
        int size = inventory.getContainerSize();
        int[] remaining = new int[size];
        for (int i = 0; i < size; i++) {
            remaining[i] = inventory.getItem(i).getCount();
        }
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < size; i++) {
                if (remaining[i] > 0 && ingredient.test(inventory.getItem(i))) {
                    remaining[i]--;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean allIngredientsKnown(Recipe<?> recipe, Set<Item> known, Set<ResourceLocation> knownIds) {
        if (recipe instanceof BrewingMixRecipe mix) {
            return ingredientKnown(mix.reagent(), known) && knownIds.contains(PotionKeys.knownId(mix.input()));
        }
        return allIngredientsKnown(recipe, known);
    }

    public static boolean allIngredientsKnown(Recipe<?> recipe, Set<Item> known) {
        List<Ingredient> ingredients = items(recipe);
        if (ingredients.isEmpty()) {
            return true;
        }
        for (Ingredient ingredient : ingredients) {
            if (!ingredientKnown(ingredient, known)) {
                return false;
            }
        }
        return true;
    }

    private static boolean ingredientKnown(Ingredient ingredient, Set<Item> known) {
        for (Item item : known) {
            if (ingredient.test(new ItemStack(item))) {
                return true;
            }
        }
        for (ItemStack stack : ingredient.getItems()) {
            if (!stack.isEmpty() && known.contains(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack result(Recipe<?> recipe, RegistryAccess access) {
        try {
            ItemStack result = recipe.getResultItem(access);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception ignored) {
        }
        ItemStack reflected = reflectResult(recipe, "getRollableResults", "getStack");
        if (!reflected.isEmpty()) {
            return reflected;
        }
        reflected = reflectResult(recipe, "getResults", null);
        return reflected;
    }

    public static Set<Item> unlockItems(Recipe<?> recipe, RegistryAccess access) {
        if (recipe instanceof BrewingMixRecipe mix) {
            Set<Item> reagents = new HashSet<>();
            for (ItemStack stack : mix.reagent().getItems()) {
                if (!stack.isEmpty()) {
                    reagents.add(stack.getItem());
                }
            }
            return reagents;
        }
        Set<Item> items = itemIngredients(recipe);
        if (items.isEmpty()) {
            ItemStack result = result(recipe, access);
            if (!result.isEmpty()) {
                items.add(result.getItem());
            }
        }
        return items;
    }

    private static void addSequencedIngredients(Recipe<?> recipe, List<Ingredient> out) {
        Iterable<?> steps = sequence(recipe);
        if (steps == null) {
            return;
        }
        reflectIngredients(recipe, "getIngredient", out);
        ItemStack transitional = transitionalItem(recipe);
        for (Object step : steps) {
            Recipe<?> stepRecipe = unwrapRecipe(step);
            if (stepRecipe == null) {
                continue;
            }
            try {
                for (Ingredient ingredient : stepRecipe.getIngredients()) {
                    if (ingredient != null && !ingredient.isEmpty() && !transitionalOnly(ingredient, transitional)) {
                        out.add(ingredient);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void addSequencedFluids(Recipe<?> recipe, Set<ResourceLocation> fluids) {
        Iterable<?> steps = sequence(recipe);
        if (steps == null) {
            return;
        }
        for (Object step : steps) {
            Recipe<?> stepRecipe = unwrapRecipe(step);
            if (stepRecipe != null) {
                fluids.addAll(reflectFluids(stepRecipe, "getFluidIngredients", "fluidIngredients"));
            }
        }
    }

    private static Iterable<?> sequence(Recipe<?> recipe) {
        try {
            Object value = recipe.getClass().getMethod("getSequence").invoke(recipe);
            return value instanceof Iterable<?> steps ? steps : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Recipe<?> unwrapRecipe(Object step) {
        if (step instanceof Recipe<?> recipe) {
            return recipe;
        }
        try {
            Object value = step.getClass().getMethod("getRecipe").invoke(step);
            return value instanceof Recipe<?> recipe ? recipe : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static ItemStack transitionalItem(Recipe<?> recipe) {
        try {
            Object value = recipe.getClass().getMethod("getTransitionalItem").invoke(recipe);
            return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean transitionalOnly(Ingredient ingredient, ItemStack transitional) {
        if (transitional.isEmpty()) {
            return false;
        }
        boolean any = false;
        for (ItemStack stack : ingredient.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            any = true;
            if (stack.getItem() != transitional.getItem()) {
                return false;
            }
        }
        return any;
    }

    private static void reflectIngredients(Recipe<?> recipe, String fieldOrMethod, List<Ingredient> out) {
        try {
            Method method = recipe.getClass().getMethod(fieldOrMethod);
            Object value = method.invoke(recipe);
            addIngredients(value, out);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            var field = recipe.getClass().getDeclaredField(fieldOrMethod);
            field.setAccessible(true);
            addIngredients(field.get(recipe), out);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void addIngredients(Object value, List<Ingredient> out) {
        if (value instanceof Ingredient ingredient && !ingredient.isEmpty()) {
            out.add(ingredient);
        } else if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                if (element instanceof Ingredient ingredient && !ingredient.isEmpty()) {
                    out.add(ingredient);
                } else if (element instanceof ItemStack stack && !stack.isEmpty()) {
                    out.add(Ingredient.of(stack));
                }
            }
        }
    }

    private static ItemStack reflectResult(Recipe<?> recipe, String listMethod, String stackMethod) {
        try {
            Method method = recipe.getClass().getMethod(listMethod);
            Object value = method.invoke(recipe);
            if (value instanceof Iterable<?> iterable) {
                for (Object element : iterable) {
                    if (element instanceof ItemStack stack && !stack.isEmpty()) {
                        return stack;
                    }
                    if (stackMethod != null && element != null) {
                        try {
                            Object stack = element.getClass().getMethod(stackMethod).invoke(element);
                            if (stack instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                                return itemStack;
                            }
                        } catch (ReflectiveOperationException ignored) {
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static Set<ResourceLocation> reflectFluids(Recipe<?> recipe, String methodName, String fieldName) {
        Set<ResourceLocation> fluids = new HashSet<>();
        collectFromMethod(recipe, methodName, fluids);
        collectFromFields(recipe, fieldName, fluids);
        if (fluids.isEmpty()) {
            try {
                collectFromFields(recipe.getClass().getMethod("getParams").invoke(recipe), fieldName, fluids);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return fluids;
    }

    private static void collectFromMethod(Object target, String methodName, Set<ResourceLocation> fluids) {
        if (target == null) {
            return;
        }
        try {
            collectFluids(target.getClass().getMethod(methodName).invoke(target), fluids);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void collectFromFields(Object target, String fieldName, Set<ResourceLocation> fluids) {
        if (target == null) {
            return;
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                collectFluids(field.get(target), fluids);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void collectFluids(Object value, Set<ResourceLocation> out) {
        if (value == null) {
            return;
        }
        if (value instanceof Fluid fluid) {
            addFluid(fluid, out);
            return;
        }
        if (value instanceof FluidStack stack) {
            addFluid(stack.getFluid(), out);
            return;
        }
        if (value instanceof SizedFluidIngredient sized) {
            for (FluidStack stack : sized.getFluids()) {
                addFluid(stack.getFluid(), out);
            }
            return;
        }
        if (value instanceof FluidIngredient ingredient) {
            for (FluidStack stack : ingredient.getStacks()) {
                addFluid(stack.getFluid(), out);
            }
            return;
        }
        if (value instanceof Object[] array) {
            for (Object element : array) {
                collectFluids(element, out);
            }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                collectFluids(element, out);
            }
            return;
        }
        for (String name : List.of("getFluid", "getFluids", "getStacks", "getMatchingFluidStacks",
                "getMatchingFluidStack", "getDisplayedStacks")) {
            try {
                collectFluids(value.getClass().getMethod(name).invoke(value), out);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void addFluid(Fluid fluid, Set<ResourceLocation> out) {
        if (fluid != null && fluid != Fluids.EMPTY) {
            out.add(BuiltInRegistries.FLUID.getKey(fluid));
        }
    }
}
