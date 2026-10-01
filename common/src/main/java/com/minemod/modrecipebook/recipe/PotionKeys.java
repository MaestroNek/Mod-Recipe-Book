package com.minemod.modrecipebook.recipe;

import com.minemod.modrecipebook.ModRecipeBook;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

public final class PotionKeys {
    private PotionKeys() {}

    public static String itemKey(ItemStack stack) {
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ResourceLocation potion = potionId(stack);
        return potion == null ? item : item + "#" + potion;
    }

    public static ResourceLocation knownId(ItemStack stack) {
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation potion = potionId(stack);
        if (potion == null) {
            return item;
        }
        return ResourceLocation.fromNamespaceAndPath(ModRecipeBook.MODID,
                "potion/" + item.getNamespace() + "/" + item.getPath()
                        + "/" + potion.getNamespace() + "/" + potion.getPath());
    }

    public static ResourceLocation potionId(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return null;
        }
        return contents.potion().get().unwrapKey().map(key -> key.location()).orElse(null);
    }
}
