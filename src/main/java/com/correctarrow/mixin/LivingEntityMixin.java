package com.correctarrow.mixin;

import com.correctarrow.common.PreHurtPose;
import com.correctarrow.common.StuckHit;
import com.correctarrow.server.ServerHits;
import com.correctarrow.server.StuckHitHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements StuckHitHolder, PreHurtPose {
    @Shadow
    @Final
    public WalkAnimationState walkAnimation;

    @Unique
    private List<StuckHit> correctarrow$hits;
    @Unique
    private long correctarrow$hurtAt;
    @Unique
    private float correctarrow$walkPos;
    @Unique
    private float correctarrow$walkSpeed;

    @Override
    public List<StuckHit> correctarrow$hits() {
        if (correctarrow$hits == null) {
            correctarrow$hits = new ArrayList<>(4);
        }
        return correctarrow$hits;
    }

    @Inject(method = "setArrowCount(I)V", at = @At("TAIL"))
    private void correctarrow$followArrowCount(int count, CallbackInfo ci) {
        if (correctarrow$hits != null && !((LivingEntity) (Object) this).level().isClientSide()) {
            ServerHits.trim(correctarrow$hits, count);
        }
    }

    @Inject(method = "handleDamageEvent(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void correctarrow$rememberPoseBeforeHurt(DamageSource source, CallbackInfo ci) {
        if (((LivingEntity) (Object) this).level().isClientSide()) {
            correctarrow$walkPos = walkAnimation.position();
            correctarrow$walkSpeed = walkAnimation.speed();
            correctarrow$hurtAt = System.currentTimeMillis();
        }
    }

    @Override
    public long correctarrow$hurtAt() {
        return correctarrow$hurtAt;
    }

    @Override
    public float correctarrow$walkPosBeforeHurt() {
        return correctarrow$walkPos;
    }

    @Override
    public float correctarrow$walkSpeedBeforeHurt() {
        return correctarrow$walkSpeed;
    }
}
