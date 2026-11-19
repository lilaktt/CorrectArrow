package com.correctarrow.client;

import com.correctarrow.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModelRaycast {
    private static final float BACKTRACK = 8.0F;

    private static final float FLAT = 0.01F;

    private ModelRaycast() {
    }

    public record Root(Matrix4f transform, ModelPart part, String name) {
    }

    public static StuckArrow.@Nullable Landing cast(List<Root> roots, Matrix4f modelToRay, Vector3f point, Vector3f dir) {
        Search search = new Search();
        search.dir.set(dir).normalize();
        search.origin.set(point).fma(-BACKTRACK, search.dir);
        for (Root root : roots) {
            search.root = root;
            search.visit(root.part(), root.name(), new Matrix4f(modelToRay).mul(root.transform()));
        }
        return search.result();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static void applyPose(ModelPart part, Matrix4f matrix) {
        matrix.translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
        if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
            matrix.rotate(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
        }
        if (part.xScale != 1.0F || part.yScale != 1.0F || part.zScale != 1.0F) {
            matrix.scale(part.xScale, part.yScale, part.zScale);
        }
    }

    private static final class Search {
        final Vector3f origin = new Vector3f();
        final Vector3f dir = new Vector3f();
        Root root;
        private final List<ModelPart> chain = new ArrayList<>();

        private final Candidate solid = new Candidate();
        private final Candidate flat = new Candidate();
        private final Candidate nearSolid = new Candidate();
        private final Candidate nearAny = new Candidate();

        private final Matrix4f inverse = new Matrix4f();
        private final Vector3f localOrigin = new Vector3f();
        private final Vector3f localDir = new Vector3f();

        void visit(ModelPart part, String path, Matrix4f parent) {
            if (!part.visible) {
                return;
            }
            ModelPartAccessor access = (ModelPartAccessor) (Object) part;
            List<ModelPart.Cube> cubes = access.correctarrow$cubes();
            Map<String, ModelPart> children = access.correctarrow$children();
            if (cubes.isEmpty() && children.isEmpty()) {
                return;
            }
            Matrix4f matrix = new Matrix4f(parent);
            applyPose(part, matrix);
            chain.add(part);
            if (!part.skipDraw && !cubes.isEmpty()) {
                matrix.invert(inverse);
                inverse.transformPosition(origin, localOrigin);
                inverse.transformDirection(dir, localDir);
                for (ModelPart.Cube cube : cubes) {
                    test(cube, matrix, path);
                }
            }
            for (Map.Entry<String, ModelPart> child : children.entrySet()) {
                visit(child.getValue(), path + "/" + child.getKey(), matrix);
            }
            chain.remove(chain.size() - 1);
        }

        private void test(ModelPart.Cube cube, Matrix4f partToRay, String path) {
            float[] min = {cube.minX / 16.0F, cube.minY / 16.0F, cube.minZ / 16.0F};
            float[] max = {cube.maxX / 16.0F, cube.maxY / 16.0F, cube.maxZ / 16.0F};
            float[] o = {localOrigin.x, localOrigin.y, localOrigin.z};
            float[] d = {localDir.x, localDir.y, localDir.z};
            boolean isFlat = cube.maxX - cube.minX < FLAT || cube.maxY - cube.minY < FLAT || cube.maxZ - cube.minZ < FLAT;

            float enter = Float.NEGATIVE_INFINITY;
            float exit = Float.POSITIVE_INFINITY;
            boolean crosses = true;
            for (int axis = 0; axis < 3 && crosses; axis++) {
                if (Math.abs(d[axis]) < 1.0E-9F) {
                    crosses = o[axis] >= min[axis] && o[axis] <= max[axis];
                } else {
                    float t1 = (min[axis] - o[axis]) / d[axis];
                    float t2 = (max[axis] - o[axis]) / d[axis];
                    enter = Math.max(enter, Math.min(t1, t2));
                    exit = Math.min(exit, Math.max(t1, t2));
                }
            }
            if (crosses && exit >= Math.max(enter, 0.0F)) {
                float t = Math.max(enter, 0.0F);
                Vector3f at = new Vector3f(localDir).mul(t).add(localOrigin);
                (isFlat ? flat : solid).offer(t, root, chain, path, at, localDir, true);
                return;
            }

            Vector3f center = new Vector3f((min[0] + max[0]) / 2, (min[1] + max[1]) / 2, (min[2] + max[2]) / 2);
            float along = Math.max(0.0F, new Vector3f(center).sub(localOrigin).dot(localDir) / localDir.lengthSquared());
            Vector3f onPath = new Vector3f(localDir).mul(along).add(localOrigin);
            Vector3f onCube = new Vector3f(
                    clamp(onPath.x, min[0], max[0]),
                    clamp(onPath.y, min[1], max[1]),
                    clamp(onPath.z, min[2], max[2]));
            float gap = partToRay.transformPosition(onCube, new Vector3f())
                    .distance(partToRay.transformPosition(onPath, new Vector3f()));
            if (!isFlat) {
                nearSolid.offer(gap, root, chain, path, onCube, localDir, false);
            }
            nearAny.offer(gap, root, chain, path, onCube, localDir, false);
        }

        StuckArrow.@Nullable Landing result() {
            for (Candidate candidate : new Candidate[]{solid, flat, nearSolid, nearAny}) {
                if (candidate.landing != null) {
                    return candidate.landing;
                }
            }
            return null;
        }
    }

    private static final class Candidate {
        float score = Float.POSITIVE_INFINITY;
        StuckArrow.@Nullable Landing landing;

        void offer(float score, Root root, List<ModelPart> chain, String path, Vector3f point, Vector3f dir, boolean onPath) {
            if (score < this.score) {
                this.score = score;
                this.landing = new StuckArrow.Landing(root, chain.toArray(new ModelPart[0]), path, new Vector3f(point), new Vector3f(dir), onPath);
            }
        }
    }
}
