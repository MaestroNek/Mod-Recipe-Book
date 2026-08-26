package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CategoryEditScreen extends Screen {
    private static final int ROW = 20;
    private static final int LEFT_W = 260;
    private static final int RIGHT_W = 180;
    private static final int GAP = 8;
    private static final int PAD = 6;
    private static final int ROW_PAD = 4;
    private static final int CELL = 18;
    private static final int CHECK = 16;
    private static final int MARK = 15;
    private static final int SCROLLBAR_W = 6;
    private static final int HEADER_Y = 40;
    private static final int LABEL_Y = 72;
    private static final int LIST_TOP = 86;
    private static final int ICON_BTN_W = 90;
    private static final int SEARCH_W = 110;
    private static final int SEARCH_H = 16;
    private static final int LIST_BG = 0x80000000;
    private static final int UNASSIGNED = 0xE8C84A;
    private static final float PREVIEW_PX_PER_MS = 0.02f;
    private static final ResourceLocation CHECKBOX =
            ResourceLocation.withDefaultNamespace("widget/checkbox");
    private static final ResourceLocation CHECKBOX_ON =
            ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
    private static final ResourceLocation WARNING =
            ResourceLocation.withDefaultNamespace("world_list/warning");
    private static final ResourceLocation SCROLLER =
            ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final ResourceLocation SCROLLER_BG =
            ResourceLocation.withDefaultNamespace("widget/scroller_background");
    private final Screen parent;
    private final RecipeCategoryConfig.Entry draft;
    private final List<String> catalog = new ArrayList<>();
    private final List<String> shown = new ArrayList<>();
    private String filter = "";
    private final Map<String, List<ItemStack>> itemsByMod = new LinkedHashMap<>();
    private final Map<String, ItemStack> modIcon = new HashMap<>();
    private int modsLeft;
    private int itemsLeft;
    private int listTop;
    private int listBottom;
    private int visibleRows;
    private int modScroll;
    private boolean draggingBar;
    private int dragOffset;
    private String previewMod;
    private float previewScroll;
    private long previewMs;

    public CategoryEditScreen(Screen parent, RecipeCategoryConfig.Entry entry) {
        super(Component.translatable("gui.modrecipebook.config.edit.title"));
        this.parent = parent;
        this.draft = entry == null ? new RecipeCategoryConfig.Entry() : entry.copy();
        if (this.draft.id == null || this.draft.id.isBlank()) {
            this.draft.id = RecipeCategoryConfig.newId();
        }
        ClientRecipeIndex.ensureIndexed();
        Set<String> withContent = namespacesWithItemsOrBlocks();
        boolean recipesReady = ModRecipeIndex.recipesIndexed();
        for (IModInfo info : ModList.get().getMods()) {
            String id = info.getModId();
            boolean listed = recipesReady ? ModRecipeIndex.hasCraftableItem(id) : withContent.contains(id);
            if (listed || draft.mods.contains(id)) {
                catalog.add(id);
            }
        }
        catalog.sort(Comparator.comparing(RecipeCategoryConfig::modName, String.CASE_INSENSITIVE_ORDER));
        catalog.remove("minecraft");
        catalog.add(0, "minecraft");
        Map<String, ItemStack> fallbackIcon = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            String ns = id.getNamespace();
            fallbackIcon.putIfAbsent(ns, stack);
            if (recipesReady) {
                continue;
            }
            itemsByMod.computeIfAbsent(ns, k -> new ArrayList<>()).add(stack);
            modIcon.putIfAbsent(ns, stack);
        }
        if (recipesReady) {
            for (String ns : catalog) {
                List<ItemStack> stacks = ModRecipeIndex.craftableStacks(ns);
                if (stacks.isEmpty()) {
                    continue;
                }
                itemsByMod.put(ns, new ArrayList<>(stacks));
                modIcon.putIfAbsent(ns, stacks.getFirst());
            }
        }
        for (String id : catalog) {
            modIcon.putIfAbsent(id, fallbackIcon.get(id));
        }
        applyFilter();
    }

    private static Set<String> namespacesWithItemsOrBlocks() {
        Set<String> namespaces = new HashSet<>();
        BuiltInRegistries.ITEM.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        BuiltInRegistries.BLOCK.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        namespaces.remove("minecraft");
        namespaces.remove("neoforge");
        namespaces.remove("modrecipebook");
        return namespaces;
    }

    @Override
    protected void init() {
        int total = LEFT_W + GAP + RIGHT_W;
        modsLeft = width / 2 - total / 2;
        itemsLeft = modsLeft + LEFT_W + GAP;
        listTop = LIST_TOP;
        listBottom = height - 40;
        visibleRows = Math.max(1, (innerBottom() - innerTop()) / ROW);
        clampScroll();
        addRenderableWidget(new IconPickButton(modsLeft, HEADER_Y, ICON_BTN_W, 20, b ->
                minecraft.setScreen(new ItemPickScreen(this, stack -> {
                    draft.icon = com.minemod.modrecipebook.recipe.PotionKeys.itemKey(stack);
                }))));
        int nameLeft = modsLeft + ICON_BTN_W + 4;
        int nameRight = itemsLeft + RIGHT_W;
        EditBox nameBox = new EditBox(font, nameLeft, HEADER_Y, nameRight - nameLeft, 20,
                Component.translatable("gui.modrecipebook.config.name"));
        nameBox.setMaxLength(40);
        nameBox.setHint(Component.translatable("gui.modrecipebook.config.name")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        nameBox.setValue(draft.name == null ? "" : draft.name);
        nameBox.setResponder(value -> draft.name = value);
        addRenderableWidget(nameBox);
        int searchW = Math.min(SEARCH_W, Math.max(70,
                LEFT_W - font.width(Component.translatable("gui.modrecipebook.config.mods")) - 8));
        EditBox search = new EditBox(font, modsLeft + LEFT_W - searchW, LABEL_Y - 3, searchW, SEARCH_H,
                Component.translatable("gui.modrecipebook.search"));
        search.setHint(Component.translatable("gui.modrecipebook.search")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        search.setValue(filter);
        search.setResponder(value -> {
            filter = value;
            modScroll = 0;
            applyFilter();
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 - 154, height - 28, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            draft.name = draft.name == null ? "" : draft.name.trim();
            RecipeCategoryConfig.Entry existing = RecipeCategoryConfig.find(draft.id);
            if (existing == null) {
                RecipeCategoryConfig.all().add(draft);
            } else {
                RecipeCategoryConfig.update(draft);
            }
            RecipeCategoryConfig.save();
            minecraft.setScreen(parent);
        }).bounds(width / 2 + 4, height - 28, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.mods"),
                modsLeft, LABEL_Y, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.items"),
                itemsLeft, LABEL_Y, 0xA0A0A0, false);
        graphics.fill(modsLeft, listTop, modsLeft + LEFT_W, listBottom, LIST_BG);
        graphics.fill(itemsLeft, listTop, itemsLeft + RIGHT_W, listBottom, LIST_BG);
        updatePreview(mouseX, mouseY);
        Component unassignedTip = renderMods(graphics, mouseX, mouseY);
        renderPreview(graphics, mouseX, mouseY);
        if (unassignedTip != null) {
            graphics.renderTooltip(font, unassignedTip, mouseX, mouseY);
        }
    }

    private Component renderMods(GuiGraphics graphics, int mouseX, int mouseY) {
        clampScroll();
        int top = innerTop();
        int bottom = rowsBottom();
        int left = modsLeft + PAD;
        int contentX = left + ROW_PAD;
        int barX = scrollbarX();
        graphics.enableScissor(left, top, barX, bottom);
        Component markTip = null;
        for (int i = 0; i < visibleRows; i++) {
            int index = modScroll + i;
            if (index >= shown.size()) {
                break;
            }
            String modId = shown.get(index);
            int y = top + i * ROW;
            boolean on = draft.mods.contains(modId);
            boolean hover = mouseX >= left && mouseX < barX && mouseY >= y && mouseY < y + ROW;
            if (hover) {
                graphics.fill(left, y, barX, y + ROW, 0x40FFFFFF);
            }
            graphics.blitSprite(on ? CHECKBOX_ON : CHECKBOX, contentX, y + 2, CHECK, CHECK);
            ItemStack icon = modIcon.get(modId);
            int iconX = contentX + CHECK + 3;
            if (icon != null) {
                RecipeCategoryConfig.renderItem(graphics, icon, iconX, y + 2);
            } else {
                graphics.fill(iconX, y + 2, iconX + 16, y + 18, 0xFF3F3F3F);
            }
            boolean free = unassigned(modId);
            int markX = barX - MARK - 2;
            int textX = iconX + 16 + 3;
            int textW = Math.max(8, (free ? markX - 2 : barX) - textX);
            int color = free ? UNASSIGNED : (on ? 0xFFFFFF : 0xA0A0A0);
            graphics.drawString(font, font.plainSubstrByWidth(RecipeCategoryConfig.modName(modId), textW),
                    textX, y + 6, color, false);
            if (free) {
                int markY = y + (ROW - MARK) / 2;
                graphics.setColor(
                        (UNASSIGNED >> 16 & 0xFF) / 255.0F,
                        (UNASSIGNED >> 8 & 0xFF) / 255.0F,
                        (UNASSIGNED & 0xFF) / 255.0F,
                        1.0F);
                graphics.blitSprite(WARNING, markX, markY, MARK, MARK);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                if (mouseX >= markX && mouseX < markX + MARK && mouseY >= markY && mouseY < markY + MARK) {
                    markTip = Component.translatable("gui.modrecipebook.config.unassigned");
                }
            }
        }
        graphics.disableScissor();
        renderScrollbar(graphics);
        return markTip;
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        int top = innerTop();
        int bottom = innerBottom();
        int left = itemsLeft + PAD;
        int right = itemsLeft + RIGHT_W - PAD;
        int innerH = Math.max(1, bottom - top);
        int innerW = Math.max(CELL, right - left);
        int cols = Math.max(1, innerW / CELL);
        if (previewMod == null) {
            graphics.drawString(font, Component.translatable("gui.modrecipebook.config.items.hint"),
                    left, top + 4, 0x808080, false);
            return;
        }
        List<ItemStack> items = itemsByMod.getOrDefault(previewMod, List.of());
        if (items.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.modrecipebook.config.items.empty"),
                    left, top + 4, 0x808080, false);
            return;
        }
        int rows = (items.size() + cols - 1) / cols;
        int contentH = rows * CELL;
        boolean overflow = contentH > innerH;
        boolean paused = overItems(mouseX, mouseY);
        long now = Util.getMillis();
        if (previewMs == 0) {
            previewMs = now;
        }
        if (overflow && !paused) {
            previewScroll += (now - previewMs) * PREVIEW_PX_PER_MS;
        }
        previewMs = now;
        if (overflow) {
            float loop = contentH;
            previewScroll %= loop;
            if (previewScroll < 0) {
                previewScroll += loop;
            }
        } else {
            previewScroll = 0;
        }
        graphics.enableScissor(left, top, right, bottom);
        ItemStack hovered = ItemStack.EMPTY;
        int copies = overflow ? 2 : 1;
        for (int copy = 0; copy < copies; copy++) {
            int originY = top - (int) previewScroll + copy * contentH;
            for (int i = 0; i < items.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int x = left + col * CELL;
                int y = originY + row * CELL;
                if (y + 16 < top || y > bottom) {
                    continue;
                }
                ItemStack stack = items.get(i);
                RecipeCategoryConfig.renderItem(graphics, stack, x, y);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
                    hovered = stack;
                }
            }
        }
        graphics.disableScissor();
        if (!hovered.isEmpty()) {
            try {
                graphics.renderTooltip(font, hovered, mouseX, mouseY);
            } catch (Throwable ignored) {
            }
        }
    }

    private void updatePreview(int mouseX, int mouseY) {
        if (!overMods(mouseX, mouseY)) {
            return;
        }
        int index = modScroll + (mouseY - innerTop()) / ROW;
        if (index < 0 || index >= shown.size()) {
            return;
        }
        String next = shown.get(index);
        if (!next.equals(previewMod)) {
            previewMod = next;
            previewScroll = 0;
            previewMs = 0;
        }
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
        if (button == 0 && overMods(mouseX, mouseY)) {
            int index = modScroll + (int) ((mouseY - innerTop()) / ROW);
            if (index >= 0 && index < shown.size()) {
                String modId = shown.get(index);
                if (draft.mods.contains(modId)) {
                    draft.mods.remove(modId);
                } else {
                    draft.mods.add(modId);
                }
                return true;
            }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overMods(mouseX, mouseY) || overScrollbar(mouseX, mouseY)) {
            int max = maxScroll();
            int next = (int) Math.max(0, Math.min(max, modScroll - Math.signum(scrollY)));
            if (next != modScroll) {
                modScroll = next;
                return true;
            }
            return max > 0;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private boolean unassigned(String modId) {
        if (draft.mods.contains(modId)) {
            return false;
        }
        for (RecipeCategoryConfig.Entry entry : RecipeCategoryConfig.all()) {
            if (draft.id.equals(entry.id)) {
                continue;
            }
            if (entry.mods.contains(modId)) {
                return false;
            }
        }
        return true;
    }

    private int innerTop() {
        return listTop + PAD;
    }

    private int innerBottom() {
        return listBottom - PAD;
    }

    private int rowsBottom() {
        return innerTop() + visibleRows * ROW;
    }

    private int scrollbarX() {
        return modsLeft + LEFT_W - PAD - SCROLLBAR_W;
    }

    private int trackHeight() {
        return Math.max(1, visibleRows * ROW);
    }

    private void applyFilter() {
        shown.clear();
        String q = filter.toLowerCase(Locale.ROOT);
        for (String id : catalog) {
            if (q.isEmpty()
                    || id.toLowerCase(Locale.ROOT).contains(q)
                    || RecipeCategoryConfig.modName(id).toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(id);
            }
        }
        clampScroll();
    }

    private int maxScroll() {
        return Math.max(0, shown.size() - visibleRows);
    }

    private void clampScroll() {
        modScroll = Math.min(modScroll, maxScroll());
    }

    private int thumbHeight() {
        int total = shown.size();
        if (total <= visibleRows) {
            return trackHeight();
        }
        return Math.min(trackHeight(), Math.max(8, trackHeight() * visibleRows / total));
    }

    private int thumbY() {
        int max = maxScroll();
        int travel = trackHeight() - thumbHeight();
        if (max == 0 || travel <= 0) {
            return innerTop();
        }
        return innerTop() + travel * modScroll / max;
    }

    private boolean overMods(double mouseX, double mouseY) {
        return mouseX >= modsLeft + PAD && mouseX < scrollbarX()
                && mouseY >= innerTop() && mouseY < rowsBottom();
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return maxScroll() > 0
                && mouseX >= scrollbarX() && mouseX < scrollbarX() + SCROLLBAR_W
                && mouseY >= innerTop() && mouseY < rowsBottom();
    }

    private boolean overItems(double mouseX, double mouseY) {
        return mouseX >= itemsLeft && mouseX < itemsLeft + RIGHT_W
                && mouseY >= listTop && mouseY < listBottom;
    }

    private void applyScrollFromMouse(double mouseY) {
        int travel = trackHeight() - thumbHeight();
        if (travel <= 0) {
            return;
        }
        double t = (mouseY - innerTop() - dragOffset) / travel;
        modScroll = (int) Math.round(Math.max(0, Math.min(maxScroll(), t * maxScroll())));
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (maxScroll() <= 0) {
            return;
        }
        graphics.blitSprite(SCROLLER_BG, scrollbarX(), innerTop(), SCROLLBAR_W, trackHeight());
        graphics.blitSprite(SCROLLER, scrollbarX(), thumbY(), SCROLLBAR_W, thumbHeight());
    }

    private final class IconPickButton extends Button {
        IconPickButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.translatable("gui.modrecipebook.config.icon"),
                    onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()),
                    getX(), getY(), getWidth(), getHeight());
            int gap = 4;
            int contentW = 16 + gap + font.width(getMessage());
            int startX = getX() + (getWidth() - contentW) / 2;
            RecipeCategoryConfig.renderItem(graphics, draft.iconStack(), startX, getY() + 2);
            graphics.drawString(font, getMessage(), startX + 16 + gap, getY() + 6, getFGColor(), false);
        }
    }

    static class ItemPickScreen extends Screen {
        private static final int COLS = 9;
        private static final int CELL = 18;
        private final CategoryEditScreen parent;
        private final java.util.function.Consumer<ItemStack> onPick;
        private final List<ItemStack> all = new ArrayList<>();
        private List<ItemStack> filtered = List.of();
        private EditBox search;
        private Button prev;
        private Button next;
        private int page;
        private int rows;
        private int gridLeft;
        private int gridTop;

        ItemPickScreen(CategoryEditScreen parent, java.util.function.Consumer<ItemStack> onPick) {
            super(Component.translatable("gui.modrecipebook.config.pick_item"));
            this.parent = parent;
            this.onPick = onPick;
            Set<String> keys = new HashSet<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty() && keys.add(com.minemod.modrecipebook.recipe.PotionKeys.itemKey(stack))) {
                    all.add(stack);
                }
            }
            for (ItemStack stack : ModRecipeIndex.allCraftableStacks()) {
                if (keys.add(com.minemod.modrecipebook.recipe.PotionKeys.itemKey(stack))) {
                    all.add(stack);
                }
            }
        }

        @Override
        protected void init() {
            gridLeft = width / 2 - (COLS * CELL) / 2;
            gridTop = 48;
            int gridBottom = height - 56;
            rows = Math.max(1, (gridBottom - gridTop) / CELL);
            search = new EditBox(font, gridLeft, 22, COLS * CELL, 16,
                    Component.translatable("gui.modrecipebook.search"));
            search.setResponder(value -> {
                page = 0;
                filter(value);
            });
            addRenderableWidget(search);
            setInitialFocus(search);
            int navY = height - 52;
            prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
                    .bounds(gridLeft, navY, 20, 20)
                    .build());
            next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
                    .bounds(gridLeft + COLS * CELL - 20, navY, 20, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                    .bounds(width / 2 - 50, height - 28, 100, 20)
                    .build());
            filter(search.getValue());
        }

        private void filter(String query) {
            String q = query.toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
                filtered = all;
            } else {
                List<ItemStack> nextItems = new ArrayList<>();
                for (ItemStack stack : all) {
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    String name = I18n.get(stack.getDescriptionId());
                    String hover = stack.getHoverName().getString();
                    if (id.contains(q) || name.toLowerCase(Locale.ROOT).contains(q)
                            || hover.toLowerCase(Locale.ROOT).contains(q)) {
                        nextItems.add(stack);
                    }
                }
                filtered = nextItems;
            }
            refreshNav();
        }

        private int pageSize() {
            return COLS * rows;
        }

        private int pageCount() {
            return Math.max(1, (filtered.size() + pageSize() - 1) / pageSize());
        }

        private void turn(int delta) {
            int count = pageCount();
            page = Math.floorMod(page + delta, count);
            refreshNav();
        }

        private void refreshNav() {
            int count = pageCount();
            page = Math.floorMod(page, count);
            boolean many = count > 1;
            if (prev != null) {
                prev.active = many;
            }
            if (next != null) {
                next.active = many;
            }
        }

        private int indexAt(int row, int col) {
            return page * pageSize() + row * COLS + col;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("gui.modrecipebook.config.page", page + 1, pageCount()),
                    width / 2, height - 47, 0xFFFFFF);
            ItemStack hovered = ItemStack.EMPTY;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < COLS; col++) {
                    int index = indexAt(row, col);
                    if (index >= filtered.size()) {
                        continue;
                    }
                    int x = gridLeft + col * CELL;
                    int y = gridTop + row * CELL;
                    ItemStack stack = filtered.get(index);
                    graphics.fill(x, y, x + 16, y + 16, 0x40808080);
                    RecipeCategoryConfig.renderItem(graphics, stack, x, y);
                    if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                        graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
                        hovered = stack;
                    }
                }
            }
            if (!hovered.isEmpty()) {
                try {
                    graphics.renderTooltip(font, hovered, mouseX, mouseY);
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < COLS; col++) {
                        int index = indexAt(row, col);
                        if (index >= filtered.size()) {
                            continue;
                        }
                        int x = gridLeft + col * CELL;
                        int y = gridTop + row * CELL;
                        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                            onPick.accept(filtered.get(index));
                            minecraft.setScreen(parent);
                            return true;
                        }
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            turn(scrollY > 0 ? -1 : 1);
            return true;
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }
    }
}
