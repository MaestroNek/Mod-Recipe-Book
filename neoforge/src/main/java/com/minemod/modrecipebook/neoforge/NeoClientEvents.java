package com.minemod.modrecipebook.neoforge;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.client.ClientGuiEvents;
import net.minecraft.client.gui.components.ImageButton;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ModRecipeBook.MODID, value = Dist.CLIENT)
public final class NeoClientEvents {
    private NeoClientEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        ClientGuiEvents.afterInit(event.getScreen(), event::addListener);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        ClientGuiEvents.beforeRender(event.getScreen());
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (ClientGuiEvents.mouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (ClientGuiEvents.keyPressed(event.getScreen(), event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (ClientGuiEvents.charTyped(event.getScreen(), event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (ClientGuiEvents.mouseScrolled(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        ClientGuiEvents.mouseReleased(event.getScreen());
    }
}
