package com.minemod.modrecipebook.net;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.ModRecipeUnlocker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BookmarkPayload(String key) implements CustomPacketPayload {
    public static final Type<BookmarkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "bookmark"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BookmarkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BookmarkPayload::key,
            BookmarkPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BookmarkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ModRecipeUnlocker.toggleBookmark(player, payload.key());
            }
        });
    }
}
