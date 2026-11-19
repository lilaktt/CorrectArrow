package com.correctarrow.common;

import org.joml.Vector3f;

public final class BodyFrame {
    private BodyFrame() {
    }

    public static Vector3f toBody(double x, double y, double z, float yawDegrees) {
        double r = Math.toRadians(yawDegrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return new Vector3f((float) (x * cos + z * sin), (float) y, (float) (-x * sin + z * cos));
    }

    public static Vector3f toWorld(Vector3f body, float yawDegrees) {
        double r = Math.toRadians(yawDegrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return new Vector3f((float) (body.x * cos - body.z * sin), body.y, (float) (body.x * sin + body.z * cos));
    }
}
