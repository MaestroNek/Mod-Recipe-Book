package com.minemod.modrecipebook.client.jei;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;

public record JeiRecipeBinding(IRecipeCategory<?> category, Object recipe, IRecipeLayoutDrawable<?> drawable) {
}
