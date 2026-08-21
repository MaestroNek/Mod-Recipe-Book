package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.client.jei.JeiDetailPanel;
import com.minemod.modrecipebook.client.layout.RecipeLayout;
import com.minemod.modrecipebook.client.layout.RecipeLayouts;
import com.minemod.modrecipebook.recipe.IngredientExtractor;
import com.minemod.modrecipebook.recipe.ModRecipeIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.recipebook.PlaceRecipe;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ModRecipeBookComponent implements PlaceRecipe<Ingredient> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "textures/gui/recipe_book.png");
    public static final ResourceLocation DETAIL_TEXTURE = ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "textures/gui/recipe_book_detail.png");
    public static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/button"),
            ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/button_highlighted")
    );
    public static final int IMAGE_WIDTH = 147;
    public static final int IMAGE_HEIGHT = 166;
    public static final int DETAIL_IMAGE_WIDTH = 177;
    public static final int DETAIL_IMAGE_HEIGHT = 166;
    /** Tabs extend 31px past the panel right edge (35px wide, 4px overlap). */
    public static final int TAB_OVERHANG = 31;
    private static final int TAB_WIDTH = 35;
    private static final int TAB_HEIGHT = 27;
    private static final int TAB_FIT = 6;
    private static final int ARROW_W = 17;
    private static final int ARROW_H = 12;
    private static final int TAB_ARROW_GAP = 3;
    private static final EnumMap<RecipeBookType, Boolean> OPEN = new EnumMap<>(RecipeBookType.class);
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);

    private final GhostRecipe ghostRecipe = new GhostRecipe();
    private final ModRecipePage page = new ModRecipePage();
    private final List<ModRecipeTabButton> tabs = new ArrayList<>();
    private final StackedContents stackedContents = new StackedContents();

    private Minecraft minecraft;
    private RecipeBookMenu<?, ?> menu;
    private AbstractContainerScreen<?> screen;
    private RecipeBookComponent vanilla;
    private EditBox searchBox;
    private StateSwitchingButton filterButton;
    private StateSwitchingButton backButton;
    private boolean visible;
    private boolean filteringCraftable;
    private boolean widthTooNarrow;
    private int width;
    private int height;
    private int bookX;
    private int bookY;
    private int tabScroll;
    private String selectedCategory;
    private String lastSearch = "";
    private int timesInventoryChanged = -1;
    private RecipeHolder<?> detail;
    private RecipeGroup detailGroup;
    private int detailIndex;
    private DeviceDetailPanel jeiDetail;
    private Component emptyDetail;

    public void init(int width, int height, Minecraft minecraft, boolean widthTooNarrow, RecipeBookMenu<?, ?> menu,
                     AbstractContainerScreen<?> screen, RecipeBookComponent vanilla) {
        this.width = width;
        this.height = height;
        this.minecraft = minecraft;
        this.widthTooNarrow = widthTooNarrow;
        this.menu = menu;
        this.screen = screen;
        this.vanilla = vanilla;
        this.visible = OPEN.getOrDefault(menu.getRecipeBookType(), false);
        if (this.visible && vanilla != null && vanilla.isVisible()) {
            vanilla.toggleVisibility();
        }
        if (visible) {
            initVisuals();
        }
    }

    public int updateScreenPosition(int screenWidth, int imageWidth) {
        if (!visible || widthTooNarrow) {
            return (screenWidth - imageWidth) / 2;
        }
        return (screenWidth - imageWidth - IMAGE_WIDTH - TAB_OVERHANG) / 2;
    }

    private void refreshLayout() {
        bookX = bookLeft();
        bookY = screen.getGuiTop();
        if (jeiDetail != null) {
            jeiDetail.initButtons(bookX, bookY, panelHeight());
        } else if (backButton != null) {
            backButton.visible = detail != null || emptyDetail != null;
            backButton.setPosition(bookX + (emptyDetail != null ? 6 : 8), bookY + (emptyDetail != null ? 4 : 6));
            if (searchBox != null) {
                searchBox.setPosition(bookX + 25, bookY + 14);
            }
            if (filterButton != null) {
                filterButton.setPosition(bookX + 110, bookY + 12);
            }
            page.init(minecraft, bookX, bookY);
        }
        positionTabs();
    }

    /** Re-anchor the book after the screen shifts (leftPos updated). */
    public void syncLayout() {
        if (!visible) {
            return;
        }
        if (searchBox == null) {
            initVisuals();
        } else {
            refreshLayout();
        }
    }

    public void initVisuals() {
        bookX = bookLeft();
        bookY = screen.getGuiTop();
        stackedContents.clear();
        if (minecraft.player != null) {
            minecraft.player.getInventory().fillStackedContents(stackedContents);
        }
        menu.fillCraftSlotsStackedContents(stackedContents);
        String search = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(minecraft.font, bookX + 25, bookY + 14, 80, 14, SEARCH_HINT);
        searchBox.setMaxLength(50);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(search);
        searchBox.setHint(SEARCH_HINT);
        filterButton = new StateSwitchingButton(bookX + 110, bookY + 12, 26, 16, filteringCraftable);
        filterButton.initTextureValues(new WidgetSprites(
                ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled"),
                ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled"),
                ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
                ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted")
        ));
        backButton = new StateSwitchingButton(bookX + 8, bookY + 6, 12, 17, true);
        backButton.initTextureValues(new WidgetSprites(
                ResourceLocation.withDefaultNamespace("recipe_book/page_backward"),
                ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted")
        ));
        backButton.visible = false;
        if (jeiDetail != null) {
            jeiDetail.initButtons(bookX, bookY, panelHeight());
        }
        rebuildTabs();
        page.init(minecraft, bookX, bookY);
        updateCollections(true);
    }

    private int panelWidth() {
        if (jeiDetail != null) {
            return jeiDetail.width();
        }
        if (emptyDetail != null) {
            return DETAIL_IMAGE_WIDTH;
        }
        return IMAGE_WIDTH;
    }

    private int panelHeight() {
        return screen != null ? screen.getYSize() : IMAGE_HEIGHT;
    }

    private int bookLeft() {
        int attached = screen.getGuiLeft() + screen.getXSize();
        int panelWidth = panelWidth();
        if (widthTooNarrow || attached + panelWidth > width) {
            return width - panelWidth - 2;
        }
        return attached + 2;
    }

    private void openDetail(RecipeGroup group, RecipeHolder<?> preferred) {
        emptyDetail = null;
        detailGroup = group;
        detailIndex = 0;
        for (int i = 0; i < group.recipes().size(); i++) {
            if (group.recipes().get(i).id().equals(preferred.id())) {
                detailIndex = i;
                break;
            }
        }
        detail = group.recipes().get(detailIndex);
        jeiDetail = JeiDetailPanel.tryCreate(group, detailIndex);
        refreshLayout();
    }

    private void closeDetail() {
        detail = null;
        detailGroup = null;
        detailIndex = 0;
        jeiDetail = null;
        emptyDetail = null;
        refreshLayout();
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
    private void rebuildTabs() {
        tabs.clear();
        tabs.add(new ModRecipeTabButton(null, new ItemStack(Items.COMPASS), Component.translatable("gui.modrecipebook.tab.all")));
        for (RecipeCategoryConfig.Entry category : RecipeCategoryConfig.all()) {
            tabs.add(new ModRecipeTabButton(category.id, category.iconStack(), category.title()));
        }
        if (selectedCategory != null && tabs.stream().noneMatch(t -> selectedCategory.equals(t.categoryId()))) {
            selectedCategory = null;
        }
        positionTabs();
        for (ModRecipeTabButton tab : tabs) {
            tab.setSelected((selectedCategory == null && tab.categoryId() == null)
                    || (selectedCategory != null && selectedCategory.equals(tab.categoryId())));
        }
    }

    private void positionTabs() {
        int maxVisible = maxVisibleTabs();
        tabScroll = Math.max(0, Math.min(tabScroll, Math.max(0, tabs.size() - maxVisible)));
        int y = tabRowTop();
        int i = 0;
        for (int index = 0; index < tabs.size(); index++) {
            ModRecipeTabButton tab = tabs.get(index);
            boolean show = index >= tabScroll && i < maxVisible;
            tab.visible = show;
            if (show) {
                tab.setPosition(bookX + panelWidth() - 5, y + i * TAB_HEIGHT);
                i++;
            }
        }
    }

    private boolean tabOverflow() {
        return tabs.size() > TAB_FIT;
    }

    private int maxVisibleTabs() {
        return tabOverflow() ? TAB_FIT - 1 : TAB_FIT;
    }

    private int tabRowTop() {
        if (!tabOverflow()) {
            return bookY + 3;
        }
        int total = ARROW_H + TAB_ARROW_GAP + maxVisibleTabs() * TAB_HEIGHT + TAB_ARROW_GAP + ARROW_H;
        return bookY + (IMAGE_HEIGHT - total) / 2 + ARROW_H + TAB_ARROW_GAP;
    }

    private boolean scrollTabs(int delta) {
        int maxScroll = Math.max(0, tabs.size() - maxVisibleTabs());
        int next = Math.max(0, Math.min(maxScroll, tabScroll + delta));
        if (next == tabScroll) {
            return tabOverflow();
        }
        tabScroll = next;
        positionTabs();
        return true;
    }

    private int tabArrowX() {
        return bookX + panelWidth() - 5 + (TAB_WIDTH - ARROW_W) / 2;
    }

    private int tabArrowY(boolean up) {
        if (up) {
            return tabRowTop() - TAB_ARROW_GAP - ARROW_H + 1;
        }
        return tabRowTop() + maxVisibleTabs() * TAB_HEIGHT + TAB_ARROW_GAP - 2;
    }

    private boolean overTabArrow(double mouseX, double mouseY, boolean up) {
        int x = tabArrowX();
        int y = tabArrowY(up);
        return mouseX >= x && mouseX < x + ARROW_W && mouseY >= y && mouseY < y + ARROW_H;
    }

    private void drawTabArrow(GuiGraphics graphics, boolean up, int mouseX, int mouseY) {
        int x = tabArrowX();
        int y = tabArrowY(up);
        boolean hover = overTabArrow(mouseX, mouseY, up);
        String name = up
                ? (hover ? "recipe_book/page_up_highlighted" : "recipe_book/page_up")
                : (hover ? "recipe_book/page_down_highlighted" : "recipe_book/page_down");
        graphics.blitSprite(ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, name), x, y, ARROW_W, ARROW_H);
    }

    public ImageButton createToggleButton(int x, int y, net.minecraft.client.gui.components.Button.OnPress onPress) {
        return new ImageButton(x, y, 20, 18, BUTTON_SPRITES, onPress,
                Component.translatable("gui.modrecipebook.toggle")) {
            @Override
            public boolean isHoveredOrFocused() {
                return isHovered() || ModRecipeBookComponent.this.isVisible();
            }
        };
    }

    public void toggleVisibility() {
        setVisible(!visible);
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        if (menu != null) {
            OPEN.put(menu.getRecipeBookType(), visible);
        }
        if (visible) {
            initVisuals();
        } else {
            detail = null;
            detailGroup = null;
            detailIndex = 0;
            jeiDetail = null;
            clearGhosts();
            lastSearch = "";
            emptyDetail = null;
            if (searchBox != null) {
                searchBox.setValue("");
                searchBox.setFocused(false);
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isVanillaVisible() {
        return vanilla != null && vanilla.isVisible();
    }

    public Rect2i overlayExclusion() {
        return new Rect2i(bookX, bookY, panelWidth() + TAB_OVERHANG, Math.max(IMAGE_HEIGHT, panelHeight()));
    }

    public void tick() {
        if (!visible) {
            return;
        }
        if (widthTooNarrow) {
            int nextX = bookLeft();
            if (nextX != bookX) {
                refreshLayout();
            }
        }
        if (searchBox != null) {
            if (!lastSearch.equals(searchBox.getValue())) {
                lastSearch = searchBox.getValue();
                updateCollections(false);
            }
        }
        int timesChanged = minecraft.player == null ? 0 : minecraft.player.getInventory().getTimesChanged();
        if (timesChanged != timesInventoryChanged) {
            timesInventoryChanged = timesChanged;
            updateCollections(false);
        }
        if (jeiDetail != null) {
            jeiDetail.tick();
        } else if (detail != null && detailGroup != null) {
            tryUpgradeDetail();
        }
        int tabX = bookX + panelWidth() - 5;
        for (ModRecipeTabButton tab : tabs) {
            if (tab.visible && tab.getX() != tabX) {
                positionTabs();
                break;
            }
        }
    }

    private void tryUpgradeDetail() {
        if (jeiDetail != null || detailGroup == null) {
            return;
        }
        JeiDetailPanel upgraded = JeiDetailPanel.tryCreate(detailGroup, detailIndex);
        if (upgraded != null) {
            jeiDetail = upgraded;
            refreshLayout();
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        if (jeiDetail != null) {
            jeiDetail.render(graphics, mouseX, mouseY, partialTick);
        } else if (emptyDetail != null) {
            int w = DETAIL_IMAGE_WIDTH;
            int h = panelHeight();
            graphics.blit(DETAIL_TEXTURE, bookX, bookY, 0, 0, w, h, w, DETAIL_IMAGE_HEIGHT);
            backButton.visible = true;
            backButton.render(graphics, mouseX, mouseY, partialTick);
            int textX = bookX + (w - minecraft.font.width(emptyDetail)) / 2;
            graphics.drawString(minecraft.font, emptyDetail, textX, bookY + h / 2 - 4, 0x404040, false);
        } else {
            graphics.blit(TEXTURE, bookX, bookY, 1, 1, IMAGE_WIDTH, IMAGE_HEIGHT);
            if (detail != null) {
                backButton.visible = true;
                backButton.render(graphics, mouseX, mouseY, partialTick);
                RecipeLayout layout = RecipeLayouts.of(detail);
                layout.render(graphics, bookX, bookY, detail, mouseX, mouseY, partialTick);
            } else {
                backButton.visible = false;
                if (searchBox != null) {
                    searchBox.render(graphics, mouseX, mouseY, partialTick);
                }
                filterButton.render(graphics, mouseX, mouseY, partialTick);
                page.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        for (ModRecipeTabButton tab : tabs) {
            if (tab.visible) {
                tab.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        if (tabScroll > 0) {
            drawTabArrow(graphics, true, mouseX, mouseY);
        }
        if (tabScroll + maxVisibleTabs() < tabs.size()) {
            drawTabArrow(graphics, false, mouseX, mouseY);
        }
        graphics.pose().popPose();
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        if (jeiDetail != null) {
            jeiDetail.renderOverlays(graphics, mouseX, mouseY);
        } else if (detail != null) {
            RecipeLayouts.of(detail).renderTooltip(graphics, bookX, bookY, detail, mouseX, mouseY);
        } else if (emptyDetail == null) {
            page.renderTooltip(graphics, mouseX, mouseY);
            if (filterButton.isHovered()) {
                graphics.renderTooltip(minecraft.font, Component.translatable(
                        filteringCraftable ? "gui.modrecipebook.filter.craftable" : "gui.modrecipebook.filter.all"
                ), mouseX, mouseY);
            }
        }
        for (ModRecipeTabButton tab : tabs) {
            if (tab.visible) {
                tab.renderTooltip(graphics, mouseX, mouseY);
            }
        }
    }

    public void renderGhostRecipe(GuiGraphics graphics, int leftPos, int topPos, float partialTick) {
        if (visible) {
            ghostRecipe.render(graphics, minecraft, leftPos, topPos, true, partialTick);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        if (button == 0) {
            if (tabOverflow()) {
                if (overTabArrow(mouseX, mouseY, true) && tabScroll > 0) {
                    if (scrollTabs(-1)) {
                        playClick();
                    }
                    return true;
                }
                if (overTabArrow(mouseX, mouseY, false) && tabScroll + maxVisibleTabs() < tabs.size()) {
                    if (scrollTabs(1)) {
                        playClick();
                    }
                    return true;
                }
            }
            for (ModRecipeTabButton tab : tabs) {
                if (tab.visible && tab.mouseClicked(mouseX, mouseY, button)) {
                    selectedCategory = tab.categoryId();
                    for (ModRecipeTabButton other : tabs) {
                        other.setSelected(other == tab);
                    }
                    detail = null;
                    detailGroup = null;
                    detailIndex = 0;
                    jeiDetail = null;
                    emptyDetail = null;
                    updateCollections(true);
                    refreshLayout();
                    return true;
                }
            }
        }
        if (jeiDetail instanceof JeiDetailPanel jei) {
            if (button == 0 && jei.backButton().mouseClicked(mouseX, mouseY, button)) {
                closeDetail();
                return true;
            }
            if (button == 1) {
                Optional<ItemStack> clicked = jei.itemUnderMouse(mouseX, mouseY);
                if (clicked.isPresent()) {
                    openItemRecipes(clicked.get());
                    playClick();
                    return true;
                }
                return jei.isMouseOver(mouseX, mouseY);
            }
            if (jei.mouseClicked(mouseX, mouseY, button)) {
                detail = jei.recipe();
                refreshLayout();
                return true;
            }
            return jei.isMouseOver(mouseX, mouseY);
        }
        if (detail != null || emptyDetail != null) {
            if (button == 0 && backButton.mouseClicked(mouseX, mouseY, button)) {
                closeDetail();
                return true;
            }
            if (button == 1 && detail != null) {
                Optional<ItemStack> clicked = RecipeLayouts.of(detail).itemUnderMouse(bookX, bookY, detail, mouseX, mouseY);
                if (clicked.isPresent()) {
                    openItemRecipes(clicked.get());
                    playClick();
                    return true;
                }
            }
            return isMouseOver(mouseX, mouseY);
        }
        if (button != 0) {
            ModRecipeButton hovered = page.hovered(mouseX, mouseY);
            if (button == 1 && hovered != null && hovered.group() != null) {
                RecipeHolder<?> preferred = placeableRecipe(hovered.group());
                openDetail(hovered.group(), preferred != null ? preferred : hovered.recipe());
                clearGhosts();
                playClick();
                return true;
            }
            return isMouseOver(mouseX, mouseY);
        }
        if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
            searchBox.setFocused(true);
            return true;
        }
        if (searchBox != null) {
            searchBox.setFocused(false);
        }
        if (filterButton.mouseClicked(mouseX, mouseY, button)) {
            filteringCraftable = !filteringCraftable;
            filterButton.setStateTriggered(filteringCraftable);
            updateCollections(true);
            return true;
        }
        if (page.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        ModRecipeButton hovered = page.hovered(mouseX, mouseY);
        if (hovered != null && hovered.group() != null) {
            RecipeHolder<?> placeable = placeableRecipe(hovered.group());
            if (placeable != null) {
                place(placeable, Screen.hasShiftDown());
            }
            return true;
        }
        return isMouseOver(mouseX, mouseY);
    }

    private void openItemRecipes(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack focused = stack.copyWithCount(1);
        RecipeGroup group = findUnlockedGroup(focused);
        RecipeHolder<?> fallback = group == null ? null : group.primary();
        JeiDetailPanel panel = JeiDetailPanel.tryCreate(focused, fallback);
        if (panel == null) {
            ItemStack plain = new ItemStack(stack.getItem());
            if (!ItemStack.isSameItemSameComponents(focused, plain)) {
                panel = JeiDetailPanel.tryCreate(plain, fallback);
            }
        }
        if (panel != null) {
            if (group != null || panel.anyRecipe(this::isRecipeKnown)) {
                emptyDetail = null;
                jeiDetail = panel;
                detail = panel.recipe();
                detailGroup = group;
                detailIndex = 0;
                refreshLayout();
            } else {
                showEmptyDetail("gui.modrecipebook.unknown_recipe");
            }
            return;
        }
        if (group != null) {
            openDetail(group, group.primary());
            return;
        }
        showEmptyDetail(anyOutputRecipeExists(focused)
                ? "gui.modrecipebook.unknown_recipe"
                : "gui.modrecipebook.no_recipe");
    }

    private boolean isRecipeKnown(Object recipe) {
        if (!(recipe instanceof RecipeHolder<?> holder)) {
            return true;
        }
        return ClientUnlockedRecipes.isUnlocked(holder.id());
    }

    private boolean anyOutputRecipeExists(ItemStack stack) {
        if (minecraft.level == null || stack.isEmpty()) {
            return false;
        }
        String key = itemKey(stack);
        for (RecipeHolder<?> holder : minecraft.level.getRecipeManager().getRecipes()) {
            ItemStack out = IngredientExtractor.result(holder.value(), minecraft.level.registryAccess());
            if (!out.isEmpty() && key.equals(itemKey(out))) {
                return true;
            }
        }
        return false;
    }

    private static String itemKey(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private RecipeGroup findUnlockedGroup(ItemStack stack) {
        if (minecraft.level == null || stack.isEmpty()) {
            return null;
        }
            String key = itemKey(stack);
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        ItemStack result = ItemStack.EMPTY;
        List<RecipeHolder<?>> source = new ArrayList<>(ModRecipeIndex.all());
        source.addAll(ModRecipeIndex.recipesByItemMod("minecraft"));
        for (RecipeHolder<?> holder : source) {
            if (!isRecipeKnown(holder)) {
                continue;
            }
            ItemStack out = IngredientExtractor.result(holder.value(), minecraft.level.registryAccess());
            if (out.isEmpty() || !key.equals(itemKey(out))) {
                continue;
            }
            if (result.isEmpty()) {
                result = out.copy();
            }
            recipes.add(holder);
        }
        return recipes.isEmpty() ? null : new RecipeGroup(result, recipes);
    }

    private void showEmptyDetail(String translationKey) {
        jeiDetail = null;
        detail = null;
        detailGroup = null;
        detailIndex = 0;
        emptyDetail = Component.translatable(translationKey);
        refreshLayout();
    }

    private RecipeHolder<?> placeableRecipe(RecipeGroup group) {
        RecipeHolder<?> fallback = null;
        for (RecipeHolder<?> holder : group.recipes()) {
            if (!canPlaceIntoMenu(holder)) {
                continue;
            }
            if (fallback == null) {
                fallback = holder;
            }
            if (canCraft(holder)) {
                return holder;
            }
        }
        return fallback;
    }

    private boolean canPlaceIntoMenu(RecipeHolder<?> recipe) {
        if (minecraft.player == null || menu == null) {
            return false;
        }
        RecipeType<?> type = recipe.value().getType();
        boolean fits = switch (menu.getRecipeBookType()) {
            case CRAFTING -> recipe.value() instanceof CraftingRecipe;
            case FURNACE -> type == RecipeType.SMELTING;
            case BLAST_FURNACE -> type == RecipeType.BLASTING;
            case SMOKER -> type == RecipeType.SMOKING;
        };
        return fits && recipe.value().canCraftInDimensions(menu.getGridWidth(), menu.getGridHeight());
    }

    private void place(RecipeHolder<?> recipe, boolean placeAll) {
        if (!canPlaceIntoMenu(recipe) || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        clearGhosts();
        minecraft.gameMode.handlePlaceRecipe(minecraft.player.containerMenu.containerId, recipe, placeAll);
    }

    private void clearGhosts() {
        ghostRecipe.clear();
        if (vanilla != null && menu != null && !menu.slots.isEmpty()) {
            vanilla.slotClicked(menu.slots.getFirst());
        }
    }

    public void setupGhostRecipe(RecipeHolder<?> recipe, List<Slot> slots) {
        ghostRecipe.clear();
        ItemStack result = IngredientExtractor.result(recipe.value(), minecraft.level.registryAccess());
        ghostRecipe.setRecipe(recipe);
        ghostRecipe.addIngredient(Ingredient.of(result), slots.get(0).x, slots.get(0).y);
        placeRecipe(menu.getGridWidth(), menu.getGridHeight(), menu.getResultSlotIndex(), recipe,
                recipe.value().getIngredients().iterator(), 0);
    }

    @Override
    public void addItemToSlot(Ingredient ingredient, int slot, int maxAmount, int x, int y) {
        if (!ingredient.isEmpty()) {
            Slot menuSlot = menu.slots.get(slot);
            ghostRecipe.addIngredient(ingredient, menuSlot.x, menuSlot.y);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchBox.setFocused(false);
                return true;
            }
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return visible && searchBox != null && searchBox.charTyped(codePoint, modifiers);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible) {
            return false;
        }
        if (jeiDetail instanceof JeiDetailPanel jei && jei.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (!tabOverflow()) {
            return false;
        }
        boolean overTabs = mouseX >= bookX + panelWidth() - 8 && mouseX < bookX + panelWidth() + TAB_WIDTH
                && mouseY >= bookY && mouseY < bookY + panelHeight();
        if (!overTabs) {
            return false;
        }
        return scrollTabs(-(int) Math.signum(delta));
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) {
            return false;
        }
        if (jeiDetail != null) {
            if (jeiDetail.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        } else if (mouseX >= bookX && mouseY >= bookY && mouseX < bookX + panelWidth() && mouseY < bookY + panelHeight()) {
            return true;
        }
        for (ModRecipeTabButton tab : tabs) {
            if (tab.visible && tab.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return tabOverflow() && (overTabArrow(mouseX, mouseY, true) || overTabArrow(mouseX, mouseY, false));
    }

    public void slotClicked(Slot slot) {
        if (visible) {
            clearGhosts();
            updateCollections(false);
        }
    }

    public void recipesUpdated() {
        if (visible) {
            updateCollections(false);
        }
    }

    private void updateCollections(boolean resetPage) {
        if (minecraft.level == null) {
            return;
        }
        stackedContents.clear();
        if (minecraft.player != null) {
            minecraft.player.getInventory().fillStackedContents(stackedContents);
            menu.fillCraftSlotsStackedContents(stackedContents);
            timesInventoryChanged = minecraft.player.getInventory().getTimesChanged();
        }
        List<RecipeHolder<?>> source;
        if (selectedCategory == null) {
            source = ModRecipeIndex.all();
            if (RecipeCategoryConfig.hideVanillaBook()) {
                source.addAll(ModRecipeIndex.recipesByItemMod("minecraft"));
            }
        } else {
            RecipeCategoryConfig.Entry category = RecipeCategoryConfig.find(selectedCategory);
            source = category == null ? List.of() : category.recipes();
        }
        Map<String, RecipeGroup> grouped = new LinkedHashMap<>();
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT);
        for (RecipeHolder<?> holder : source) {
            if (!isRecipeKnown(holder)) {
                continue;
            }
            ItemStack result = IngredientExtractor.result(holder.value(), minecraft.level.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            if (!query.isEmpty()) {
                String name = I18n.get(result.getDescriptionId()).toLowerCase(Locale.ROOT);
                String id = holder.id().toString().toLowerCase(Locale.ROOT);
                if (!name.contains(query) && !id.contains(query)) {
                    continue;
                }
            }
            String key = itemKey(result);
            grouped.computeIfAbsent(key, k -> new RecipeGroup(result.copy(), new ArrayList<>())).recipes().add(holder);
        }
        addPlaceableRecipes(grouped);
        List<RecipeGroup> collections = new ArrayList<>(grouped.values());
        if (filteringCraftable) {
            collections.removeIf(group -> {
                for (RecipeHolder<?> holder : group.recipes()) {
                    if (canCraft(holder)) {
                        return false;
                    }
                }
                return true;
            });
        }
        page.setCollections(collections, resetPage, this::canCraft);
    }

    private void addPlaceableRecipes(Map<String, RecipeGroup> grouped) {
        if (grouped.isEmpty() || minecraft.level == null) {
            return;
        }
        for (RecipeHolder<?> holder : minecraft.level.getRecipeManager().getRecipes()) {
            if (!canPlaceIntoMenu(holder)) {
                continue;
            }
            ItemStack result = IngredientExtractor.result(holder.value(), minecraft.level.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            RecipeGroup group = grouped.get(itemKey(result));
            if (group == null) {
                continue;
            }
            boolean known = false;
            for (RecipeHolder<?> existing : group.recipes()) {
                if (existing.id().equals(holder.id())) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                group.recipes().add(holder);
            }
        }
    }

    private boolean canCraft(RecipeHolder<?> holder) {
        if (minecraft.player == null || menu == null) {
            return false;
        }
        if (!canPlaceIntoMenu(holder)) {
            return false;
        }
        if (!holder.value().canCraftInDimensions(menu.getGridWidth(), menu.getGridHeight())) {
            return false;
        }
        return IngredientExtractor.canCraft(holder.value(), minecraft.player.getInventory(), stackedContents);
    }
}
