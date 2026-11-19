package com.correctarrow.client;

import com.correctarrow.network.StuckArrowPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class CorrectArrowClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //#if MC >= 1.20.5
        ClientPlayNetworking.registerGlobalReceiver(StuckArrowPayload.TYPE, (payload, context) -> {
            if (context.client().isSameThread()) {
                ClientArrows.accept(payload, context.client());
            } else {
                context.client().execute(() -> ClientArrows.accept(payload, context.client()));
            }
        });
        //#else
        ClientPlayNetworking.registerGlobalReceiver(StuckArrowPayload.ID, (client, handler, buf, sender) -> {
            StuckArrowPayload payload = StuckArrowPayload.read(buf);
            client.execute(() -> ClientArrows.accept(payload, client));
        });
        //#endif
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> ClientArrows.forget(entity.getId()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientArrows.clear());
    }
}
