package dev.sphereworld.client;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ClientPlanet {
    private ClientPlanet() {
    }

    public static @Nullable PlanetGeometry geometry() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || !client.isSameThread()) return null;
        return Planets.of(client.level);
    }

    private static @Nullable Player player() {
        return Minecraft.getInstance().player;
    }

    public static double x(double x) {
        PlanetGeometry g = geometry();
        Player p = player();
        return g == null || p == null ? x : g.nearestImage(x, p.getX());
    }

    public static double z(double z) {
        PlanetGeometry g = geometry();
        Player p = player();
        return g == null || p == null ? z : g.nearestImage(z, p.getZ());
    }

    public static int chunkX(int x) {
        PlanetGeometry g = geometry();
        Player p = player();
        return g == null || p == null ? x : g.nearestImageChunk(x, Math.floorDiv((int) Math.floor(p.getX()), 16));
    }

    public static int chunkZ(int z) {
        PlanetGeometry g = geometry();
        Player p = player();
        return g == null || p == null ? z : g.nearestImageChunk(z, Math.floorDiv((int) Math.floor(p.getZ()), 16));
    }

    public static ChunkPos chunk(ChunkPos pos) {
        int x = chunkX(pos.x());
        int z = chunkZ(pos.z());
        return x == pos.x() && z == pos.z() ? pos : new ChunkPos(x, z);
    }

    public static BlockPos pos(BlockPos pos) {
        PlanetGeometry g = geometry();
        Player p = player();
        if (g == null || p == null) return pos;
        int x = g.nearestImage(pos.getX(), (int) Math.floor(p.getX()));
        int z = g.nearestImage(pos.getZ(), (int) Math.floor(p.getZ()));
        return x == pos.getX() && z == pos.getZ() ? pos : new BlockPos(x, pos.getY(), z);
    }

    public static Vec3 vec(Vec3 pos) {
        double x = x(pos.x);
        double z = z(pos.z);
        return x == pos.x && z == pos.z ? pos : new Vec3(x, pos.y, z);
    }

    public static Vec3 shown(Vec3 pos) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        if (g == null) return pos;
        double x = g.canonical(pos.x);
        double z = g.canonical(pos.z);
        return x == pos.x && z == pos.z ? pos : new Vec3(x, pos.y, z);
    }

    public static double shownX(double x) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        return g == null ? x : g.canonical(x);
    }

    public static int shownBlock(int coordinate) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        return g == null ? coordinate : g.canonical(coordinate);
    }

    public static BlockPos shown(BlockPos pos) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        if (g == null) return pos;
        int x = g.canonical(pos.getX());
        int z = g.canonical(pos.getZ());
        return x == pos.getX() && z == pos.getZ() ? pos : new BlockPos(x, pos.getY(), z);
    }
}
