package com.minemod.modrecipebook.client.jei;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record BookFluid(Fluid fluid, int amount) {
    public boolean isEmpty() {
        return fluid == null || fluid == Fluids.EMPTY || amount <= 0;
    }
}
