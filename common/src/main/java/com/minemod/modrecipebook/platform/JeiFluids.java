package com.minemod.modrecipebook.platform;

import com.minemod.modrecipebook.client.jei.BookFluid;
import dev.architectury.injectables.annotations.ExpectPlatform;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.world.level.material.Fluid;

public final class JeiFluids {
    @ExpectPlatform
    public static IIngredientType<?> type() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Object stack(Fluid fluid, int amount) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static BookFluid view(Object ingredient) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean same(Object ingredient, Fluid fluid) {
        throw new AssertionError();
    }

    private JeiFluids() {}
}
