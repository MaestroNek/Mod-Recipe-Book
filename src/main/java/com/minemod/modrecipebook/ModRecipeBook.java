package com.minemod.modrecipebook;

import com.minemod.modrecipebook.client.ClientModSetup;
import com.minemod.modrecipebook.net.ModNetworking;
import com.minemod.modrecipebook.recipe.ModRecipeBookAttachments;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ModRecipeBook.MODID)
public class ModRecipeBook {
    public static final String MODID = "modrecipebook";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModRecipeBook(IEventBus modEventBus, ModContainer container) {
        ModRecipeBookAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModSetup.init(container);
        }
    }
}
