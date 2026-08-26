package com.minemod.modrecipebook.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {
    public BrewingStandScreenMixin(BrewingStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyVariable(method = "renderBg", at = @At("STORE"), index = 5)
    private int modrecipebook$bgX(int x) {
        return this.leftPos;
    }

    @ModifyVariable(method = "renderBg", at = @At("STORE"), index = 6)
    private int modrecipebook$bgY(int y) {
        return this.topPos;
    }
}
