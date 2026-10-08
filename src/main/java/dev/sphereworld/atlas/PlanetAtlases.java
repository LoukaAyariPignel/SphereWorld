package dev.sphereworld.atlas;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.net.PlanetAtlasPayload;
import dev.sphereworld.net.PlanetAtlasUpdatePayload;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;

public final class PlanetAtlases {
    private static final int FORMAT = 2;

    private static final int CHUNKS_PER_TICK = 96;
    private static final int SEND_INTERVAL = 20;
    private static final int SAVE_INTERVAL = 20 * 120;

    private static final long STAGE_MILLIS_IN_VIEW = 1500;

    private static final Map<Identifier, PlanetAtlas> READY = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, LongLinkedOpenHashSet> DIRTY_CHUNKS = new ConcurrentHashMap<>();

    private static final Map<Identifier, Int2LongLinkedOpenHashMap> PENDING = new ConcurrentHashMap<>();
    private static final Map<Identifier, Boolean> UNSAVED = new ConcurrentHashMap<>();
    private static int ticks;

    private PlanetAtlases() {
    }

    public static void buildBeforeStart(MinecraftServer server) {
        java.util.List<ServerLevel> levels = new java.util.ArrayList<>();
        server.getAllLevels().forEach(levels::add);
        levels.sort(java.util.Comparator.comparing(level -> level.dimension() != Level.OVERWORLD));
        CompletableFuture<Void> building = CompletableFuture.runAsync(() -> {
            for (ServerLevel level : levels) {
                PlanetGeometry geometry = Planets.of(level);
                if (geometry == null || level.dimensionType().hasCeiling() || READY.containsKey(level.dimension().identifier())) continue;
                try {
                    PlanetAtlas atlas = loadOrBuild(level, geometry, cacheFile(server, level), level.dimension() == Level.OVERWORLD ? STAGE_MILLIS_IN_VIEW : 0);
                    READY.put(atlas.dimension(), atlas);
                } catch (RuntimeException e) {
                    SphereWorld.LOGGER.error("Could not build planet atlas for {}", level.dimension().identifier(), e);
                }
            }
        }, Util.backgroundExecutor());

        server.managedBlock(building::isDone);
    }

    public static void start(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            PlanetGeometry geometry = Planets.of(level);
            if (geometry == null || level.dimensionType().hasCeiling() || READY.containsKey(level.dimension().identifier())) continue;
            Path cache = cacheFile(server, level);
            CompletableFuture.supplyAsync(() -> loadOrBuild(level, geometry, cache, 0), Util.backgroundExecutor())
                    .thenAcceptAsync(atlas -> {
                        READY.put(atlas.dimension(), atlas);
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, atlas);
                    }, server)
                    .exceptionally(error -> {
                        SphereWorld.LOGGER.error("Could not build planet atlas for {}", level.dimension().identifier(), error);
                        return null;
                    });
        }
    }

    public static void stop(MinecraftServer server) {
        saveAll(server, false);
        READY.clear();
        DIRTY_CHUNKS.clear();
        PENDING.clear();
        UNSAVED.clear();
    }

    public static void markDirty(Level level, ChunkPos pos) {
        if (level.isClientSide() || level.dimensionType().hasCeiling() || Planets.of(level) == null) return;
        LongLinkedOpenHashSet set = DIRTY_CHUNKS.computeIfAbsent(level.dimension(), key -> new LongLinkedOpenHashSet());
        synchronized (set) {
            set.add(pos.pack());
        }
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            PlanetAtlas atlas = READY.get(level.dimension().identifier());
            LongLinkedOpenHashSet dirty = DIRTY_CHUNKS.get(level.dimension());
            if (atlas == null || dirty == null) continue;
            long[] batch;
            synchronized (dirty) {
                int n = Math.min(CHUNKS_PER_TICK, dirty.size());
                batch = new long[n];
                LongIterator it = dirty.iterator();
                for (int i = 0; i < n; i++) {
                    batch[i] = it.nextLong();
                    it.remove();
                }
            }
            for (long packed : batch) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
                if (chunk != null) sampleChunk(atlas, chunk);
            }
        }
        if (++ticks % SEND_INTERVAL == 0) flush(server);
        if (ticks % SAVE_INTERVAL == 0) saveAll(server, true);
    }

    private static void sampleChunk(PlanetAtlas atlas, LevelChunk chunk) {
        int surfaceTop = chunk.getLevel() instanceof ServerLevel server
                && server.getChunkSource().getGenerator() instanceof dev.sphereworld.worldgen.stacked.StackedChunkGenerator
                ? dev.sphereworld.worldgen.stacked.StackBand.OVERWORLD.worldMaxY() : Integer.MAX_VALUE;
        int cell = atlas.cellSize();
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        int half = atlas.circumference() / 2;

        int firstX = x0 + Math.floorMod(cell / 2 - (x0 + half), cell);
        int firstZ = z0 + Math.floorMod(cell / 2 - (z0 + half), cell);
        for (int z = firstZ; z < z0 + 16; z += cell) {
            for (int x = firstX; x < x0 + 16; x += cell) {
                AtlasPalette.Sample sample = AtlasPalette.sample(chunk, x, z, surfaceTop);
                int index = atlas.index(x, z);
                short height = sample == null ? PlanetAtlas.VOID : sample.height();
                int color = sample == null ? 0 : sample.color();
                if (atlas.heights()[index] == height && atlas.colors()[index] == color) continue;
                atlas.heights()[index] = height;
                atlas.colors()[index] = color;
                UNSAVED.put(atlas.dimension(), Boolean.TRUE);
                PENDING.computeIfAbsent(atlas.dimension(), key -> new Int2LongLinkedOpenHashMap())
                        .put(index, ((long) height << 32) | (color & 0xFFFFFFFFL));
            }
        }
    }

    private static void flush(MinecraftServer server) {
        for (var entry : PENDING.entrySet()) {
            Int2LongLinkedOpenHashMap cells = entry.getValue();
            while (!cells.isEmpty()) {
                int n = Math.min(PlanetAtlasUpdatePayload.MAX_CELLS, cells.size());
                int[] index = new int[n];
                short[] heights = new short[n];
                int[] colors = new int[n];
                var it = cells.int2LongEntrySet().fastIterator();
                for (int i = 0; i < n; i++) {
                    var cell = it.next();
                    index[i] = cell.getIntKey();
                    heights[i] = (short) (cell.getLongValue() >> 32);
                    colors[i] = (int) cell.getLongValue();
                    it.remove();
                }
                PlanetAtlasUpdatePayload payload = new PlanetAtlasUpdatePayload(entry.getKey(), index, heights, colors);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (ServerPlayNetworking.canSend(player, PlanetAtlasUpdatePayload.TYPE)) ServerPlayNetworking.send(player, payload);
                }
            }
        }
    }

    public static void sendAll(ServerPlayer player) {
        READY.values().forEach(atlas -> send(player, atlas));
    }

    public static PlanetAtlas get(Identifier dimension) {
        return READY.get(dimension);
    }

    private static void send(ServerPlayer player, PlanetAtlas atlas) {
        if (ServerPlayNetworking.canSend(player, PlanetAtlasPayload.TYPE)) {
            ServerPlayNetworking.send(player, new PlanetAtlasPayload(atlas.copy()));
        }
    }

    private static void saveAll(MinecraftServer server, boolean async) {
        for (ServerLevel level : server.getAllLevels()) {
            PlanetAtlas atlas = READY.get(level.dimension().identifier());
            PlanetGeometry geometry = Planets.of(level);
            if (atlas == null || geometry == null || UNSAVED.remove(atlas.dimension()) == null) continue;
            PlanetAtlas snapshot = atlas.copy();
            Path cache = cacheFile(server, level);
            long key = cacheKey(level, geometry);
            if (async) {
                CompletableFuture.runAsync(() -> write(cache, key, snapshot), Util.ioPool());
            } else {
                write(cache, key, snapshot);
            }
        }
    }

    private static Path cacheFile(MinecraftServer server, ServerLevel level) {
        Identifier id = level.dimension().identifier();
        return server.getWorldPath(LevelResource.ROOT).resolve("data")
                .resolve("sphereworld_atlas_" + id.getNamespace() + "_" + id.getPath().replace('/', '_') + ".bin");
    }

    private static long cacheKey(ServerLevel level, PlanetGeometry geometry) {
        return level.getSeed() * 31 + geometry.circumference() * 17L + FORMAT;
    }

    private static PlanetAtlas loadOrBuild(ServerLevel level, PlanetGeometry geometry, Path cache, long minStageMillis) {
        long key = cacheKey(level, geometry);
        try {
            if (Files.isRegularFile(cache) && !Boolean.getBoolean("sphereworld.rebuildAtlas")) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(Files.readAllBytes(cache)));
                if (buf.readLong() == key) {
                    PlanetAtlas atlas = PlanetAtlas.CODEC.decode(buf);
                    if (atlas.size() == PlanetAtlas.sizeFor(geometry)) return atlas;
                }
            }
        } catch (RuntimeException | IOException e) {
            SphereWorld.LOGGER.warn("Ignoring unreadable planet atlas cache {}", cache, e);
        }
        PlanetAtlas atlas = AtlasBuilder.build(level, geometry, minStageMillis);
        write(cache, key, atlas);
        return atlas;
    }

    private static void write(Path cache, long key, PlanetAtlas atlas) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeLong(key);
            PlanetAtlas.CODEC.encode(buf, atlas);
            Files.createDirectories(cache.getParent());
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            Path temp = cache.resolveSibling(cache.getFileName() + ".tmp");
            Files.write(temp, bytes);
            Files.move(temp, cache, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            SphereWorld.LOGGER.warn("Could not save planet atlas {}", cache, e);
        }
    }
}
