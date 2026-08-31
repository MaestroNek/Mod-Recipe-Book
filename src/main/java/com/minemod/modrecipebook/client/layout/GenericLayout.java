package com.minemod.modrecipebook.client.layout;

import com.minemod.modrecipebook.recipe.ContainerFillRecipe;
import com.minemod.modrecipebook.recipe.IngredientExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

public final class GenericLayout implements RecipeLayout {
    @Override
    public void render(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY, float partialTick) {
        LayoutItems.title(graphics, typeName(recipe), originX, originY);
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int y = originY + 64;
        int count = Math.min(ingredients.size(), 5);
        int startX = originX + 12;
        for (int i = 0; i < count; i++) {
            int x = startX + i * 18;
            slot(graphics, x, y);
            LayoutItems.item(graphics, LayoutItems.cycle(ingredients.get(i)), x, y);
        }
        graphics.drawString(Minecraft.getInstance().font, "->", originX + 104, y + 4, 0x404040, false);
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        slot(graphics, originX + 122, y);
        LayoutItems.item(graphics, result, originX + 122, y);
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY) {
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int y = originY + 64;
        int count = Math.min(ingredients.size(), 5);
        int startX = originX + 12;
        for (int i = 0; i < count; i++) {
            LayoutItems.tooltip(graphics, ingredients.get(i), startX + i * 18, y, mouseX, mouseY);
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        LayoutItems.tooltip(graphics, result, originX + 122, y, mouseX, mouseY);
    }

    @Override
    public Optional<ItemStack> itemUnderMouse(int originX, int originY, RecipeHolder<?> recipe, double mouseX, double mouseY) {
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int y = originY + 64;
        int count = Math.min(ingredients.size(), 5);
        int startX = originX + 12;
        for (int i = 0; i < count; i++) {
            Optional<ItemStack> stack = LayoutItems.pick(ingredients.get(i), startX + i * 18, y, mouseX, mouseY);
            if (stack.isPresent()) {
                return stack;
            }
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        return LayoutItems.pick(result, originX + 122, y, mouseX, mouseY);
    }

    static String typeName(RecipeHolder<?> recipe) {
        RecipeType<?> type = recipe.value().getType();
        var id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (id == null) {
            return recipe.id().toString();
        }
        String path = id.getPath().replace('_', ' ');
        if (path.isEmpty()) {
            return id.toString();
        }
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
    }
}
