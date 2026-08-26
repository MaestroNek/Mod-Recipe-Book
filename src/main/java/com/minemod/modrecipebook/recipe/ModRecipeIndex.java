package com.minemod.modrecipebook.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ModRecipeIndex {
    private static Map<String, List<RecipeHolder<?>>> byMod = Map.of();
    private static Map<String, List<RecipeHolder<?>>> byResultMod = Map.of();
    private static Map<Item, List<RecipeHolder<?>>> byIngredient = Map.of();
    private static Map<Item, List<RecipeHolder<?>>> byResult = Map.of();
    private static Map<ResourceLocation, List<RecipeHolder<?>>> byKnownKey = Map.of();
    private static Map<ResourceLocation, RecipeHolder<?>> byId = Map.of();
    private static Set<String> craftableMods = Set.of();
    private static Set<Item> craftableItems = Set.of();
    private static Map<String, List<ItemStack>> craftableStacks = Map.of();
    private static List<ItemStack> allCraftableStacks = List.of();
    private static List<String> modOrder = List.of();

    private ModRecipeIndex() {}

    public static void rebuild(RecipeManager manager, RegistryAccess access) {
        rebuild(manager, access, PotionBrewing.EMPTY);
    }

    public static void rebuild(RecipeManager manager, RegistryAccess access, PotionBrewing brewing) {
        Map<String, List<RecipeHolder<?>>> mods = new LinkedHashMap<>();
        Map<String, List<RecipeHolder<?>>> resultMods = new LinkedHashMap<>();
        Map<Item, List<RecipeHolder<?>>> ingredients = new HashMap<>();
        Map<Item, List<RecipeHolder<?>>> results = new HashMap<>();
        Map<ResourceLocation, List<RecipeHolder<?>>> knownKeys = new HashMap<>();
        Map<ResourceLocation, RecipeHolder<?>> ids = new HashMap<>();
        Set<String> craftable = new HashSet<>();
        Set<Item> craftableResults = new HashSet<>();
        Set<String> emptyResultMods = new HashSet<>();
        Map<String, ItemStack> stackByKey = new LinkedHashMap<>();
        Map<String, List<ItemStack>> stacksByNs = new LinkedHashMap<>();

        List<RecipeHolder<?>> holders = new ArrayList<>(manager.getRecipes());
        holders.addAll(BrewingRecipes.collect(brewing));

        for (RecipeHolder<?> holder : holders) {
            Recipe<?> recipe = holder.value();
            ItemStack result = IngredientExtractor.result(recipe, access);
            if (result.isEmpty() && IngredientExtractor.items(recipe).isEmpty()) {
                continue;
            }
            ResourceLocation id = holder.id();
            ids.put(id, holder);
            String recipeNs = id.getNamespace();
            if (!"minecraft".equals(recipeNs)) {
                mods.computeIfAbsent(recipeNs, ns -> new ArrayList<>()).add(holder);
            }
            for (Item item : IngredientExtractor.unlockItems(recipe, access)) {
                ingredients.computeIfAbsent(item, k -> new ArrayList<>()).add(holder);
            }
            if (recipe instanceof BrewingMixRecipe mix) {
                knownKeys.computeIfAbsent(PotionKeys.knownId(mix.input()), k -> new ArrayList<>()).add(holder);
                if (!result.isEmpty()) {
                    knownKeys.computeIfAbsent(PotionKeys.knownId(result), k -> new ArrayList<>()).add(holder);
                }
            }
            boolean usable = IngredientExtractor.hasInputs(recipe)
                    && (result.isEmpty() || result.getItem() != Items.BARRIER);
            if (usable && !"minecraft".equals(recipeNs) && !"neoforge".equals(recipeNs)
                    && !"modrecipebook".equals(recipeNs)) {
                craftable.add(recipeNs);
                if (result.isEmpty()) {
                    emptyResultMods.add(recipeNs);
                    resultMods.computeIfAbsent(recipeNs, ns -> new ArrayList<>()).add(holder);
                }
            }
            if (!result.isEmpty()) {
                results.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(holder);
                String itemNs = BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace();
                if (!"neoforge".equals(itemNs) && !"modrecipebook".equals(itemNs)) {
                    resultMods.computeIfAbsent(itemNs, ns -> new ArrayList<>()).add(holder);
                    if (usable) {
                        craftable.add(itemNs);
                        craftableResults.add(result.getItem());
                        String key = PotionKeys.itemKey(result);
                        if (stackByKey.putIfAbsent(key, result.copy()) == null) {
                            stacksByNs.computeIfAbsent(itemNs, ns -> new ArrayList<>()).add(result.copy());
                        }
                    }
                }
            }
        }
        if (!emptyResultMods.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (emptyResultMods.contains(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                    craftableResults.add(item);
                }
            }
        }

        mods.values().forEach(list -> list.sort((a, b) -> a.id().compareTo(b.id())));
        resultMods.values().forEach(list -> list.sort((a, b) -> a.id().compareTo(b.id())));
        byMod = Map.copyOf(mods);
        byResultMod = Map.copyOf(resultMods);
        byIngredient = Map.copyOf(ingredients);
        byResult = Map.copyOf(results);
        byKnownKey = Map.copyOf(knownKeys);
        byId = Map.copyOf(ids);
        craftableMods = Set.copyOf(craftable);
        craftableItems = Set.copyOf(craftableResults);
        Map<String, List<ItemStack>> stackCopy = new HashMap<>();
        stacksByNs.forEach((ns, list) -> stackCopy.put(ns, List.copyOf(list)));
        craftableStacks = Map.copyOf(stackCopy);
        allCraftableStacks = List.copyOf(stackByKey.values());
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

    public static boolean hasCraftableItem(String namespace) {
        return craftableMods.contains(namespace);
    }

    public static boolean isCraftableResult(Item item) {
        return craftableItems.contains(item);
    }

    public static List<ItemStack> craftableStacks(String namespace) {
        return craftableStacks.getOrDefault(namespace, List.of());
    }

    public static List<ItemStack> allCraftableStacks() {
        return allCraftableStacks;
    }

    public static boolean recipesIndexed() {
        return !byResult.isEmpty();
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

    public static List<RecipeHolder<?>> byKnownKey(ResourceLocation key) {
        return byKnownKey.getOrDefault(key, List.of());
    }

    public static Optional<RecipeHolder<?>> byId(ResourceLocation id) {
        return Optional.ofNullable(byId.get(id));
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
