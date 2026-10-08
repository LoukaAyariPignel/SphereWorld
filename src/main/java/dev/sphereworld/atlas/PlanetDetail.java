package dev.sphereworld.atlas;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.compat.VoxyPregen;
import dev.sphereworld.net.PlanetDetailPayload;
import dev.sphereworld.net.PlanetViewPayload;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBufferPool;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;
import org.jspecify.annotations.Nullable;

public final class PlanetDetail {
    private record Level(int divisor, int grid) {
    }

    private static final Level[] LEVELS = {new Level(4, 384), new Level(2, 384)};
    private static final int CHECK_INTERVAL = 20;
    private static final int TILE = 64;
    private static final int STRIP = 16;
    private static final int STEP_Y = 8;
    private static final int LATTICE = 4;
    private static final int MAX_TILES = 192;
    private static final int BELOW = 48;
    private static final int ABOVE = 96;

    private record Patch(int centreX, int centreZ) {
    }

    private record Tile(short[] heights, int[] colors) {
    }

    private static final class PlayerState {
        final Patch[] sent = new Patch[LEVELS.length];
        String dimension = "";
        @Nullable CompletableFuture<?> running;
        boolean drawn;
        boolean voxy;
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    private static final Map<String, Long2ObjectLinkedOpenHashMap<CompletableFuture<Tile>>> TILES = new HashMap<>();

    private static final ForkJoinPool WORKERS = new ForkJoinPool(
            Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 4)), pool -> {
                var thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                thread.setName("SphereWorld planet detail " + thread.getPoolIndex());
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                return thread;
            }, null, true);
    private static int ticks;

    private PlanetDetail() {
    }

    public static void view(ServerPlayer player, PlanetViewPayload payload) {
        PlayerState state = STATES.computeIfAbsent(player.getUUID(), id -> new PlayerState());
        if (!state.drawn && payload.detail()) Arrays.fill(state.sent, null);
        state.drawn = payload.detail();
        state.voxy = payload.voxy();
    }

    public static void clear() {
        STATES.clear();
        TILES.clear();
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % CHECK_INTERVAL != 0) return;

        if (VoxyPregen.active()) return;
        STATES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        boolean voxyHasPlanet = VoxyPregen.planetComplete();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerState state = STATES.get(player.getUUID());
            if (state == null || !state.drawn || state.voxy && voxyHasPlanet) continue;
            if (!ServerPlayNetworking.canSend(player, PlanetDetailPayload.TYPE)) continue;
            ServerLevel level = player.level();
            PlanetGeometry geometry = Planets.of(level);
            PlanetAtlas atlas = geometry == null ? null : PlanetAtlases.get(level.dimension().identifier());
            if (atlas == null || player.getY() < atlas.seaLevel() - 16) continue;
            String dimension = level.dimension().identifier().toString();
            if (!state.dimension.equals(dimension)) {
                Arrays.fill(state.sent, null);
                state.dimension = dimension;
            }
            if (state.running != null && !state.running.isDone()) continue;

            int px = geometry.canonical(Mth.floor(player.getX()));
            int pz = geometry.canonical(Mth.floor(player.getZ()));
            for (int i = 0; i < LEVELS.length; i++) {
                int cell = Math.max(1, atlas.cellSize() / LEVELS[i].divisor);
                int half = cell * LEVELS[i].grid / 2;
                Patch sent = state.sent[i];
                if (sent != null && Math.abs(geometry.delta(sent.centreX, px)) < half / 3 && Math.abs(geometry.delta(sent.centreZ, pz)) < half / 3) {
                    continue;
                }
                int snap = cell * TILE;
                Patch patch = new Patch(snap(geometry, px, snap), snap(geometry, pz, snap));
                state.sent[i] = patch;
                state.running = request(server, player, level, geometry, atlas, dimension, i, cell, patch);
                break;
            }
        }
    }

    private static int snap(PlanetGeometry geometry, int block, int snap) {
        int h = geometry.half();
        return geometry.canonical(-h + Math.floorDiv(block + h + snap / 2, snap) * snap);
    }

    private static long key(int tx, int tz) {
        return ((long) tx << 32) | (tz & 0xFFFFFFFFL);
    }

    private static int[] tilesSpanned(int first, int count, int cellsAround) {
        IntArrayList tiles = new IntArrayList();
        for (int k = 0; k < count; k++) {
            int tile = Math.floorMod(first + k, cellsAround) / TILE;
            if (tiles.isEmpty() || tiles.getInt(tiles.size() - 1) != tile) {
                if (tiles.contains(tile)) continue;
                tiles.add(tile);
            }
        }
        return tiles.toIntArray();
    }

    private static CompletableFuture<?> request(MinecraftServer server, ServerPlayer player, ServerLevel level, PlanetGeometry geometry,
                                                PlanetAtlas atlas, String dimension, int index, int cell, Patch patch) {
        int grid = LEVELS[index].grid;
        int h = geometry.half();
        int cellsAround = Math.ceilDiv(geometry.circumference(), cell);
        int firstX = Math.floorDiv(geometry.canonical(patch.centreX - cell * grid / 2) + h, cell);
        int firstZ = Math.floorDiv(geometry.canonical(patch.centreZ - cell * grid / 2) + h, cell);
        int seaLevel = atlas.seaLevel();
        Long2ObjectLinkedOpenHashMap<CompletableFuture<Tile>> cache = TILES.computeIfAbsent(dimension + "#" + index,
                key -> new Long2ObjectLinkedOpenHashMap<>());
        Long2ObjectOpenHashMap<CompletableFuture<Tile>> tiles = new Long2ObjectOpenHashMap<>();
        for (int tz : tilesSpanned(firstZ, grid, cellsAround)) {
            for (int tx : tilesSpanned(firstX, grid, cellsAround)) {
                long key = key(tx, tz);
                CompletableFuture<Tile> tile = cache.getAndMoveToLast(key);
                if (tile == null || tile.isCompletedExceptionally()) {
                    tile = CompletableFuture.supplyAsync(() -> computeTile(level, geometry, seaLevel, cell, cellsAround, tx, tz), WORKERS);
                    cache.putAndMoveToLast(key, tile);
                }
                tiles.put(key, tile);
            }
        }
        while (cache.size() > MAX_TILES) cache.removeFirst();

        return CompletableFuture.allOf(tiles.values().toArray(CompletableFuture[]::new))
                .thenApplyAsync(ignored -> assemble(level, index, cell, grid, patch, firstX, firstZ, cellsAround, tiles), WORKERS)
                .thenAcceptAsync(payload -> {
                    if (!player.hasDisconnected() && player.level() == level) ServerPlayNetworking.send(player, payload);
                }, server)
                .exceptionally(error -> {
                    SphereWorld.LOGGER.warn("Could not compute the planet's detail round {}", player.getName().getString(), error);
                    return null;
                });
    }

    private static PlanetDetailPayload assemble(ServerLevel level, int index, int cell, int grid, Patch patch, int firstX, int firstZ,
                                                int cellsAround, Long2ObjectOpenHashMap<CompletableFuture<Tile>> tiles) {
        short[] heights = new short[grid * grid];
        int[] colors = new int[grid * grid];
        for (int row = 0; row < grid; row++) {
            int j = Math.floorMod(firstZ + row, cellsAround);
            int tz = j / TILE;
            int lz = j % TILE;
            for (int col = 0; col < grid; col++) {
                int i = Math.floorMod(firstX + col, cellsAround);
                Tile tile = tiles.get(key(i / TILE, tz)).join();
                int from = lz * TILE + i % TILE;
                heights[row * grid + col] = tile.heights[from];
                colors[row * grid + col] = tile.colors[from];
            }
        }
        return new PlanetDetailPayload(level.dimension().identifier(), index, patch.centreX, patch.centreZ, cell, grid, heights, colors);
    }

    private static Tile computeTile(ServerLevel level, PlanetGeometry geometry, int seaLevel, int cell, int cellsAround, int tx, int tz) {
        int h = geometry.half();
        int width = Math.min(TILE, cellsAround - tx * TILE);
        int depth = Math.min(TILE, cellsAround - tz * TILE);
        int offset = cell >= LATTICE ? cell / 2 / LATTICE * LATTICE : cell / 2;
        int x0 = -h + tx * TILE * cell + offset;
        int z0 = -h + tz * TILE * cell + offset;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        BiomeResolver resolver = generator.getBiomeSource().createUncachedResolver(randomState);
        int minY = level.getMinY();
        int[] tops = surface(generator, randomState, level, x0, z0, cell, width, depth);
        short[] heights = new short[TILE * TILE];
        int[] colors = new int[TILE * TILE];
        for (int lz = 0; lz < depth; lz++) {
            int z = z0 + lz * cell;
            for (int lx = 0; lx < width; lx++) {
                int x = x0 + lx * cell;
                int top = tops[lz * width + lx];
                int i = lz * TILE + lx;
                if (top <= minY + 1) {
                    heights[i] = PlanetAtlas.VOID;
                    continue;
                }
                int biomeY = QuartPos.fromBlock(Math.max(top, seaLevel));
                Holder<Biome> biome = resolver.getNoiseBiome(QuartPos.fromBlock(geometry.canonical(x)), biomeY, QuartPos.fromBlock(geometry.canonical(z)));
                AtlasPalette.Sample sample = AtlasPalette.estimate(biome, top, seaLevel, x, z);
                heights[i] = sample.height();
                colors[i] = sample.color() & 0xFFFFFF;
            }
        }
        return new Tile(heights, colors);
    }

    private static int[] surface(ChunkGenerator generator, RandomState randomState, ServerLevel level, int x0, int z0, int step, int width, int depth) {
        int[] tops = new int[width * depth];
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            for (int lz = 0; lz < depth; lz++) {
                for (int lx = 0; lx < width; lx++) {
                    tops[lz * width + lx] = generator.getBaseHeight(x0 + lx * step, z0 + lz * step, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                }
            }
            return tops;
        }
        NoiseSettings settings = noise.generatorSettings().value().noiseSettings();
        int minY = settings.minY();
        int maxY = minY + settings.height() / STEP_Y * STEP_Y;
        NoiseRouter router = noise.generatorSettings().value().noiseRouter();
        DensitySampler density = randomState.getSampler(router.finalDensity());
        DensitySampler estimate = randomState.getSampler(router.chunkSurfaceLevel());
        boolean lattice = step % LATTICE == 0 && Math.floorMod(x0, LATTICE) == 0 && Math.floorMod(z0, LATTICE) == 0
                && Math.floorMod(minY, STEP_Y) == 0;
        boolean perColumn = lattice && step != LATTICE;
        DensityBufferPool pool = randomState.acquireDensityBufferPool();
        try {
            SamplerContext context = SamplerContext.builder().useBufferArena(pool).build();
            SamplerContext cached = SamplerContext.builder().useBufferArena(pool).enableCaches().build();
            for (int strip = 0; strip < depth; strip += STRIP) {
                int rows = Math.min(STRIP, depth - strip);
                int z = z0 + strip * step;
                float low = Float.POSITIVE_INFINITY;
                float high = Float.NEGATIVE_INFINITY;
                DensityVolume flat = new DensityVolume(width, 1, rows, x0, 0, z, step, 1, step);
                try (ScopedDensityBuffer levels = cached.acquireBuffer(flat)) {
                    estimate.sampleVolume(cached, levels, flat);
                    for (int i = 0; i < width * rows; i++) {
                        low = Math.min(low, levels.get(i));
                        high = Math.max(high, levels.get(i));
                    }
                }
                int bottom = Math.clamp(minY + Math.floorDiv(Mth.floor(low) - BELOW - minY, STEP_Y) * STEP_Y, minY, maxY - STEP_Y);
                int top = Math.clamp(minY + Math.ceilDiv(Mth.ceil(high) + ABOVE - minY, STEP_Y) * STEP_Y, bottom + STEP_Y, maxY);
                boolean found = scan(density, context, x0, z, step, width, rows, bottom, top, minY, maxY, perColumn, tops, strip * width);
                if (!found) scan(density, context, x0, z, step, width, rows, minY, maxY, minY, maxY, perColumn, tops, strip * width);
            }
        } finally {
            randomState.releaseDensityBufferPool(pool);
        }
        return tops;
    }

    private static boolean scan(DensitySampler density, SamplerContext context, int x0, int z, int step, int width, int rows,
                                int bottom, int top, int minY, int maxY, boolean perColumn, int[] tops, int offset) {
        int samples = (top - bottom) / STEP_Y + 1;
        boolean complete = true;
        if (perColumn) {
            for (int lz = 0; lz < rows; lz++) {
                for (int lx = 0; lx < width; lx++) {
                    DensityVolume volume = new DensityVolume(1, samples, 1, x0 + lx * step, bottom, z + lz * step, LATTICE, STEP_Y, LATTICE);
                    try (ScopedDensityBuffer buffer = context.acquireBuffer(volume)) {
                        density.sampleVolume(context, buffer, volume);
                        int surface = surface(buffer, volume, 0, 0, samples, bottom, top, minY, maxY);
                        if (surface == Integer.MIN_VALUE) complete = false;
                        tops[offset + lz * width + lx] = surface;
                    }
                }
            }
            return complete;
        }
        DensityVolume volume = new DensityVolume(width, samples, rows, x0, bottom, z, step, STEP_Y, step);
        try (ScopedDensityBuffer buffer = context.acquireBuffer(volume)) {
            density.sampleVolume(context, buffer, volume);
            for (int lz = 0; lz < rows; lz++) {
                for (int lx = 0; lx < width; lx++) {
                    int surface = surface(buffer, volume, lx, lz, samples, bottom, top, minY, maxY);
                    if (surface == Integer.MIN_VALUE) complete = false;
                    tops[offset + lz * width + lx] = surface;
                }
            }
        }
        return complete;
    }

    private static int surface(ScopedDensityBuffer buffer, DensityVolume volume, int lx, int lz, int samples, int bottom, int top, int minY, int maxY) {
        if (top < maxY && buffer.get(volume.indexUnchecked(lx, samples - 1, lz)) > 0.0F) return Integer.MIN_VALUE;
        for (int y = samples - 2; y >= 0; y--) {
            float below = buffer.get(volume.indexUnchecked(lx, y, lz));
            if (below <= 0.0F) continue;
            float above = buffer.get(volume.indexUnchecked(lx, y + 1, lz));

            double t = above >= 0.0F ? 1.0 : below / (double) (below - above);
            return bottom + (int) Math.floor((y + t) * STEP_Y) + 1;
        }
        return bottom > minY ? Integer.MIN_VALUE : minY;
    }
}
