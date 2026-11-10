package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.net.BookmarkSyncPayload;
import com.minemod.modrecipebook.net.DebugUnlockPayload;
import com.minemod.modrecipebook.net.UnlockRecipesPayload;
import com.minemod.modrecipebook.net.UnlockRulesPayload;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import com.minemod.modrecipebook.recipe.UnlockOptions;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientRecipeUpdateEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;

public final class ModRecipeBookClient {
    private static boolean applyUnlockRules;

    private ModRecipeBookClient() {}

    public static void init() {
        RecipeCategoryConfig.load();
        NetworkManager.registerReceiver(NetworkManager.s2c(), UnlockRecipesPayload.TYPE, UnlockRecipesPayload.STREAM_CODEC, ModRecipeBookClient::unlockRecipes);
        NetworkManager.registerReceiver(NetworkManager.s2c(), BookmarkSyncPayload.TYPE, BookmarkSyncPayload.STREAM_CODEC, ModRecipeBookClient::bookmarks);
        NetworkManager.registerReceiver(NetworkManager.s2c(), UnlockRulesPayload.TYPE, UnlockRulesPayload.STREAM_CODEC, ModRecipeBookClient::rules);
        ClientRecipeUpdateEvent.EVENT.register(manager -> ClientUnlockedRecipes.onRecipesUpdated());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> applyUnlockRules = true);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> CategorySettingsScreen.clearServerRules());
        ClientTickEvent.CLIENT_POST.register(ModRecipeBookClient::applyUnlockRules);
    }

    public static String currentWorldKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() == null) {
            return null;
        }
        return minecraft.getSingleplayerServer().getWorldData().getLevelName();
    }

    private static void applyUnlockRules(Minecraft minecraft) {
        if (!applyUnlockRules || minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
            return;
        }
        String world = currentWorldKey();
        if (world == null || RecipeCategoryConfig.rulesApplied(world)) {
            applyUnlockRules = false;
            return;
        }
        if (UnlockOptions.requireCraftingMethod && !ModRecipeIndex.stationsReady()) {
            return;
        }
        NetworkManager.sendToServer(new DebugUnlockPayload(DebugUnlockPayload.RETHINK));
        RecipeCategoryConfig.markRulesApplied(world);
        applyUnlockRules = false;
    }

    public static void unlockRecipes(UnlockRecipesPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> ClientUnlockedRecipes.apply(payload));
    }

    public static void bookmarks(BookmarkSyncPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> RecipeBookmarks.replace(payload.keys()));
    }

    public static void rules(UnlockRulesPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> CategorySettingsScreen.applyServerRules(payload.requireAll(), payload.requireMethod()));
    }
}
