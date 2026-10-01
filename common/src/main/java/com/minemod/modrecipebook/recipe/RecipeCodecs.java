package com.minemod.modrecipebook.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RecipeCodecs {
    public static final Codec<Set<ResourceLocation>> IDS =
            ResourceLocation.CODEC.listOf().xmap(HashSet::new, ArrayList::new);
    public static final Codec<Set<ResourceLocation>> ORDERED_IDS =
            ResourceLocation.CODEC.listOf().xmap(LinkedHashSet::new, ArrayList::new);
    public static final Codec<Set<String>> KEYS =
            Codec.STRING.listOf().xmap(LinkedHashSet::new, ArrayList::new);

    private RecipeCodecs() {}
}
