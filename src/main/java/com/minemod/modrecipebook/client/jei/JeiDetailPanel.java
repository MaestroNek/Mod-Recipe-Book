package com.minemod.modrecipebook.client.jei;

import com.minemod.modrecipebook.client.RecipeGroup;
import com.minemod.modrecipebook.client.DeviceDetailPanel;
import com.minemod.modrecipebook.client.ModRecipeBookComponent;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

public final class JeiDetailPanel implements DeviceDetailPanel {
    public static final int HEADER_HEIGHT = 24;
    public static final int PADDING = 8;
    public static final int TAB_SIZE = 24;
    public static final int TAB_OVERLAP = 3;
    public static final int MIN_WIDTH = ModRecipeBookComponent.DETAIL_IMAGE_WIDTH;
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath(com.minemod.modrecipebook.ModRecipeBook.MODID, "recipe_book/category_top");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath(com.minemod.modrecipebook.ModRecipeBook.MODID, "recipe_book/category_top_selected");

    private final RecipeHolder<?> fallbackRecipe;
    private final List<JeiRecipeLookup.CategoryPage> pages;
    private int categoryIndex;
    private int recipeIndex;
    private JeiRecipeBinding binding;
    private int panelX;
    private int panelY;
    private int panelHeight;
    private int tabScroll;
    private StateSwitchingButton backButton;
    private StateSwitchingButton nextButton;
    private StateSwitchingButton prevButton;

    private JeiDetailPanel(RecipeHolder<?> fallbackRecipe, List<JeiRecipeLookup.CategoryPage> pages,
                           int categoryIndex, int recipeIndex, JeiRecipeBinding binding) {
        this.fallbackRecipe = fallbackRecipe;
        this.pages = pages;
        this.categoryIndex = categoryIndex;
        this.recipeIndex = recipeIndex;
        this.binding = binding;
    }

    public static JeiDetailPanel tryCreate(RecipeGroup group, int index) {
        if (group == null) {
            return null;
        }
        return tryCreate(group.result(), group.recipes().isEmpty() ? null : group.recipes().getFirst());
    }

    public static JeiDetailPanel tryCreate(ItemStack result, RecipeHolder<?> fallback) {
        if (result == null || result.isEmpty() || !ModJeiPlugin.isAvailable()) {
            return null;
        }
        List<JeiRecipeLookup.CategoryPage> pages = JeiRecipeLookup.lookupOutput(result);
        if (pages.isEmpty()) {
            return null;
        }
        return new JeiDetailPanel(fallback, pages, 0, 0, pages.getFirst().recipes().getFirst());
    }

    public static JeiDetailPanel tryCreateFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty() || !ModJeiPlugin.isAvailable()) {
            return null;
        }
        List<JeiRecipeLookup.CategoryPage> pages = JeiRecipeLookup.lookupFluidOutput(fluid);
        if (pages.isEmpty()) {
            return null;
        }
        return new JeiDetailPanel(null, pages, 0, 0, pages.getFirst().recipes().getFirst());
    }

    @Override
    public void initButtons(int panelX, int panelY, int panelHeight) {
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelHeight = panelHeight;
        backButton = new StateSwitchingButton(panelX + 6, panelY + 4, 12, 17, true);
        backButton.initTextureValues(new WidgetSprites(
                ResourceLocation.withDefaultNamespace("recipe_book/page_backward"),
                ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted")
        ));
        prevButton = new StateSwitchingButton(0, 0, 12, 17, true);
        prevButton.initTextureValues(new WidgetSprites(
                ResourceLocation.withDefaultNamespace("recipe_book/page_backward"),
                ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted")
        ));
        nextButton = new StateSwitchingButton(0, 0, 12, 17, false);
        nextButton.initTextureValues(new WidgetSprites(
                ResourceLocation.withDefaultNamespace("recipe_book/page_forward"),
                ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted")
        ));
        positionPagination();
        positionRecipe();
    }

    @Override
    public int width() {
        return ModRecipeBookComponent.DETAIL_IMAGE_WIDTH;
    }

    @Override
    public int height() {
        return ModRecipeBookComponent.DETAIL_IMAGE_HEIGHT;
    }

    @Override
    public RecipeHolder<?> recipe() {
        Object recipe = binding.recipe();
        if (recipe instanceof RecipeHolder<?> holder) {
            return holder;
        }
        return fallbackRecipe;
    }

    private JeiRecipeLookup.CategoryPage currentPage() {
        return pages.get(categoryIndex);
    }

    public boolean hasPagination() {
        return currentPage().recipes().size() > 1;
    }

    public boolean tryNext() {
        return selectRecipe(recipeIndex + 1);
    }

    public boolean tryPrev() {
        return selectRecipe(recipeIndex - 1);
    }

    private boolean selectCategory(int nextCategory) {
        if (nextCategory < 0 || nextCategory >= pages.size()) {
            return false;
        }
        categoryIndex = nextCategory;
        recipeIndex = 0;
        binding = currentPage().recipes().getFirst();
        ensureTabVisible();
        positionRecipe();
        return true;
    }

    private boolean selectRecipe(int nextRecipe) {
        List<JeiRecipeBinding> recipes = currentPage().recipes();
        if (nextRecipe < 0 || nextRecipe >= recipes.size()) {
            return false;
        }
        recipeIndex = nextRecipe;
        binding = recipes.get(recipeIndex);
        positionRecipe();
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY < tabY() || mouseY >= panelY + 4 || mouseX < panelX || mouseX >= panelX + width()) {
            return false;
        }
        return scrollTabs(-(int) Math.signum(delta));
    }

    private boolean scrollTabs(int delta) {
        int maxScroll = Math.max(0, pages.size() - maxVisibleTabs());
        int next = Math.max(0, Math.min(maxScroll, tabScroll + delta));
        if (next == tabScroll) {
            return pages.size() > maxVisibleTabs();
        }
        tabScroll = next;
        return true;
    }

    private boolean tabOverflow() {
        return pages.size() > Math.max(1, (width() - 16) / TAB_SIZE);
    }

    private int maxVisibleTabs() {
        int inset = tabOverflow() ? 28 : 16;
        return Math.max(1, (width() - inset) / TAB_SIZE);
    }

    private void ensureTabVisible() {
        int max = maxVisibleTabs();
        if (categoryIndex < tabScroll) {
            tabScroll = categoryIndex;
        } else if (categoryIndex >= tabScroll + max) {
            tabScroll = categoryIndex - max + 1;
        }
    }

    private int tabY() {
        return panelY - TAB_SIZE + TAB_OVERLAP;
    }

    private int tabX(int visibleIndex) {
        return tabRowLeft() + visibleIndex * TAB_SIZE;
    }

    private int tabRowLeft() {
        return panelX + (tabOverflow() ? 16 : 10);
    }

    private int contentBottom() {
        return panelY + panelHeight - (hasPagination() ? 29 : PADDING);
    }

    private void positionRecipe() {
        Rect2i rect = binding.drawable().getRectWithBorder();
        int contentX = panelX + PADDING + (width() - PADDING * 2 - rect.getWidth()) / 2;
        int contentTop = panelY + HEADER_HEIGHT;
        int availableHeight = contentBottom() - contentTop;
        int contentY = contentTop + Math.max(0, (availableHeight - rect.getHeight()) / 2);
        binding.drawable().setPosition(contentX, contentY);
        positionPagination();
    }

    private void positionPagination() {
        if (prevButton == null) {
            return;
        }
        int center = panelX + width() / 2;
        int arrowY = panelY + panelHeight - 29;
        prevButton.setPosition(center - 35, arrowY);
        nextButton.setPosition(center + 20, arrowY);
    }

    @Override
    public void tick() {
        binding.drawable().tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int w = width();
        int h = panelHeight;
        graphics.blit(ModRecipeBookComponent.DETAIL_TEXTURE, panelX, panelY, 0, 0, w, h, w, h);

        IRecipeCategory<?> category = binding.category();
        Component title = category.getTitle();
        int titleWidth = Minecraft.getInstance().font.width(title);
        graphics.drawString(Minecraft.getInstance().font, title, panelX + (w - titleWidth) / 2, panelY + 8, 0x404040, false);

        int max = maxVisibleTabs();
        int shown = Math.min(max, pages.size() - tabScroll);
        for (int i = 0; i < shown; i++) {
            drawCategoryTab(graphics, tabScroll + i, i);
        }
        if (tabScroll > 0) {
            drawTabArrow(graphics, true, mouseX, mouseY);
        }
        if (tabScroll + max < pages.size()) {
            drawTabArrow(graphics, false, mouseX, mouseY);
        }

        backButton.render(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(panelX, panelY + HEADER_HEIGHT, panelX + w, contentBottom());
        binding.drawable().drawRecipe(graphics, mouseX, mouseY);
        graphics.disableScissor();

        if (hasPagination()) {
            prevButton.visible = recipeIndex > 0;
            nextButton.visible = recipeIndex + 1 < currentPage().recipes().size();
            if (prevButton.visible) {
                prevButton.render(graphics, mouseX, mouseY, partialTick);
            }
            if (nextButton.visible) {
                nextButton.render(graphics, mouseX, mouseY, partialTick);
            }
            Component pageText = Component.translatable("gui.recipebook.page", recipeIndex + 1, currentPage().recipes().size());
            int pageX = panelX + width() / 2 - Minecraft.getInstance().font.width(pageText) / 2;
            graphics.drawString(Minecraft.getInstance().font, pageText, pageX, panelY + panelHeight - 25, 0x404040, false);
        }
    }

    private void drawCategoryTab(GuiGraphics graphics, int pageIndex, int visibleIndex) {
        int x = tabX(visibleIndex);
        int y = tabY();
        boolean selected = pageIndex == categoryIndex;
        graphics.blitSprite(selected ? TAB_SELECTED : TAB, x, y, TAB_SIZE, TAB_SIZE);
        IDrawable icon = pages.get(pageIndex).category().getIcon();
        if (icon != null) {
            int ix = x + (TAB_SIZE - icon.getWidth()) / 2 - 1;
            int iy = y + (TAB_SIZE - icon.getHeight()) / 2 - 1;
            icon.draw(graphics, ix, iy);
        }
    }

    private int tabArrowX(boolean left) {
        if (left) {
            return tabRowLeft() - 13;
        }
        int shown = Math.min(maxVisibleTabs(), pages.size() - tabScroll);
        return tabX(Math.max(0, shown - 1)) + TAB_SIZE;
    }

    private void drawTabArrow(GuiGraphics graphics, boolean left, int mouseX, int mouseY) {
        int x = tabArrowX(left);
        int y = tabY() + 2;
        boolean hover = overTabArrow(mouseX, mouseY, left);
        String name = left
                ? (hover ? "recipe_book/page_backward_highlighted" : "recipe_book/page_backward")
                : (hover ? "recipe_book/page_forward_highlighted" : "recipe_book/page_forward");
        graphics.blitSprite(ResourceLocation.withDefaultNamespace(name), x, y, 12, 17);
    }

    private boolean overTabArrow(double mouseX, double mouseY, boolean left) {
        int x = tabArrowX(left);
        int y = tabY() + 2;
        return mouseX >= x && mouseX < x + 12 && mouseY >= y && mouseY < y + 17;
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void renderOverlays(GuiGraphics graphics, int mouseX, int mouseY) {
        binding.drawable().drawOverlays(graphics, mouseX, mouseY);
        int max = maxVisibleTabs();
        int shown = Math.min(max, pages.size() - tabScroll);
        for (int i = 0; i < shown; i++) {
            int x = tabX(i);
            int y = tabY();
            if (mouseX >= x && mouseY >= y && mouseX < x + TAB_SIZE && mouseY < y + TAB_SIZE) {
                graphics.renderTooltip(Minecraft.getInstance().font, pages.get(tabScroll + i).category().getTitle(), mouseX, mouseY);
                return;
            }
        }
    }

    public boolean anyRecipe(java.util.function.Predicate<Object> test) {
        for (JeiRecipeLookup.CategoryPage page : pages) {
            for (JeiRecipeBinding binding : page.recipes()) {
                if (test.test(binding.recipe())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Optional<ItemStack> itemUnderMouse(double mouseX, double mouseY) {
        return binding.drawable().getItemStackUnderMouse((int) mouseX, (int) mouseY)
                .filter(stack -> !stack.isEmpty());
    }

    public Optional<FluidStack> fluidUnderMouse(double mouseX, double mouseY) {
        return binding.drawable().getIngredientUnderMouse((int) mouseX, (int) mouseY, NeoForgeTypes.FLUID_STACK)
                .filter(fluid -> fluid != null && !fluid.isEmpty());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int max = maxVisibleTabs();
        int shown = Math.min(max, pages.size() - tabScroll);
        if (tabOverflow()) {
            if (overTabArrow(mouseX, mouseY, true) && tabScroll > 0) {
                if (scrollTabs(-1)) {
                    playClick();
                }
                return true;
            }
            if (overTabArrow(mouseX, mouseY, false) && tabScroll + max < pages.size()) {
                if (scrollTabs(1)) {
                    playClick();
                }
                return true;
            }
        }
        for (int i = 0; i < shown; i++) {
            int x = tabX(i);
            int y = tabY();
            if (mouseX >= x && mouseY >= y && mouseX < x + TAB_SIZE && mouseY < y + TAB_SIZE) {
                selectCategory(tabScroll + i);
                playClick();
                return true;
            }
        }
        if (hasPagination()) {
            if (prevButton.visible && prevButton.mouseClicked(mouseX, mouseY, button)) {
                tryPrev();
                return true;
            }
            if (nextButton.visible && nextButton.mouseClicked(mouseX, mouseY, button)) {
                tryNext();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= panelX && mouseY >= tabY() && mouseX < panelX + width() && mouseY < panelY + panelHeight;
    }

    @Override
    public StateSwitchingButton backButton() {
        return backButton;
    }
}
