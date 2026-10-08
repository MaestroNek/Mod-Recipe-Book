package com.minemod.modrecipebook.platform;

import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.tags.TagManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Platform {
    @ExpectPlatform
    public static Path configDir() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoaded(String id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String modName(String modId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<String> loadedModIds() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Set<ResourceLocation> unlocked(ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setUnlocked(ServerPlayer player, Set<ResourceLocation> value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Set<ResourceLocation> knownItems(ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setKnownItems(ServerPlayer player, Set<ResourceLocation> value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Set<String> bookmarks(ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setBookmarks(ServerPlayer player, Set<String> value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean vanillaImported(ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setVanillaImported(ServerPlayer player, boolean value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean absorbFluids(Object value, Set<ResourceLocation> out) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int bucketVolume() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canFill(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static FluidFill tryFill(ItemStack empty, Fluid fluid) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Fluid drainFluid(Item item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<PackResources> installedDatapacks() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void injectRecipeConditions(RecipeManager recipes, TagManager tags, RegistryAccess access) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addModBrews(PotionBrewing brewing, Map<ResourceLocation, RecipeHolder<BrewingMixRecipe>> out) {
        throw new AssertionError();
    }

    private Platform() {}
}
