package com.minemod.modrecipebook.fabric;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.client.ClientGuiEvents;
import com.minemod.modrecipebook.fabric.mixin.ScreenWidgetInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class ModRecipeBookFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModRecipeBook.initClient();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?>) {
                ClientGuiEvents.afterInit(screen, button -> ((ScreenWidgetInvoker) screen).modrecipebook$add(button));
            }
            ScreenEvents.beforeRender(screen).register((current, graphics, mouseX, mouseY, tickDelta) ->
                    ClientGuiEvents.beforeRender(current));
            ScreenMouseEvents.allowMouseClick(screen).register((current, mouseX, mouseY, button) ->
                    !ClientGuiEvents.mouseClicked(current, mouseX, mouseY, button));
            ScreenKeyboardEvents.allowKeyPress(screen).register((current, key, scancode, modifiers) ->
                    !ClientGuiEvents.keyPressed(current, key, scancode, modifiers));
            ScreenMouseEvents.allowMouseScroll(screen).register((current, mouseX, mouseY, horizontal, vertical) ->
                    !ClientGuiEvents.mouseScrolled(current, mouseX, mouseY, vertical));
            ScreenMouseEvents.afterMouseRelease(screen).register((current, mouseX, mouseY, button) ->
                    ClientGuiEvents.mouseReleased(current));
        });
    }
}
