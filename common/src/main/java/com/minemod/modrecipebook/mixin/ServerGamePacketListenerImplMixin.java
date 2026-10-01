package com.minemod.modrecipebook.mixin;

import com.minemod.modrecipebook.recipe.PlaceRecipeContext;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"))
    private void modrecipebook$enterPlace(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        PlaceRecipeContext.enter(this.player);
    }

    @Inject(method = "handlePlaceRecipe", at = @At("RETURN"))
    private void modrecipebook$exitPlace(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        PlaceRecipeContext.exit();
    }
}
