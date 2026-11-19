//#if MC >= 1.21.2
package com.correctarrow.mixin.client;

import com.correctarrow.client.ArrowPlacement;
import com.correctarrow.client.ClientArrows;
import com.correctarrow.client.StuckArrowState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements StuckArrowState {
    @Unique
    private ClientArrows.@Nullable Entry correctarrow$arrows;
    @Unique
    private float correctarrow$bodyYaw;
    @Unique
    private ArrowPlacement.@Nullable Feet correctarrow$feet;
    @Unique
    private boolean correctarrow$drawn;

    @Override
    public ClientArrows.@Nullable Entry correctarrow$arrows() {
        return correctarrow$arrows;
    }

    @Override
    public void correctarrow$setArrows(ClientArrows.@Nullable Entry arrows, float bodyYaw) {
        this.correctarrow$arrows = arrows;
        this.correctarrow$bodyYaw = bodyYaw;
        this.correctarrow$feet = null;
        this.correctarrow$drawn = false;
    }

    @Override
    public float correctarrow$bodyYaw() {
        return correctarrow$bodyYaw;
    }

    @Override
    public ArrowPlacement.@Nullable Feet correctarrow$feet() {
        return correctarrow$feet;
    }

    @Override
    public void correctarrow$setFeet(ArrowPlacement.@Nullable Feet feet) {
        this.correctarrow$feet = feet;
    }

    @Override
    public boolean correctarrow$drawn() {
        return correctarrow$drawn;
    }

    @Override
    public void correctarrow$setDrawn(boolean drawn) {
        this.correctarrow$drawn = drawn;
    }
}
//#endif
