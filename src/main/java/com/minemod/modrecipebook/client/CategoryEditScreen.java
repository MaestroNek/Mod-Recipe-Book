package com.minemod.modrecipebook.client;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CategoryEditScreen extends Screen {
    private static final int ROW = 14;
    private final Screen parent;
    private final RecipeCategoryConfig.Entry draft;
    private final List<String> catalog = new ArrayList<>();
    private int modScroll;
    private int listTop;
    private int listBottom;
    private int listLeft;
    private int visibleRows;

    public CategoryEditScreen(Screen parent, RecipeCategoryConfig.Entry entry) {
        super(Component.translatable("gui.modrecipebook.config.edit"));
        this.parent = parent;
        this.draft = entry == null ? new RecipeCategoryConfig.Entry() : entry.copy();
        if (this.draft.id == null || this.draft.id.isBlank()) {
            this.draft.id = RecipeCategoryConfig.newId();
        }
        Set<String> withContent = namespacesWithItemsOrBlocks();
        for (IModInfo info : ModList.get().getMods()) {
            String id = info.getModId();
            if (withContent.contains(id) || draft.mods.contains(id)) {
                catalog.add(id);
            }
        }
        catalog.sort(Comparator.comparing(RecipeCategoryConfig::modName, String.CASE_INSENSITIVE_ORDER));
        catalog.remove("minecraft");
        catalog.add(0, "minecraft");
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
        listLeft = width / 2 - 150;
        listTop = 78;
        listBottom = height - 40;
        visibleRows = Math.max(1, (listBottom - listTop) / ROW);
        addRenderableWidget(Button.builder(Component.translatable("gui.modrecipebook.config.icon"),
                b -> minecraft.setScreen(new ItemPickScreen(this, stack -> {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    draft.icon = id.toString();
                })))
                .bounds(listLeft + 24, 40, 70, 20)
                .build());
        EditBox nameBox = new EditBox(font, listLeft + 100, 40, 200, 20,
                Component.translatable("gui.modrecipebook.config.name"));
        nameBox.setMaxLength(40);
        nameBox.setHint(Component.translatable("gui.modrecipebook.config.name"));
        nameBox.setValue(draft.name == null ? "" : draft.name);
        nameBox.setResponder(value -> draft.name = value);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 - 154, height - 28, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            draft.name = draft.name == null ? "" : draft.name.trim();
            RecipeCategoryConfig.Entry existing = RecipeCategoryConfig.find(draft.id);
            if (existing == null) {
                if (RecipeCategoryConfig.all().size() < RecipeCategoryConfig.MAX) {
                    RecipeCategoryConfig.all().add(draft);
                }
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
        graphics.drawString(font, Component.translatable("gui.modrecipebook.config.mods"), listLeft, 64, 0xA0A0A0, false);
        graphics.fill(listLeft - 1, 39, listLeft + 19, 59, 0xFF8B8B8B);
        RecipeCategoryConfig.renderItem(graphics, draft.iconStack(), listLeft, 40);
        int maxScroll = Math.max(0, catalog.size() - visibleRows);
        modScroll = Math.min(modScroll, maxScroll);
        graphics.enableScissor(listLeft, listTop, listLeft + 300, listBottom);
        for (int i = 0; i < visibleRows; i++) {
            int index = modScroll + i;
            if (index >= catalog.size()) {
                break;
            }
            String modId = catalog.get(index);
            int y = listTop + i * ROW;
            boolean on = draft.mods.contains(modId);
            boolean hover = mouseX >= listLeft && mouseX < listLeft + 300 && mouseY >= y && mouseY < y + ROW;
            if (hover) {
                graphics.fill(listLeft, y, listLeft + 300, y + ROW, 0x40FFFFFF);
            }
            graphics.drawString(font, (on ? "[x] " : "[ ] ") + RecipeCategoryConfig.modName(modId),
                    listLeft, y + 2, on ? 0xFFFFFF : 0xA0A0A0, false);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listLeft && mouseX < listLeft + 300 && mouseY >= listTop && mouseY < listBottom) {
            int index = modScroll + (int) ((mouseY - listTop) / ROW);
            if (index >= 0 && index < catalog.size()) {
                String modId = catalog.get(index);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listLeft && mouseX < listLeft + 300 && mouseY >= listTop && mouseY < listBottom) {
            int maxScroll = Math.max(0, catalog.size() - visibleRows);
            modScroll = (int) Math.max(0, Math.min(maxScroll, modScroll - Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    static class ItemPickScreen extends Screen {
        private static final int COLS = 9;
        private static final int ROWS = 5;
        private static final int CELL = 18;
        private static final int PAGE = COLS * ROWS;
        private final CategoryEditScreen parent;
        private final java.util.function.Consumer<ItemStack> onPick;
        private final List<ItemStack> all = new ArrayList<>();
        private List<ItemStack> filtered = List.of();
        private EditBox search;
        private Button prev;
        private Button next;
        private int page;
        private int gridLeft;
        private int gridTop;

        ItemPickScreen(CategoryEditScreen parent, java.util.function.Consumer<ItemStack> onPick) {
            super(Component.translatable("gui.modrecipebook.config.pick_item"));
            this.parent = parent;
            this.onPick = onPick;
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    all.add(stack);
                }
            }
        }

        @Override
        protected void init() {
            gridLeft = width / 2 - (COLS * CELL) / 2;
            gridTop = 48;
            search = new EditBox(font, gridLeft, 22, COLS * CELL, 16,
                    Component.translatable("gui.modrecipebook.search"));
            search.setResponder(value -> {
                page = 0;
                filter(value);
            });
            addRenderableWidget(search);
            setInitialFocus(search);
            int navY = gridTop + ROWS * CELL + 6;
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
                    if (id.contains(q) || name.toLowerCase(Locale.ROOT).contains(q)) {
                        nextItems.add(stack);
                    }
                }
                filtered = nextItems;
            }
            refreshNav();
        }

        private int pageCount() {
            return Math.max(1, (filtered.size() + PAGE - 1) / PAGE);
        }

        private void turn(int delta) {
            page = Math.max(0, Math.min(pageCount() - 1, page + delta));
            refreshNav();
        }

        private void refreshNav() {
            page = Math.max(0, Math.min(pageCount() - 1, page));
            if (prev != null) {
                prev.active = page > 0;
            }
            if (next != null) {
                next.active = page < pageCount() - 1;
            }
        }

        private int indexAt(int row, int col) {
            return page * PAGE + row * COLS + col;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("gui.modrecipebook.config.page", page + 1, pageCount()),
                    width / 2, gridTop + ROWS * CELL + 11, 0xFFFFFF);
            ItemStack hovered = ItemStack.EMPTY;
            for (int row = 0; row < ROWS; row++) {
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
                for (int row = 0; row < ROWS; row++) {
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
