package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = com.minemod.modrecipebook.ModRecipeBook.MODID, value = Dist.CLIENT)
public final class ClientGuiEvents {
    private ClientGuiEvents() {}

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() instanceof ModRecipeBookAccess access
                && access.modrecipebook$component().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof ModRecipeBookAccess)) {
            return;
        }
        if (screen.getFocused() instanceof ImageButton button && button.getWidth() == 20 && button.getHeight() == 18) {
            if (screen instanceof ModRecipeBookAccess access && access.modrecipebook$component().isVanillaVisible()) {
                return;
            }
            button.setFocused(false);
            screen.setFocused(null);
        }
    }
}
