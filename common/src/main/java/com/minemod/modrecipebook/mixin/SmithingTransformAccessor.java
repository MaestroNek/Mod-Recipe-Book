package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.recipe.SmithingParts;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformAccessor extends SmithingParts {
    @Override
    @Accessor("template")
    Ingredient modrecipebook$template();

    @Override
    @Accessor("base")
    Ingredient modrecipebook$base();

    @Override
    @Accessor("addition")
    Ingredient modrecipebook$addition();
}
