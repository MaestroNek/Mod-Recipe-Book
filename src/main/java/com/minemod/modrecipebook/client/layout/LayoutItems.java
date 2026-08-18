package com.minemod.modrecipebook.client.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LayoutItems {
    private LayoutItems() {}

    public static ItemStack cycle(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        return items[(int) ((System.currentTimeMillis() / 1000L) % items.length)];
    }

    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    public static boolean hover(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 16;
    }

    public static Optional<ItemStack> pick(ItemStack stack, int x, int y, double mouseX, double mouseY) {
        if (!stack.isEmpty() && hover(x, y, (int) mouseX, (int) mouseY)) {
            return Optional.of(stack);
        }
        return Optional.empty();
    }

    public static Optional<ItemStack> pick(Ingredient ingredient, int x, int y, double mouseX, double mouseY) {
        return pick(cycle(ingredient), x, y, mouseX, mouseY);
    }

    public static void tooltip(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (!stack.isEmpty() && hover(x, y, mouseX, mouseY)) {
            graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
        }
    }

    public static void tooltip(GuiGraphics graphics, Ingredient ingredient, int x, int y, int mouseX, int mouseY) {
        tooltip(graphics, cycle(ingredient), x, y, mouseX, mouseY);
    }

    public static void title(GuiGraphics graphics, String text, int originX, int originY) {
        Font font = Minecraft.getInstance().font;
        int x = originX + (147 - font.width(text)) / 2;
        graphics.drawString(font, text, x, originY + 8, 0x404040, false);
    }

    public static List<Ingredient> nonEmpty(List<Ingredient> ingredients) {
        List<Ingredient> list = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient != null && !ingredient.isEmpty()) {
                list.add(ingredient);
            }
        }
        return list;
    }
}
