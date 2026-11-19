//#if MC >= 1.21.2
package com.correctarrow.mixin.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArrowRenderer.class)
public interface ArrowRendererInvoker {
    @Invoker("getTextureLocation")
    Identifier correctarrow$getTextureLocation(ArrowRenderState state);
}
//#endif
