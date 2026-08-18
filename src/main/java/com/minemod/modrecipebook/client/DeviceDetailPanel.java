package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface DeviceDetailPanel {
    void initButtons(int panelX, int panelY, int panelHeight);

    int width();

    int height();

    RecipeHolder<?> recipe();

    void tick();

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    void renderOverlays(GuiGraphics graphics, int mouseX, int mouseY);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean isMouseOver(double mouseX, double mouseY);

    StateSwitchingButton backButton();
}
