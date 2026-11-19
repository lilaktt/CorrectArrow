//#if !FABRIC_GAMETEST
package com.correctarrow.test;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

final class OwnHarness implements Harness {
    private static final AtomicLong TICKS = new AtomicLong();
    private static boolean started;
    private static volatile boolean createWorld;

    static void install() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            synchronized (TICKS) {
                TICKS.incrementAndGet();
                TICKS.notifyAll();
            }
            if (!started) {
                mc.options.pauseOnLostFocus = false;
            }
            if (!started && mc.options.onboardAccessibility) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            }
            if (createWorld && mc.getOverlay() == null) {
                createWorld = false;
                createWorld(mc);
            }
            if (!started && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                started = true;
                Thread thread = new Thread(() -> new OwnHarness(mc).run(), "CorrectArrow test");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    private final Minecraft mc;

    private OwnHarness(Minecraft mc) {
        this.mc = mc;
    }

    private void run() {
        Scenarios scenarios = new Scenarios(this);
        try {
            scenarios.mute();
            onClient(m -> m.getWindow().setWindowed(1280, 720));
            waitTicks(20);
            createWorld = true;
            if (!waitFor(m -> m.level != null && m.player != null && m.getSingleplayerServer() != null, 2400)) {
                throw new IllegalStateException("the test world did not open");
            }
            waitTicks(20);
            onClient(m -> m.setScreen(null));
            waitForChunks();
            scenarios.runInWorld();
            scenarios.runDedicated();
            scenarios.finish();
        } catch (Throwable t) {
            Scenarios.LOG.error("test run failed", t);
        } finally {
            shutDown();
        }
    }

    private void shutDown() {
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                return;
            }
            Scenarios.LOG.warn("the game did not close; halting");
            Runtime.getRuntime().halt(0);
        }, "CorrectArrow test watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        try {
            onClient(m -> {
                if (m.level != null) {
                    m.level.disconnect();
                }
                //#if MC >= 1.20.2
                m.disconnect();
                //#else
                m.clearLevel();
                //#endif
            });
            waitFor(m -> m.getSingleplayerServer() == null, 600);
        } catch (Throwable t) {
            Scenarios.LOG.warn("leaving the test world failed", t);
        }
        onClient(Minecraft::stop);
    }

    private static void createWorld(Minecraft mc) {
        //#if MC >= 1.21.2
        GameRules rules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
        //#else
        GameRules rules = new GameRules();
        //#endif
        LevelSettings settings = new LevelSettings("correctarrow_test", GameType.CREATIVE, false, Difficulty.EASY, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("correctarrow_test_" + System.currentTimeMillis(), settings,
                new WorldOptions(1L, false, false),
                //#if MC >= 1.21.2
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions()
                //#else
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()
                //#endif
                //#if MC >= 1.20.3
                , null
                //#endif
        );
    }

    @Override
    public void waitTicks(int ticks) {
        long until = TICKS.get() + ticks;
        synchronized (TICKS) {
            while (TICKS.get() < until) {
                try {
                    TICKS.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Override
    public boolean waitFor(Predicate<Minecraft> condition, int maxTicks) {
        for (int i = 0; i <= maxTicks; i++) {
            if (fromClient(condition::test)) {
                return true;
            }
            waitTicks(1);
        }
        return false;
    }

    @Override
    public void onClient(Consumer<Minecraft> action) {
        fromClient(m -> {
            action.accept(m);
            return null;
        });
    }

    @Override
    public <T> T fromClient(Function<Minecraft, T> action) {
        CompletableFuture<T> result = mc.submit(() -> action.apply(mc));
        return result.join();
    }

    @Override
    public <T> T fromServer(Function<MinecraftServer, T> action) {
        MinecraftServer server = fromClient(Minecraft::getSingleplayerServer);
        return server.submit(() -> action.apply(server)).join();
    }

    @Override
    public void command(String command) {
        fromServer(server -> {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            return null;
        });
    }

    @Override
    public void screenshot(String name) {
        waitTicks(2);
        onClient(m -> Screenshot.grab(m.gameDirectory, name + ".png", m.getMainRenderTarget(), message -> { }));
        waitTicks(2);
    }

    @Override
    public void holdUse(int ticks) {
        onClient(m -> KeyMapping.set(m.options.keyUse.getDefaultKey(), true));
        waitTicks(ticks);
        onClient(m -> KeyMapping.set(m.options.keyUse.getDefaultKey(), false));
        waitTicks(2);
    }

    @Override
    public void hideHud() {
        onClient(m -> m.options.hideGui = true);
    }

    @Override
    public void waitForChunks() {
        waitTicks(100);
    }

    @Override
    public boolean onDedicatedServer(Runnable scenario) {
        return false;
    }
}
//#endif
