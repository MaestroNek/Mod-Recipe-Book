package com.minemod.modrecipebook.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int left();

    @Accessor("topPos")
    int top();

    @Accessor("imageWidth")
    int width();

    @Accessor("imageHeight")
    int height();

    @Accessor("leftPos")
    void setLeftPos(int leftPos);
}
