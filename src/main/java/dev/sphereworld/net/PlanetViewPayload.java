package dev.sphereworld.net;

import dev.sphereworld.SphereWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlanetViewPayload(boolean detail, boolean voxy) implements CustomPacketPayload {
    public static final Type<PlanetViewPayload> TYPE = new Type<>(SphereWorld.id("view"));
    public static final StreamCodec<FriendlyByteBuf, PlanetViewPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.detail);
                buf.writeBoolean(payload.voxy);
            },
            buf -> new PlanetViewPayload(buf.readBoolean(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
