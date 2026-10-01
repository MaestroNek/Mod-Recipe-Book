package com.minemod.modrecipebook.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.item.alchemy.PotionBrewing$Mix")
public interface PotionMixAccessor {
    @Accessor("from")
    Holder<?> modrecipebook$from();

    @Accessor("ingredient")
    Ingredient modrecipebook$ingredient();

    @Accessor("to")
    Holder<?> modrecipebook$to();
}
