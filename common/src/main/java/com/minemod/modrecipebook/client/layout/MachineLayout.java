package com.minemod.modrecipebook.client.layout;

import com.minemod.modrecipebook.recipe.IngredientExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public final class MachineLayout implements RecipeLayout {
    private final ItemStack machine;
    private final String title;

    public MachineLayout(ItemStack machine, String title) {
        this.machine = machine;
        this.title = title;
    }

    @Override
    public void render(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY, float partialTick) {
        LayoutItems.title(graphics, title, originX, originY);
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int startX = originX + 16;
        int y = originY + 64;
        for (int i = 0; i < ingredients.size() && i < 4; i++) {
            int x = startX + i * 18;
            slot(graphics, x, y);
            LayoutItems.item(graphics, LayoutItems.cycle(ingredients.get(i)), x, y);
        }
        LayoutItems.item(graphics, machine, originX + 90, y);
        graphics.drawString(Minecraft.getInstance().font, "->", originX + 108, y + 4, 0x404040, false);
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        slot(graphics, originX + 122, y);
        LayoutItems.item(graphics, result, originX + 122, y);
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY) {
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int startX = originX + 16;
        int y = originY + 64;
        for (int i = 0; i < ingredients.size() && i < 4; i++) {
            LayoutItems.tooltip(graphics, ingredients.get(i), startX + i * 18, y, mouseX, mouseY);
        }
        LayoutItems.tooltip(graphics, machine, originX + 90, y, mouseX, mouseY);
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        LayoutItems.tooltip(graphics, result, originX + 122, y, mouseX, mouseY);
    }

    @Override
    public Optional<ItemStack> itemUnderMouse(int originX, int originY, RecipeHolder<?> recipe, double mouseX, double mouseY) {
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int startX = originX + 16;
        int y = originY + 64;
        for (int i = 0; i < ingredients.size() && i < 4; i++) {
            Optional<ItemStack> stack = LayoutItems.pick(ingredients.get(i), startX + i * 18, y, mouseX, mouseY);
            if (stack.isPresent()) {
                return stack;
            }
        }
        Optional<ItemStack> clickedMachine = LayoutItems.pick(machine, originX + 90, y, mouseX, mouseY);
        if (clickedMachine.isPresent()) {
            return clickedMachine;
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        return LayoutItems.pick(result, originX + 122, y, mouseX, mouseY);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
    }

    public static ItemStack item(String id) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(id)));
    }

    public static MachineLayout of(String itemId, String title) {
        ItemStack stack = item(itemId);
        if (stack.isEmpty()) {
            stack = new ItemStack(Items.CRAFTING_TABLE);
        }
        return new MachineLayout(stack, title);
    }
}
