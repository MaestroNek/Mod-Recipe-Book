package com.minemod.modrecipebook.platform.fabric;

import com.minemod.modrecipebook.fabric.FabricAttachments;
import com.minemod.modrecipebook.platform.FluidFill;
import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlatformImpl {
    public static final int BUCKET = 81000;

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static String modName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    public static List<String> loadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId())
                .toList();
    }

    public static Set<ResourceLocation> unlocked(ServerPlayer player) {
        return new LinkedHashSet<>(player.getAttachedOrCreate(FabricAttachments.UNLOCKED));
    }

    public static void setUnlocked(ServerPlayer player, Set<ResourceLocation> value) {
        player.setAttached(FabricAttachments.UNLOCKED, new LinkedHashSet<>(value));
    }

    public static Set<ResourceLocation> knownItems(ServerPlayer player) {
        return new HashSet<>(player.getAttachedOrCreate(FabricAttachments.KNOWN_ITEMS));
    }

    public static void setKnownItems(ServerPlayer player, Set<ResourceLocation> value) {
        player.setAttached(FabricAttachments.KNOWN_ITEMS, new HashSet<>(value));
    }

    public static Set<String> bookmarks(ServerPlayer player) {
        return new LinkedHashSet<>(player.getAttachedOrCreate(FabricAttachments.BOOKMARKS));
    }

    public static void setBookmarks(ServerPlayer player, Set<String> value) {
        player.setAttached(FabricAttachments.BOOKMARKS, new LinkedHashSet<>(value));
    }

    public static boolean vanillaImported(ServerPlayer player) {
        return player.getAttachedOrCreate(FabricAttachments.VANILLA_IMPORTED);
    }

    public static void setVanillaImported(ServerPlayer player, boolean value) {
        player.setAttached(FabricAttachments.VANILLA_IMPORTED, value);
    }

    public static boolean absorbFluids(Object value, Set<ResourceLocation> out) {
        if (value instanceof FluidVariant variant && !variant.isBlank()) {
            Fluid fluid = variant.getFluid();
            if (fluid != null && fluid != Fluids.EMPTY) {
                out.add(BuiltInRegistries.FLUID.getKey(fluid));
            }
            return true;
        }
        return false;
    }

    public static int bucketVolume() {
        return BUCKET;
    }

    public static boolean canFill(ItemStack stack) {
        Held held = new Held(stack.copyWithCount(1));
        Storage<FluidVariant> storage = fluids(held);
        return storage != null && storage.supportsInsertion();
    }

    public static FluidFill tryFill(ItemStack empty, Fluid fluid) {
        Held held = new Held(empty.copyWithCount(1));
        Storage<FluidVariant> storage = fluids(held);
        if (storage == null) {
            return null;
        }
        long moved;
        try (Transaction transaction = Transaction.openOuter()) {
            moved = storage.insert(FluidVariant.of(fluid), Long.MAX_VALUE, transaction);
            if (moved <= 0) {
                return null;
            }
            transaction.commit();
        }
        ItemStack result = held.stack.copy();
        if (result.isEmpty() || result.getItem() == empty.getItem()) {
            return null;
        }
        return new FluidFill((int) Math.min(moved, Integer.MAX_VALUE), result);
    }

    public static Fluid drainFluid(Item item) {
        Held held = new Held(new ItemStack(item));
        Storage<FluidVariant> storage = fluids(held);
        if (storage == null) {
            return null;
        }
        for (StorageView<FluidVariant> view : storage) {
            FluidVariant variant = view.getResource();
            if (variant != null && !variant.isBlank() && view.getAmount() > 0) {
                return variant.getFluid();
            }
        }
        return null;
    }

    public static List<PackResources> installedDatapacks() {
        List<ModResourcePack> packs = new ArrayList<>();
        ModResourcePackUtil.appendModResourcePacks(packs, PackType.SERVER_DATA, null);
        return new ArrayList<>(packs);
    }

    public static void injectRecipeConditions(RecipeManager recipes, TagManager tags, RegistryAccess access) {
    }

    public static void addModBrews(PotionBrewing brewing, Map<ResourceLocation, RecipeHolder<BrewingMixRecipe>> out) {
    }

    private static Storage<FluidVariant> fluids(Held held) {
        return FluidStorage.ITEM.find(held.stack, ContainerItemContext.ofSingleSlot(held));
    }

    private PlatformImpl() {}

    private static final class Held implements SingleSlotStorage<ItemVariant> {
        private ItemStack stack;

        private Held(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) {
                return 0;
            }
            if (!stack.isEmpty() && !resource.matches(stack)) {
                return 0;
            }
            int space = stack.isEmpty()
                    ? resource.getItem().getDefaultMaxStackSize()
                    : stack.getMaxStackSize() - stack.getCount();
            int moved = (int) Math.min(maxAmount, Math.max(0, space));
            if (moved <= 0) {
                return 0;
            }
            if (stack.isEmpty()) {
                stack = resource.toStack(moved);
            } else {
                stack.grow(moved);
            }
            return moved;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (stack.isEmpty() || maxAmount <= 0 || !resource.matches(stack)) {
                return 0;
            }
            int moved = (int) Math.min(maxAmount, stack.getCount());
            stack.shrink(moved);
            if (stack.isEmpty()) {
                stack = ItemStack.EMPTY;
            }
            return moved;
        }

        @Override
        public boolean isResourceBlank() {
            return stack.isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return stack.isEmpty() ? ItemVariant.blank() : ItemVariant.of(stack);
        }

        @Override
        public long getAmount() {
            return stack.getCount();
        }

        @Override
        public long getCapacity() {
            return stack.isEmpty() ? 64 : stack.getMaxStackSize();
        }
    }
}
