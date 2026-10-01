package com.minemod.modrecipebook.client.jei;

import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import com.minemod.modrecipebook.recipe.ModRecipeUnlocker;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class JeiStations {
    private JeiStations() {}

    public static void importIfAvailable() {
        if (!ModJeiPlugin.isAvailable()) {
            return;
        }
        IRecipeManager recipes = ModJeiPlugin.runtime().getRecipeManager();
        Map<ResourceLocation, IRecipeCategory<?>> categoryById = new HashMap<>();
        for (IRecipeCategory<?> category : recipes.createRecipeCategoryLookup().get().toList()) {
            for (Object recipe : recipes.createRecipeLookup(category.getRecipeType()).get().toList()) {
                ResourceLocation id = idOf(category, recipe);
                if (id != null) {
                    categoryById.putIfAbsent(id, category);
                }
            }
        }
        Map<RecipeType<?>, Set<Item>> byType = new HashMap<>();
        for (RecipeHolder<?> holder : ModRecipeIndex.indexed()) {
            RecipeType<?> type = holder.value().getType();
            if (ModRecipeIndex.hasFixedStations(type) || byType.containsKey(type)) {
                continue;
            }
            IRecipeCategory<?> category = categoryById.get(holder.id());
            if (category == null) {
                byType.put(type, Set.of());
                continue;
            }
            LinkedHashSet<Item> items = new LinkedHashSet<>();
            recipes.createRecipeCatalystLookup(category.getRecipeType()).getItemStack().forEach(stack -> {
                if (stack != null && !stack.isEmpty()) {
                    items.add(stack.getItem());
                }
            });
            byType.put(type, items);
            ModRecipeIndex.addCatalystStations(type, items);
        }
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
