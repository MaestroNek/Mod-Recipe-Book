package com.minemod.modrecipebook.recipe;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ContainerFillRecipes {
    private static final Method CREATE_CAN = method(
            "com.simibubi.create.content.fluids.transfer.GenericItemFilling",
            "canItemBeFilled", Level.class, ItemStack.class);
    private static final Method CREATE_AMOUNT = method(
            "com.simibubi.create.content.fluids.transfer.GenericItemFilling",
            "getRequiredAmountForItem", Level.class, ItemStack.class, FluidStack.class);
    private static final Method CREATE_FILL = method(
            "com.simibubi.create.content.fluids.transfer.GenericItemFilling",
            "fillItem", Level.class, int.class, ItemStack.class, FluidStack.class);

    private ContainerFillRecipes() {}

    public static List<RecipeHolder<ContainerFillRecipe>> collect(
            Iterable<RecipeHolder<?>> existing, RegistryAccess access) {
        Set<Item> taken = new HashSet<>();
        for (RecipeHolder<?> holder : existing) {
            ItemStack result = IngredientExtractor.result(holder.value(), access);
            if (!result.isEmpty()) {
                taken.add(result.getItem());
            }
        }
        List<Fluid> fluids = sourceFluids();
        List<RecipeHolder<ContainerFillRecipe>> out = new ArrayList<>();
        for (Fluid fluid : fluids) {
            Item bucket = fluid.getBucket();
            if (bucket == Items.AIR || bucket == Items.BUCKET || taken.contains(bucket)) {
                continue;
            }
            add(out, taken, new ItemStack(Items.BUCKET), fluid, FluidType.BUCKET_VOLUME, new ItemStack(bucket));
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || item == Items.BUCKET) {
                continue;
            }
            ItemStack empty = new ItemStack(item);
            if (!canFill(empty)) {
                continue;
            }
            for (Fluid fluid : fluids) {
                Fill fill = fill(empty, fluid);
                if (fill == null || taken.contains(fill.result.getItem())) {
                    continue;
                }
                add(out, taken, empty, fluid, fill.amount, fill.result);
            }
        }
        return out;
    }

    private static void add(List<RecipeHolder<ContainerFillRecipe>> out, Set<Item> taken,
                            ItemStack empty, Fluid fluid, int amount, ItemStack result) {
        if (result.isEmpty() || result.getItem() == empty.getItem() || taken.contains(result.getItem())) {
            return;
        }
        taken.add(result.getItem());
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        ResourceLocation emptyId = BuiltInRegistries.ITEM.getKey(empty.getItem());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID,
                "fill/" + fluidId.getNamespace() + "/" + fluidId.getPath()
                        + "/" + emptyId.getNamespace() + "/" + emptyId.getPath());
        out.add(new RecipeHolder<>(id, new ContainerFillRecipe(empty, fluid, amount, result)));
    }

    private static List<Fluid> sourceFluids() {
        List<Fluid> fluids = new ArrayList<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            try {
                if (fluid.defaultFluidState().isSource()) {
                    fluids.add(fluid);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return fluids;
    }

    private static boolean canFill(ItemStack empty) {
        if (CREATE_CAN != null) {
            try {
                return Boolean.TRUE.equals(CREATE_CAN.invoke(null, null, empty));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        return empty.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }

    private static Fill fill(ItemStack empty, Fluid fluid) {
        try {
            if (CREATE_FILL != null && CREATE_AMOUNT != null) {
                ItemStack stack = empty.copyWithCount(1);
                FluidStack available = new FluidStack(fluid, Integer.MAX_VALUE);
                int amount = (Integer) CREATE_AMOUNT.invoke(null, null, stack, available);
                if (amount <= 0) {
                    return null;
                }
                ItemStack result = (ItemStack) CREATE_FILL.invoke(null, null, amount, stack, new FluidStack(fluid, amount));
                return accept(empty, amount, result);
            }
            ItemStack split = empty.copyWithCount(1);
            IFluidHandlerItem handler = split.getCapability(Capabilities.FluidHandler.ITEM);
            if (handler == null || !validHandler(split, handler)) {
                return null;
            }
            int amount = handler.fill(new FluidStack(fluid, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
            if (amount <= 0) {
                return null;
            }
            handler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
            return accept(empty, amount, handler.getContainer());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Fill accept(ItemStack empty, int amount, ItemStack result) {
        if (result == null || result.isEmpty() || result.getItem() == empty.getItem()) {
            return null;
        }
        return new Fill(amount, result);
    }

    private static boolean validHandler(ItemStack stack, IFluidHandlerItem handler) {
        if (handler instanceof FluidBucketWrapper) {
            Item item = stack.getItem();
            return item instanceof BucketItem || item instanceof MilkBucketItem;
        }
        return true;
    }

    private static Method method(String className, String name, Class<?>... args) {
        try {
            return Class.forName(className).getMethod(name, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record Fill(int amount, ItemStack result) {}
}
