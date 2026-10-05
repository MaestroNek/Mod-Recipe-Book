package com.minemod.modrecipebook.mixin;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Checkbox.class)
public interface CheckboxAccessor {
    @Accessor("textWidget")
    MultiLineTextWidget modrecipebook$text();
}
