package com.minemod.modrecipebook.client.layout;

import com.minemod.modrecipebook.recipe.IngredientExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

public final class CookingLayout implements RecipeLayout {
    @Override
    public void render(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY, float partialTick) {
        List<Ingredient> ingredients = IngredientExtractor.items(recipe.value());
        int inX = originX + 34;
        int inY = originY + 64;
        slot(graphics, inX, inY);
        if (!ingredients.isEmpty()) {
            LayoutItems.item(graphics, LayoutItems.cycle(ingredients.get(0)), inX, inY);
        }
        ItemStack machine = machine(recipe);
        LayoutItems.item(graphics, machine, originX + 65, originY + 64);
        graphics.drawString(Minecraft.getInstance().font, "->", originX + 84, originY + 68, 0x404040, false);
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        slot(graphics, originX + 102, originY + 64);
        LayoutItems.item(graphics, result, originX + 102, originY + 64);
        if (recipe.value() instanceof AbstractCookingRecipe cooking) {
            LayoutItems.title(graphics, (cooking.getCookingTime() / 20) + "s", originX, originY);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY) {
        List<Ingredient> ingredients = IngredientExtractor.items(recipe.value());
        if (!ingredients.isEmpty()) {
            LayoutItems.tooltip(graphics, ingredients.get(0), originX + 34, originY + 64, mouseX, mouseY);
        }
        LayoutItems.tooltip(graphics, machine(recipe), originX + 65, originY + 64, mouseX, mouseY);
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        LayoutItems.tooltip(graphics, result, originX + 102, originY + 64, mouseX, mouseY);
    }

    @Override
    public Optional<ItemStack> itemUnderMouse(int originX, int originY, RecipeHolder<?> recipe, double mouseX, double mouseY) {
        List<Ingredient> ingredients = IngredientExtractor.items(recipe.value());
        if (!ingredients.isEmpty()) {
            Optional<ItemStack> input = LayoutItems.pick(ingredients.get(0), originX + 34, originY + 64, mouseX, mouseY);
            if (input.isPresent()) {
                return input;
            }
        }
        Optional<ItemStack> machine = LayoutItems.pick(machine(recipe), originX + 65, originY + 64, mouseX, mouseY);
        if (machine.isPresent()) {
            return machine;
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        return LayoutItems.pick(result, originX + 102, originY + 64, mouseX, mouseY);
    }

    private static ItemStack machine(RecipeHolder<?> recipe) {
        RecipeType<?> type = recipe.value().getType();
        if (type == RecipeType.BLASTING) {
            return new ItemStack(Items.BLAST_FURNACE);
        }
        if (type == RecipeType.SMOKING) {
            return new ItemStack(Items.SMOKER);
        }
        if (type == RecipeType.CAMPFIRE_COOKING) {
            return new ItemStack(Items.CAMPFIRE);
        }
        return new ItemStack(Items.FURNACE);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
    }
}
