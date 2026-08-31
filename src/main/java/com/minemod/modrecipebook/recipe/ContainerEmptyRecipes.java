package com.minemod.modrecipebook.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

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
            FluidStack extracted = drain(item);
            if (!extracted.isEmpty()) {
                add(containers, extracted.getFluid(), item);
            }
        }
    }

    private static void add(Map<ResourceLocation, LinkedHashSet<Item>> containers, Fluid fluid, Item item) {
        if (fluid == null || fluid == Fluids.EMPTY || item == Items.AIR) {
            return;
        }
        containers.computeIfAbsent(BuiltInRegistries.FLUID.getKey(fluid), k -> new LinkedHashSet<>()).add(item);
    }

    private static FluidStack drain(Item item) {
        try {
            ItemStack split = new ItemStack(item);
            IFluidHandlerItem handler = split.getCapability(Capabilities.FluidHandler.ITEM);
            if (handler == null) {
                return FluidStack.EMPTY;
            }
            return handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        } catch (RuntimeException ignored) {
            return FluidStack.EMPTY;
        }
    }
}
