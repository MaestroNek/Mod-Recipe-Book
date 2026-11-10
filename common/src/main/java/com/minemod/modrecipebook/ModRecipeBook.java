package com.minemod.modrecipebook;

import com.minemod.modrecipebook.net.ModNetworking;
import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import com.minemod.modrecipebook.recipe.UnlockOptions;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ModRecipeBook {
    public static final String MODID = "modrecipebook";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        UnlockOptions.load();
        ModNetworking.init();
        // Static init registers these. Must run before the recipe type registry freezes.
        BrewingMixRecipe.TYPE.toString();
    }
}
