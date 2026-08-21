package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

import java.util.List;

public final class VanillaRecipeBookGuard {
    private static final int DRIFT = 4;

    private VanillaRecipeBookGuard() {}

    public static final class Layout {
        private final int defaultRelX;
        private final int defaultRelY;
        private final int stackDx;
        private final int stackDy;
        private int anchorRelX;
        private int anchorRelY;
        private boolean external;
        private int lastLeft = Integer.MIN_VALUE;
        private int lastTop = Integer.MIN_VALUE;

        private Layout(int defaultRelX, int defaultRelY, int stackDx, int stackDy) {
            this.defaultRelX = defaultRelX;
            this.defaultRelY = defaultRelY;
            this.stackDx = stackDx;
            this.stackDy = stackDy;
            this.anchorRelX = defaultRelX;
            this.anchorRelY = defaultRelY;
        }
    }

    public static Layout stackBelow(int defaultRelX, int defaultRelY) {
        return new Layout(defaultRelX, defaultRelY, 0, 20);
    }

    public static Layout stackRight(int defaultRelX, int defaultRelY) {
        return new Layout(defaultRelX, defaultRelY, 22, 0);
    }

    public static void apply(RecipeBookComponent vanilla, ImageButton ourButton, List<Renderable> renderables,
                            Layout layout, int leftPos, int topPos) {
        ImageButton vanillaButton = findVanillaRecipeButton(ourButton, renderables);
        boolean hide = RecipeCategoryConfig.hideVanillaBook();
        boolean guiMoved = layout.lastLeft != leftPos || layout.lastTop != topPos;

        // ponytail: live coords are stale on the frame leftPos jumps; other mods' new slot is adopted next frame.
        if (vanillaButton != null && !guiMoved) {
            int liveRelX = vanillaButton.getX() - leftPos;
            int liveRelY = vanillaButton.getY() - topPos;
            if (Math.abs(liveRelX - layout.anchorRelX) > DRIFT
                    || Math.abs(liveRelY - placedVanillaRelY(layout, hide)) > DRIFT) {
                layout.anchorRelX = liveRelX;
                layout.anchorRelY = liveRelY;
                layout.external = Math.abs(liveRelX - layout.defaultRelX) > DRIFT
                        || Math.abs(liveRelY - layout.defaultRelY) > DRIFT;
            }
        }
        layout.lastLeft = leftPos;
        layout.lastTop = topPos;

        int shiftY = pairShiftY(layout, hide);
        int vanillaX = leftPos + layout.anchorRelX;
        int vanillaY = topPos + layout.anchorRelY - shiftY;
        int ourX = hide ? vanillaX : vanillaX + layout.stackDx;
        int ourY = hide ? vanillaY : vanillaY + layout.stackDy;

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

    private static int pairShiftY(Layout layout, boolean hide) {
        if (hide || layout.external || layout.stackDx != 0) {
            return 0;
        }
        return layout.stackDy / 2;
    }

    private static int placedVanillaRelY(Layout layout, boolean hide) {
        return layout.anchorRelY - pairShiftY(layout, hide);
    }

    private static ImageButton findVanillaRecipeButton(ImageButton ourButton, List<Renderable> renderables) {
        for (Renderable renderable : renderables) {
            if (renderable instanceof ImageButton button && button != ourButton
                    && button.getWidth() == 20 && button.getHeight() == 18
                    && button.getMessage().getString().isEmpty()) {
                return button;
            }
        }
        return null;
    }
}
