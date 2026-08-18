package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.net.UnlockRecipesPayload;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = com.minemod.modrecipebook.ModRecipeBook.MODID, value = Dist.CLIENT)
public final class ClientUnlockedRecipes {
    private static final Set<ResourceLocation> UNLOCKED = new HashSet<>();

    private ClientUnlockedRecipes() {}

    public static boolean isUnlocked(ResourceLocation id) {
        return UNLOCKED.contains(id);
    }

    public static void apply(UnlockRecipesPayload payload) {
        if (payload.replace()) {
            UNLOCKED.clear();
            UNLOCKED.addAll(payload.recipes());
            refreshOpenBook();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean added = false;
        for (ResourceLocation id : payload.recipes()) {
            if (!UNLOCKED.add(id)) {
                continue;
            }
            added = true;
            if (minecraft.level == null || "minecraft".equals(id.getNamespace())) {
                continue;
            }
            minecraft.level.getRecipeManager().byKey(id).ifPresent(holder ->
                    RecipeToast.addOrUpdate(minecraft.getToasts(), (RecipeHolder<?>) holder));
        }
        if (added) {
            refreshOpenBook();
        }
    }

    private static void refreshOpenBook() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModRecipeBookAccess access) {
            access.modrecipebook$component().recipesUpdated();
        }
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ModRecipeIndex.rebuild(minecraft.level.getRecipeManager(), minecraft.level.registryAccess());
        }
    }
}
