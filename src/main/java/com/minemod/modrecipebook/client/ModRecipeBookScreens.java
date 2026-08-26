package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ModRecipeBookScreens {
    private static final Map<AbstractContainerScreen<?>, Host> HOSTS = new WeakHashMap<>();

    private ModRecipeBookScreens() {}

    public static ModRecipeBookComponent component(Screen screen) {
        if (screen instanceof ModRecipeBookAccess access) {
            return access.modrecipebook$component();
        }
        if (screen instanceof AbstractContainerScreen<?> container) {
            Host host = HOSTS.get(container);
            return host == null ? null : host.book;
        }
        return null;
    }

    public static List<ImageButton> attach(AbstractContainerScreen<?> screen) {
        if (screen instanceof ModRecipeBookAccess) {
            return List.of();
        }
        AbstractContainerMenu menu = screen.getMenu();
        boolean brewing = menu instanceof BrewingStandMenu;
        if (!(menu instanceof RecipeBookMenu<?, ?>) && !brewing) {
            return List.of();
        }
        Host host = HOSTS.computeIfAbsent(screen, s -> new Host());
        if (host.book == null) {
            host.book = new ModRecipeBookComponent();
        }
        RecipeBookComponent vanilla = vanillaBook(screen);
        boolean narrow = screen.width < 379;
        host.book.init(screen.width, screen.height, Minecraft.getInstance(), narrow, menu, screen, vanilla);
        ImageButton vanillaButton = VanillaRecipeBookGuard.findRecipeButton(screen, null);
        int relX;
        int relY;
        if (brewing) {
            relX = 15;
            relY = screen.getYSize() / 2 - 44;
        } else if (vanillaButton == null) {
            relX = 5;
            relY = 22;
        } else {
            relX = vanillaButton.getX() - screen.getGuiLeft();
            relY = vanillaButton.getY() - screen.getGuiTop();
        }
        host.layout = VanillaRecipeBookGuard.forMode(relX, relY, RecipeCategoryConfig.bookButtonLayout(screenId(screen)));
        host.button = host.book.createToggleButton(
                screen.getGuiLeft() + relX, screen.getGuiTop() + relY, b -> toggle(screen, host));
        if (brewing) {
            host.rotate = null;
        } else {
            host.rotate = host.book.createRotateButton(
                    screen.getGuiLeft() + relX, screen.getGuiTop() + relY + 20, b -> toggleStack(screen, host));
        }
        if (host.book.isVisible()) {
            setLeftPos(screen, host.book.updateScreenPosition(screen.width, screen.getXSize()));
            host.book.syncLayout();
        }
        reposition(screen, host);
        return host.rotate == null ? List.of(host.button) : List.of(host.button, host.rotate);
    }

    public static void tick(Screen screen) {
        Host host = host(screen);
        if (host == null) {
            return;
        }
        AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) screen;
        host.book.tick();
        reposition(container, host);
        if (host.book.isVanillaVisible() && host.book.isVisible()) {
            host.book.setVisible(false);
            RecipeBookComponent vanilla = vanillaBook(container);
            if (vanilla != null) {
                setLeftPos(container, vanilla.updateScreenPosition(container.width, container.getXSize()));
            }
            reposition(container, host);
        }
    }

    public static void reposition(Screen screen) {
        Host host = host(screen);
        if (host != null) {
            reposition((AbstractContainerScreen<?>) screen, host);
        }
    }

    public static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        ModRecipeBookComponent book = component(screen);
        if (book == null) {
            return;
        }
        Host host = HOSTS.get(container);
        if (host != null) {
            reposition(container, host);
        }
        book.render(graphics, mouseX, mouseY, partialTick);
        book.renderGhostRecipe(graphics, container.getGuiLeft(), container.getGuiTop(), partialTick);
        book.renderTooltip(graphics, mouseX, mouseY);
    }

    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        Host host = host(screen);
        return host != null && host.book.mouseClicked(mouseX, mouseY, button);
    }

    public static boolean keyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        Host host = host(screen);
        return host != null && host.book.keyPressed(keyCode, scanCode, modifiers);
    }

    public static boolean charTyped(Screen screen, char codePoint, int modifiers) {
        Host host = host(screen);
        return host != null && host.book.charTyped(codePoint, modifiers);
    }

    public static void slotClicked(AbstractContainerScreen<?> screen, Slot slot) {
        Host host = HOSTS.get(screen);
        if (host != null) {
            host.book.slotClicked(slot);
        }
    }

    public static boolean overBook(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Host host = HOSTS.get(screen);
        return host != null && host.book.isMouseOver(mouseX, mouseY);
    }

    private static void toggleStack(AbstractContainerScreen<?> screen, Host host) {
        int next = Math.floorMod(host.layout.mode() + 1, VanillaRecipeBookGuard.LAYOUT_COUNT);
        host.layout.setMode(next);
        RecipeCategoryConfig.setBookButtonLayout(screenId(screen), next);
        reposition(screen, host);
    }

    private static void toggle(AbstractContainerScreen<?> screen, Host host) {
        RecipeBookComponent vanilla = vanillaBook(screen);
        if (!host.book.isVisible() && vanilla != null && vanilla.isVisible()) {
            vanilla.toggleVisibility();
        }
        host.book.toggleVisibility();
        setLeftPos(screen, host.book.updateScreenPosition(screen.width, screen.getXSize()));
        host.book.syncLayout();
        reposition(screen, host);
    }

    private static void reposition(AbstractContainerScreen<?> screen, Host host) {
        RecipeBookComponent vanilla = vanillaBook(screen);
        VanillaRecipeBookGuard.apply(vanilla, host.button, screen.children(),
                host.layout, screen.getGuiLeft(), screen.getGuiTop());
        if (RecipeCategoryConfig.hideVanillaBook() && !host.book.isVisible() && vanilla != null) {
            setLeftPos(screen, vanilla.updateScreenPosition(screen.width, screen.getXSize()));
        }
        if (host.button != null) {
            host.button.visible = true;
            host.button.active = true;
        }
        placeRotate(screen, host, vanilla);
    }

    private static void placeRotate(AbstractContainerScreen<?> screen, Host host, RecipeBookComponent vanilla) {
        if (host.rotate == null) {
            return;
        }
        boolean show = !RecipeCategoryConfig.hideVanillaBook()
                && (host.book.isVisible() || host.book.isVanillaVisible());
        host.rotate.visible = show;
        host.rotate.active = show;
        if (!show) {
            return;
        }
        if (host.book.isVisible()) {
            host.book.placeRotateButton(host.rotate);
            return;
        }
        int xOffset = screen.width < 379 ? 0 : 86;
        int x = (screen.width - 147) / 2 - xOffset;
        int y = (screen.height - 166) / 2;
        host.rotate.setPosition(x, y + 166 + 2);
    }

    private static void setLeftPos(AbstractContainerScreen<?> screen, int leftPos) {
        ((AbstractContainerScreenAccessor) screen).setLeftPos(leftPos);
    }

    private static String screenId(Screen screen) {
        return screen.getClass().getName();
    }

    private static RecipeBookComponent vanillaBook(Screen screen) {
        return screen instanceof RecipeUpdateListener listener ? listener.getRecipeBookComponent() : null;
    }

    private static Host host(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> container ? HOSTS.get(container) : null;
    }

    private static final class Host {
        ModRecipeBookComponent book;
        ImageButton button;
        ImageButton rotate;
        VanillaRecipeBookGuard.Layout layout;
    }
}
