package com.correctarrow.test;

import com.correctarrow.client.ClientArrows;
import com.correctarrow.client.StuckArrow;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//#if MC >= 1.21.2
import net.minecraft.world.entity.EntitySpawnReason;
//#endif

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class Scenarios {
    static final Logger LOG = LoggerFactory.getLogger("CorrectArrowTest");

    private static final double HUSK = 1.0625;

    private static final double TOLERANCE = 0.03;

    private final Harness h;
    private final List<String> failures = new ArrayList<>();
    private final String only = System.getProperty("correctarrow.test.only", "");
    private Vec3 base;
    private boolean hudHidden;

    Scenarios(Harness harness) {
        this.h = harness;
    }

    private void check(boolean ok, String what) {
        LOG.info("[check] {} {}", ok ? "ok  " : "FAIL", what);
        if (!ok) {
            failures.add(what);
        }
    }

    private boolean wanted(String part) {
        return only.isEmpty() || List.of(only.split(",")).contains(part);
    }

    private <T> T onServer(Function<MinecraftServer, T> call) {
        return h.fromServer(call);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static ServerLevel level(MinecraftServer server) {
        return (ServerLevel) player(server).level();
    }

    private static EntityType<?> husk() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace("husk")).orElseThrow();
    }

    void mute() {
        h.onClient(mc -> mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
    }

    void runInWorld() {
        prepare();
        if (wanted("humanoid")) {
            humanoid();
        }
        if (wanted("modded")) {
            modded();
        }
        if (wanted("nomodel")) {
            noModel();
        }
        if (wanted("sync")) {
            lateSync();
        }
        if (wanted("self")) {
            selfHit();
        }
        if (wanted("bow")) {
            bowShot();
        }
    }

    void runDedicated() {
        if (wanted("dedicated") && !h.onDedicatedServer(this::dedicatedServer)) {
            LOG.info("[check] skip dedicated server: this harness cannot start one");
        }
    }

    void finish() {
        if (failures.isEmpty()) {
            LOG.info("all checks passed");
        } else {
            throw new AssertionError("failed: " + failures);
        }
    }

    void prepare() {
        h.command("gamemode creative @a");
        h.command("time set 6000");
        h.command("weather clear");
        //#if MC >= 1.21.11
        h.command("gamerule spawn_mobs false");
        //#else
        h.command("gamerule doMobSpawning false");
        //#endif
        h.command("difficulty easy");
        Vec3 at = onServer(s -> player(s).position());
        base = new Vec3(Math.floor(at.x) + 0.5, Math.floor(at.y), Math.floor(at.z) + 0.5);
        int x = (int) Math.floor(base.x);
        int y = (int) base.y;
        int z = (int) Math.floor(base.z);
        h.command("fill %d %d %d %d %d %d air".formatted(x - 10, y, z - 10, x + 10, y + 12, z + 14));
        h.command("fill %d %d %d %d %d %d smooth_stone".formatted(x - 10, y - 1, z - 10, x + 10, y - 1, z + 14));
        h.command("forceload add %d %d".formatted(x, z));
        if (!hudHidden) {
            h.hideHud();
            hudHidden = true;
        }
        h.waitTicks(10);
    }

    private int spawn(EntityType<?> type, Vec3 at) {
        return onServer(s -> {
            ServerLevel level = level(s);
            //#if MC >= 1.21.2
            Entity entity = type.create(level, EntitySpawnReason.COMMAND);
            //#else
            Entity entity = type.create(level);
            //#endif
            //#if MC >= 1.21.5
            entity.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            //#else
            entity.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            //#endif
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setPersistenceRequired();
            }
            if (entity instanceof LivingEntity living) {
                living.setYBodyRot(0.0F);
                living.setYHeadRot(0.0F);
                living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
                living.setHealth(1000.0F);
                living.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            }
            level.addFreshEntity(entity);
            return entity.getId();
        });
    }

    private void shoot(Vec3 from, Vec3 to) {
        onServer(s -> {
            ServerLevel level = level(s);
            //#if MC >= 1.21
            Arrow arrow = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
            //#elseif MC >= 1.20.3
            Arrow arrow = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW));
            //#else
            Arrow arrow = new Arrow(level, from.x, from.y, from.z);
            //#endif
            arrow.setNoGravity(true);
            arrow.setDeltaMovement(to.subtract(from).normalize().scale(3.0));
            level.addFreshEntity(arrow);
            return null;
        });
    }

    private void place(Vec3 at, float yaw, float pitch) {
        onServer(s -> {
            ServerPlayer p = player(s);
            p.teleportTo(at.x, at.y, at.z);
            p.setYRot(yaw);
            p.setXRot(pitch);
            p.setYHeadRot(yaw);
            return null;
        });
        h.onClient(mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        });
        h.waitTicks(5);
    }

    private List<StuckArrow> arrows(int entityId) {
        return h.fromClient(mc -> new ArrayList<>(ClientArrows.get(entityId)));
    }

    private boolean waitDrawn(int entityId, int count) {
        boolean ok = h.waitFor(mc -> {
            List<StuckArrow> list = ClientArrows.get(entityId);
            return list.size() == count && list.stream().allMatch(a -> a.lastDrawnAt != null);
        }, 100);
        if (!ok) {
            List<StuckArrow> list = arrows(entityId);
            LOG.warn("waiting for {} drawn arrows on {}: have {}", count, entityId, list.size());
            for (StuckArrow a : list) {
                LOG.warn("  hit {} {} drawn {} landing {}", a.hit().point(), a.hit().dir(), a.lastDrawnAt,
                        a.anyLanding() == null ? null : a.anyLanding().path());
            }
            h.screenshot("correctarrow_timeout_" + entityId);
        }
        return ok;
    }

    private Vec3 clientPos(int entityId) {
        return h.fromClient(mc -> mc.level.getEntity(entityId).position());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean onPart(int entityId, StuckArrow.Landing landing, String part) {
        if (landing == null) {
            return false;
        }
        return h.fromClient(mc -> {
            Entity entity = mc.level.getEntity(entityId);
            Object model = ((LivingEntityRenderer) mc.getEntityRenderDispatcher().getRenderer(entity)).getModel();
            ModelPart target = switch (part) {
                case "/head" -> ((HumanoidModel) model).head;
                case "/body" -> ((HumanoidModel) model).body;
                case "/left_leg" -> ((HumanoidModel) model).leftLeg;
                case "/wheel" -> ((TestEntitiesClient.SpinnerModel) model).wheel;
                case "/post" -> ((TestEntitiesClient.SpinnerModel) model).post;
                default -> null;
            };
            return target != null && Arrays.asList(landing.chain()).contains(target);
        });
    }

    private void expectArrow(String what, int entityId, int index, Vec3 expected, String part, boolean onPath) {
        List<StuckArrow> list = arrows(entityId);
        if (list.size() <= index || list.get(index).lastDrawnAt == null) {
            check(false, what + ": no drawn arrow " + index);
            return;
        }
        StuckArrow arrow = list.get(index);
        Vec3 feet = clientPos(entityId);
        Vector3f drawn = arrow.lastDrawnAt;
        double off = new Vec3(drawn.x, drawn.y, drawn.z).subtract(feet).distanceTo(expected);
        StuckArrow.Landing landing = arrow.anyLanding();
        check(off < TOLERANCE, "%s: drawn %.3f from the body surface (at %s, wanted %s)".formatted(
                what, off, new Vec3(drawn.x, drawn.y, drawn.z).subtract(feet), expected));
        if (part != null) {
            check(onPart(entityId, landing, part), what + ": on part " + part + " (got " + (landing == null ? "none" : landing.path()) + ")");
            check(landing != null && landing.onPath() == onPath, what + (onPath ? ": the flight path crosses the part"
                    : ": a grazing hit goes to the nearest part"));
        }
    }

    private void shot(String name) {
        h.waitTicks(2);
        h.screenshot("correctarrow_" + name);
    }

    private void humanoid() {
        Vec3 at = base.add(-2.5, 0, 0);
        int husk = spawn(husk(), at);
        place(at.add(1.6, 0, 2.6), 150.0F, 20.0F);

        shoot(at.add(0, 1.85, 2.5), at.add(0, 1.85, 0));
        check(waitDrawn(husk, 1), "husk: head shot arrives and is drawn");
        expectArrow("husk head", husk, 0, new Vec3(0, 1.85, 0.25 * HUSK), "/head", true);
        h.waitTicks(22);

        shoot(at.add(0, 1.0, 2.5), at.add(0, 1.0, 0));
        check(waitDrawn(husk, 2), "husk: chest shot arrives");
        expectArrow("husk chest (hitbox is 0.3 out, body 0.13)", husk, 1, new Vec3(0, 1.0, 0.125 * HUSK), "/body", true);
        h.waitTicks(22);

        shoot(at.add(0, 1.0, -2.5), at.add(0, 1.0, 0));
        check(waitDrawn(husk, 3), "husk: back shot arrives");
        expectArrow("husk back", husk, 2, new Vec3(0, 1.0, -0.125 * HUSK), "/body", true);
        h.waitTicks(22);

        shoot(at.add(2.5, 0.4, 0), at.add(0, 0.4, 0));
        check(waitDrawn(husk, 4), "husk: leg shot arrives");

        h.waitTicks(40);
        expectArrow("husk leg from the side", husk, 3, new Vec3(3.9 / 16 * HUSK, 0.4, 0), "/left_leg", true);

        check(onServer(s -> ((LivingEntity) level(s).getEntity(husk)).getArrowCount()) == 4, "husk: vanilla counted 4 arrows");
        shot("husk");
    }

    private void modded() {
        Vec3 at = base;
        int spinner = spawn(TestEntities.SPINNER, at);
        place(at.add(0, 0, 3.2), 180.0F, 25.0F);

        shoot(at.add(0.3, 3.5, 0), at.add(0.3, 0, 0));
        boolean wheelShot = waitDrawn(spinner, 1);
        check(wheelShot, "spinner: wheel shot from above arrives");
        if (!wheelShot) {
            return;
        }
        StuckArrow.Landing wheel = arrows(spinner).get(0).anyLanding();
        check(onPart(spinner, wheel, "/wheel"), "spinner: the arrow is on the wheel (got " + (wheel == null ? "none" : wheel.path()) + ")");
        Vector3f first = new Vector3f(arrows(spinner).get(0).lastDrawnAt);
        Vec3 feet = clientPos(spinner);
        check(Math.abs(first.y - feet.y - 1.6885) < TOLERANCE, "spinner: sits on the wheel's top face: " + (first.y - feet.y));
        shot("spinner_a");
        h.waitTicks(10);
        Vector3f later = new Vector3f(arrows(spinner).get(0).lastDrawnAt);
        double r1 = Math.hypot(first.x - feet.x, first.z - feet.z);
        double r2 = Math.hypot(later.x - feet.x, later.z - feet.z);
        double moved = Math.hypot(later.x - first.x, later.z - first.z);
        check(Math.abs(r1 - 0.3) < TOLERANCE && Math.abs(r2 - 0.3) < TOLERANCE,
                "spinner: keeps its distance from the axis while turning: %.3f, %.3f".formatted(r1, r2));
        check(moved > 0.15, "spinner: turns with the wheel: moved %.3f".formatted(moved));
        shot("spinner_b");

        shoot(at.add(0, 0.6, 2.5), at.add(0, 0.6, 0));
        check(waitDrawn(spinner, 2), "spinner: post shot arrives");
        expectArrow("spinner post (hitbox 0.5 out, post 0.06)", spinner, 1, new Vec3(0, 0.6, 1.0 / 16), "/post", true);

        h.waitTicks(22);
        shoot(at.add(0.35, 0.6, 2.5), at.add(0.35, 0.6, 0));
        check(waitDrawn(spinner, 3), "spinner: grazing shot arrives");
        expectArrow("spinner graze", spinner, 2, new Vec3(1.0 / 16, 0.6, 0), "/post", false);
        shot("spinner_c");
    }

    private void noModel() {
        Vec3 at = base.add(2.5, 0, 0);
        int blob = spawn(TestEntities.BLOB, at);
        place(at.add(0, 0, 3.0), 180.0F, 15.0F);
        shoot(at.add(0, 0.4, 2.5), at.add(0, 0.4, 0));
        check(waitDrawn(blob, 1), "blob: arrow is drawn although the renderer has no model");
        check(arrows(blob).isEmpty() || arrows(blob).get(0).anyLanding() == null, "blob: no model landing");
        expectArrow("blob hitbox", blob, 0, new Vec3(0, 0.4, 0.4 - 0.1), null, true);
    }

    private void lateSync() {
        Vec3 at = base.add(-2.5, 0, 0);
        int husk = onServer(s -> level(s).getEntities(husk(), e -> true).stream()
                .findFirst().map(Entity::getId).orElse(-1));
        if (husk < 0) {
            husk = spawn(husk(), at);
            place(at.add(1.6, 0, 2.6), 150.0F, 20.0F);
            shoot(at.add(0, 1.85, 2.5), at.add(0, 1.85, 0));
            waitDrawn(husk, 1);
        }
        int id = husk;
        place(at.add(1.6, 0, 2.6), 150.0F, 20.0F);
        List<Vector3f> before = arrows(id).stream().map(a -> new Vector3f(a.lastDrawnAt)).toList();

        place(base.add(0, 60, 600), 180.0F, 0.0F);
        if (!h.waitFor(mc -> mc.level.getEntity(id) == null, 200)) {
            LOG.warn("the husk stayed tracked");
        }
        check(h.fromClient(mc -> mc.level.getEntity(id) == null && ClientArrows.get(id).isEmpty()),
                "sync: far away the client forgets the husk and its arrows");

        place(at.add(1.6, 0, 2.6), 150.0F, 20.0F);
        check(waitDrawn(id, before.size()), "sync: coming back brings all %d arrows again".formatted(before.size()));
        List<StuckArrow> after = arrows(id);
        check(after.stream().noneMatch(a -> a.hit().fresh()), "sync: they came as a resend, not as new hits");
        for (int i = 0; i < Math.min(before.size(), after.size()); i++) {
            if (after.get(i).lastDrawnAt == null) {
                continue;
            }
            double off = new Vector3f(after.get(i).lastDrawnAt).distance(before.get(i));
            check(off < 0.05, "sync: arrow %d in the same spot as before (%.3f)".formatted(i, off));
        }
        shot("sync");
    }

    private void selfHit() {
        Vec3 at = base.add(0, 0, 6.0);
        place(at, 180.0F, 0.0F);
        h.command("gamemode survival @a");
        h.onClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        h.waitTicks(10);
        int me = h.fromClient(mc -> mc.player.getId());
        Vec3 feet = onServer(s -> player(s).position());
        shoot(feet.add(0, 1.2, -2.5), feet.add(0, 1.2, 0));
        check(waitDrawn(me, 1), "self: the arrow in the own body arrives and is drawn");
        StuckArrow.Landing landing = arrows(me).isEmpty() ? null : arrows(me).get(0).anyLanding();
        check(onPart(me, landing, "/body") && landing.onPath(),
                "self: on the chest (got " + (landing == null ? "none" : landing.path()) + ")");
        check(h.waitFor(mc -> mc.player.getArrowCount() == 1, 40), "self: vanilla counts one arrow");
        check(arrows(me).size() == 1, "self: still exactly one arrow drawn after the count arrived");
        shot("self");
        h.command("gamemode creative @a");

        onServer(s -> {
            player(s).setArrowCount(0);
            return null;
        });
        h.waitTicks(15);
        check(arrows(me).isEmpty(), "self: arrows pulled out by vanilla disappear");
        h.onClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
    }

    private void bowShot() {
        Vec3 target = base.add(-6.5, 0, -4);
        int husk = spawn(husk(), target);
        Vec3 from = target.add(0, 0, 7);

        float pitch = (float) Math.toDegrees(Math.atan2(1.62 - 1.1, 7.0));
        place(from, 180.0F, pitch);
        onServer(s -> {
            ServerPlayer p = player(s);
            //#if MC >= 1.21.5
            p.getInventory().setItem(p.getInventory().getSelectedSlot(), new ItemStack(Items.BOW));
            //#else
            p.getInventory().setItem(p.getInventory().selected, new ItemStack(Items.BOW));
            //#endif
            return null;
        });
        h.waitTicks(3);
        h.holdUse(25);
        check(waitDrawn(husk, 1), "bow: the shot sticks in the husk and is drawn");
        StuckArrow.Landing landing = arrows(husk).isEmpty() ? null : arrows(husk).get(0).anyLanding();
        check(landing != null && landing.onPath(), "bow: the real flight path crosses the model");
        place(target.add(1.2, 0, 2.2), 150.0F, 25.0F);
        shot("bow");
    }

    private void dedicatedServer() {
        prepare();
        Vec3 at = base.add(-2.5, 0, 0);
        int husk = spawn(husk(), at);
        place(at.add(1.6, 0, 2.6), 150.0F, 20.0F);
        h.waitTicks(10);
        shoot(at.add(0, 1.85, 2.5), at.add(0, 1.85, 0));
        check(waitDrawn(husk, 1), "dedicated: the hit arrives over the network");
        expectArrow("dedicated husk head", husk, 0, new Vec3(0, 1.85, 0.25 * HUSK), "/head", true);
        shot("dedicated");
    }
}
