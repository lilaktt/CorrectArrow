package com.correctarrow.mixin;

import com.correctarrow.server.ServerHits;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Inject(
            method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setArrowCount(I)V", shift = At.Shift.AFTER)
    )
    private void correctarrow$recordStuckArrow(EntityHitResult hit, CallbackInfo ci) {
        ServerHits.onArrowStuck((AbstractArrow) (Object) this, hit);
    }
}
