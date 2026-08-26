package com.minemod.modrecipebook.recipe;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.mixin.PotionBrewingAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BrewingRecipes {
    public static final String BOOK_KEY = "MODRECIPEBOOK_BREWING";

    private BrewingRecipes() {}

    public static List<RecipeHolder<BrewingMixRecipe>> collect(PotionBrewing brewing) {
        if (brewing == null || brewing == PotionBrewing.EMPTY) {
            return List.of();
        }
        Map<ResourceLocation, RecipeHolder<BrewingMixRecipe>> out = new LinkedHashMap<>();
        PotionBrewingAccessor access = (PotionBrewingAccessor) brewing;
        List<Item> containers = containerItems(access.getContainers());
        for (Object mix : access.getPotionMixes()) {
            Holder<Potion> from = holder(mix, "from");
            Ingredient reagent = ingredient(mix);
            Holder<Potion> to = holder(mix, "to");
            if (from == null || to == null || reagent == null || reagent.isEmpty()) {
                continue;
            }
            for (Item container : containers) {
                add(out, PotionContents.createItemStack(container, from), reagent,
                        PotionContents.createItemStack(container, to));
            }
        }
        for (Object mix : access.getContainerMixes()) {
            Holder<Item> from = holder(mix, "from");
            Ingredient reagent = ingredient(mix);
            Holder<Item> to = holder(mix, "to");
            if (from == null || to == null || reagent == null || reagent.isEmpty()) {
                continue;
            }
            BuiltInRegistries.POTION.holders().forEach(potion -> {
                ResourceLocation id = potion.unwrapKey().map(key -> key.location()).orElse(null);
                if (id != null && "empty".equals(id.getPath())) {
                    return;
                }
                add(out, PotionContents.createItemStack(from.value(), potion), reagent,
                        PotionContents.createItemStack(to.value(), potion));
            });
        }
        for (IBrewingRecipe recipe : brewing.getRecipes()) {
            if (!(recipe instanceof BrewingRecipe brewingRecipe)) {
                continue;
            }
            ItemStack[] inputs = brewingRecipe.getInput().getItems();
            if (inputs.length == 0 || brewingRecipe.getIngredient().isEmpty() || brewingRecipe.getOutput().isEmpty()) {
                continue;
            }
            add(out, inputs[0], brewingRecipe.getIngredient(), brewingRecipe.getOutput());
        }
        return List.copyOf(out.values());
    }

    private static void add(Map<ResourceLocation, RecipeHolder<BrewingMixRecipe>> out,
                            ItemStack input, Ingredient reagent, ItemStack result) {
        if (input.isEmpty() || result.isEmpty()) {
            return;
        }
        ResourceLocation id = id(input, reagent, result);
        out.putIfAbsent(id, new RecipeHolder<>(id, new BrewingMixRecipe(input, reagent, result)));
    }

    private static ResourceLocation id(ItemStack input, Ingredient reagent, ItemStack result) {
        String path = "brewing/" + token(BuiltInRegistries.ITEM.getKey(input.getItem()))
                + "/" + token(PotionKeys.potionId(input), BuiltInRegistries.ITEM.getKey(input.getItem()))
                + "_" + token(firstItem(reagent))
                + "_" + token(PotionKeys.potionId(result), BuiltInRegistries.ITEM.getKey(result.getItem()));
        path = path.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, path);
    }

    private static String token(ResourceLocation id) {
        return id.getNamespace() + "/" + id.getPath().replace('/', '_');
    }

    private static String token(ResourceLocation preferred, ResourceLocation fallback) {
        return token(preferred == null ? fallback : preferred);
    }

    private static ResourceLocation firstItem(Ingredient ingredient) {
        for (ItemStack stack : ingredient.getItems()) {
            if (!stack.isEmpty()) {
                return BuiltInRegistries.ITEM.getKey(stack.getItem());
            }
        }
        return ResourceLocation.withDefaultNamespace("air");
    }

    private static List<Item> containerItems(List<Ingredient> containers) {
        List<Item> items = new ArrayList<>();
        for (Ingredient ingredient : containers) {
            for (ItemStack stack : ingredient.getItems()) {
                if (!stack.isEmpty() && !items.contains(stack.getItem())) {
                    items.add(stack.getItem());
                }
            }
        }
        if (items.isEmpty()) {
            items.add(Items.POTION);
            items.add(Items.SPLASH_POTION);
            items.add(Items.LINGERING_POTION);
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private static <T> Holder<T> holder(Object mix, String method) {
        Object value = call(mix, method);
        return value instanceof Holder<?> holder ? (Holder<T>) holder : null;
    }

    private static Ingredient ingredient(Object mix) {
        Object value = call(mix, "ingredient");
        return value instanceof Ingredient ingredient ? ingredient : null;
    }

    private static Object call(Object mix, String method) {
        try {
            return mix.getClass().getMethod(method).invoke(mix);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
