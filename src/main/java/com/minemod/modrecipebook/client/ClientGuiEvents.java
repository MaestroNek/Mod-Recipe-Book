package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = com.minemod.modrecipebook.ModRecipeBook.MODID, value = Dist.CLIENT)
public final class ClientGuiEvents {
    private ClientGuiEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        for (ImageButton button : ModRecipeBookScreens.attach(screen)) {
            event.addListener(button);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        ModRecipeBookScreens.reposition(event.getScreen());
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (ModRecipeBookScreens.mouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (ModRecipeBookScreens.keyPressed(event.getScreen(), event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (ModRecipeBookScreens.charTyped(event.getScreen(), event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        ModRecipeBookComponent book = ModRecipeBookScreens.component(event.getScreen());
        if (book != null && book.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        Screen screen = event.getScreen();
        ModRecipeBookComponent book = ModRecipeBookScreens.component(screen);
        if (book == null || book.isVanillaVisible()) {
            return;
        }
        if (screen.getFocused() instanceof ImageButton button
                && ((button.getWidth() == 20 && button.getHeight() == 18)
                || (button.getWidth() == 11 && button.getHeight() == 11))) {
            button.setFocused(false);
            screen.setFocused(null);
        }
    }
}
