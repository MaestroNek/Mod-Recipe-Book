package com.minemod.modrecipebook.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModRecipeIndex {
    private static Map<String, List<RecipeHolder<?>>> byMod = Map.of();
    private static Map<String, List<RecipeHolder<?>>> byResultMod = Map.of();
    private static Map<Item, List<RecipeHolder<?>>> byIngredient = Map.of();
    private static Map<Item, List<RecipeHolder<?>>> byResult = Map.of();
    private static List<String> modOrder = List.of();

    private ModRecipeIndex() {}

    public static void rebuild(RecipeManager manager, RegistryAccess access) {
        Map<String, List<RecipeHolder<?>>> mods = new LinkedHashMap<>();
        Map<String, List<RecipeHolder<?>>> resultMods = new LinkedHashMap<>();
        Map<Item, List<RecipeHolder<?>>> ingredients = new HashMap<>();
        Map<Item, List<RecipeHolder<?>>> results = new HashMap<>();

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            ItemStack result = IngredientExtractor.result(recipe, access);
            if (result.isEmpty() && IngredientExtractor.items(recipe).isEmpty()) {
                continue;
            }
            ResourceLocation id = holder.id();
            if (!"minecraft".equals(id.getNamespace())) {
                mods.computeIfAbsent(id.getNamespace(), ns -> new ArrayList<>()).add(holder);
            }
            for (Item item : IngredientExtractor.unlockItems(recipe, access)) {
                ingredients.computeIfAbsent(item, k -> new ArrayList<>()).add(holder);
            }
            if (!result.isEmpty()) {
                results.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(holder);
                String itemNs = BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace();
                if (!"neoforge".equals(itemNs) && !"modrecipebook".equals(itemNs)) {
                    resultMods.computeIfAbsent(itemNs, ns -> new ArrayList<>()).add(holder);
                }
            }
        }

        mods.values().forEach(list -> list.sort((a, b) -> a.id().compareTo(b.id())));
        resultMods.values().forEach(list -> list.sort((a, b) -> a.id().compareTo(b.id())));
        byMod = Map.copyOf(mods);
        byResultMod = Map.copyOf(resultMods);
        byIngredient = Map.copyOf(ingredients);
        byResult = Map.copyOf(results);
        List<String> order = new ArrayList<>(mods.keySet());
        Collections.sort(order);
        modOrder = List.copyOf(order);
    }

    public static List<String> mods() {
        return modOrder;
    }

    public static List<RecipeHolder<?>> recipes(String namespace) {
        return byMod.getOrDefault(namespace, List.of());
    }

    public static List<RecipeHolder<?>> recipesByItemMod(String namespace) {
        return byResultMod.getOrDefault(namespace, List.of());
    }

    public static Collection<List<RecipeHolder<?>>> allByMod() {
        return byMod.values();
    }

    public static List<RecipeHolder<?>> byIngredient(Item item) {
        return byIngredient.getOrDefault(item, List.of());
    }

    public static List<RecipeHolder<?>> byResult(Item item) {
        return byResult.getOrDefault(item, List.of());
    }

    public static List<RecipeHolder<?>> all() {
        List<RecipeHolder<?>> all = new ArrayList<>();
        List<String> order = new ArrayList<>(byResultMod.keySet());
        Collections.sort(order);
        for (String mod : order) {
            if ("minecraft".equals(mod)) {
                continue;
            }
            all.addAll(byResultMod.getOrDefault(mod, List.of()));
        }
        return all;
    }
}
