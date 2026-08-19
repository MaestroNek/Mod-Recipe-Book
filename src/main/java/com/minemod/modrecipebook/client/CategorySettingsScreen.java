package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class CategorySettingsScreen extends Screen {
    private static final int ROW = 28;
    private static final int SCROLLBAR_W = 6;
    private static final ResourceLocation SCROLLER =
            ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final ResourceLocation SCROLLER_BG =
            ResourceLocation.withDefaultNamespace("widget/scroller_background");
    private final Screen parent;
    private int listLeft;
    private int listTop;
    private int listBottom;
    private int visibleRows;
    private int listScroll;
    private boolean draggingBar;
    private int dragOffset;

    public CategorySettingsScreen(Screen parent) {
        super(Component.translatable("gui.modrecipebook.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        listLeft = width / 2 - 150;
        listTop = 132;
        listBottom = height - 56;
        visibleRows = Math.max(1, (listBottom - listTop) / ROW);
        clampScroll();
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.hide_jei"), font)
                .pos(listLeft, 28)
                .selected(RecipeCategoryConfig.hideJei())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.hide_jei.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setHideJei(value))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.hide_vanilla"), font)
                .pos(listLeft, 48)
                .selected(RecipeCategoryConfig.hideVanillaBook())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.hide_vanilla.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setHideVanillaBook(value))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.require_all"), font)
                .pos(listLeft, 68)
                .selected(RecipeCategoryConfig.requireAllIngredients())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.require_all.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setRequireAllIngredients(value))
                .build());
        List<RecipeCategoryConfig.Entry> categories = RecipeCategoryConfig.all();
        int shown = Math.min(visibleRows, Math.max(0, categories.size() - listScroll));
        for (int i = 0; i < shown; i++) {
            int index = listScroll + i;
            String id = categories.get(index).id;
            int row = listTop + i * ROW;
            Button up = Button.builder(Component.literal("^"), b -> {
                RecipeCategoryConfig.move(id, -1);
                rebuildWidgets();
            }).bounds(listLeft + 170, row, 14, 20).build();
            up.active = index > 0;
            addRenderableWidget(up);
            Button down = Button.builder(Component.literal("v"), b -> {
                RecipeCategoryConfig.move(id, 1);
                rebuildWidgets();
            }).bounds(listLeft + 184, row, 14, 20).build();
            down.active = index < categories.size() - 1;
            addRenderableWidget(down);
            addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.edit"),
                    b -> minecraft.setScreen(new CategoryEditScreen(this, RecipeCategoryConfig.find(id))))
                    .bounds(listLeft + 200, row, 48, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.delete"),
                    b -> {
                        RecipeCategoryConfig.delete(id);
                        rebuildWidgets();
                    })
                    .bounds(listLeft + 252, row, 48, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.add"), b ->
                minecraft.setScreen(new CategoryEditScreen(this, null)))
                .bounds(listLeft, height - 52, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.categories"), listLeft, 92, 0xA0A0A0, false);
        graphics.renderItem(new ItemStack(Items.COMPASS), listLeft, 104);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.tab.all"), listLeft + 22, 108, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.locked"), listLeft + 200, 108, 0x808080, false);
        List<RecipeCategoryConfig.Entry> categories = RecipeCategoryConfig.all();
        int shown = Math.min(visibleRows, Math.max(0, categories.size() - listScroll));
        for (int i = 0; i < shown; i++) {
            RecipeCategoryConfig.Entry entry = categories.get(listScroll + i);
            int y = listTop + i * ROW;
            RecipeCategoryConfig.renderItem(graphics, entry.iconStack(), listLeft, y);
            graphics.drawString(font, font.plainSubstrByWidth(entry.title().getString(), 145),
                    listLeft + 22, y + 4, 0xFFFFFF, false);
        }
        renderScrollbar(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overList(mouseX, mouseY) || overScrollbar(mouseX, mouseY)) {
            return setScroll((int) (listScroll - Math.signum(scrollY)));
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overScrollbar(mouseX, mouseY)) {
            int thumbTop = thumbY();
            int thumbH = thumbHeight();
            if (mouseY >= thumbTop && mouseY < thumbTop + thumbH) {
                dragOffset = (int) mouseY - thumbTop;
            } else {
                dragOffset = thumbH / 2;
                applyScrollFromMouse(mouseY);
            }
            draggingBar = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar) {
            applyScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void clampScroll() {
        listScroll = Math.min(listScroll, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, RecipeCategoryConfig.all().size() - visibleRows);
    }

    private boolean setScroll(int next) {
        next = Math.max(0, Math.min(maxScroll(), next));
        if (next == listScroll) {
            return maxScroll() > 0;
        }
        listScroll = next;
        rebuildWidgets();
        return true;
    }

    private int scrollbarX() {
        return listLeft + 304;
    }

    private int trackHeight() {
        return Math.max(1, listBottom - listTop);
    }

    private int thumbHeight() {
        int total = RecipeCategoryConfig.all().size();
        if (total <= visibleRows) {
            return trackHeight();
        }
        return Math.max(32, trackHeight() * visibleRows / total);
    }

    private int thumbY() {
        int max = maxScroll();
        int travel = trackHeight() - thumbHeight();
        if (max == 0 || travel <= 0) {
            return listTop;
        }
        return listTop + travel * listScroll / max;
    }

    private boolean overList(double mouseX, double mouseY) {
        return mouseX >= listLeft && mouseX < listLeft + 300 && mouseY >= listTop && mouseY < listBottom;
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return maxScroll() > 0
                && mouseX >= scrollbarX() && mouseX < scrollbarX() + SCROLLBAR_W
                && mouseY >= listTop && mouseY < listBottom;
    }

    private void applyScrollFromMouse(double mouseY) {
        int travel = trackHeight() - thumbHeight();
        if (travel <= 0) {
            return;
        }
        double t = (mouseY - listTop - dragOffset) / travel;
        setScroll((int) Math.round(t * maxScroll()));
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (maxScroll() <= 0) {
            return;
        }
        graphics.blitSprite(SCROLLER_BG, scrollbarX(), listTop, SCROLLBAR_W, trackHeight());
        graphics.blitSprite(SCROLLER, scrollbarX(), thumbY(), SCROLLBAR_W, thumbHeight());
    }
}
