package com.minemod.modrecipebook.client;

import net.minecraft.client.Minecraft;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RecipeBookmarks {
    private static final Set<String> KEYS = new LinkedHashSet<>();

    private RecipeBookmarks() {}

    public static void replace(Collection<String> keys) {
        KEYS.clear();
        KEYS.addAll(keys);
        ModRecipeBookComponent book = ModRecipeBookScreens.component(Minecraft.getInstance().screen);
        if (book != null) {
            book.bookmarksUpdated();
        }
    }

    public static boolean contains(String key) {
        return KEYS.contains(key);
    }

    public static boolean isEmpty() {
        return KEYS.isEmpty();
    }
}
