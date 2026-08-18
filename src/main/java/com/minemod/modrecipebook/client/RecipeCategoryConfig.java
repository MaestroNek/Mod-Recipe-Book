package com.minemod.modrecipebook.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import com.minemod.modrecipebook.recipe.UnlockOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RecipeCategoryConfig {
    public static final int MAX = 5;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("modrecipebook-categories.json");
    private static final List<Entry> CATEGORIES = new ArrayList<>();
    private static final Set<Item> BROKEN_ICONS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean hideJei = true;
    private static boolean hideVanillaBook = true;
    private static boolean requireAllIngredients = true;
    private static final int DEFAULTS = 1;

    private RecipeCategoryConfig() {}

    public static List<Entry> all() {
        return CATEGORIES;
    }

    public static Entry find(String id) {
        for (Entry entry : CATEGORIES) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }

    public static boolean hideJei() {
        return hideJei;
    }

    public static void setHideJei(boolean value) {
        hideJei = value;
        save();
    }

    public static boolean hideVanillaBook() {
        return hideVanillaBook;
    }

    public static void setHideVanillaBook(boolean value) {
        hideVanillaBook = value;
        save();
    }

    public static boolean requireAllIngredients() {
        return requireAllIngredients;
    }

    public static void setRequireAllIngredients(boolean value) {
        requireAllIngredients = value;
        UnlockOptions.requireAllIngredients = value;
        save();
    }

    public static void load() {
        CATEGORIES.clear();
        hideJei = true;
        hideVanillaBook = true;
        requireAllIngredients = true;
        try {
            if (!Files.exists(FILE)) {
                CATEGORIES.add(defaultMinecraft());
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(FILE)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                if (data == null) {
                    CATEGORIES.add(defaultMinecraft());
                    save();
                    return;
                }
                boolean migrate = data.defaults == null || data.defaults < DEFAULTS;
                if (data.hideJei != null) {
                    hideJei = data.hideJei;
                }
                if (migrate) {
                    hideVanillaBook = true;
                } else if (data.hideVanillaBook != null) {
                    hideVanillaBook = data.hideVanillaBook;
                }
                if (data.requireAllIngredients != null) {
                    requireAllIngredients = data.requireAllIngredients;
                }
                if (data.categories != null) {
                    for (Entry entry : data.categories) {
                        if (entry == null) {
                            continue;
                        }
                        if (entry.id == null || entry.id.isBlank()) {
                            entry.id = newId();
                        }
                        if (entry.icon == null || entry.icon.isBlank()) {
                            entry.icon = "minecraft:book";
                        }
                        if (entry.mods == null) {
                            entry.mods = new ArrayList<>();
                        }
                        if (entry.name == null) {
                            entry.name = "";
                        }
                        CATEGORIES.add(entry);
                        if (CATEGORIES.size() >= MAX) {
                            break;
                        }
                    }
                }
                if (migrate) {
                    ensureMinecraftCategory();
                    save();
                }
            }
        } catch (Exception e) {
            ModRecipeBook.LOGGER.warn("Failed to read category config", e);
        } finally {
            UnlockOptions.requireAllIngredients = requireAllIngredients;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                FileData data = new FileData();
                data.hideJei = hideJei;
                data.hideVanillaBook = hideVanillaBook;
                data.requireAllIngredients = requireAllIngredients;
                data.defaults = DEFAULTS;
                data.categories = CATEGORIES;
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            ModRecipeBook.LOGGER.warn("Failed to write category config", e);
        }
    }

    public static void delete(String id) {
        CATEGORIES.removeIf(entry -> entry.id.equals(id));
        save();
    }

    public static void move(String id, int delta) {
        int from = -1;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (CATEGORIES.get(i).id.equals(id)) {
                from = i;
                break;
            }
        }
        int to = from + delta;
        if (from < 0 || to < 0 || to >= CATEGORIES.size()) {
            return;
        }
        Collections.swap(CATEGORIES, from, to);
        save();
    }

    public static void update(Entry edited) {
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (CATEGORIES.get(i).id.equals(edited.id)) {
                CATEGORIES.set(i, edited);
                save();
                return;
            }
        }
    }

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String modName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(modId);
    }

    public static void renderItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        Item item = stack.getItem();
        if (BROKEN_ICONS.contains(item)) {
            graphics.fill(x, y, x + 16, y + 16, 0xFF3F3F3F);
            return;
        }
        try {
            graphics.renderItem(stack, x, y);
        } catch (Throwable ignored) {
            // ponytail: Create Chromatic Compound tints via player; null on the mods menu
            BROKEN_ICONS.add(item);
            graphics.fill(x, y, x + 16, y + 16, 0xFF3F3F3F);
        }
    }

    private static void ensureMinecraftCategory() {
        if (find("minecraft") != null || CATEGORIES.size() >= MAX) {
            return;
        }
        CATEGORIES.add(0, defaultMinecraft());
    }

    private static Entry defaultMinecraft() {
        Entry entry = new Entry();
        entry.id = "minecraft";
        entry.name = "Minecraft";
        entry.icon = "minecraft:grass_block";
        entry.mods = new ArrayList<>(List.of("minecraft"));
        return entry;
    }

    private static class FileData {
        Boolean hideJei;
        Boolean hideVanillaBook;
        Boolean requireAllIngredients;
        Integer defaults;
        List<Entry> categories;
    }

    public static final class Entry {
        public String id = "";
        public String name = "";
        public String icon = "minecraft:book";
        public List<String> mods = new ArrayList<>();

        public ItemStack iconStack() {
            ResourceLocation location = ResourceLocation.tryParse(icon);
            if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
                return new ItemStack(Items.BOOK);
            }
            return new ItemStack(BuiltInRegistries.ITEM.get(location));
        }

        public Component title() {
            if (name != null && !name.isBlank()) {
                return Component.literal(name.trim());
            }
            if (mods.isEmpty()) {
                return Component.translatable("gui.modrecipebook.category.empty");
            }
            List<String> names = new ArrayList<>();
            for (String mod : mods) {
                names.add(modName(mod));
            }
            return Component.literal(String.join(", ", names));
        }

        public List<RecipeHolder<?>> recipes() {
            List<RecipeHolder<?>> out = new ArrayList<>();
            for (String mod : mods) {
                out.addAll(ModRecipeIndex.recipesByItemMod(mod));
            }
            return out;
        }

        public Entry copy() {
            Entry copy = new Entry();
            copy.id = id;
            copy.name = name;
            copy.icon = icon;
            copy.mods = new ArrayList<>(mods);
            return copy;
        }
    }
}
