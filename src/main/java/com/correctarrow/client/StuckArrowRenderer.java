package com.correctarrow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
//#if MC >= 1.21.2
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//#endif
//#if MC >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;
//#else
import net.minecraft.client.renderer.MultiBufferSource;
//#endif
//#if MC < 1.21.2
import com.correctarrow.common.StuckHit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;
//#if MC >= 1.20.5
import net.minecraft.world.item.alchemy.PotionContents;
//#else
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
//#endif
import net.minecraft.world.item.alchemy.Potions;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
//#endif

public final class StuckArrowRenderer {
    private static final float HITBOX_SINK = 0.1F;

    private StuckArrowRenderer() {
    }

    //#if MC >= 1.21.2
    private static final ArrowRenderState ARROW_STATE = new ArrowRenderState();
    private static @Nullable EntityModelSet bakedFrom;
    private static @Nullable ArrowModel arrowModel;

    private static ArrowModel arrowModel() {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (arrowModel == null || bakedFrom != models) {
            arrowModel = new ArrowModel(models.bakeLayer(ModelLayers.ARROW));
            bakedFrom = models;
        }
        return arrowModel;
    }

    public static int drawnCount(EntityRenderState state) {
        ClientArrows.Entry entry = ((StuckArrowState) state).correctarrow$arrows();
        return entry == null ? 0 : entry.arrows.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean onModel(LivingEntityRenderState state, EntityModel model, PoseStack poseStack, ArrowPlacement.Drawer drawer) {
        StuckArrowState ext = (StuckArrowState) state;
        ClientArrows.Entry entry = ext.correctarrow$arrows();
        ArrowPlacement.Feet feet = ext.correctarrow$feet();
        if (entry == null || feet == null) {
            return false;
        }
        ext.correctarrow$setDrawn(true);
        Matrix4f modelToFeet = new Matrix4f(feet.pose()).invert().mul(poseStack.last().pose());

        model.setupAnim(state);
        ModelPart root = model.root();
        ArrowPlacement.place(entry, root, List.of(new ModelRaycast.Root(new Matrix4f(), root, "")), modelToFeet, state.bodyRot,
                new ArrowPlacement.Poser() {
                    @Override
                    public void pose(float walkPos, float walkSpeed) {
                        float pos = state.walkAnimationPos;
                        float speed = state.walkAnimationSpeed;
                        state.walkAnimationPos = walkPos;
                        state.walkAnimationSpeed = walkSpeed;
                        model.setupAnim(state);
                        state.walkAnimationPos = pos;
                        state.walkAnimationSpeed = speed;
                    }

                    @Override
                    public void restore() {
                        model.setupAnim(state);
                    }
                }, drawer);
        return true;
    }
    //#endif

    //#if MC >= 1.21.9
    public static void submitOnModel(LivingEntityRenderState state, EntityModel<?> model, PoseStack poseStack, SubmitNodeCollector collector) {
        ArrowPlacement.Feet feet = ((StuckArrowState) state).correctarrow$feet();
        onModel(state, model, poseStack, (arrow, at, dir) -> submit(state, arrow, feet, at, dir, poseStack, collector));
    }

    public static void submitOnBody(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        StuckArrowState ext = (StuckArrowState) state;
        if (ext.correctarrow$arrows() == null) {
            return;
        }
        ArrowPlacement.Feet feet = ArrowPlacement.Feet.of(poseStack);
        ArrowPlacement.placeOnBody(ext.correctarrow$arrows(), ext.correctarrow$bodyYaw(), HITBOX_SINK,
                (arrow, at, dir) -> submit(state, arrow, feet, at, dir, poseStack, collector));
    }

    private static void submit(EntityRenderState state, StuckArrow arrow, ArrowPlacement.Feet feet, org.joml.Vector3f at,
                               org.joml.Vector3f dir, PoseStack poseStack, SubmitNodeCollector collector) {
        ArrowPlacement.remember(arrow, at, dir, state.x, state.y, state.z);
        feet.pushArrowPose(poseStack, at, dir);
        ArrowModel model = arrowModel();
        //#if MC >= 26.3
        collector.submitModel(model, ARROW_STATE, poseStack, arrow.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        //#else
        collector.submitModel(model, ARROW_STATE, poseStack, model.renderType(arrow.texture), state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        //#endif
        poseStack.popPose();
    }
    //#elseif MC >= 1.21.2
    public static void renderOnModel(LivingEntityRenderState state, EntityModel<?> model, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ArrowPlacement.Feet feet = ((StuckArrowState) state).correctarrow$feet();
        onModel(state, model, poseStack, (arrow, at, dir) -> render(state, arrow, feet, at, dir, poseStack, buffers, light));
    }

    public static void renderOnBody(EntityRenderState state, PoseStack poseStack, MultiBufferSource buffers, int light) {
        StuckArrowState ext = (StuckArrowState) state;
        if (ext.correctarrow$arrows() == null) {
            return;
        }
        ArrowPlacement.Feet feet = ArrowPlacement.Feet.of(poseStack);
        ArrowPlacement.placeOnBody(ext.correctarrow$arrows(), ext.correctarrow$bodyYaw(), HITBOX_SINK,
                (arrow, at, dir) -> render(state, arrow, feet, at, dir, poseStack, buffers, light));
    }

    private static void render(EntityRenderState state, StuckArrow arrow, ArrowPlacement.Feet feet, org.joml.Vector3f at,
                               org.joml.Vector3f dir, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ArrowPlacement.remember(arrow, at, dir, state.x, state.y, state.z);
        feet.pushArrowPose(poseStack, at, dir);
        ArrowModel model = arrowModel();
        model.setupAnim(ARROW_STATE);
        model.renderToBuffer(poseStack, buffers.getBuffer(model.renderType(arrow.texture)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
    //#else

    public static final class Frame {
        final LivingEntity entity;
        final ClientArrows.Entry entry;
        final ArrowPlacement.Feet feet;
        final float partialTick;
        float walkPos;
        float walkSpeed;
        float bob;
        float headYaw;
        float pitch;

        final List<ModelRaycast.Root> roots = new ArrayList<>();
        @Nullable Matrix4f modelPose;

        Frame(LivingEntity entity, ClientArrows.Entry entry, ArrowPlacement.Feet feet, float partialTick) {
            this.entity = entity;
            this.entry = entry;
            this.feet = feet;
            this.partialTick = partialTick;
        }
    }

    private static final Frame NONE = new Frame(null, null, null, 0);
    private static final ArrayDeque<Frame> FRAMES = new ArrayDeque<>();
    private static int captureDepth = -1;
    private static final Map<EntityModel<?>, Map<ModelPart, String>> PART_NAMES = new WeakHashMap<>();
    private static final Map<String, Entity> DUMMIES = new HashMap<>();
    private static @Nullable ClientLevel dummyLevel;

    public static void begin(LivingEntity entity, PoseStack poseStack, float partialTick) {
        ClientArrows.Entry entry = entity.isSpectator() ? null : ClientArrows.update(entity);
        FRAMES.push(entry == null ? NONE : new Frame(entity, entry, ArrowPlacement.Feet.of(poseStack), partialTick));
    }

    public static void end() {
        FRAMES.poll();
        captureDepth = -1;
    }

    private static @Nullable Frame current() {
        Frame frame = FRAMES.peek();
        return frame == null || frame == NONE ? null : frame;
    }

    public static void recordAnim(float walkPos, float walkSpeed, float bob, float headYaw, float pitch) {
        Frame frame = current();
        if (frame != null) {
            frame.walkPos = walkPos;
            frame.walkSpeed = walkSpeed;
            frame.bob = bob;
            frame.headYaw = headYaw;
            frame.pitch = pitch;
        }
    }

    public static void startCapture(PoseStack poseStack) {
        Frame frame = current();
        if (frame != null) {
            frame.roots.clear();
            frame.modelPose = new Matrix4f(poseStack.last().pose());
            captureDepth = 0;
        }
    }

    public static void stopCapture() {
        captureDepth = -1;
    }

    public static void enterPart(ModelPart part, PoseStack poseStack) {
        if (captureDepth < 0) {
            return;
        }
        Frame frame = current();
        if (captureDepth++ == 0 && frame != null && frame.modelPose != null && part.visible) {
            Matrix4f transform = new Matrix4f(frame.modelPose).invert().mul(poseStack.last().pose());
            frame.roots.add(new ModelRaycast.Root(transform, part, "/" + partName(frame, part)));
        }
    }

    public static void exitPart() {
        if (captureDepth > 0) {
            captureDepth--;
        }
    }

    public static int drawnCount(LivingEntity entity) {
        return ClientArrows.get(entity.getId()).size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void renderOnModel(EntityModel model, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Frame frame = current();
        if (frame == null) {
            return;
        }
        LivingEntity entity = frame.entity;
        float pt = frame.partialTick;
        List<ModelRaycast.Root> roots = frame.roots.isEmpty() ? rootsFromFields(model) : frame.roots;
        Matrix4f modelToFeet = new Matrix4f(frame.feet.pose()).invert().mul(poseStack.last().pose());
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        ArrowPlacement.place(frame.entry, model, roots, modelToFeet, bodyYaw, new ArrowPlacement.Poser() {
            @Override
            public void pose(float walkPos, float walkSpeed) {
                float pos = entity.isBaby() ? walkPos * 3.0F : walkPos;
                float speed = Math.min(walkSpeed, 1.0F);
                model.prepareMobModel(entity, pos, speed, pt);
                model.setupAnim(entity, pos, speed, frame.bob, frame.headYaw, frame.pitch);
            }

            @Override
            public void restore() {
                model.prepareMobModel(entity, frame.walkPos, frame.walkSpeed, pt);
                model.setupAnim(entity, frame.walkPos, frame.walkSpeed, frame.bob, frame.headYaw, frame.pitch);
            }
        }, (arrow, at, dir) -> drawArrow(entity, arrow, frame.feet, at, dir, poseStack, buffers, light, pt));
    }

    public static void renderOnBody(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffers, int light, float partialTick) {
        ClientArrows.Entry entry = ClientArrows.update(entity);
        if (entry == null) {
            return;
        }
        ArrowPlacement.Feet feet = ArrowPlacement.Feet.of(poseStack);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        ArrowPlacement.placeOnBody(entry, bodyYaw, HITBOX_SINK,
                (arrow, at, dir) -> drawArrow(entity, arrow, feet, at, dir, poseStack, buffers, light, partialTick));
    }

    private static void drawArrow(LivingEntity entity, StuckArrow arrow, ArrowPlacement.Feet feet, Vector3f at, Vector3f dir,
                                  PoseStack poseStack, MultiBufferSource buffers, int light, float pt) {
        ArrowPlacement.remember(arrow, at, dir, Mth.lerp(pt, entity.xo, entity.getX()), Mth.lerp(pt, entity.yo, entity.getY()),
                Mth.lerp(pt, entity.zo, entity.getZ()));
        Entity dummy = dummyArrow(arrow.hit);
        if (dummy == null) {
            return;
        }
        float yRot = ArrowPlacement.yRot(dir);
        float xRot = ArrowPlacement.xRot(dir);
        dummy.setYRot(yRot);
        dummy.setXRot(xRot);
        dummy.yRotO = yRot;
        dummy.xRotO = xRot;
        poseStack.pushPose();
        poseStack.last().pose().set(feet.pose());
        poseStack.last().normal().set(feet.normal());
        poseStack.translate(at.x, at.y, at.z);

        Minecraft.getInstance().getEntityRenderDispatcher().render(dummy, 0.0, 0.0, 0.0, 0.0F, pt, poseStack, buffers, light);
        poseStack.popPose();
    }

    private static @Nullable Entity dummyArrow(StuckHit hit) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        if (level != dummyLevel) {
            DUMMIES.clear();
            dummyLevel = level;
        }
        return DUMMIES.computeIfAbsent(hit.arrowType() + (hit.tipped() ? "#tipped" : ""), key -> {
            if (hit.tipped() && hit.arrowType().equals("minecraft:arrow")) {
                //#if MC >= 1.21
                return new Arrow(level, 0, 0, 0, PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON), null);
                //#elseif MC >= 1.20.5
                return new Arrow(level, 0, 0, 0, PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON));
                //#else
                Arrow tipped = new Arrow(EntityType.ARROW, level);
                tipped.setEffectsFromItem(PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.POISON));
                return tipped;
                //#endif
            }
            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(hit.arrowType())).orElse(null);
                Entity entity = type == null ? null : type.create(level);
                if (entity instanceof AbstractArrow) {
                    return entity;
                }
            } catch (RuntimeException e) {
            }
            return new Arrow(EntityType.ARROW, level);
        });
    }

    private static List<ModelRaycast.Root> rootsFromFields(EntityModel<?> model) {
        Map<ModelPart, String> names = partNames(model);
        Set<ModelPart> nested = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelPart part : names.keySet()) {
            for (ModelPart child : ((com.correctarrow.mixin.client.ModelPartAccessor) (Object) part).correctarrow$children().values()) {
                collect(child, nested);
            }
        }
        List<ModelRaycast.Root> roots = new ArrayList<>();
        names.forEach((part, name) -> {
            if (!nested.contains(part)) {
                roots.add(new ModelRaycast.Root(new Matrix4f(), part, "/" + name));
            }
        });
        return roots;
    }

    private static void collect(ModelPart part, Set<ModelPart> into) {
        if (into.add(part)) {
            for (ModelPart child : ((com.correctarrow.mixin.client.ModelPartAccessor) (Object) part).correctarrow$children().values()) {
                collect(child, into);
            }
        }
    }

    private static String partName(Frame frame, ModelPart part) {
        for (Map.Entry<EntityModel<?>, Map<ModelPart, String>> e : PART_NAMES.entrySet()) {
            if (e.getValue().containsKey(part)) {
                return e.getValue().get(part);
            }
        }
        return "part" + frame.roots.size();
    }

    private static Map<ModelPart, String> partNames(EntityModel<?> model) {
        return PART_NAMES.computeIfAbsent(model, m -> {
            Map<ModelPart, String> names = new IdentityHashMap<>();
            for (Class<?> c = m.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.getType() == ModelPart.class && !Modifier.isStatic(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(m);
                            if (value != null) {
                                names.putIfAbsent((ModelPart) value, field.getName());
                            }
                        } catch (ReflectiveOperationException | RuntimeException ignored) {
                        }
                    }
                }
            }
            return names;
        });
    }

    public static void prepareNames(EntityModel<?> model) {
        if (current() != null) {
            partNames(model);
        }
    }
    //#endif
}
