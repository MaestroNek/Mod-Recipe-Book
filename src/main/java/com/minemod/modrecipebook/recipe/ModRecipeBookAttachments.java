package com.minemod.modrecipebook.recipe;

import com.minemod.modrecipebook.ModRecipeBook;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class ModRecipeBookAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModRecipeBook.MODID);

    public static final Codec<Set<ResourceLocation>> IDS_CODEC =
            ResourceLocation.CODEC.listOf().xmap(HashSet::new, ArrayList::new);

    public static final Supplier<AttachmentType<Set<ResourceLocation>>> UNLOCKED = ATTACHMENT_TYPES.register(
            "unlocked_recipes",
            () -> AttachmentType.builder(() -> (Set<ResourceLocation>) new HashSet<ResourceLocation>())
                    .serialize(IDS_CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Set<ResourceLocation>>> KNOWN_ITEMS = ATTACHMENT_TYPES.register(
            "known_items",
            () -> AttachmentType.builder(() -> (Set<ResourceLocation>) new HashSet<ResourceLocation>())
                    .serialize(IDS_CODEC)
                    .copyOnDeath()
                    .build()
    );

    private ModRecipeBookAttachments() {}
}
