package com.minemod.modrecipebook.net;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record UnlockRecipesPayload(List<ResourceLocation> recipes, boolean replace) implements CustomPacketPayload {
    public static final Type<UnlockRecipesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "unlock_recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockRecipesPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), UnlockRecipesPayload::recipes,
            ByteBufCodecs.BOOL, UnlockRecipesPayload::replace,
            UnlockRecipesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UnlockRecipesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.minemod.modrecipebook.client.ClientUnlockedRecipes.apply(payload));
    }
}
