package com.minemod.modrecipebook;

import com.minemod.modrecipebook.client.CategorySettingsScreen;
import com.minemod.modrecipebook.client.ClientUnlockedRecipes;
import com.minemod.modrecipebook.client.RecipeCategoryConfig;
import com.minemod.modrecipebook.net.DebugUnlockPayload;
import com.minemod.modrecipebook.net.ModNetworking;
import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import com.minemod.modrecipebook.recipe.UnlockOptions;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientRecipeUpdateEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class ModRecipeBook {
    public static final String MODID = "modrecipebook";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean applyUnlockRules;

    public static void init() {
        UnlockOptions.load();
        ModNetworking.init();
        // Static init registers these. Must run before the recipe type registry freezes.
        BrewingMixRecipe.TYPE.toString();
    }

    public static void initClient() {
        RecipeCategoryConfig.load();
        ModNetworking.initClient();
        ClientRecipeUpdateEvent.EVENT.register(manager -> ClientUnlockedRecipes.onRecipesUpdated());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> applyUnlockRules = true);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> CategorySettingsScreen.clearServerRules());
        ClientTickEvent.CLIENT_POST.register(client -> applyUnlockRules(client));
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
}
