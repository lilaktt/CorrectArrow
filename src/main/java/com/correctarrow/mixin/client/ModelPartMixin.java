//#if MC < 1.21.2
package com.correctarrow.mixin.client;

import com.correctarrow.client.StuckArrowRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin {
    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    //#if MC >= 1.21
    private void correctarrow$enter(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color, CallbackInfo ci) {
    //#else
    private void correctarrow$enter(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a,
                                    CallbackInfo ci) {
    //#endif
        StuckArrowRenderer.enterPart((ModelPart) (Object) this, poseStack);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    //#if MC >= 1.21
    private void correctarrow$exit(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color, CallbackInfo ci) {
    //#else
    private void correctarrow$exit(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a,
                                   CallbackInfo ci) {
    //#endif
        StuckArrowRenderer.exitPart();
    }
}
//#endif
