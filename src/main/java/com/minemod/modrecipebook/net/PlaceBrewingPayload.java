package com.minemod.modrecipebook.net;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Predicate;

public record PlaceBrewingPayload(ResourceLocation recipe, boolean placeAll) implements CustomPacketPayload {
    public static final Type<PlaceBrewingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "place_brewing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceBrewingPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PlaceBrewingPayload::recipe,
            ByteBufCodecs.BOOL, PlaceBrewingPayload::placeAll,
            PlaceBrewingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlaceBrewingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof BrewingStandMenu menu)) {
                return;
            }
            ModRecipeIndex.byId(payload.recipe()).ifPresent(holder -> {
                if (holder.value() instanceof BrewingMixRecipe mix) {
                    place(player, menu, mix, payload.placeAll());
                }
            });
        });
    }

    private static void place(ServerPlayer player, BrewingStandMenu menu, BrewingMixRecipe mix, boolean placeAll) {
        for (int i = 0; i < 4; i++) {
            returnToPlayer(player, menu.getSlot(i));
        }
        int bottles = placeAll ? 3 : 1;
        ItemStack input = mix.input();
        for (int i = 0; i < bottles; i++) {
            move(menu, menu.getSlot(i), stack -> ItemStack.isSameItemSameComponents(stack, input), 1);
        }
        Ingredient reagent = mix.reagent();
        move(menu, menu.getSlot(3), reagent, 1);
        menu.broadcastChanges();
    }

    private static void returnToPlayer(ServerPlayer player, Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        ItemStack moving = slot.remove(stack.getCount());
        if (!player.getInventory().add(moving)) {
            player.drop(moving, false);
        }
    }

    private static void move(BrewingStandMenu menu, Slot target, Predicate<ItemStack> match, int want) {
        int moved = 0;
        for (int i = 5; i < menu.slots.size() && moved < want; i++) {
            Slot from = menu.getSlot(i);
            ItemStack stack = from.getItem();
            if (stack.isEmpty() || !match.test(stack)) {
                continue;
            }
            int take = Math.min(want - moved, stack.getCount());
            ItemStack remain = target.safeInsert(stack.copyWithCount(take));
            int used = take - remain.getCount();
            if (used > 0) {
                from.remove(used);
                moved += used;
            }
        }
    }
}
