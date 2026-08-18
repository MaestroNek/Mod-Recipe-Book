package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.client.ModRecipeBookAccess;
import com.minemod.modrecipebook.client.ModRecipeBookComponent;
import com.minemod.modrecipebook.client.RecipeCategoryConfig;
import com.minemod.modrecipebook.client.VanillaRecipeBookGuard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> implements ModRecipeBookAccess {
    @Shadow
    @Final
    public AbstractFurnaceRecipeBookComponent recipeBookComponent;

    @Unique
    private final ModRecipeBookComponent modrecipebook$book = new ModRecipeBookComponent();

    @Unique
    private ImageButton modrecipebook$button;

    public AbstractFurnaceScreenMixin(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public ModRecipeBookComponent modrecipebook$component() {
        return this.modrecipebook$book;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void modrecipebook$init(CallbackInfo ci) {
        boolean narrow = this.width < 379;
        this.modrecipebook$book.init(this.width, this.height, this.minecraft, narrow, this.menu, this, this.recipeBookComponent);
        if (this.modrecipebook$book.isVisible()) {
            this.leftPos = this.modrecipebook$book.updateScreenPosition(this.width, this.imageWidth);
            this.modrecipebook$book.syncLayout();
        }
        this.modrecipebook$button = this.addRenderableWidget(this.modrecipebook$book.createToggleButton(
                this.leftPos + 20, this.height / 2 - 40, button -> this.modrecipebook$toggle()));
        this.modrecipebook$repositionButtons();
    }

    @Unique
    private void modrecipebook$toggle() {
        if (!this.modrecipebook$book.isVisible() && this.recipeBookComponent.isVisible()) {
            this.recipeBookComponent.toggleVisibility();
        }
        this.modrecipebook$book.toggleVisibility();
        this.leftPos = this.modrecipebook$book.updateScreenPosition(this.width, this.imageWidth);
        this.modrecipebook$book.syncLayout();
        this.modrecipebook$repositionButtons();
    }

    @Unique
    private void modrecipebook$repositionButtons() {
        int x = this.leftPos + 20;
        int y = this.height / 2 - 60;
        VanillaRecipeBookGuard.apply(this.recipeBookComponent, this.modrecipebook$button, this.renderables,
                x, y, x, y + 20);
        if (RecipeCategoryConfig.hideVanillaBook() && !this.modrecipebook$book.isVisible()) {
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        }
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void modrecipebook$tick(CallbackInfo ci) {
        this.modrecipebook$book.tick();
        this.modrecipebook$repositionButtons();
        if (this.recipeBookComponent.isVisible() && this.modrecipebook$book.isVisible()) {
            this.modrecipebook$book.setVisible(false);
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            this.modrecipebook$repositionButtons();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void modrecipebook$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.modrecipebook$book.render(graphics, mouseX, mouseY, partialTick);
        this.modrecipebook$book.renderGhostRecipe(graphics, this.leftPos, this.topPos, partialTick);
        this.modrecipebook$book.renderTooltip(graphics, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void modrecipebook$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.modrecipebook$book.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void modrecipebook$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.modrecipebook$book.keyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void modrecipebook$charTyped(char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.modrecipebook$book.charTyped(codePoint, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void modrecipebook$hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (this.modrecipebook$book.isMouseOver(mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void modrecipebook$slotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        this.modrecipebook$book.slotClicked(slot);
    }

    @Inject(method = "recipesUpdated", at = @At("TAIL"))
    private void modrecipebook$recipesUpdated(CallbackInfo ci) {
        this.modrecipebook$book.recipesUpdated();
    }
}
