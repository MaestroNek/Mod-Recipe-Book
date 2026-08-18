package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

import java.util.List;

public final class VanillaRecipeBookGuard {
    private VanillaRecipeBookGuard() {}

    public static void apply(RecipeBookComponent vanilla, ImageButton ourButton, List<Renderable> renderables,
                            int vanillaX, int vanillaY, int ourX, int ourY) {
        ImageButton vanillaButton = null;
        for (Renderable renderable : renderables) {
            if (renderable instanceof ImageButton button && button != ourButton
                    && button.getWidth() == 20 && button.getHeight() == 18) {
                vanillaButton = button;
                break;
            }
        }
        if (RecipeCategoryConfig.hideVanillaBook()) {
            if (vanilla != null && vanilla.isVisible()) {
                vanilla.toggleVisibility();
            }
            if (vanillaButton != null) {
                vanillaButton.visible = false;
                vanillaButton.active = false;
            }
            if (ourButton != null) {
                ourButton.setPosition(vanillaX, vanillaY);
            }
            return;
        }
        if (vanillaButton != null) {
            vanillaButton.visible = true;
            vanillaButton.active = true;
            vanillaButton.setPosition(vanillaX, vanillaY);
            vanillaButton.setFocused(vanilla != null && vanilla.isVisible());
        }
        if (ourButton != null) {
            ourButton.setPosition(ourX, ourY);
        }
    }
}
