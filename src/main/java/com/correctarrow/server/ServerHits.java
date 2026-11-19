package com.correctarrow.server;

import com.correctarrow.common.BodyFrame;
import com.correctarrow.common.StuckHit;
import com.correctarrow.network.Net;
import com.correctarrow.network.StuckArrowPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ServerHits {
    private ServerHits() {
    }

    public static void onArrowStuck(AbstractArrow arrow, EntityHitResult hit) {
        if (!(arrow.level() instanceof ServerLevel) || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Vec3 dir = arrow.getDeltaMovement();
        if (dir.lengthSqr() < 1.0E-8) {
            return;
        }
        dir = dir.normalize();
        Vec3 at = entryPoint(arrow, target, hit, dir).subtract(target.position());
        float yaw = target.yBodyRot;
        StuckHit stuck = new StuckHit(
                BodyFrame.toBody(at.x, at.y, at.z, yaw),
                BodyFrame.toBody(dir.x, dir.y, dir.z, yaw),
                yaw,
                true,
                arrow.getId(),
                BuiltInRegistries.ENTITY_TYPE.getKey(arrow.getType()).toString(),
                arrow instanceof Arrow plain && plain.getColor() > 0
        );

        List<StuckHit> hits = ((StuckHitHolder) target).correctarrow$hits();
        hits.add(stuck.settled());
        trim(hits, target.getArrowCount());

        StuckArrowPayload payload = new StuckArrowPayload(target.getId(), false, List.of(stuck));
        for (ServerPlayer player : PlayerLookup.tracking(target)) {
            send(player, payload);
        }
        if (target instanceof ServerPlayer self) {
            send(self, payload);
        }
    }

    private static Vec3 entryPoint(AbstractArrow arrow, LivingEntity target, EntityHitResult hit, Vec3 dir) {
        Vec3 from = arrow.position().subtract(dir.scale(2.0));
        Vec3 to = arrow.position().add(dir.scale(arrow.getDeltaMovement().length() + 4.0));
        AABB box = target.getBoundingBox();
        return box.clip(from, to)
                .or(() -> box.inflate(0.3).clip(from, to))
                .orElse(hit.getLocation());
    }

    public static void sync(Entity entity, ServerPlayer player) {
        if (entity instanceof LivingEntity living) {
            List<StuckHit> hits = ((StuckHitHolder) living).correctarrow$hits();
            if (!hits.isEmpty()) {
                send(player, new StuckArrowPayload(living.getId(), true, new ArrayList<>(hits)));
            }
        }
    }

    public static void trim(List<StuckHit> hits, int arrowCount) {
        int extra = hits.size() - Math.max(0, Math.min(arrowCount, StuckArrowPayload.MAX_HITS));
        if (extra > 0) {
            hits.subList(0, extra).clear();
        }
    }

    private static void send(ServerPlayer player, StuckArrowPayload payload) {
        Net.send(player, payload);
    }
}
