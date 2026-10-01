package com.minemod.modrecipebook.client.layout;

import com.minemod.modrecipebook.recipe.IngredientExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;
import java.util.Optional;

public final class CraftingLayout implements RecipeLayout {
    @Override
    public void render(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY, float partialTick) {
        int gridX = originX + 23;
        int gridY = originY + 48;
        int width = 3;
        int height = 3;
        List<Ingredient> ingredients = recipe.value().getIngredients();
        if (recipe.value() instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        }
        int i = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int slotX = gridX + x * 18;
                int slotY = gridY + y * 18;
                graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF373737);
                if (x < width && y < height && i < ingredients.size()) {
                    LayoutItems.item(graphics, LayoutItems.cycle(ingredients.get(i)), slotX, slotY);
                    i++;
                }
            }
        }
        graphics.drawString(Minecraft.getInstance().font, "->", originX + 82, originY + 66, 0x404040, false);
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        graphics.fill(originX + 104, originY + 63, originX + 122, originY + 81, 0xFF8B8B8B);
        graphics.fill(originX + 105, originY + 64, originX + 121, originY + 80, 0xFF373737);
        LayoutItems.item(graphics, result, originX + 105, originY + 64);
        if (recipe.value() instanceof CraftingRecipe crafting && crafting.isSpecial()) {
            LayoutItems.title(graphics, "Special", originX, originY);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY) {
        int gridX = originX + 23;
        int gridY = originY + 48;
        int width = 3;
        int height = 3;
        List<Ingredient> ingredients = recipe.value().getIngredients();
        if (recipe.value() instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        }
        int i = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x < width && y < height && i < ingredients.size()) {
                    LayoutItems.tooltip(graphics, ingredients.get(i), gridX + x * 18, gridY + y * 18, mouseX, mouseY);
                    i++;
                }
            }
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        LayoutItems.tooltip(graphics, result, originX + 105, originY + 64, mouseX, mouseY);
    }

    @Override
    public Optional<ItemStack> itemUnderMouse(int originX, int originY, RecipeHolder<?> recipe, double mouseX, double mouseY) {
        int gridX = originX + 23;
        int gridY = originY + 48;
        int width = 3;
        int height = 3;
        List<Ingredient> ingredients = recipe.value().getIngredients();
        if (recipe.value() instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        }
        int i = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x < width && y < height && i < ingredients.size()) {
                    Optional<ItemStack> stack = LayoutItems.pick(ingredients.get(i), gridX + x * 18, gridY + y * 18, mouseX, mouseY);
                    if (stack.isPresent()) {
                        return stack;
                    }
                    i++;
                }
            }
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        return LayoutItems.pick(result, originX + 105, originY + 64, mouseX, mouseY);
    }
}
