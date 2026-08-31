package com.minemod.modrecipebook.client.jei;

import com.minemod.modrecipebook.ModRecipeBook;
import com.minemod.modrecipebook.client.ModRecipeBookAccess;
import com.minemod.modrecipebook.client.RecipeCategoryConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class ModJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID, "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Replaces JEI's default handler. null = no overlay, wrench, or bookmark buttons.
        registration.addGuiScreenHandler(AbstractContainerScreen.class, screen -> {
            if (RecipeCategoryConfig.hideJei()) {
                return null;
            }
            return containerProperties(screen);
        });
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen) {
                if (RecipeCategoryConfig.hideJei()) {
                    return List.of();
                }
                if (screen instanceof ModRecipeBookAccess access && access.modrecipebook$component().isVisible()) {
                    return List.of(access.modrecipebook$component().overlayExclusion());
                }
                return List.of();
            }
        });
    }

    private static IGuiProperties containerProperties(AbstractContainerScreen<?> screen) {
        if (screen.width <= 0 || screen.height <= 0) {
            return null;
        }
        int x = screen.getGuiLeft();
        int y = screen.getGuiTop();
        int w = screen.getXSize();
        int h = screen.getYSize();
        if (x < 0) {
            w -= x;
            x = 0;
        }
        if (y < 0) {
            h -= y;
            y = 0;
        }
        if (w <= 0 || h <= 0) {
            return null;
        }
        int left = x;
        int top = y;
        int width = w;
        int height = h;
        return new IGuiProperties() {
            @Override
            public Class<? extends Screen> screenClass() {
                return screen.getClass();
            }

            @Override
            public int guiLeft() {
                return left;
            }

            @Override
            public int guiTop() {
                return top;
            }

            @Override
            public int guiXSize() {
                return width;
            }

            @Override
            public int guiYSize() {
                return height;
            }

            @Override
            public int screenWidth() {
                return screen.width;
            }

            @Override
            public int screenHeight() {
                return screen.height;
            }
        };
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        JeiStations.importIfAvailable();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        JeiRecipeLookup.clearCache();
    }

    public static boolean isAvailable() {
        return runtime != null;
    }

    public static IJeiRuntime runtime() {
        return runtime;
    }
}
