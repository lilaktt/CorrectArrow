//#if FABRIC_GAMETEST
package com.correctarrow.test;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CorrectArrowClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        FabricHarness harness = new FabricHarness(context);
        Scenarios scenarios = new Scenarios(harness);
        scenarios.mute();
        context.getInput().resizeWindow(1280, 720);
        context.waitTicks(5);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            harness.server = world.getServer();
            harness.world = world;
            harness.waitForChunks();
            scenarios.runInWorld();
        }
        scenarios.runDedicated();
        scenarios.finish();
    }

    private static final class FabricHarness implements Harness {
        private final ClientGameTestContext context;
        private TestServerContext server;
        private Object world;

        FabricHarness(ClientGameTestContext context) {
            this.context = context;
        }

        @Override
        public void waitTicks(int ticks) {
            context.waitTicks(ticks);
        }

        @Override
        public boolean waitFor(Predicate<Minecraft> condition, int maxTicks) {
            try {
                context.waitFor(condition, maxTicks);
                return true;
            } catch (AssertionError | RuntimeException timeout) {
                return false;
            }
        }

        @Override
        public void onClient(Consumer<Minecraft> action) {
            context.runOnClient(action::accept);
        }

        @Override
        public <T> T fromClient(Function<Minecraft, T> action) {
            return context.computeOnClient(action::apply);
        }

        @Override
        public <T> T fromServer(Function<MinecraftServer, T> action) {
            return server.computeOnServer(action::apply);
        }

        @Override
        public void command(String command) {
            server.runCommand(command);
        }

        @Override
        public void screenshot(String name) {
            context.takeScreenshot(name);
        }

        @Override
        public void holdUse(int ticks) {
            context.getInput().holdKeyFor(options -> options.keyUse, ticks);
        }

        @Override
        public void hideHud() {
            context.getInput().pressKey(InputConstants.KEY_F1);
        }

        @Override
        public void waitForChunks() {
            try {
                Object target = world;
                for (String getter : new String[]{"getConnection", "getClientLevel", "getClientWorld"}) {
                    Method method = publicMethod(target, "waitForChunksRender") == null ? publicMethod(world, getter) : null;
                    if (method != null) {
                        target = method.invoke(world);
                        break;
                    }
                }
                Method wait = publicMethod(target, "waitForChunksRender");
                if (wait == null) {
                    throw new IllegalStateException("no waitForChunksRender on " + target.getClass());
                }
                wait.invoke(target);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean onDedicatedServer(Runnable scenario) {
            try (TestDedicatedServerContext dedicated = context.worldBuilder().createServer();
                 var connection = dedicated.connect()) {
                server = dedicated;
                world = connection;
                waitForChunks();
                scenario.run();
            }
            return true;
        }

        private static Method publicMethod(Object object, String name) {
            List<Class<?>> types = new ArrayList<>(List.of(object.getClass().getInterfaces()));
            for (int i = 0; i < types.size(); i++) {
                try {
                    return types.get(i).getMethod(name);
                } catch (NoSuchMethodException e) {
                    types.addAll(List.of(types.get(i).getInterfaces()));
                }
            }
            return null;
        }
    }
}
//#endif
