package com.minemod.modrecipebook.net;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.ModRecipeUnlocker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DebugUnlockPayload(byte action) implements CustomPacketPayload {
    public static final byte DISCOVER = 0;
    public static final byte RESET = 1;
    public static final byte RETHINK = 2;

    public static final Type<DebugUnlockPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "debug_unlock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugUnlockPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeByte(payload.action),
            buf -> new DebugUnlockPayload(buf.readByte())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DebugUnlockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ModRecipeUnlocker.debug(player, payload.action);
            }
        });
    }
}
