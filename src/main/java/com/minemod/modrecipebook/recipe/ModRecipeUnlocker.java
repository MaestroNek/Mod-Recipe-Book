package com.minemod.modrecipebook.recipe;

import com.minemod.modrecipebook.net.UnlockRecipesPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = com.minemod.modrecipebook.ModRecipeBook.MODID)
public final class ModRecipeUnlocker {
    private ModRecipeUnlocker() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();
        ModRecipeIndex.rebuild(server.getRecipeManager(), server.registryAccess());
        if (event.getPlayer() != null) {
            login(event.getPlayer());
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                login(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        checkInventory(player, true);
    }

    private static void login(ServerPlayer player) {
        syncAll(player);
        checkInventory(player, true);
    }

    public static void syncAll(ServerPlayer player) {
        Set<ResourceLocation> unlocked = player.getData(ModRecipeBookAttachments.UNLOCKED);
        PacketDistributor.sendToPlayer(player, new UnlockRecipesPayload(List.copyOf(unlocked), true));
    }

    public static void checkInventory(ServerPlayer player, boolean toast) {
        Set<ResourceLocation> unlocked = player.getData(ModRecipeBookAttachments.UNLOCKED);
        Set<ResourceLocation> knownIds = player.getData(ModRecipeBookAttachments.KNOWN_ITEMS);
        List<ResourceLocation> newly = new ArrayList<>();
        List<Item> newlyKnown = new ArrayList<>();
        Inventory inventory = player.getInventory();
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !seen.add(stack.getItem())) {
                continue;
            }
            Item item = stack.getItem();
            if (knownIds.add(BuiltInRegistries.ITEM.getKey(item))) {
                newlyKnown.add(item);
            }
        }
        if (newlyKnown.isEmpty()) {
            return;
        }
        player.setData(ModRecipeBookAttachments.KNOWN_ITEMS, knownIds);
        Set<Item> knownItems = knownItemSet(knownIds);
        boolean requireAll = UnlockOptions.requireAllIngredients;
        for (Item item : newlyKnown) {
            for (RecipeHolder<?> holder : ModRecipeIndex.byIngredient(item)) {
                if (requireAll && !IngredientExtractor.allIngredientsKnown(holder.value(), knownItems)) {
                    continue;
                }
                if (unlocked.add(holder.id())) {
                    newly.add(holder.id());
                }
            }
            for (RecipeHolder<?> holder : ModRecipeIndex.byResult(item)) {
                if (unlocked.add(holder.id())) {
                    newly.add(holder.id());
                }
            }
        }
        if (!newly.isEmpty()) {
            player.setData(ModRecipeBookAttachments.UNLOCKED, unlocked);
            if (toast) {
                PacketDistributor.sendToPlayer(player, new UnlockRecipesPayload(newly, false));
            }
        }
    }

    private static Set<Item> knownItemSet(Set<ResourceLocation> knownIds) {
        Set<Item> items = new HashSet<>();
        for (ResourceLocation id : knownIds) {
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                items.add(BuiltInRegistries.ITEM.get(id));
            }
        }
        return items;
    }
}
