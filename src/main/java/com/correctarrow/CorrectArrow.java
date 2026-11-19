package com.correctarrow;

import com.correctarrow.network.Net;
import com.correctarrow.server.ServerHits;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
//#if MC >= 26.1
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
//#else
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
//#endif

public class CorrectArrow implements ModInitializer {
    public static final String MOD_ID = "correctarrow";

    @Override
    public void onInitialize() {
        Net.register();

        EntityTrackingEvents.START_TRACKING.register(ServerHits::sync);

        //#if MC >= 26.1
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> ServerHits.sync(player, player));
        //#else
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> ServerHits.sync(player, player));
        //#endif
    }
}
