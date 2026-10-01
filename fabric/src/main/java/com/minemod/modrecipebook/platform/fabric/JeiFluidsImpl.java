package com.minemod.modrecipebook.platform.fabric;

import com.minemod.modrecipebook.client.jei.BookFluid;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.fabric.ingredients.fluids.JeiFluidIngredient;
import mezz.jei.api.ingredients.IIngredientType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.material.Fluid;

public final class JeiFluidsImpl {
    public static IIngredientType<?> type() {
        return FabricTypes.FLUID_STACK;
    }

    public static Object stack(Fluid fluid, int amount) {
        return new JeiFluidIngredient(FluidVariant.of(fluid), amount);
    }

    public static BookFluid view(Object ingredient) {
        if (ingredient instanceof IJeiFluidIngredient fluid && fluid.getFluidVariant() != null && !fluid.getFluidVariant().isBlank()) {
            return new BookFluid(fluid.getFluidVariant().getFluid(), (int) Math.min(Integer.MAX_VALUE, fluid.getAmount()));
        }
        return null;
    }

    public static boolean same(Object ingredient, Fluid fluid) {
        BookFluid view = view(ingredient);
        return view != null && !view.isEmpty() && view.fluid().isSame(fluid);
    }

    private JeiFluidsImpl() {}
}
