package dev.sphereworld.net;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.atlas.PlanetAtlas;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlanetAtlasPayload(PlanetAtlas atlas) implements CustomPacketPayload {
    public static final Type<PlanetAtlasPayload> TYPE = new Type<>(SphereWorld.id("atlas"));
    public static final int MAX_SIZE = 8 * 1024 * 1024;
    public static final StreamCodec<FriendlyByteBuf, PlanetAtlasPayload> CODEC =
            PlanetAtlas.CODEC.map(PlanetAtlasPayload::new, PlanetAtlasPayload::atlas);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
