//#if MC >= 1.21.2
package com.correctarrow.mixin.client;

import com.correctarrow.client.ClientArrows;
import com.correctarrow.client.StuckArrowState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(
            method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private void correctarrow$attachStuckArrows(Entity entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
        if (entity instanceof LivingEntity living) {
            ClientArrows.Entry arrows = ClientArrows.update(living);
            float bodyYaw = Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot);
            ((StuckArrowState) cir.getReturnValue()).correctarrow$setArrows(arrows, bodyYaw);
        } else {
            ((StuckArrowState) cir.getReturnValue()).correctarrow$setArrows(null, 0.0F);
        }
    }
}
//#endif
