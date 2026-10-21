package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

import java.util.List;

public final class VanillaRecipeBookGuard {
    private static final int DRIFT = 4;

    private VanillaRecipeBookGuard() {}

    public static final class Layout {
        private int anchorRelX;
        private int anchorRelY;
        private int lastLeft = Integer.MIN_VALUE;
        private int lastTop = Integer.MIN_VALUE;

        public Layout(int relX, int relY) {
            this.anchorRelX = relX;
            this.anchorRelY = relY;
        }
    }

    public static void apply(RecipeBookComponent vanilla, ImageButton ourButton, List<? extends GuiEventListener> widgets,
                            Layout layout, int leftPos, int topPos) {
        ImageButton vanillaButton = findRecipeButton(widgets, ourButton);
        boolean guiMoved = layout.lastLeft != leftPos || layout.lastTop != topPos;

        // ponytail: live coords are stale on the frame leftPos jumps; other mods' new slot is adopted next frame.
        if (vanillaButton != null && !guiMoved) {
            int liveRelX = vanillaButton.getX() - leftPos;
            int liveRelY = vanillaButton.getY() - topPos;
            if (Math.abs(liveRelX - layout.anchorRelX) > DRIFT || Math.abs(liveRelY - layout.anchorRelY) > DRIFT) {
                layout.anchorRelX = liveRelX;
                layout.anchorRelY = liveRelY;
            }
        }
        layout.lastLeft = leftPos;
        layout.lastTop = topPos;

        int x = leftPos + layout.anchorRelX;
        int y = topPos + layout.anchorRelY;

        if (vanilla != null && vanilla.isVisible()) {
            vanilla.toggleVisibility();
        }
        if (vanillaButton != null) {
            vanillaButton.visible = false;
            vanillaButton.active = false;
            vanillaButton.setPosition(x, y);
            vanillaButton.setFocused(false);
        }
        if (ourButton != null) {
            ourButton.setPosition(x, y);
        }
    }

    public static ImageButton findRecipeButton(Screen screen, ImageButton ourButton) {
        return findRecipeButton(screen.children(), ourButton);
    }

    public static ImageButton findRecipeButton(List<? extends GuiEventListener> widgets, ImageButton ourButton) {
        for (GuiEventListener widget : widgets) {
            if (widget instanceof ImageButton button && button != ourButton
                    && button.getWidth() == 20 && button.getHeight() == 18
                    && button.getMessage().getString().isEmpty()) {
                return button;
            }
        }
        return null;
    }
}
