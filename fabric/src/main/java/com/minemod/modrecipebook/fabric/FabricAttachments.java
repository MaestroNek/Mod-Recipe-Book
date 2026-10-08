package com.minemod.modrecipebook.fabric;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.RecipeCodecs;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class FabricAttachments {
    public static final AttachmentType<Set<ResourceLocation>> UNLOCKED = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "unlocked_recipes"),
            builder -> builder.initializer(LinkedHashSet::new).persistent(RecipeCodecs.ORDERED_IDS).copyOnDeath()
    );

    public static final AttachmentType<Set<ResourceLocation>> KNOWN_ITEMS = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "known_items"),
            builder -> builder.initializer(HashSet::new).persistent(RecipeCodecs.IDS).copyOnDeath()
    );

    public static final AttachmentType<Set<String>> BOOKMARKS = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "bookmarks"),
            builder -> builder.initializer(LinkedHashSet::new).persistent(RecipeCodecs.KEYS).copyOnDeath()
    );

    public static final AttachmentType<Boolean> VANILLA_IMPORTED = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "vanilla_book_imported"),
            builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath()
    );

    public static void register() {
        // Touch the fields so the attachments are created during mod init.
    }

    private FabricAttachments() {}
}
