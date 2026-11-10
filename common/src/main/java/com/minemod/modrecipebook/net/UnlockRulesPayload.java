package com.minemod.modrecipebook.net;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlockRulesPayload(boolean requireAll, boolean requireMethod) implements CustomPacketPayload {
    public static final Type<UnlockRulesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "unlock_rules"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockRulesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, UnlockRulesPayload::requireAll,
            ByteBufCodecs.BOOL, UnlockRulesPayload::requireMethod,
            UnlockRulesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
