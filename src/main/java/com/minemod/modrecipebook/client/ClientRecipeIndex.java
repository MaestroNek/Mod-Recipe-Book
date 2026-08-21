package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.tags.TagManager;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.conditions.ConditionContext;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class ClientRecipeIndex {
    private static boolean attemptedWithoutWorld;

    private ClientRecipeIndex() {}

    static void ensureIndexed() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ModRecipeIndex.rebuild(minecraft.level.getRecipeManager(), minecraft.level.registryAccess());
            return;
        }
        if (ModRecipeIndex.recipesIndexed() || attemptedWithoutWorld) {
            return;
        }
        attemptedWithoutWorld = true;
        try {
            loadFromInstalledDatapacks();
        } catch (Exception e) {
            ModRecipeBook.LOGGER.warn("Could not index recipes before joining a world", e);
        }
    }

    private static void loadFromInstalledDatapacks() {
        var packs = ServerPacksSource.createVanillaTrustedRepository();
        packs.reload();
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, packs.openAllSelected())) {
            RegistryAccess.Frozen access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            TagManager tags = new TagManager(access);
            RecipeManager recipes = new RecipeManager(access);
            ICondition.IContext context = new ConditionContext(tags);
            recipes.injectContext(context, access);
            SimpleReloadInstance.create(
                    resources,
                    List.of(tags, recipes),
                    Runnable::run,
                    Runnable::run,
                    CompletableFuture.completedFuture(Unit.INSTANCE),
                    false)
                .done()
                .join();
            ModRecipeIndex.rebuild(recipes, access);
            ModRecipeBook.LOGGER.info("Indexed {} recipes from installed mods (no world)", recipes.getRecipes().size());
        }
    }
}
