package com.minemod.modrecipebook.fabric;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.ModRecipeUnlocker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class ModRecipeBookFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricAttachments.register();
        ModRecipeBook.init();
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
                ModRecipeUnlocker.onDatapackSync(player.getServer(), player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                ModRecipeUnlocker.onPlayerTick(player);
            }
        });
    }
}
