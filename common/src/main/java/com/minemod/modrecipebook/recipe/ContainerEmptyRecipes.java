package com.minemod.modrecipebook.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.minemod.modrecipebook.platform.Platform;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ContainerEmptyRecipes {
    private ContainerEmptyRecipes() {}

    public static void addContainers(Map<ResourceLocation, LinkedHashSet<Item>> containers) {
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            try {
                if (!fluid.defaultFluidState().isSource()) {
                    continue;
                }
            } catch (RuntimeException ignored) {
                continue;
            }
            Item bucket = fluid.getBucket();
            if (bucket != Items.AIR && bucket != Items.BUCKET) {
                add(containers, fluid, bucket);
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            Fluid extracted = Platform.drainFluid(item);
            if (extracted != null && extracted != Fluids.EMPTY) {
                add(containers, extracted, item);
            }
        }
    }

    private static void add(Map<ResourceLocation, LinkedHashSet<Item>> containers, Fluid fluid, Item item) {
        if (fluid == null || fluid == Fluids.EMPTY || item == Items.AIR) {
            return;
        }
        containers.computeIfAbsent(BuiltInRegistries.FLUID.getKey(fluid), k -> new LinkedHashSet<>()).add(item);
    }
}
