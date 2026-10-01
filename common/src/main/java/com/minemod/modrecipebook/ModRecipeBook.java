package com.minemod.modrecipebook;

import com.minemod.modrecipebook.client.ClientUnlockedRecipes;
import com.minemod.modrecipebook.client.RecipeCategoryConfig;
import com.minemod.modrecipebook.net.ModNetworking;
import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.client.ClientRecipeUpdateEvent;
import org.slf4j.Logger;

public class ModRecipeBook {
    public static final String MODID = "modrecipebook";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        ModNetworking.init();
        // Static init registers these. Must run before the recipe type registry freezes.
        BrewingMixRecipe.TYPE.toString();
    }

    public static void initClient() {
        RecipeCategoryConfig.load();
        ModNetworking.initClient();
        ClientRecipeUpdateEvent.EVENT.register(manager -> ClientUnlockedRecipes.onRecipesUpdated());
    }
}
