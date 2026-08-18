package com.minemod.modrecipebook.client.layout;

import com.minemod.modrecipebook.recipe.IngredientExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public final class ProcessingLayout implements RecipeLayout {
    private final ItemStack machine;
    private final ItemStack base;
    private final String title;
    private final boolean vertical;

    public ProcessingLayout(ItemStack machine, ItemStack base, String title, boolean vertical) {
        this.machine = machine;
        this.base = base;
        this.title = title;
        this.vertical = vertical;
    }

    public static ProcessingLayout horizontal(String machineId, String title) {
        return new ProcessingLayout(stack(machineId), ItemStack.EMPTY, title, false);
    }

    public static ProcessingLayout withBase(String machineId, String baseId, String title, boolean vertical) {
        return new ProcessingLayout(stack(machineId), stack(baseId), title, vertical);
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    @Override
    public void render(GuiGraphics graphics, int originX, int originY, RecipeHolder<?> recipe, int mouseX, int mouseY, float partialTick) {
        LayoutItems.title(graphics, title, originX, originY);
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int y = originY + 64;
        int startX = originX + 12;
        int count = Math.min(ingredients.size(), 3);
        for (int i = 0; i < count; i++) {
            int x = startX + i * 18;
            slot(graphics, x, y);
            LayoutItems.item(graphics, LayoutItems.cycle(ingredients.get(i)), x, y);
        }
        int machineX = originX + 70;
        int machineY = y;
        if (vertical && !base.isEmpty()) {
            renderBlock(graphics, machine, machineX, y - 4, 1.6F);
            renderBlock(graphics, base, machineX, y + 20, 1.6F);
        } else if (!base.isEmpty()) {
            renderBlock(graphics, machine, machineX - 8, y, 1.5F);
            renderBlock(graphics, base, machineX + 12, y, 1.5F);
        } else {
            renderBlock(graphics, machine, machineX, y, 1.6F);
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
        int startX = originX + 12;
        int count = Math.min(ingredients.size(), 3);
        for (int i = 0; i < count; i++) {
            LayoutItems.tooltip(graphics, ingredients.get(i), startX + i * 18, y, mouseX, mouseY);
        }
        int machineX = originX + 70;
        blockTooltip(graphics, machine, machineX, y - 4, mouseX, mouseY);
        if (!base.isEmpty()) {
            blockTooltip(graphics, base, machineX, y + 20, mouseX, mouseY);
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        LayoutItems.tooltip(graphics, result, originX + 122, y, mouseX, mouseY);
    }

    @Override
    public Optional<ItemStack> itemUnderMouse(int originX, int originY, RecipeHolder<?> recipe, double mouseX, double mouseY) {
        List<Ingredient> ingredients = LayoutItems.nonEmpty(IngredientExtractor.items(recipe.value()));
        int y = originY + 64;
        int startX = originX + 12;
        int count = Math.min(ingredients.size(), 3);
        for (int i = 0; i < count; i++) {
            Optional<ItemStack> stack = LayoutItems.pick(ingredients.get(i), startX + i * 18, y, mouseX, mouseY);
            if (stack.isPresent()) {
                return stack;
            }
        }
        int machineX = originX + 70;
        if (!machine.isEmpty() && mouseX >= machineX && mouseY >= y - 4 && mouseX < machineX + 32 && mouseY < y - 4 + 32) {
            return Optional.of(machine);
        }
        if (!base.isEmpty() && mouseX >= machineX && mouseY >= y + 20 && mouseX < machineX + 32 && mouseY < y + 52) {
            return Optional.of(base);
        }
        ItemStack result = IngredientExtractor.result(recipe.value(), Minecraft.getInstance().level.registryAccess());
        return LayoutItems.pick(result, originX + 122, y, mouseX, mouseY);
    }

    private static void renderBlock(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        if (stack.isEmpty()) {
            return;
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 8, y + 8, 50);
        pose.scale(scale, scale, scale);
        pose.translate(-8, -8, 0);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
    }

    private static void blockTooltip(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (!stack.isEmpty() && mouseX >= x && mouseY >= y && mouseX < x + 32 && mouseY < y + 32) {
            graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
        }
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
    }
}
