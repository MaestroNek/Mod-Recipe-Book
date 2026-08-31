package com.minemod.modrecipebook.net;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(UnlockRecipesPayload.TYPE, UnlockRecipesPayload.STREAM_CODEC, UnlockRecipesPayload::handle);
        registrar.playToServer(PlaceBrewingPayload.TYPE, PlaceBrewingPayload.STREAM_CODEC, PlaceBrewingPayload::handle);
        registrar.playToServer(DebugUnlockPayload.TYPE, DebugUnlockPayload.STREAM_CODEC, DebugUnlockPayload::handle);
    }
}
