package com.minemod.modrecipebook.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class CategorySettingsScreen extends Screen {
    private static final int ROW = 28;
    private static final int ROW_BTN = 20;
    private static final int ARROW_W = 11;
    private static final int ARROW_H = 16;
    private static final int ICON_GAP = 3;
    private static final int ARROW_SLOT = ARROW_W + ICON_GAP;
    private static final int ICON_X = 0;
    private static final int TEXT_X = 16 + ICON_GAP;
    private static final int LIST_W = 300;
    private static final int SCROLLBAR_W = 6;
    private static final int SIDE_PAD = (ARROW_SLOT - SCROLLBAR_W) / 2;
    private static final int PAD = 6;
    private static final int LIST_BG = 0x80000000;
    private static final int DIVIDER_Y = 116;
    private static final int ADD_BTN_Y = 126;
    private static final int HEADER_TEXT_Y = 132;
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
        ClientRecipeIndex.ensureIndexed();
    }

    @Override
    protected void init() {
        listLeft = (width - panelWidth()) / 2 + ARROW_SLOT + SIDE_PAD;
        listTop = 150 + PAD;
        int limit = height - 32 - PAD;
        visibleRows = Math.max(1, (limit - listTop - ROW_BTN) / ROW + 1);
        listBottom = listTop + (visibleRows - 1) * ROW + ROW_BTN;
        clampScroll();
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.hide_jei"), font)
                .pos(panelLeft(), 28)
                .selected(RecipeCategoryConfig.hideJei())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.hide_jei.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setHideJei(value))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.hide_vanilla"), font)
                .pos(panelLeft(), 48)
                .selected(RecipeCategoryConfig.hideVanillaBook())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.hide_vanilla.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setHideVanillaBook(value))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.require_all"), font)
                .pos(panelLeft(), 68)
                .selected(RecipeCategoryConfig.requireAllIngredients())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.require_all.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setRequireAllIngredients(value))
                .build());
        addRenderableWidget(Checkbox.builder(Component.translatable("gui.modrecipebook.config.require_method"), font)
                .pos(panelLeft(), 88)
                .selected(RecipeCategoryConfig.requireCraftingMethod())
                .tooltip(Tooltip.create(Component.translatable("gui.modrecipebook.config.require_method.tooltip")))
                .onValueChange((box, value) -> RecipeCategoryConfig.setRequireCraftingMethod(value))
                .build());
        List<RecipeCategoryConfig.Entry> categories = RecipeCategoryConfig.all();
        int shown = Math.min(visibleRows, Math.max(0, rowCount() - listScroll));
        for (int i = 0; i < shown; i++) {
            int index = listScroll + i;
            if (index == 0) {
                continue;
            }
            int catIndex = index - 1;
            String id = categories.get(catIndex).id;
            int row = listTop + i * ROW;
            boolean canUp = catIndex > 0;
            boolean canDown = catIndex < categories.size() - 1;
            if (canUp || canDown) {
                addRenderableWidget(new SortArrows(listLeft - ARROW_SLOT, row + (ROW_BTN - ARROW_H) / 2, canUp, canDown, () -> {
                    RecipeCategoryConfig.move(id, -1);
                    rebuildWidgets();
                }, () -> {
                    RecipeCategoryConfig.move(id, 1);
                    rebuildWidgets();
                }));
            }
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
        addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.debug"), b ->
                minecraft.setScreen(new DebugSettingsScreen(this)))
                .bounds(width - 8 - 60, 6, 60, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.add"), b ->
                minecraft.setScreen(new CategoryEditScreen(this, null)))
                .bounds(panelRight() - 150, ADD_BTN_Y, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft(), listTop - PAD, panelRight(), listBottom + PAD, LIST_BG);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.fill(panelLeft(), DIVIDER_Y, panelRight(), DIVIDER_Y + 1, 0xFF000000);
        graphics.fill(panelLeft(), DIVIDER_Y + 1, panelRight(), DIVIDER_Y + 2, 0x55FFFFFF);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.categories"),
                panelLeft(), HEADER_TEXT_Y, 0xA0A0A0, false);
        List<RecipeCategoryConfig.Entry> categories = RecipeCategoryConfig.all();
        int shown = Math.min(visibleRows, Math.max(0, rowCount() - listScroll));
        for (int i = 0; i < shown; i++) {
            int index = listScroll + i;
            int y = listTop + i * ROW;
            if (index == 0) {
                graphics.renderItem(new ItemStack(Items.COMPASS), listLeft + ICON_X, y);
                graphics.drawString(font, Component.translatable("gui.modrecipebook.tab.all"),
                        listLeft + TEXT_X, y + 4, 0xFFFFFF, false);
                graphics.drawString(font, Component.translatable("gui.modrecipebook.config.locked"),
                        listLeft + 200, y + 4, 0x808080, false);
                continue;
            }
            RecipeCategoryConfig.Entry entry = categories.get(index - 1);
            RecipeCategoryConfig.renderItem(graphics, entry.iconStack(), listLeft + ICON_X, y);
            graphics.drawString(font, font.plainSubstrByWidth(entry.title().getString(), 145),
                    listLeft + TEXT_X, y + 4, 0xFFFFFF, false);
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

    private int rowCount() {
        return 1 + RecipeCategoryConfig.all().size();
    }

    private int maxScroll() {
        return Math.max(0, rowCount() - visibleRows);
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

    private static int panelWidth() {
        return LIST_W + 2 * ARROW_SLOT + SIDE_PAD;
    }

    private int panelLeft() {
        return listLeft - ARROW_SLOT - SIDE_PAD;
    }

    private int panelRight() {
        return listLeft + LIST_W + ARROW_SLOT;
    }

    private int scrollbarX() {
        return listLeft + LIST_W + SIDE_PAD;
    }

    private int trackHeight() {
        return Math.max(1, listBottom - listTop);
    }

    private int thumbHeight() {
        int total = rowCount();
        if (total <= visibleRows) {
            return trackHeight();
        }
        return Math.min(trackHeight(), Math.max(8, trackHeight() * visibleRows / total));
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
        return mouseX >= listLeft && mouseX < listLeft + LIST_W && mouseY >= listTop && mouseY < listBottom;
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

    private static final class SortArrows extends AbstractWidget {
        private static final ResourceLocation UP =
                ResourceLocation.withDefaultNamespace("transferable_list/move_up");
        private static final ResourceLocation UP_HOVER =
                ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted");
        private static final ResourceLocation DOWN =
                ResourceLocation.withDefaultNamespace("transferable_list/move_down");
        private static final ResourceLocation DOWN_HOVER =
                ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted");
        private final boolean canUp;
        private final boolean canDown;
        private final Runnable up;
        private final Runnable down;

        private SortArrows(int x, int y, boolean canUp, boolean canDown, Runnable up, Runnable down) {
            super(x, y, ARROW_W, ARROW_H, Component.empty());
            this.canUp = canUp;
            this.canDown = canDown;
            this.up = up;
            this.down = down;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int mid = y + ARROW_H / 2;
            graphics.enableScissor(x, y, x + ARROW_W, y + ARROW_H);
            if (this.canUp) {
                boolean hover = mouseX >= x && mouseX < x + ARROW_W && mouseY >= y && mouseY < mid;
                graphics.blitSprite(hover ? UP_HOVER : UP, x - 18, y - 5, 32, 32);
            }
            if (this.canDown) {
                boolean hover = mouseX >= x && mouseX < x + ARROW_W && mouseY >= mid && mouseY < y + ARROW_H;
                graphics.blitSprite(hover ? DOWN_HOVER : DOWN, x - 18, y - 12, 32, 32);
            }
            graphics.disableScissor();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (this.canUp && mouseY < this.getY() + ARROW_H / 2) {
                this.up.run();
            } else if (this.canDown && mouseY >= this.getY() + ARROW_H / 2) {
                this.down.run();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
