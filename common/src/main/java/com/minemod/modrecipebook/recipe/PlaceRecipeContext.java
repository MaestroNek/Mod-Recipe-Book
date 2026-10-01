package com.minemod.modrecipebook.recipe;

import net.minecraft.server.level.ServerPlayer;

public final class PlaceRecipeContext {
    private static final ThreadLocal<ServerPlayer> PLAYER = new ThreadLocal<>();

    private PlaceRecipeContext() {}

    public static void enter(ServerPlayer player) {
        PLAYER.set(player);
    }

    public static void exit() {
        PLAYER.remove();
    }

    public static ServerPlayer current() {
        return PLAYER.get();
    }
}
