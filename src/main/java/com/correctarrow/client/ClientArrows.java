package com.correctarrow.client;

import com.correctarrow.common.PreHurtPose;
import com.correctarrow.common.StuckHit;
import com.correctarrow.network.StuckArrowPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientArrows {
    private static final long SETTLE_MS = 2000;

    private static final long HURT_MATCH_MS = 250;

    private static final Int2ObjectMap<Entry> ENTITIES = new Int2ObjectOpenHashMap<>();

    private ClientArrows() {
    }

    public static final class Entry {
        final List<StuckArrow> arrows = new ArrayList<>(4);
        private int lastCount = -1;

        public List<StuckArrow> arrows() {
            return Collections.unmodifiableList(arrows);
        }
    }

    static void accept(StuckArrowPayload payload, Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Entry entry = ENTITIES.computeIfAbsent(payload.entityId(), id -> new Entry());
        if (payload.replace()) {
            entry.arrows.clear();
        }

        PreHurtPose pose = mc.level.getEntity(payload.entityId()) instanceof PreHurtPose hurt
                && now - hurt.correctarrow$hurtAt() < HURT_MATCH_MS ? hurt : null;
        for (StuckHit hit : payload.hits()) {
            boolean undo = pose != null && hit.fresh();
            entry.arrows.add(new StuckArrow(hit, ArrowTextures.forHit(hit, mc.level), now,
                    undo ? pose.correctarrow$walkPosBeforeHurt() : Float.NaN,
                    undo ? pose.correctarrow$walkSpeedBeforeHurt() : Float.NaN));
        }
        int extra = entry.arrows.size() - StuckArrowPayload.MAX_HITS;
        if (extra > 0) {
            entry.arrows.subList(0, extra).clear();
        }
    }

    public static @Nullable Entry update(LivingEntity entity) {
        Entry entry = ENTITIES.get(entity.getId());
        if (entry == null) {
            return null;
        }
        List<StuckArrow> arrows = entry.arrows;
        int count = entity.getArrowCount();
        if (entry.lastCount >= 0 && count < entry.lastCount) {
            arrows.subList(0, Math.min(entry.lastCount - count, arrows.size())).clear();
        }
        entry.lastCount = count;

        if (arrows.size() > count && System.currentTimeMillis() - arrows.get(arrows.size() - 1).receivedAt > SETTLE_MS) {
            arrows.subList(0, arrows.size() - count).clear();
        }
        if (arrows.isEmpty()) {
            ENTITIES.remove(entity.getId());
            return null;
        }
        return entry;
    }

    public static List<StuckArrow> get(int entityId) {
        Entry entry = ENTITIES.get(entityId);
        return entry == null ? List.of() : entry.arrows();
    }

    static void forget(int entityId) {
        ENTITIES.remove(entityId);
    }

    static void clear() {
        ENTITIES.clear();
    }
}
