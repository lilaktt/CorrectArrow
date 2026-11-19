package com.correctarrow.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 1.20.5
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//#else
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
//#endif

public final class Net {
    private Net() {
    }

    public static void register() {
        //#if MC >= 26.1
        PayloadTypeRegistry.clientboundPlay().register(StuckArrowPayload.TYPE, StuckArrowPayload.CODEC);
        //#elseif MC >= 1.20.5
        PayloadTypeRegistry.playS2C().register(StuckArrowPayload.TYPE, StuckArrowPayload.CODEC);
        //#endif
    }

    public static void send(ServerPlayer player, StuckArrowPayload payload) {
        //#if MC >= 1.20.5
        if (ServerPlayNetworking.canSend(player, StuckArrowPayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
        //#else
        if (ServerPlayNetworking.canSend(player, StuckArrowPayload.ID)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            payload.write(buf);
            ServerPlayNetworking.send(player, StuckArrowPayload.ID, buf);
        }
        //#endif
    }
}
