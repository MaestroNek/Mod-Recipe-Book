package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ModRecipeTabButton extends AbstractWidget {
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/tab_right");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/tab_right_selected");

    private final String categoryId;
    private final ItemStack icon;
    private boolean selected;

    public ModRecipeTabButton(String categoryId, ItemStack icon, Component title) {
        super(0, 0, 35, 27, title);
        this.categoryId = categoryId;
        this.icon = icon.isEmpty() ? new ItemStack(Items.KNOWLEDGE_BOOK) : icon;
    }

    public String categoryId() {
        return categoryId;
    }

    public boolean selected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite = selected ? TAB_SELECTED : TAB;
        graphics.blitSprite(sprite, getX(), getY(), width, height);
        graphics.renderFakeItem(icon, getX() + 9, getY() + 5);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovered()) {
            graphics.renderTooltip(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
        }
    }
}
