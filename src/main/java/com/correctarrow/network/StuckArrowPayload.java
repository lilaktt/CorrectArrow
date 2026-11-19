package com.correctarrow.network;

import com.correctarrow.CorrectArrow;
import com.correctarrow.common.StuckHit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
//#if MC >= 1.20.5
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#endif

import java.util.ArrayList;
import java.util.List;

public record StuckArrowPayload(int entityId, boolean replace, List<StuckHit> hits)
//#if MC >= 1.20.5
        implements CustomPacketPayload
//#endif
{
    public static final int MAX_HITS = 256;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(CorrectArrow.MOD_ID, "stuck_arrows");

    //#if MC >= 1.20.5
    public static final Type<StuckArrowPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, StuckArrowPayload> CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), StuckArrowPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //#endif

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(replace);
        int count = Math.min(hits.size(), MAX_HITS);
        buf.writeVarInt(count);
        for (int i = hits.size() - count; i < hits.size(); i++) {
            hits.get(i).write(buf);
        }
    }

    public static StuckArrowPayload read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        boolean replace = buf.readBoolean();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_HITS) {
            throw new IllegalArgumentException("Too many stuck arrows in one packet: " + count);
        }
        List<StuckHit> hits = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            hits.add(StuckHit.read(buf));
        }
        return new StuckArrowPayload(entityId, replace, hits);
    }
}
