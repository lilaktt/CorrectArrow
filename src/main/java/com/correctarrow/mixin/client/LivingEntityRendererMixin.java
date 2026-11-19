package com.correctarrow.mixin.client;

import com.correctarrow.client.StuckArrowRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 1.21.2
import com.correctarrow.client.ArrowPlacement;
import com.correctarrow.client.StuckArrowState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//#endif
//#if MC >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
import net.minecraft.client.renderer.MultiBufferSource;
//#endif
//#if MC < 1.21.2
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Shadow;
//#endif

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    //#if MC >= 1.21.9
    private static final String RENDER = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void correctarrow$rememberFeet(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                           CameraRenderState camera, CallbackInfo ci) {
        StuckArrowState ext = (StuckArrowState) state;
        if (ext.correctarrow$arrows() != null) {
            ext.correctarrow$setFeet(ArrowPlacement.Feet.of(poseStack));
        }
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void correctarrow$drawStuckArrows(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                              CameraRenderState camera, CallbackInfo ci) {
        if (((StuckArrowState) state).correctarrow$arrows() != null) {
            StuckArrowRenderer.submitOnModel(state, ((LivingEntityRenderer<?, ?, ?>) (Object) this).getModel(), poseStack, collector);
        }
    }
    //#elseif MC >= 1.21.2
    private static final String RENDER = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void correctarrow$rememberFeet(LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers, int light,
                                           CallbackInfo ci) {
        StuckArrowState ext = (StuckArrowState) state;
        if (ext.correctarrow$arrows() != null) {
            ext.correctarrow$setFeet(ArrowPlacement.Feet.of(poseStack));
        }
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void correctarrow$drawStuckArrows(LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers, int light,
                                              CallbackInfo ci) {
        if (((StuckArrowState) state).correctarrow$arrows() != null) {
            StuckArrowRenderer.renderOnModel(state, ((LivingEntityRenderer<?, ?, ?>) (Object) this).getModel(), poseStack, buffers, light);
        }
    }
    //#else
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Shadow
    protected EntityModel<?> model;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void correctarrow$begin(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                    int light, CallbackInfo ci) {
        StuckArrowRenderer.begin(entity, poseStack, partialTick);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void correctarrow$recordAnim(EntityModel<?> model, Entity entity, float walkPos, float walkSpeed, float bob, float headYaw,
                                         float pitch, Operation<Void> original) {
        original.call(model, entity, walkPos, walkSpeed, bob, headYaw, pitch);
        StuckArrowRenderer.recordAnim(walkPos, walkSpeed, bob, headYaw, pitch);
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void correctarrow$startCapture(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                           int light, CallbackInfo ci) {
        StuckArrowRenderer.prepareNames(model);
        StuckArrowRenderer.startCapture(poseStack);
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void correctarrow$stopCapture(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                          int light, CallbackInfo ci) {
        StuckArrowRenderer.stopCapture();
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void correctarrow$drawStuckArrows(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                              int light, CallbackInfo ci) {
        StuckArrowRenderer.renderOnModel(model, poseStack, buffers, light);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void correctarrow$end(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                  int light, CallbackInfo ci) {
        StuckArrowRenderer.end();
    }
    //#endif
}
