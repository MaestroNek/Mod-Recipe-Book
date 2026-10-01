package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

import java.util.List;

public final class VanillaRecipeBookGuard {
    private static final int DRIFT = 4;

    private VanillaRecipeBookGuard() {}

    public static final int LAYOUT_COUNT = 6;

    public static final class Layout {
        private final int defaultRelX;
        private final int defaultRelY;
        private int stackDx;
        private int stackDy;
        private boolean center;
        private int mode;
        private int anchorRelX;
        private int anchorRelY;
        private int lastLeft = Integer.MIN_VALUE;
        private int lastTop = Integer.MIN_VALUE;

        private Layout(int defaultRelX, int defaultRelY, int stackDx, int stackDy, boolean center) {
            this.defaultRelX = defaultRelX;
            this.defaultRelY = defaultRelY;
            this.stackDx = stackDx;
            this.stackDy = stackDy;
            this.center = center;
            this.anchorRelX = defaultRelX;
            this.anchorRelY = defaultRelY;
        }

        public int mode() {
            return mode;
        }

        public void setMode(int mode) {
            this.mode = Math.floorMod(mode, LAYOUT_COUNT);
            switch (this.mode) {
                case 1 -> {
                    this.stackDx = 22;
                    this.stackDy = 0;
                    this.center = true;
                }
                case 2 -> {
                    this.stackDx = 0;
                    this.stackDy = 20;
                    this.center = false;
                }
                case 3 -> {
                    this.stackDx = 22;
                    this.stackDy = 0;
                    this.center = false;
                }
                case 4 -> {
                    this.stackDx = 0;
                    this.stackDy = -20;
                    this.center = false;
                }
                case 5 -> {
                    this.stackDx = -22;
                    this.stackDy = 0;
                    this.center = false;
                }
                default -> {
                    this.stackDx = 0;
                    this.stackDy = 20;
                    this.center = true;
                }
            }
            this.lastLeft = Integer.MIN_VALUE;
        }
    }

    public static Layout stackBelow(int defaultRelX, int defaultRelY) {
        return new Layout(defaultRelX, defaultRelY, 0, 20, true);
    }

    public static Layout stackRight(int defaultRelX, int defaultRelY) {
        return new Layout(defaultRelX, defaultRelY, 22, 0, false);
    }

    public static Layout forMode(int defaultRelX, int defaultRelY, int mode) {
        Layout layout = stackBelow(defaultRelX, defaultRelY);
        layout.setMode(mode);
        return layout;
    }

    public static void apply(RecipeBookComponent vanilla, ImageButton ourButton, List<? extends GuiEventListener> widgets,
                            Layout layout, int leftPos, int topPos) {
        ImageButton vanillaButton = findRecipeButton(widgets, ourButton);
        boolean hide = RecipeCategoryConfig.hideVanillaBook();
        boolean pair = !hide && vanillaButton != null;
        boolean guiMoved = layout.lastLeft != leftPos || layout.lastTop != topPos;

        // ponytail: live coords are stale on the frame leftPos jumps; other mods' new slot is adopted next frame.
        int shiftX = pairShiftX(layout, !pair);
        int shiftY = pairShiftY(layout, !pair);
        if (vanillaButton != null && !guiMoved) {
            int liveRelX = vanillaButton.getX() - leftPos;
            int liveRelY = vanillaButton.getY() - topPos;
            if (Math.abs(liveRelX - (layout.anchorRelX - shiftX)) > DRIFT
                    || Math.abs(liveRelY - (layout.anchorRelY - shiftY)) > DRIFT) {
                layout.anchorRelX = liveRelX + shiftX;
                layout.anchorRelY = liveRelY + shiftY;
            }
        }
        layout.lastLeft = leftPos;
        layout.lastTop = topPos;

        int vanillaX = leftPos + layout.anchorRelX - shiftX;
        int vanillaY = topPos + layout.anchorRelY - shiftY;
        int ourX = pair ? vanillaX + layout.stackDx : vanillaX;
        int ourY = pair ? vanillaY + layout.stackDy : vanillaY;

        if (hide && vanilla != null && vanilla.isVisible()) {
            vanilla.toggleVisibility();
        }
        if (vanillaButton != null) {
            vanillaButton.visible = !hide;
            vanillaButton.active = !hide;
            vanillaButton.setPosition(vanillaX, vanillaY);
            vanillaButton.setFocused(!hide && vanilla != null && vanilla.isVisible());
        }
        if (ourButton != null) {
            ourButton.setPosition(ourX, ourY);
        }
    }

    private static int pairShiftX(Layout layout, boolean hide) {
        return !hide && layout.center ? layout.stackDx / 2 : 0;
    }

    private static int pairShiftY(Layout layout, boolean hide) {
        return !hide && layout.center ? layout.stackDy / 2 : 0;
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
