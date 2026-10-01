package com.minemod.modrecipebook.neoforge;

import com.minemod.modrecipebook.recipe.RecipeCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class NeoAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "modrecipebook");

    public static final Supplier<AttachmentType<Set<ResourceLocation>>> UNLOCKED = TYPES.register(
            "unlocked_recipes",
            () -> AttachmentType.builder(() -> (Set<ResourceLocation>) new LinkedHashSet<ResourceLocation>())
                    .serialize(RecipeCodecs.ORDERED_IDS)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Set<ResourceLocation>>> KNOWN_ITEMS = TYPES.register(
            "known_items",
            () -> AttachmentType.builder(() -> (Set<ResourceLocation>) new HashSet<ResourceLocation>())
                    .serialize(RecipeCodecs.IDS)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Set<String>>> BOOKMARKS = TYPES.register(
            "bookmarks",
            () -> AttachmentType.builder(() -> (Set<String>) new LinkedHashSet<String>())
                    .serialize(RecipeCodecs.KEYS)
                    .copyOnDeath()
                    .build()
    );

    private NeoAttachments() {}
}
