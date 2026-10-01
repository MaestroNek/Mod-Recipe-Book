package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class GuiOrigin {
    public static int x(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).left();
    }

    public static int y(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).top();
    }

    public static int width(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).width();
    }

    public static int height(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).height();
    }

    private GuiOrigin() {}
}
