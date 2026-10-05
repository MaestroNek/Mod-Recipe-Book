package com.minemod.modrecipebook.recipe;

import com.google.gson.Gson;
import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.platform.Platform;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UnlockOptions {
    public static boolean requireAllIngredients = true;
    public static boolean requireCraftingMethod = true;

    private UnlockOptions() {}

    public static void load() {
        Path file = Platform.configDir().resolve("modrecipebook-categories.json");
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            FileData data = new Gson().fromJson(reader, FileData.class);
            if (data == null) {
                return;
            }
            if (data.requireAllIngredients != null) {
                requireAllIngredients = data.requireAllIngredients;
            }
            if (data.requireCraftingMethod != null) {
                requireCraftingMethod = data.requireCraftingMethod;
            }
        } catch (Exception e) {
            ModRecipeBook.LOGGER.warn("Failed to read unlock options", e);
        }
    }

    private static class FileData {
        Boolean requireAllIngredients;
        Boolean requireCraftingMethod;
    }
}
