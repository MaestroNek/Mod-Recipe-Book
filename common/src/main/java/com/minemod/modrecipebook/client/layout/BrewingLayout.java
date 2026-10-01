package com.minemod.modrecipebook.client.layout;

import com.minemod.modrecipebook.recipe.BrewingMixRecipe;
import com.minemod.modrecipebook.recipe.IngredientExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;

public final class BrewingLayout implements RecipeLayout {
    @Override
    public void render(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY, float partialTick) {
        LayoutItems.title(graphics, Component.translatable("gui.modrecipebook.layout.brewing").getString(), originX, originY);
        if (!(recipe.value() instanceof BrewingMixRecipe mix)) {
            return;
        }
        int y = originY + 64;
        slot(graphics, originX + 12, y);
        LayoutItems.item(graphics, mix.input(), originX + 12, y);
        slot(graphics, originX + 30, y);
        LayoutItems.item(graphics, LayoutItems.cycle(mix.reagent()), originX + 30, y);
        graphics.drawString(Minecraft.getInstance().font, "->", originX + 104, y + 4, 0x404040, false);
        ItemStack result = IngredientExtractor.result(mix, Minecraft.getInstance().level.registryAccess());
        slot(graphics, originX + 122, y);
        LayoutItems.item(graphics, result, originX + 122, y);
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY) {
        if (!(recipe.value() instanceof BrewingMixRecipe mix)) {
            return;
        }
        int y = originY + 64;
        LayoutItems.tooltip(graphics, mix.input(), originX + 12, y, mouseX, mouseY);
        LayoutItems.tooltip(graphics, mix.reagent(), originX + 30, y, mouseX, mouseY);
        ItemStack result = IngredientExtractor.result(mix, Minecraft.getInstance().level.registryAccess());
        LayoutItems.tooltip(graphics, result, originX + 122, y, mouseX, mouseY);
    }

    @Override
    public Optional<ItemStack> itemUnderMouse(int originX, int originY, RecipeHolder<?> recipe, double mouseX, double mouseY) {
        if (!(recipe.value() instanceof BrewingMixRecipe mix)) {
            return Optional.empty();
        }
        int y = originY + 64;
        Optional<ItemStack> input = LayoutItems.pick(mix.input(), originX + 12, y, mouseX, mouseY);
        if (input.isPresent()) {
            return input;
        }
        Optional<ItemStack> reagent = LayoutItems.pick(mix.reagent(), originX + 30, y, mouseX, mouseY);
        if (reagent.isPresent()) {
            return reagent;
        }
        ItemStack result = IngredientExtractor.result(mix, Minecraft.getInstance().level.registryAccess());
        return LayoutItems.pick(result, originX + 122, y, mouseX, mouseY);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
    }
}
