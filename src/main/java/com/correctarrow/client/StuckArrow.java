package com.correctarrow.client;

import com.correctarrow.common.StuckHit;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class StuckArrow {
    static final long FRESH_MS = 1000;

    final StuckHit hit;

    final @Nullable Identifier texture;
    final long receivedAt;

    final float walkPosBeforeHurt;
    final float walkSpeedBeforeHurt;

    private final Map<Object, Landing> landings = new HashMap<>(2);

    public @Nullable Vector3f lastDrawnAt;

    public @Nullable Vector3f lastDrawnDir;

    StuckArrow(StuckHit hit, @Nullable Identifier texture, long receivedAt, float walkPosBeforeHurt, float walkSpeedBeforeHurt) {
        this.hit = hit;
        this.texture = texture;
        this.receivedAt = receivedAt;
        this.walkPosBeforeHurt = walkPosBeforeHurt;
        this.walkSpeedBeforeHurt = walkSpeedBeforeHurt;
    }

    boolean landsBeforeHurt(long now) {
        return !Float.isNaN(walkPosBeforeHurt) && now - receivedAt < FRESH_MS;
    }

    public StuckHit hit() {
        return hit;
    }

    @Nullable Landing landing(Object model) {
        return landings.get(model);
    }

    void land(Object model, Landing landing) {
        if (landings.size() >= 4) {
            landings.clear();
        }
        landings.put(model, landing);
    }

    public @Nullable Landing anyLanding() {
        return landings.isEmpty() ? null : landings.values().iterator().next();
    }

    float yawFor(float currentBodyYaw, long now) {
        return hit.fresh() && now - receivedAt < FRESH_MS ? hit.yaw() : currentBodyYaw;
    }

    public record Landing(ModelRaycast.Root root, ModelPart[] chain, String path, Vector3f point, Vector3f dir, boolean onPath) {
        Matrix4f partTo(Matrix4f rootToFeet, @Nullable Matrix4f currentRootTransform) {
            Matrix4f m = new Matrix4f(rootToFeet).mul(currentRootTransform != null ? currentRootTransform : root.transform());
            for (ModelPart part : chain) {
                ModelRaycast.applyPose(part, m);
            }
            return m;
        }
    }
}
