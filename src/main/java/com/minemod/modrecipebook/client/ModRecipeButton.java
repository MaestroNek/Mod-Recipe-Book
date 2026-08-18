package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ModRecipeButton extends AbstractWidget {
    private static final ResourceLocation SLOT_CRAFTABLE =
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/slot_craftable");
    private static final ResourceLocation SLOT_UNCRAFTABLE =
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/slot_uncraftable");
    private static final ResourceLocation SLOT_MANY_CRAFTABLE =
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/slot_many_craftable");
    private static final ResourceLocation SLOT_MANY_UNCRAFTABLE =
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/slot_many_uncraftable");

    private RecipeGroup group;
    private boolean craftable;

    public ModRecipeButton() {
        super(0, 0, 25, 25, Component.empty());
    }

    public void init(RecipeGroup group, boolean craftable) {
        this.group = group;
        this.craftable = craftable;
        this.visible = group != null;
    }

    public RecipeGroup group() {
        return group;
    }

    public RecipeHolder<?> recipe() {
        if (group == null) {
            return null;
        }
        for (RecipeHolder<?> holder : group.recipes()) {
            if (holder.value() instanceof CraftingRecipe) {
                return holder;
            }
        }
        return group.primary();
    }

    public boolean hasCrafting() {
        if (group == null) {
            return false;
        }
        for (RecipeHolder<?> holder : group.recipes()) {
            if (holder.value() instanceof CraftingRecipe) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (group == null) {
            return;
        }
        ResourceLocation sprite;
        boolean many = group.recipes().size() > 1;
        if (craftable) {
            sprite = many ? SLOT_MANY_CRAFTABLE : SLOT_CRAFTABLE;
        } else {
            sprite = many ? SLOT_MANY_UNCRAFTABLE : SLOT_UNCRAFTABLE;
        }
        graphics.blitSprite(sprite, getX(), getY(), width, height);
        ItemStack result = group.result();
        graphics.renderItem(result, getX() + 4, getY() + 4);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
