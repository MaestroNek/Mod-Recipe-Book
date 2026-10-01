package com.minemod.modrecipebook.platform.neoforge;

import com.minemod.modrecipebook.neoforge.NeoAttachments;
import com.minemod.modrecipebook.platform.FluidFill;
import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import com.minemod.modrecipebook.recipe.BrewingRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.tags.TagManager;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.common.conditions.ConditionContext;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlatformImpl {
    private static final Method CREATE_CAN = method(
            "com.simibubi.create.content.fluids.transfer.GenericItemFilling",
            "canItemBeFilled", Level.class, ItemStack.class);
    private static final Method CREATE_AMOUNT = method(
            "com.simibubi.create.content.fluids.transfer.GenericItemFilling",
            "getRequiredAmountForItem", Level.class, ItemStack.class, FluidStack.class);
    private static final Method CREATE_FILL = method(
            "com.simibubi.create.content.fluids.transfer.GenericItemFilling",
            "fillItem", Level.class, int.class, ItemStack.class, FluidStack.class);

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }

    public static String modName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    public static List<String> loadedModIds() {
        return ModList.get().getMods().stream().map(info -> info.getModId()).toList();
    }

    public static Set<ResourceLocation> unlocked(ServerPlayer player) {
        return new LinkedHashSet<>(holder(player).getData(NeoAttachments.UNLOCKED));
    }

    public static void setUnlocked(ServerPlayer player, Set<ResourceLocation> value) {
        holder(player).setData(NeoAttachments.UNLOCKED, new LinkedHashSet<>(value));
    }

    public static Set<ResourceLocation> knownItems(ServerPlayer player) {
        return new HashSet<>(holder(player).getData(NeoAttachments.KNOWN_ITEMS));
    }

    public static void setKnownItems(ServerPlayer player, Set<ResourceLocation> value) {
        holder(player).setData(NeoAttachments.KNOWN_ITEMS, new HashSet<>(value));
    }

    public static Set<String> bookmarks(ServerPlayer player) {
        return new LinkedHashSet<>(holder(player).getData(NeoAttachments.BOOKMARKS));
    }

    public static void setBookmarks(ServerPlayer player, Set<String> value) {
        holder(player).setData(NeoAttachments.BOOKMARKS, new LinkedHashSet<>(value));
    }

    public static boolean absorbFluids(Object value, Set<ResourceLocation> out) {
        if (value instanceof FluidStack stack) {
            add(stack.getFluid(), out);
            return true;
        }
        if (value instanceof SizedFluidIngredient sized) {
            for (FluidStack stack : sized.getFluids()) {
                add(stack.getFluid(), out);
            }
            return true;
        }
        if (value instanceof FluidIngredient ingredient) {
            for (FluidStack stack : ingredient.getStacks()) {
                add(stack.getFluid(), out);
            }
            return true;
        }
        return false;
    }

    public static int bucketVolume() {
        return FluidType.BUCKET_VOLUME;
    }

    public static boolean canFill(ItemStack stack) {
        if (CREATE_CAN != null) {
            try {
                return Boolean.TRUE.equals(CREATE_CAN.invoke(null, null, stack));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        return Capabilities.FluidHandler.ITEM.getCapability(stack, null) != null;
    }

    public static FluidFill tryFill(ItemStack empty, Fluid fluid) {
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
            IFluidHandlerItem handler = Capabilities.FluidHandler.ITEM.getCapability(split, null);
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

    public static Fluid drainFluid(Item item) {
        try {
            ItemStack split = new ItemStack(item);
            IFluidHandlerItem handler = Capabilities.FluidHandler.ITEM.getCapability(split, null);
            if (handler == null) {
                return null;
            }
            FluidStack drained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                return null;
            }
            return drained.getFluid();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static List<PackResources> installedDatapacks() {
        return List.of();
    }

    public static void injectRecipeConditions(RecipeManager recipes, TagManager tags, RegistryAccess access) {
        Method method = null;
        for (Class<?> lookup : new Class<?>[] { HolderLookup.Provider.class, RegistryAccess.class }) {
            try {
                method = recipes.getClass().getMethod("injectContext", ICondition.IContext.class, lookup);
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (method == null) {
            return;
        }
        try {
            method.invoke(recipes, new ConditionContext(tags), access);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void addModBrews(PotionBrewing brewing, Map<ResourceLocation, RecipeHolder<BrewingMixRecipe>> out) {
        List<IBrewingRecipe> recipes;
        try {
            recipes = (List<IBrewingRecipe>) brewing.getClass().getMethod("getRecipes").invoke(brewing);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        for (IBrewingRecipe recipe : recipes) {
            if (!(recipe instanceof BrewingRecipe brewingRecipe)) {
                continue;
            }
            ItemStack[] inputs = brewingRecipe.getInput().getItems();
            if (inputs.length == 0 || brewingRecipe.getIngredient().isEmpty() || brewingRecipe.getOutput().isEmpty()) {
                continue;
            }
            BrewingRecipes.add(out, inputs[0], brewingRecipe.getIngredient(), brewingRecipe.getOutput());
        }
    }

    private static IAttachmentHolder holder(ServerPlayer player) {
        return (IAttachmentHolder) (Object) player;
    }

    private static FluidFill accept(ItemStack empty, int amount, ItemStack result) {
        if (result == null || result.isEmpty() || result.getItem() == empty.getItem()) {
            return null;
        }
        return new FluidFill(amount, result);
    }

    private static boolean validHandler(ItemStack stack, IFluidHandlerItem handler) {
        if (handler instanceof FluidBucketWrapper) {
            Item item = stack.getItem();
            return item instanceof BucketItem || item instanceof MilkBucketItem;
        }
        return true;
    }

    private static void add(Fluid fluid, Set<ResourceLocation> out) {
        if (fluid != null && fluid != Fluids.EMPTY) {
            out.add(BuiltInRegistries.FLUID.getKey(fluid));
        }
    }

    private static Method method(String className, String name, Class<?>... args) {
        try {
            return Class.forName(className).getMethod(name, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private PlatformImpl() {}
}
