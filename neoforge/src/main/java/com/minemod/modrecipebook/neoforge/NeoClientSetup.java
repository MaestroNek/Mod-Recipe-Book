package com.minemod.modrecipebook.neoforge;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.client.CategorySettingsScreen;
import com.minemod.modrecipebook.client.ModRecipeBookClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = ModRecipeBook.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NeoClientSetup {
    private NeoClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(ModRecipeBook.MODID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class,
                        (modContainer, parent) -> new CategorySettingsScreen(parent)));
        ModRecipeBookClient.init();
    }
}
