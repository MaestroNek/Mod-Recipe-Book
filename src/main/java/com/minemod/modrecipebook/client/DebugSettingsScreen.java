package com.minemod.modrecipebook.client;

import com.minemod.modrecipebook.net.DebugUnlockPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

public class DebugSettingsScreen extends Screen {
    private final Screen parent;

    public DebugSettingsScreen(Screen parent) {
        super(Component.translatable("gui.modrecipebook.debug.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean inWorld = inWorld();
        int x = width / 2 - 100;
        addRenderableWidget(actionButton(x, 60, "gui.modrecipebook.debug.discover",
                DebugUnlockPayload.DISCOVER, inWorld));
        addRenderableWidget(actionButton(x, 88, "gui.modrecipebook.debug.reset",
                DebugUnlockPayload.RESET, inWorld));
        addRenderableWidget(actionButton(x, 116, "gui.modrecipebook.debug.rethink",
                DebugUnlockPayload.RETHINK, inWorld));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private Button actionButton(int x, int y, String key, byte action, boolean inWorld) {
        MutableComponent text = Component.translatable(key + ".tip").withStyle(ChatFormatting.WHITE);
        if (!inWorld) {
            text.append(Component.literal("\n"))
                    .append(Component.translatable("gui.modrecipebook.debug.disabled")
                            .withStyle(ChatFormatting.YELLOW));
        }
        Button button = Button.builder(Component.translatable(key), b ->
                        PacketDistributor.sendToServer(new DebugUnlockPayload(action)))
                .bounds(x, y, 200, 20)
                .tooltip(Tooltip.create(text))
                .build();
        button.active = inWorld;
        return button;
    }

    private boolean inWorld() {
        return minecraft != null
                && minecraft.player != null
                && minecraft.level != null
                && minecraft.getConnection() != null;
    }
}
