package com.correctarrow.mixin.client;

import com.correctarrow.client.StuckArrowRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//#if MC >= 1.21.9
import com.correctarrow.client.StuckArrowState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.MultiBufferSource;
//#endif
//#if MC >= 1.21.2 && MC < 1.21.9
import com.correctarrow.client.StuckArrowState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
//#endif
//#if MC < 1.21.2
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
//#endif

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    //#if MC >= 1.21.9
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void correctarrow$drawArrowsWithoutModel(EntityRenderState state, CameraRenderState camera, double x, double y, double z,
                                                     PoseStack poseStack, SubmitNodeCollector collector, CallbackInfo ci) {
        StuckArrowState ext = (StuckArrowState) state;
        if (ext.correctarrow$arrows() != null && !ext.correctarrow$drawn()) {
            EntityRenderer<?, ?> renderer = ((EntityRenderDispatcher) (Object) this).getRenderer(state);
            if (!(renderer instanceof LivingEntityRenderer)) {
                StuckArrowRenderer.submitOnBody(state, poseStack, collector);
            }
        }
    }
    //#elseif MC >= 1.21.2

    //#if MC >= 1.21.5
    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    //#else
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    //#endif
    private void correctarrow$drawArrowsWithoutModel(EntityRenderer<?, ?> renderer, EntityRenderState state, PoseStack poseStack,
                                                     MultiBufferSource buffers, int light, Operation<Void> original) {
        original.call(renderer, state, poseStack, buffers, light);
        StuckArrowState ext = (StuckArrowState) state;
        if (ext.correctarrow$arrows() != null && !ext.correctarrow$drawn() && !(renderer instanceof LivingEntityRenderer)) {
            StuckArrowRenderer.renderOnBody(state, poseStack, buffers, light);
        }
    }
    //#else
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void correctarrow$drawArrowsWithoutModel(EntityRenderer<?> renderer, Entity entity, float yaw, float partialTick,
                                                     PoseStack poseStack, MultiBufferSource buffers, int light, Operation<Void> original) {
        original.call(renderer, entity, yaw, partialTick, poseStack, buffers, light);
        if (entity instanceof LivingEntity living && !(renderer instanceof LivingEntityRenderer)) {
            StuckArrowRenderer.renderOnBody(living, poseStack, buffers, light, partialTick);
        }
    }
    //#endif
}
