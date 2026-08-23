package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ModRecipePage {
    private static final int PER_PAGE = 20;
    private final List<ModRecipeButton> buttons = new ArrayList<>();
    private final StateSwitchingButton forward;
    private final StateSwitchingButton back;
    private List<RecipeGroup> collections = List.of();
    private Predicate<RecipeHolder<?>> craftable = holder -> false;
    private int page;
    private int lastPage;
    private int x;
    private int y;
    private Minecraft minecraft;

    public ModRecipePage() {
        for (int i = 0; i < PER_PAGE; i++) {
            buttons.add(new ModRecipeButton());
        }
        WidgetSprites forwardSprites = new WidgetSprites(
                ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/page_forward"),
                ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/page_forward_highlighted")
        );
        WidgetSprites backSprites = new WidgetSprites(
                ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/page_backward"),
                ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "recipe_book/page_backward_highlighted")
        );
        forward = new StateSwitchingButton(0, 0, 12, 17, false);
        back = new StateSwitchingButton(0, 0, 12, 17, true);
        forward.initTextureValues(forwardSprites);
        back.initTextureValues(backSprites);
    }

    public void init(Minecraft minecraft, int x, int y) {
        this.minecraft = minecraft;
        this.x = x;
        this.y = y;
        forward.setPosition(x + 93, y + 137);
        back.setPosition(x + 38, y + 137);
        updateButtons();
    }

    public void setCollections(List<RecipeGroup> collections, boolean resetPage, Predicate<RecipeHolder<?>> craftable) {
        this.collections = collections;
        this.craftable = craftable == null ? holder -> false : craftable;
        lastPage = Math.max(0, (collections.size() - 1) / PER_PAGE);
        if (resetPage) {
            page = 0;
        }
        page = Math.min(page, lastPage);
        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            ModRecipeButton button = buttons.get(i);
            if (index < collections.size()) {
                RecipeGroup group = collections.get(index);
                boolean craftable = canCraft(group);
                button.init(group, craftable);
                int col = i % 5;
                int row = i / 5;
                button.setPosition(x + 11 + col * 25, y + 31 + row * 25);
            } else {
                button.init(null, false);
            }
        }
        forward.visible = lastPage > 0 && page < lastPage;
        back.visible = page > 0;
    }

    private boolean canCraft(RecipeGroup group) {
        if (group == null) {
            return false;
        }
        for (RecipeHolder<?> holder : group.recipes()) {
            if (craftable.test(holder)) {
                return true;
            }
        }
        return false;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft == null) {
            return;
        }
        if (lastPage > 0) {
            Component label = Component.translatable("gui.recipebook.page", page + 1, lastPage + 1);
            int labelX = x - minecraft.font.width(label) / 2 + 73;
            graphics.drawString(minecraft.font, label, labelX, y + 141, 0xFFFFFFFF, false);
        }
        for (ModRecipeButton button : buttons) {
            if (button.visible) {
                button.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        forward.render(graphics, mouseX, mouseY, partialTick);
        back.render(graphics, mouseX, mouseY, partialTick);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (minecraft == null) {
            return;
        }
        for (ModRecipeButton button : buttons) {
            if (button.visible && button.isHovered() && button.group() != null) {
                RecipeCategoryConfig.renderItemTooltip(graphics, minecraft.font, button.group().result(), mouseX, mouseY);
                return;
            }
        }
    }

    public boolean mouseScrolled(double delta) {
        if (lastPage <= 0) {
            return false;
        }
        int next = Math.max(0, Math.min(lastPage, page - (int) Math.signum(delta)));
        if (next != page) {
            page = next;
            updateButtons();
        }
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (forward.mouseClicked(mouseX, mouseY, button)) {
            page++;
            updateButtons();
            return true;
        }
        if (back.mouseClicked(mouseX, mouseY, button)) {
            page--;
            updateButtons();
            return true;
        }
        return false;
    }

    public ModRecipeButton hovered(double mouseX, double mouseY) {
        for (ModRecipeButton button : buttons) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }
}
