package com.correctarrow.client;

import com.correctarrow.CorrectArrow;
import com.correctarrow.common.StuckHit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
//#if MC >= 1.21.2
import com.correctarrow.mixin.client.ArrowRendererInvoker;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.SpectralArrowRenderer;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//#endif
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

final class ArrowTextures {
    private static final String ARROW = "minecraft:arrow";
    private static final String SPECTRAL_ARROW = "minecraft:spectral_arrow";
    private static final Map<String, Identifier> BY_TYPE = new HashMap<>();

    private ArrowTextures() {
    }

    static @Nullable Identifier forHit(StuckHit hit, ClientLevel level) {
        //#if MC < 1.21.2
        return null;
        //#else
        if (hit.arrowId() >= 0) {
            Identifier texture = fromEntity(level.getEntity(hit.arrowId()));
            if (texture != null) {
                return texture;
            }
        }
        if (hit.arrowType().equals(ARROW)) {
            return hit.tipped() ? TippableArrowRenderer.TIPPED_ARROW_LOCATION : TippableArrowRenderer.NORMAL_ARROW_LOCATION;
        }
        if (hit.arrowType().equals(SPECTRAL_ARROW)) {
            return SpectralArrowRenderer.SPECTRAL_ARROW_LOCATION;
        }
        return BY_TYPE.computeIfAbsent(hit.arrowType(), type -> fromType(type, level));
        //#endif
    }
    //#if MC >= 1.21.2

    private static Identifier fromType(String type, ClientLevel level) {
        try {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(type)).orElse(null);
            Entity probe = entityType == null ? null : entityType.create(level, EntitySpawnReason.LOAD);
            Identifier texture = fromEntity(probe);
            if (texture != null) {
                return texture;
            }
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger(CorrectArrow.MOD_ID).debug("No texture for arrow type {}", type, e);
        }
        return TippableArrowRenderer.NORMAL_ARROW_LOCATION;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable Identifier fromEntity(@Nullable Entity entity) {
        if (!(entity instanceof AbstractArrow)) {
            return null;
        }
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof ArrowRenderer)) {
            return null;
        }
        EntityRenderState state = renderer.createRenderState(entity, 1.0F);
        return ((ArrowRendererInvoker) renderer).correctarrow$getTextureLocation((ArrowRenderState) state);
    }
    //#endif
}
