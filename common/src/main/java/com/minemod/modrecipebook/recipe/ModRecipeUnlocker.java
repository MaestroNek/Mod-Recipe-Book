package com.minemod.modrecipebook.recipe;

import com.minemod.modrecipebook.net.BookmarkSyncPayload;
import com.minemod.modrecipebook.net.UnlockRecipesPayload;
import com.minemod.modrecipebook.net.UnlockRulesPayload;
import com.minemod.modrecipebook.net.DebugUnlockPayload;
import com.minemod.modrecipebook.platform.Platform;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ModRecipeUnlocker {
    private static final Map<ServerPlayer, Integer> swept = new WeakHashMap<>();
    private static final Map<ServerPlayer, Boolean> syncQueued = new WeakHashMap<>();

    private ModRecipeUnlocker() {}

    public static void onDatapackSync(MinecraftServer server, ServerPlayer only) {
        PotionBrewing brewing = server.overworld() == null ? PotionBrewing.EMPTY : server.overworld().potionBrewing();
        ModRecipeIndex.rebuild(server.getRecipeManager(), server.registryAccess(), brewing);
        if (only != null) {
            syncQueued.put(only, Boolean.TRUE);
        } else {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                syncQueued.put(player, Boolean.TRUE);
            }
        }
    }

    public static void onPlayerTick(Player entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (syncQueued.remove(player) != null) {
            if (!finishVanillaImport(player)) {
                syncQueued.put(player, Boolean.TRUE);
                return;
            }
            syncAll(player);
            NetworkManager.sendToPlayer(player, new UnlockRulesPayload(
                    UnlockOptions.requireAllIngredients, UnlockOptions.requireCraftingMethod));
            checkInventory(player, true, true);
            swept.put(player, ModRecipeIndex.stationsGeneration());
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        boolean staleStations = !Integer.valueOf(ModRecipeIndex.stationsGeneration()).equals(swept.get(player));
        checkInventory(player, true, staleStations);
    }

    private static boolean finishVanillaImport(ServerPlayer player) {
        if (Platform.vanillaImported(player)) {
            return true;
        }
        if (ModRecipeIndex.indexed().isEmpty()) {
            return false;
        }
        Set<ResourceLocation> unlocked = orderedUnlocked(player);
        var book = player.getRecipeBook();
        int added = 0;
        for (RecipeHolder<?> holder : ModRecipeIndex.indexed()) {
            if (book.contains(holder.id()) && unlocked.add(holder.id())) {
                added++;
            }
        }
        // Fabric joins before the recipe book is readable; wait and try again.
        if (added == 0 && player.tickCount < 200) {
            return false;
        }
        if (added > 0) {
            Platform.setUnlocked(player, unlocked);
        }
        Platform.setVanillaImported(player, true);
        return true;
    }

    public static void debug(ServerPlayer player, byte action) {
        switch (action) {
            case DebugUnlockPayload.DISCOVER -> discoverAll(player);
            case DebugUnlockPayload.RESET -> resetAll(player);
            case DebugUnlockPayload.RETHINK -> rethinkAll(player);
            default -> {
            }
        }
    }

    private static void discoverAll(ServerPlayer player) {
        Set<ResourceLocation> unlocked = new LinkedHashSet<>();
        for (RecipeHolder<?> holder : ModRecipeIndex.indexed()) {
            unlocked.add(holder.id());
        }
        Platform.setUnlocked(player, unlocked);
        Set<ResourceLocation> known = new HashSet<>(Platform.knownItems(player));
        for (Item station : ModRecipeIndex.stationItems()) {
            known.add(BuiltInRegistries.ITEM.getKey(station));
        }
        NetworkManager.sendToPlayer(player, new UnlockRecipesPayload(
                List.copyOf(unlocked), List.copyOf(known), true));
        player.sendSystemMessage(Component.translatable("chat.modrecipebook.debug.discover", player.getName()));
    }

    private static void resetAll(ServerPlayer player) {
        Platform.setUnlocked(player, new LinkedHashSet<>());
        Platform.setKnownItems(player, new HashSet<>());
        syncAll(player);
        player.sendSystemMessage(Component.translatable("chat.modrecipebook.debug.reset", player.getName()));
    }

    private static void rethinkAll(ServerPlayer player) {
        if (UnlockOptions.requireCraftingMethod && !ModRecipeIndex.stationsReady()) {
            player.sendSystemMessage(Component.translatable("chat.modrecipebook.debug.rethink_wait"));
            return;
        }
        Platform.setUnlocked(player, new LinkedHashSet<>());
        checkInventory(player, false, true);
        syncAll(player);
        player.sendSystemMessage(Component.translatable("chat.modrecipebook.debug.rethink", player.getName()));
    }

    public static void syncAll(ServerPlayer player) {
        pruneBookmarks(player);
        Set<ResourceLocation> unlocked = Platform.unlocked(player);
        NetworkManager.sendToPlayer(player, new UnlockRecipesPayload(
                List.copyOf(unlocked), List.copyOf(Platform.knownItems(player)), true));
        syncBookmarks(player);
    }

    public static void toggleBookmark(ServerPlayer player, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        Set<String> bookmarks = new LinkedHashSet<>(Platform.bookmarks(player));
        if (bookmarks.contains(key)) {
            bookmarks.remove(key);
        } else if (!unlockedItemKeys(player).contains(key)) {
            return;
        } else {
            bookmarks.add(key);
        }
        Platform.setBookmarks(player, bookmarks);
        syncBookmarks(player);
    }

    private static void pruneBookmarks(ServerPlayer player) {
        Set<String> bookmarks = Platform.bookmarks(player);
        if (bookmarks.isEmpty()) {
            return;
        }
        Set<String> live = unlockedItemKeys(player);
        Set<String> next = new LinkedHashSet<>();
        for (String key : bookmarks) {
            if (live.contains(key)) {
                next.add(key);
            }
        }
        if (next.size() != bookmarks.size()) {
            Platform.setBookmarks(player, next);
        }
    }

    private static void syncBookmarks(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new BookmarkSyncPayload(
                List.copyOf(Platform.bookmarks(player))));
    }

    private static Set<String> unlockedItemKeys(ServerPlayer player) {
        Set<String> keys = new HashSet<>();
        RegistryAccess access = player.registryAccess();
        for (ResourceLocation id : Platform.unlocked(player)) {
            ModRecipeIndex.byId(id).ifPresent(holder -> {
                ItemStack result = IngredientExtractor.result(holder.value(), access);
                if (!result.isEmpty()) {
                    keys.add(PotionKeys.itemKey(result));
                }
            });
        }
        return keys;
    }

    public static void checkInventory(ServerPlayer player, boolean toast, boolean force) {
        Set<ResourceLocation> unlocked = orderedUnlocked(player);
        Set<ResourceLocation> knownIds = Platform.knownItems(player);
        List<ResourceLocation> newly = new ArrayList<>();
        List<ResourceLocation> newlyKeys = new ArrayList<>();
        Inventory inventory = player.getInventory();
        Set<ResourceLocation> seen = new HashSet<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (seen.add(itemId) && knownIds.add(itemId)) {
                newlyKeys.add(itemId);
            }
            ResourceLocation potionKey = PotionKeys.knownId(stack);
            if (!potionKey.equals(itemId) && seen.add(potionKey) && knownIds.add(potionKey)) {
                newlyKeys.add(potionKey);
            }
        }
        if (!newlyKeys.isEmpty()) {
            Platform.setKnownItems(player, knownIds);
        }
        if (!force && newlyKeys.isEmpty()) {
            return;
        }
        swept.put(player, ModRecipeIndex.stationsGeneration());
        Set<Item> knownItems = knownItemSet(knownIds);
        boolean requireAll = UnlockOptions.requireAllIngredients;
        boolean requireMethod = UnlockOptions.requireCraftingMethod;
        RegistryAccess access = player.registryAccess();
        Iterable<ResourceLocation> keys = List.copyOf(knownIds);
        for (ResourceLocation key : keys) {
            if (BuiltInRegistries.ITEM.containsKey(key)) {
                Item item = BuiltInRegistries.ITEM.get(key);
                for (RecipeHolder<?> holder : ModRecipeIndex.byIngredient(item)) {
                    if (unlocked.contains(holder.id())) {
                        continue;
                    }
                    if (!smithingInputsMatch(holder.value(), knownItems, requireAll)) {
                        continue;
                    }
                    if (requireAll && !ModRecipeIndex.allUnlockItemsKnown(holder, knownItems, knownIds)) {
                        continue;
                    }
                    if (requireMethod && !ModRecipeIndex.stationKnown(holder, knownItems)) {
                        continue;
                    }
                    if (unlocked.add(holder.id())) {
                        newly.add(holder.id());
                    }
                }
                for (RecipeHolder<?> holder : ModRecipeIndex.byResult(item)) {
                    if (unlocked.contains(holder.id())) {
                        continue;
                    }
                    if (holder.value() instanceof BrewingMixRecipe) {
                        ItemStack result = IngredientExtractor.result(holder.value(), access);
                        if (!knownIds.contains(PotionKeys.knownId(result))) {
                            continue;
                        }
                    }
                    if (requireMethod && !ModRecipeIndex.stationKnown(holder, knownItems)) {
                        continue;
                    }
                    if (unlocked.add(holder.id())) {
                        newly.add(holder.id());
                    }
                }
            }
            for (RecipeHolder<?> holder : ModRecipeIndex.byKnownKey(key)) {
                if (unlocked.contains(holder.id())) {
                    continue;
                }
                ItemStack result = IngredientExtractor.result(holder.value(), access);
                boolean resultKnown = PotionKeys.knownId(result).equals(key);
                if (!resultKnown && !smithingInputsMatch(holder.value(), knownItems, requireAll)) {
                    continue;
                }
                if (!resultKnown && requireAll
                        && !ModRecipeIndex.allUnlockItemsKnown(holder, knownItems, knownIds)) {
                    continue;
                }
                if (requireMethod && !ModRecipeIndex.stationKnown(holder, knownItems)) {
                    continue;
                }
                if (unlocked.add(holder.id())) {
                    newly.add(holder.id());
                }
            }
        }
        if (!newly.isEmpty()) {
            Platform.setUnlocked(player, unlocked);
        }
        if (toast && (!newly.isEmpty() || !newlyKeys.isEmpty())) {
            NetworkManager.sendToPlayer(player, new UnlockRecipesPayload(newly, newlyKeys, false));
        }
    }

    private static Set<ResourceLocation> orderedUnlocked(ServerPlayer player) {
        Set<ResourceLocation> unlocked = Platform.unlocked(player);
        return unlocked instanceof LinkedHashSet ? unlocked : new LinkedHashSet<>(unlocked);
    }

    private static boolean smithingInputsMatch(net.minecraft.world.item.crafting.Recipe<?> recipe, Set<Item> known, boolean requireAll) {
        if (!(recipe instanceof SmithingRecipe)) {
            return true;
        }
        return requireAll
                ? IngredientExtractor.allIngredientsKnown(recipe, known)
                : IngredientExtractor.anyIngredientKnown(recipe, known);
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
