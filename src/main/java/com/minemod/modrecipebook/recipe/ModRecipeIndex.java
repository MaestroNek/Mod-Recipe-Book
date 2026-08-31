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
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static Map<ResourceLocation, List<FluidProducer>> fluidProducers = Map.of();
    private static Map<ResourceLocation, Set<Item>> fluidContainers = Map.of();
    private static Map<RecipeType<?>, List<Set<Item>>> stationsByType = Map.of();
    private static Map<RecipeType<?>, List<Set<Item>>> catalystStations = Map.of();
    private static boolean stationsReady = true;
    private static int stationsGeneration;

    public record FluidProducer(Set<Item> items, Set<ResourceLocation> fluids, Recipe<?> recipe) {
        public FluidProducer(Set<Item> items, Set<ResourceLocation> fluids, Recipe<?> recipe) {
            this.items = Set.copyOf(items);
            this.fluids = Set.copyOf(fluids);
            this.recipe = recipe;
        }
    }

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
        Map<String, ItemStack> stackByKey = new LinkedHashMap<>();
        Map<String, List<ItemStack>> stacksByNs = new LinkedHashMap<>();
        Map<ResourceLocation, LinkedHashSet<FluidProducer>> producers = new HashMap<>();
        Map<ResourceLocation, LinkedHashSet<Item>> containers = new HashMap<>();
        Map<RecipeType<?>, LinkedHashSet<Item>> toasts = new HashMap<>();

        List<RecipeHolder<?>> holders = new ArrayList<>(manager.getRecipes());
        holders.addAll(BrewingRecipes.collect(brewing));
        holders.addAll(ContainerFillRecipes.collect(holders, access));
        ContainerEmptyRecipes.addContainers(containers);

        for (RecipeHolder<?> holder : holders) {
            Recipe<?> recipe = holder.value();
            Set<Item> itemInputs = IngredientExtractor.itemIngredients(recipe);
            Set<ResourceLocation> fluidInputs = IngredientExtractor.fluidInputs(recipe);
            if (itemInputs.isEmpty() && fluidInputs.isEmpty()) {
                continue;
            }
            FluidProducer producer = new FluidProducer(itemInputs, fluidInputs, recipe);
            for (ResourceLocation fluid : IngredientExtractor.fluidOutputs(recipe)) {
                producers.computeIfAbsent(fluid, k -> new LinkedHashSet<>()).add(producer);
            }
        }
        for (RecipeHolder<?> holder : holders) {
            Recipe<?> recipe = holder.value();
            Item toast = toastItem(recipe);
            if (toast != null && vanillaStations(recipe.getType()) == null) {
                toasts.computeIfAbsent(recipe.getType(), k -> new LinkedHashSet<>()).add(toast);
            }
        }

        Map<RecipeType<?>, List<Set<Item>>> stations = new HashMap<>();
        for (RecipeHolder<?> holder : holders) {
            RecipeType<?> type = holder.value().getType();
            if (stations.containsKey(type)) {
                continue;
            }
            List<Set<Item>> vanilla = vanillaStations(type);
            if (vanilla != null) {
                stations.put(type, vanilla);
                continue;
            }
            LinkedHashSet<Item> icons = toasts.get(type);
            if (icons == null || icons.isEmpty()) {
                continue;
            }
            List<Set<Item>> alts = new ArrayList<>();
            for (Item item : icons) {
                alts.add(Set.of(item));
            }
            stations.put(type, List.copyOf(alts));
        }
        stationsByType = Map.copyOf(stations);

        for (RecipeHolder<?> holder : holders) {
            Recipe<?> recipe = holder.value();
            ItemStack result = IngredientExtractor.result(recipe, access);
            if (result.isEmpty()) {
                continue;
            }
            ResourceLocation id = holder.id();
            ids.put(id, holder);
            String recipeNs = id.getNamespace();
            if (!"minecraft".equals(recipeNs)) {
                mods.computeIfAbsent(recipeNs, ns -> new ArrayList<>()).add(holder);
            }
            Set<Item> needed = new HashSet<>();
            collectRecipeItems(recipe, needed, producers, containers, new HashSet<>());
            collectStationItems(recipe, needed);
            if (needed.isEmpty()) {
                needed.addAll(IngredientExtractor.unlockItems(recipe, access));
            }
            for (Item item : needed) {
                ingredients.computeIfAbsent(item, k -> new ArrayList<>()).add(holder);
            }
            if (recipe instanceof BrewingMixRecipe mix) {
                knownKeys.computeIfAbsent(PotionKeys.knownId(mix.input()), k -> new ArrayList<>()).add(holder);
                knownKeys.computeIfAbsent(PotionKeys.knownId(result), k -> new ArrayList<>()).add(holder);
            }
            boolean usable = IngredientExtractor.hasInputs(recipe) && result.getItem() != Items.BARRIER;
            results.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(holder);
            String itemNs = BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace();
            if (!"neoforge".equals(itemNs) && !"modrecipebook".equals(itemNs)) {
                resultMods.computeIfAbsent(itemNs, ns -> new ArrayList<>()).add(holder);
                if (usable) {
                    craftable.add(itemNs);
                    if (!"minecraft".equals(recipeNs) && !"neoforge".equals(recipeNs)
                            && !"modrecipebook".equals(recipeNs)) {
                        craftable.add(recipeNs);
                    }
                    craftableResults.add(result.getItem());
                    String key = PotionKeys.itemKey(result);
                    if (stackByKey.putIfAbsent(key, result.copy()) == null) {
                        stacksByNs.computeIfAbsent(itemNs, ns -> new ArrayList<>()).add(result.copy());
                    }
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
        Map<ResourceLocation, List<FluidProducer>> producerCopy = new HashMap<>();
        producers.forEach((fluid, options) -> producerCopy.put(fluid, List.copyOf(options)));
        fluidProducers = Map.copyOf(producerCopy);
        Map<ResourceLocation, Set<Item>> containerCopy = new HashMap<>();
        containers.forEach((fluid, items) -> containerCopy.put(fluid, Set.copyOf(items)));
        fluidContainers = Map.copyOf(containerCopy);
        craftableMods = Set.copyOf(craftable);
        craftableItems = Set.copyOf(craftableResults);
        Map<String, List<ItemStack>> stackCopy = new HashMap<>();
        stacksByNs.forEach((ns, list) -> stackCopy.put(ns, List.copyOf(list)));
        craftableStacks = Map.copyOf(stackCopy);
        allCraftableStacks = List.copyOf(stackByKey.values());
        List<String> order = new ArrayList<>(mods.keySet());
        Collections.sort(order);
        modOrder = List.copyOf(order);
        stationsReady = !ModList.get().isLoaded("jei") || !catalystStations.isEmpty();
        applyCatalystStations();
        stationsGeneration++;
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

    public static boolean allUnlockItemsKnown(RecipeHolder<?> holder, Set<Item> known, Set<ResourceLocation> knownIds) {
        if (holder.value() instanceof BrewingMixRecipe) {
            return IngredientExtractor.allIngredientsKnown(holder.value(), known, knownIds);
        }
        return recipeInputsKnown(holder.value(), known, new HashSet<>());
    }

    public static boolean stationKnown(RecipeHolder<?> holder, Set<Item> known) {
        return stationKnown(holder.value(), known);
    }

    private static boolean stationKnown(Recipe<?> recipe, Set<Item> known) {
        List<Recipe<?>> steps = IngredientExtractor.sequencedSteps(recipe);
        if (!steps.isEmpty()) {
            for (Recipe<?> step : steps) {
                if (!typeStationKnown(step, known)) {
                    return false;
                }
            }
            return true;
        }
        return typeStationKnown(recipe, known);
    }

    private static boolean typeStationKnown(Recipe<?> recipe, Set<Item> known) {
        if (recipe.getType() == ContainerFillRecipe.TYPE) {
            return fillStationKnown(known);
        }
        if (stationless(recipe)) {
            return true;
        }
        List<Set<Item>> alts = stationsByType.get(recipe.getType());
        if (alts == null || alts.isEmpty()) {
            return stationsReady && IngredientExtractor.declaredItemSlots(recipe) >= 2;
        }
        return anyAltKnown(alts, known);
    }

    private static boolean fillStationKnown(Set<Item> known) {
        RecipeType<?> filling = BuiltInRegistries.RECIPE_TYPE
                .get(ResourceLocation.fromNamespaceAndPath("create", "filling"));
        if (filling == null) {
            return true;
        }
        List<Set<Item>> alts = stationsByType.get(filling);
        if (alts == null || alts.isEmpty()) {
            return stationsReady;
        }
        return anyAltKnown(alts, known);
    }

    private static boolean anyAltKnown(List<Set<Item>> alts, Set<Item> known) {
        for (Set<Item> alt : alts) {
            if (known.containsAll(alt)) {
                return true;
            }
        }
        return false;
    }

    private static boolean recipeInputsKnown(Recipe<?> recipe, Set<Item> known, Set<ResourceLocation> visiting) {
        if (!IngredientExtractor.allIngredientsKnown(recipe, known)) {
            return false;
        }
        for (ResourceLocation fluid : IngredientExtractor.fluidInputs(recipe)) {
            if (!fluidKnown(fluid, known, visiting)) {
                return false;
            }
        }
        return true;
    }

    private static boolean fluidKnown(ResourceLocation fluid, Set<Item> known, Set<ResourceLocation> visiting) {
        if (!visiting.add(fluid)) {
            return true;
        }
        Set<Item> held = fluidContainers.get(fluid);
        List<FluidProducer> options = fluidProducers.get(fluid);
        if ((options == null || options.isEmpty()) && (held == null || held.isEmpty())) {
            return true;
        }
        if (held != null) {
            for (Item item : held) {
                if (known.contains(item)) {
                    return true;
                }
            }
        }
        if (options != null) {
            for (FluidProducer option : options) {
                if (producerKnown(option, known, visiting)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean producerKnown(FluidProducer option, Set<Item> known, Set<ResourceLocation> visiting) {
        if (!known.containsAll(option.items())) {
            return false;
        }
        if (UnlockOptions.requireCraftingMethod && !stationKnown(option.recipe(), known)) {
            return false;
        }
        for (ResourceLocation fluid : option.fluids()) {
            if (!fluidKnown(fluid, known, visiting)) {
                return false;
            }
        }
        return true;
    }

    private static void collectRecipeItems(Recipe<?> recipe, Set<Item> out,
                                           Map<ResourceLocation, LinkedHashSet<FluidProducer>> producers,
                                           Map<ResourceLocation, LinkedHashSet<Item>> containers,
                                           Set<ResourceLocation> visiting) {
        out.addAll(IngredientExtractor.itemIngredients(recipe));
        for (ResourceLocation fluid : IngredientExtractor.fluidInputs(recipe)) {
            collectFluidItems(fluid, out, producers, containers, visiting);
        }
    }

    private static void collectFluidItems(ResourceLocation fluid, Set<Item> out,
                                          Map<ResourceLocation, LinkedHashSet<FluidProducer>> producers,
                                          Map<ResourceLocation, LinkedHashSet<Item>> containers,
                                          Set<ResourceLocation> visiting) {
        if (!visiting.add(fluid)) {
            return;
        }
        Set<Item> held = containers.get(fluid);
        if (held != null) {
            out.addAll(held);
        }
        for (FluidProducer option : producers.getOrDefault(fluid, new LinkedHashSet<>())) {
            out.addAll(option.items());
            for (ResourceLocation nested : option.fluids()) {
                collectFluidItems(nested, out, producers, containers, visiting);
            }
        }
    }

    private static void collectStationItems(Recipe<?> recipe, Set<Item> out) {
        List<Recipe<?>> steps = IngredientExtractor.sequencedSteps(recipe);
        if (!steps.isEmpty()) {
            for (Recipe<?> step : steps) {
                addTypeStations(step, out);
            }
            return;
        }
        addTypeStations(recipe, out);
    }

    public static void addCatalystStations(RecipeType<?> type, Collection<Item> items) {
        if (vanillaStations(type) != null) {
            return;
        }
        List<Set<Item>> alts = new ArrayList<>();
        for (Item item : items) {
            if (item != null && item != Items.AIR && item != Items.CRAFTING_TABLE) {
                alts.add(Set.of(item));
            }
        }
        if (alts.isEmpty()) {
            return;
        }
        Map<RecipeType<?>, List<Set<Item>>> next = new HashMap<>(catalystStations);
        next.put(type, List.copyOf(alts));
        catalystStations = Map.copyOf(next);
        applyCatalystStations();
        stationsGeneration++;
    }

    private static void applyCatalystStations() {
        if (catalystStations.isEmpty()) {
            return;
        }
        Map<RecipeType<?>, List<Set<Item>>> stations = new HashMap<>(stationsByType);
        stations.putAll(catalystStations);
        stationsByType = Map.copyOf(stations);
        Map<Item, List<RecipeHolder<?>>> ingredients = new HashMap<>();
        byIngredient.forEach((item, list) -> ingredients.put(item, new ArrayList<>(list)));
        for (RecipeHolder<?> holder : byId.values()) {
            List<Set<Item>> alts = catalystStations.get(holder.value().getType());
            if (alts == null || stationless(holder.value())) {
                continue;
            }
            for (Set<Item> alt : alts) {
                for (Item item : alt) {
                    List<RecipeHolder<?>> list = ingredients.computeIfAbsent(item, k -> new ArrayList<>());
                    if (!list.contains(holder)) {
                        list.add(holder);
                    }
                }
            }
        }
        byIngredient = Map.copyOf(ingredients);
    }

    public static void markStationsReady() {
        stationsReady = true;
        stationsGeneration++;
    }

    public static boolean stationsReady() {
        return stationsReady;
    }

    public static int stationsGeneration() {
        return stationsGeneration;
    }

    public static boolean hasFixedStations(RecipeType<?> type) {
        return vanillaStations(type) != null;
    }

    public static Set<Item> stationItems() {
        Set<Item> items = new HashSet<>();
        for (List<Set<Item>> alts : stationsByType.values()) {
            for (Set<Item> alt : alts) {
                items.addAll(alt);
            }
        }
        return items;
    }

    public static Collection<RecipeHolder<?>> indexed() {
        return byId.values();
    }

    private static boolean stationless(Recipe<?> recipe) {
        RecipeType<?> type = recipe.getType();
        if (type == ContainerFillRecipe.TYPE) {
            return true;
        }
        return type == RecipeType.CRAFTING && recipe.canCraftInDimensions(2, 2);
    }

    private static void addTypeStations(Recipe<?> recipe, Set<Item> out) {
        if (stationless(recipe)) {
            return;
        }
        List<Set<Item>> alts = stationsByType.get(recipe.getType());
        if (alts == null) {
            return;
        }
        for (Set<Item> alt : alts) {
            out.addAll(alt);
        }
    }

    private static List<Set<Item>> vanillaStations(RecipeType<?> type) {
        if (type == RecipeType.CRAFTING) {
            return List.of(Set.of(Items.CRAFTING_TABLE));
        }
        if (type == RecipeType.SMELTING) {
            return List.of(Set.of(Items.FURNACE));
        }
        if (type == RecipeType.BLASTING) {
            return List.of(Set.of(Items.BLAST_FURNACE));
        }
        if (type == RecipeType.SMOKING) {
            return List.of(Set.of(Items.SMOKER));
        }
        if (type == RecipeType.CAMPFIRE_COOKING) {
            return List.of(Set.of(Items.CAMPFIRE));
        }
        if (type == RecipeType.STONECUTTING) {
            return List.of(Set.of(Items.STONECUTTER));
        }
        if (type == RecipeType.SMITHING) {
            return List.of(Set.of(Items.SMITHING_TABLE));
        }
        if (type == BrewingMixRecipe.TYPE) {
            return List.of(Set.of(Items.BREWING_STAND));
        }
        return null;
    }

    private static Item toastItem(Recipe<?> recipe) {
        try {
            ItemStack toast = recipe.getToastSymbol();
            if (toast == null || toast.isEmpty()) {
                return null;
            }
            Item item = toast.getItem();
            if (item == Items.CRAFTING_TABLE && recipe.getType() != RecipeType.CRAFTING) {
                return null;
            }
            return item;
        } catch (Exception ignored) {
            return null;
        }
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
