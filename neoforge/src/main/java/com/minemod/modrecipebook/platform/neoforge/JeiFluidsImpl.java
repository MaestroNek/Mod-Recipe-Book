package com.minemod.modrecipebook.platform.neoforge;

import com.minemod.modrecipebook.client.jei.BookFluid;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public final class JeiFluidsImpl {
    public static IIngredientType<?> type() {
        return NeoForgeTypes.FLUID_STACK;
    }

    public static Object stack(Fluid fluid, int amount) {
        return new FluidStack(fluid, amount);
    }

    public static BookFluid view(Object ingredient) {
        if (ingredient instanceof FluidStack stack && !stack.isEmpty()) {
            return new BookFluid(stack.getFluid(), stack.getAmount());
        }
        return null;
    }

    public static boolean same(Object ingredient, Fluid fluid) {
        return ingredient instanceof FluidStack stack && !stack.isEmpty() && stack.getFluid().isSame(fluid);
    }

    private JeiFluidsImpl() {}
}
