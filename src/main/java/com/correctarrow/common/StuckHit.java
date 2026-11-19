package com.correctarrow.common;

import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

public record StuckHit(Vector3f point, Vector3f dir, float yaw, boolean fresh, int arrowId, String arrowType, boolean tipped) {
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(point.x);
        buf.writeFloat(point.y);
        buf.writeFloat(point.z);
        buf.writeFloat(dir.x);
        buf.writeFloat(dir.y);
        buf.writeFloat(dir.z);
        buf.writeFloat(yaw);
        buf.writeBoolean(fresh);
        buf.writeVarInt(arrowId + 1);
        buf.writeUtf(arrowType, 256);
        buf.writeBoolean(tipped);
    }

    public static StuckHit read(FriendlyByteBuf buf) {
        return new StuckHit(
                new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readVarInt() - 1,
                buf.readUtf(256),
                buf.readBoolean());
    }

    public StuckHit settled() {
        return new StuckHit(point, dir, yaw, false, -1, arrowType, tipped);
    }
}
