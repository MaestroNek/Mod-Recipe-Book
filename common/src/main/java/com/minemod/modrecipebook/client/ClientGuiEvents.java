package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.function.Consumer;

public final class ClientGuiEvents {
    private ClientGuiEvents() {}

    public static void afterInit(Screen screen, Consumer<ImageButton> add) {
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        for (ImageButton button : ModRecipeBookScreens.attach(container)) {
            add.accept(button);
        }
    }

    public static void beforeRender(Screen screen) {
        ModRecipeBookScreens.reposition(screen);
    }

    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        return ModRecipeBookScreens.mouseClicked(screen, mouseX, mouseY, button);
    }

    public static boolean keyPressed(Screen screen, int key, int scanCode, int modifiers) {
        return ModRecipeBookScreens.keyPressed(screen, key, scanCode, modifiers);
    }

    public static boolean charTyped(Screen screen, char codePoint, int modifiers) {
        return ModRecipeBookScreens.charTyped(screen, codePoint, modifiers);
    }

    public static boolean mouseScrolled(Screen screen, double mouseX, double mouseY, double delta) {
        ModRecipeBookComponent book = ModRecipeBookScreens.component(screen);
        return book != null && book.mouseScrolled(mouseX, mouseY, delta);
    }

    public static void mouseReleased(Screen screen) {
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
