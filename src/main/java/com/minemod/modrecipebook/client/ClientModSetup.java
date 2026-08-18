package com.minemod.modrecipebook.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ClientModSetup {
    private ClientModSetup() {}

    public static void init(ModContainer container) {
        RecipeCategoryConfig.load();
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new CategorySettingsScreen(parent));
    }
}
