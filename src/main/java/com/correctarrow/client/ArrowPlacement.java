package com.correctarrow.client;

import com.correctarrow.common.BodyFrame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ArrowPlacement {
    private ArrowPlacement() {
    }

    public interface Poser {
        void pose(float walkPos, float walkSpeed);

        void restore();
    }

    public interface Drawer {
        void draw(StuckArrow arrow, Vector3f at, Vector3f dir);
    }

    public record Feet(Matrix4f pose, Matrix3f normal) {
        public static Feet of(PoseStack poseStack) {
            return new Feet(new Matrix4f(poseStack.last().pose()), new Matrix3f(poseStack.last().normal()));
        }

        public void pushArrowPose(PoseStack poseStack, Vector3f at, Vector3f dir) {
            poseStack.pushPose();
            poseStack.last().pose().set(pose);
            poseStack.last().normal().set(normal);
            poseStack.translate(at.x, at.y, at.z);
            //#if MC >= 26.3
            poseStack.rotateDegrees(Axis.YP, yRot(dir) - 90.0F);
            poseStack.rotateDegrees(Axis.ZP, xRot(dir));
            //#else
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot(dir) - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(xRot(dir)));
            //#endif
        }
    }

    public static float yRot(Vector3f dir) {
        return (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
    }

    public static float xRot(Vector3f dir) {
        return (float) Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
    }

    public static void place(ClientArrows.Entry entry, Object modelKey, List<ModelRaycast.Root> roots, Matrix4f modelToFeet,
                             float bodyYaw, Poser poser, Drawer drawer) {
        long now = System.currentTimeMillis();
        boolean reposed = false;
        for (StuckArrow arrow : entry.arrows) {
            if (arrow.landing(modelKey) != null) {
                continue;
            }
            if (arrow.landsBeforeHurt(now)) {
                poser.pose(arrow.walkPosBeforeHurt, arrow.walkSpeedBeforeHurt);
                reposed = true;
            } else if (reposed) {
                poser.restore();
                reposed = false;
            }
            float yaw = arrow.yawFor(bodyYaw, now);
            StuckArrow.Landing landing = ModelRaycast.cast(roots, modelToFeet,
                    BodyFrame.toWorld(arrow.hit.point(), yaw), BodyFrame.toWorld(arrow.hit.dir(), yaw));
            if (landing != null) {
                arrow.land(modelKey, landing);
            }
        }
        if (reposed) {
            poser.restore();
        }

        Map<ModelPart, Matrix4f> rootTransforms = new IdentityHashMap<>();
        for (ModelRaycast.Root root : roots) {
            rootTransforms.put(root.part(), root.transform());
        }
        for (StuckArrow arrow : entry.arrows) {
            StuckArrow.Landing landing = arrow.landing(modelKey);
            if (landing == null) {
                continue;
            }
            Matrix4f partToFeet = landing.partTo(modelToFeet, rootTransforms.get(landing.root().part()));
            Vector3f at = partToFeet.transformPosition(landing.point(), new Vector3f());
            Vector3f dir = partToFeet.transformDirection(landing.dir(), new Vector3f()).normalize();
            drawer.draw(arrow, at, dir);
        }
    }

    public static void placeOnBody(ClientArrows.Entry entry, float bodyYaw, float sink, Drawer drawer) {
        long now = System.currentTimeMillis();
        for (StuckArrow arrow : entry.arrows) {
            float yaw = arrow.yawFor(bodyYaw, now);
            Vector3f dir = BodyFrame.toWorld(arrow.hit.dir(), yaw).normalize();
            Vector3f at = BodyFrame.toWorld(arrow.hit.point(), yaw).fma(sink, dir);
            drawer.draw(arrow, at, dir);
        }
    }

    static void remember(StuckArrow arrow, Vector3f at, Vector3f dir, double feetX, double feetY, double feetZ) {
        arrow.lastDrawnAt = new Vector3f(at).add((float) feetX, (float) feetY, (float) feetZ);
        arrow.lastDrawnDir = new Vector3f(dir);
    }
}
