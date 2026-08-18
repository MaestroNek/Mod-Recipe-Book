package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class CategorySettingsScreen extends Screen {
    private final Screen parent;

    public CategorySettingsScreen(Screen parent) {
        super(Component.translatable("gui.modrecipebook.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.hide_jei"), font)
                .pos(left, 28)
                .selected(RecipeCategoryConfig.hideJei())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.hide_jei.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setHideJei(value))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.hide_vanilla"), font)
                .pos(left, 48)
                .selected(RecipeCategoryConfig.hideVanillaBook())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.hide_vanilla.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setHideVanillaBook(value))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.require_all"), font)
                .pos(left, 68)
                .selected(RecipeCategoryConfig.requireAllIngredients())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.require_all.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setRequireAllIngredients(value))
                .build());
        int y = 132;
        List<RecipeCategoryConfig.Entry> categories = RecipeCategoryConfig.all();
        for (int i = 0; i < categories.size(); i++) {
            String id = categories.get(i).id;
            int row = y;
            Button up = Button.builder(Component.literal("^"), b -> {
                RecipeCategoryConfig.move(id, -1);
                rebuildWidgets();
            }).bounds(left + 170, row, 14, 20).build();
            up.active = i > 0;
            addRenderableWidget(up);
            Button down = Button.builder(Component.literal("v"), b -> {
                RecipeCategoryConfig.move(id, 1);
                rebuildWidgets();
            }).bounds(left + 184, row, 14, 20).build();
            down.active = i < categories.size() - 1;
            addRenderableWidget(down);
            addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.edit"),
                    b -> minecraft.setScreen(new CategoryEditScreen(this, RecipeCategoryConfig.find(id))))
                    .bounds(left + 200, row, 48, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.delete"),
                    b -> {
                        RecipeCategoryConfig.delete(id);
                        rebuildWidgets();
                    })
                    .bounds(left + 252, row, 48, 20)
                    .build());
            y += 28;
        }
        if (RecipeCategoryConfig.all().size() < RecipeCategoryConfig.MAX) {
            addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.add"), b ->
                    minecraft.setScreen(new CategoryEditScreen(this, null)))
                    .bounds(left, y, 150, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int left = width / 2 - 150;
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.categories"), left, 92, 0xA0A0A0, false);
        graphics.renderItem(new ItemStack(Items.COMPASS), left, 104);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.tab.all"), left + 22, 108, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.locked"), left + 200, 108, 0x808080, false);
        int y = 132;
        for (RecipeCategoryConfig.Entry entry : RecipeCategoryConfig.all()) {
            RecipeCategoryConfig.renderItem(graphics, entry.iconStack(), left, y);
            graphics.drawString(font, font.plainSubstrByWidth(entry.title().getString(), 145),
                    left + 22, y + 4, 0xFFFFFF, false);
            y += 28;
        }
        if (RecipeCategoryConfig.all().size() >= RecipeCategoryConfig.MAX) {
            graphics.drawCenteredString(font, Component.translatable("gui.modrecipebook.config.max"),
                    width / 2, height - 48, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
