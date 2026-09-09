package com.minemod.modrecipebook.net;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record BookmarkSyncPayload(List<String> keys) implements CustomPacketPayload {
    public static final Type<BookmarkSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "bookmark_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BookmarkSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), BookmarkSyncPayload::keys,
            BookmarkSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BookmarkSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.minemod.modrecipebook.client.RecipeBookmarks.replace(payload.keys()));
    }
}
