package com.minemod.modrecipebook.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

public final class ModNetworking {
    private ModNetworking() {}

    public static void init() {
        // Client registers these again with a receiver. A second registration crashes NeoForge.
        if (Platform.getEnvironment() != Env.CLIENT) {
            NetworkManager.registerS2CPayloadType(UnlockRecipesPayload.TYPE, UnlockRecipesPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(BookmarkSyncPayload.TYPE, BookmarkSyncPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(UnlockRulesPayload.TYPE, UnlockRulesPayload.STREAM_CODEC);
        }
        NetworkManager.registerReceiver(NetworkManager.c2s(), PlaceBrewingPayload.TYPE, PlaceBrewingPayload.STREAM_CODEC, PlaceBrewingPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), DebugUnlockPayload.TYPE, DebugUnlockPayload.STREAM_CODEC, DebugUnlockPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), BookmarkPayload.TYPE, BookmarkPayload.STREAM_CODEC, BookmarkPayload::handle);
    }

    public static void initClient() {
        NetworkManager.registerReceiver(NetworkManager.s2c(), UnlockRecipesPayload.TYPE, UnlockRecipesPayload.STREAM_CODEC, UnlockRecipesPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.s2c(), BookmarkSyncPayload.TYPE, BookmarkSyncPayload.STREAM_CODEC, BookmarkSyncPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.s2c(), UnlockRulesPayload.TYPE, UnlockRulesPayload.STREAM_CODEC, UnlockRulesPayload::handle);
    }
}
