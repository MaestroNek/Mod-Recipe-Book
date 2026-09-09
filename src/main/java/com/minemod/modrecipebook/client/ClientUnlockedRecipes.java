package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.client.jei.JeiStations;
import com.minemod.modrecipebook.net.UnlockRecipesPayload;
import com.minemod.modrecipebook.recipe.IngredientExtractor;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

import java.util.LinkedHashSet;
import java.util.Set;

@EventBusSubscriber(modid = com.minemod.modrecipebook.ModRecipeBook.MODID, value = Dist.CLIENT)
public final class ClientUnlockedRecipes {
    private static final Set<ResourceLocation> UNLOCKED = new LinkedHashSet<>();
    private static final Set<ResourceLocation> KNOWN = new LinkedHashSet<>();
    private static final Set<ResourceLocation> METHODS = new LinkedHashSet<>();
    private static boolean ownToast;

    private ClientUnlockedRecipes() {}

    public static boolean isUnlocked(ResourceLocation id) {
        return UNLOCKED.contains(id);
    }

    public static boolean isItemKnown(Item item) {
        return KNOWN.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public static void rememberMethod(ResourceLocation uid) {
        if (uid != null) {
            METHODS.add(uid);
        }
    }

    public static int methodOrder(ResourceLocation uid) {
        int i = 0;
        for (ResourceLocation id : METHODS) {
            if (id.equals(uid)) {
                return i;
            }
            i++;
        }
        return Integer.MAX_VALUE;
    }

    public static boolean showingOwnToast() {
        return ownToast;
    }

    public static void apply(UnlockRecipesPayload payload) {
        if (payload.replace()) {
            UNLOCKED.clear();
            KNOWN.clear();
            METHODS.clear();
            UNLOCKED.addAll(payload.recipes());
            KNOWN.addAll(payload.known());
            rememberRecipes(payload.recipes());
            refreshOpenBook();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean added = KNOWN.addAll(payload.known());
        for (ResourceLocation id : payload.recipes()) {
            if (!UNLOCKED.add(id)) {
                continue;
            }
            added = true;
            rememberRecipe(id);
            if (minecraft.level == null) {
                continue;
            }
            if ("minecraft".equals(id.getNamespace()) && !RecipeCategoryConfig.hideVanillaBook()) {
                continue;
            }
            ModRecipeIndex.byId(id).ifPresent(holder -> {
                if (IngredientExtractor.result(holder.value(), minecraft.level.registryAccess()).isEmpty()) {
                    return;
                }
                ownToast = true;
                try {
                    RecipeToast.addOrUpdate(minecraft.getToasts(), holder);
                } finally {
                    ownToast = false;
                }
            });
        }
        if (added) {
            refreshOpenBook();
        }
    }

    private static void rememberRecipes(Iterable<ResourceLocation> ids) {
        for (ResourceLocation id : ids) {
            rememberRecipe(id);
        }
    }

    private static void rememberRecipe(ResourceLocation id) {
        ModRecipeIndex.byId(id).ifPresent(holder ->
                rememberMethod(BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType())));
    }

    private static void refreshOpenBook() {
        ModRecipeBookComponent book = ModRecipeBookScreens.component(Minecraft.getInstance().screen);
        if (book != null) {
            book.recipesUpdated();
        }
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ModRecipeIndex.rebuild(minecraft.level.getRecipeManager(), minecraft.level.registryAccess(),
                    minecraft.level.potionBrewing());
            JeiStations.importIfAvailable();
        }
        METHODS.clear();
        rememberRecipes(UNLOCKED);
        refreshOpenBook();
    }
}
