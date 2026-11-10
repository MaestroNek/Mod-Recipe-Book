package com.minemod.modrecipebook.neoforge;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.recipe.ModRecipeUnlocker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

@Mod(ModRecipeBook.MODID)
public class ModRecipeBookNeoForge {
    public ModRecipeBookNeoForge(IEventBus modBus, ModContainer container) {
        NeoAttachments.TYPES.register(modBus);
        ModRecipeBook.init();
    }

    @EventBusSubscriber(modid = ModRecipeBook.MODID)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            ModRecipeUnlocker.onDatapackSync(event.getPlayerList().getServer(), event.getPlayer());
        }

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                ModRecipeUnlocker.onPlayerTick(player);
            }
        }
    }
}
