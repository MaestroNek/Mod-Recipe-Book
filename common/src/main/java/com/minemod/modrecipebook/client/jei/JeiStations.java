package com.minemod.modrecipebook.client.jei;

import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import com.minemod.modrecipebook.recipe.ModRecipeUnlocker;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JeiStations {
    private JeiStations() {}

    public static void importIfAvailable() {
        if (!ModJeiPlugin.isAvailable()) {
            return;
        }
        IRecipeManager recipes = ModJeiPlugin.runtime().getRecipeManager();
        Map<ResourceLocation, List<IRecipeCategory<?>>> categoriesById = new HashMap<>();
        for (IRecipeCategory<?> category : recipes.createRecipeCategoryLookup().get().toList()) {
            for (Object recipe : recipes.createRecipeLookup(category.getRecipeType()).get().toList()) {
                ResourceLocation id = idOf(category, recipe);
                if (id != null) {
                    categoriesById.computeIfAbsent(id, key -> new ArrayList<>()).add(category);
                }
            }
        }
        Map<RecipeType<?>, List<Item>> catalysts = new HashMap<>();
        Map<ResourceLocation, Set<Item>> byRecipe = new HashMap<>();
        categoriesById.forEach((id, categories) -> {
            LinkedHashSet<Item> items = new LinkedHashSet<>();
            for (IRecipeCategory<?> category : categories) {
                items.addAll(catalysts.computeIfAbsent(category.getRecipeType(), type ->
                        recipes.createRecipeCatalystLookup(type).getItemStack()
                                .filter(stack -> stack != null && !stack.isEmpty())
                                .map(ItemStack::getItem)
                                .toList()));
            }
            byRecipe.put(id, items);
        });
        ModRecipeIndex.setRecipeStations(byRecipe);
        ModRecipeIndex.markStationsReady();
        recheckHost();
    }

    private static void recheckHost() {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (player != null) {
            ModRecipeUnlocker.checkInventory(player, true, true);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResourceLocation idOf(IRecipeCategory category, Object recipe) {
        ResourceLocation id = category.getRegistryName(recipe);
        if (id != null) {
            return id;
        }
        if (recipe instanceof RecipeHolder<?> holder) {
            return holder.id();
        }
        return null;
    }
}
