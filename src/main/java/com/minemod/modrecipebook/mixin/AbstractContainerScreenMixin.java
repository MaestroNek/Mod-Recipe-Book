package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.client.ModRecipeBookScreens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void modrecipebook$renderBook(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ModRecipeBookScreens.render((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void modrecipebook$tick(CallbackInfo ci) {
        ModRecipeBookScreens.tick((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void modrecipebook$hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (ModRecipeBookScreens.overBook((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void modrecipebook$slotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        ModRecipeBookScreens.slotClicked((AbstractContainerScreen<?>) (Object) this, slot);
    }
}
