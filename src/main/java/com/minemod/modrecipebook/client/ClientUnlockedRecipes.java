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

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = com.minemod.modrecipebook.ModRecipeBook.MODID, value = Dist.CLIENT)
public final class ClientUnlockedRecipes {
    private static final Set<ResourceLocation> UNLOCKED = new HashSet<>();
    private static final Set<ResourceLocation> KNOWN = new HashSet<>();
    private static boolean ownToast;

    private ClientUnlockedRecipes() {}

    public static boolean isUnlocked(ResourceLocation id) {
        return UNLOCKED.contains(id);
    }

    public static boolean isItemKnown(Item item) {
        return KNOWN.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public static boolean showingOwnToast() {
        return ownToast;
    }

    public static void apply(UnlockRecipesPayload payload) {
        if (payload.replace()) {
            UNLOCKED.clear();
            KNOWN.clear();
            UNLOCKED.addAll(payload.recipes());
            KNOWN.addAll(payload.known());
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
        refreshOpenBook();
    }
}
