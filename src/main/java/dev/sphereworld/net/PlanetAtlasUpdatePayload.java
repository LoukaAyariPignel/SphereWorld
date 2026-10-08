package dev.sphereworld.net;

import dev.sphereworld.SphereWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlanetAtlasUpdatePayload(Identifier dimension, int[] cells, short[] heights, int[] colors) implements CustomPacketPayload {
    public static final Type<PlanetAtlasUpdatePayload> TYPE = new Type<>(SphereWorld.id("atlas_update"));
    public static final int MAX_CELLS = 16384;
    public static final StreamCodec<FriendlyByteBuf, PlanetAtlasUpdatePayload> CODEC =
            StreamCodec.of(PlanetAtlasUpdatePayload::write, PlanetAtlasUpdatePayload::read);

    private static void write(FriendlyByteBuf buf, PlanetAtlasUpdatePayload payload) {
        buf.writeIdentifier(payload.dimension);
        buf.writeVarInt(payload.cells.length);
        for (int i = 0; i < payload.cells.length; i++) {
            buf.writeVarInt(payload.cells[i]);
            buf.writeShort(payload.heights[i]);
            buf.writeMedium(payload.colors[i]);
        }
    }

    private static PlanetAtlasUpdatePayload read(FriendlyByteBuf buf) {
        Identifier dimension = buf.readIdentifier();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_CELLS) throw new IllegalArgumentException("Bad atlas update size " + count);
        int[] cells = new int[count];
        short[] heights = new short[count];
        int[] colors = new int[count];
        for (int i = 0; i < count; i++) {
            cells[i] = buf.readVarInt();
            heights[i] = buf.readShort();
            colors[i] = buf.readUnsignedMedium();
        }
        return new PlanetAtlasUpdatePayload(dimension, cells, heights, colors);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
