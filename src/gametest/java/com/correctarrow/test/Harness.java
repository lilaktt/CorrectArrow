package com.correctarrow.test;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Harness {
    void waitTicks(int ticks);

    boolean waitFor(Predicate<Minecraft> condition, int maxTicks);

    void onClient(Consumer<Minecraft> action);

    <T> T fromClient(Function<Minecraft, T> action);

    <T> T fromServer(Function<MinecraftServer, T> action);

    void command(String command);

    void screenshot(String name);

    void holdUse(int ticks);

    void hideHud();

    void waitForChunks();

    boolean onDedicatedServer(Runnable scenario);
}
