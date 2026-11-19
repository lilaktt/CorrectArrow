package com.correctarrow.mixin.client;

import com.correctarrow.client.StuckArrowRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC >= 1.21.9
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//#elseif MC >= 1.21.2
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//#else
import net.minecraft.world.entity.LivingEntity;
//#endif

@Mixin(ArrowLayer.class)
public abstract class ArrowLayerMixin {
    //#if MC >= 1.21.9
    @Inject(method = "numStuck(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)I", at = @At("RETURN"), cancellable = true)
    private void correctarrow$skipPlacedArrows(AvatarRenderState state, CallbackInfoReturnable<Integer> cir) {
    //#elseif MC >= 1.21.2
    @Inject(method = "numStuck(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)I", at = @At("RETURN"), cancellable = true)
    private void correctarrow$skipPlacedArrows(PlayerRenderState state, CallbackInfoReturnable<Integer> cir) {
    //#else
    @Inject(method = "numStuck(Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), cancellable = true)
    private void correctarrow$skipPlacedArrows(LivingEntity state, CallbackInfoReturnable<Integer> cir) {
    //#endif
        int drawn = StuckArrowRenderer.drawnCount(state);
        if (drawn > 0) {
            cir.setReturnValue(Math.max(0, cir.getReturnValueI() - drawn));
        }
    }
}
