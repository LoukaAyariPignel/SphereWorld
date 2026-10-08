package dev.sphereworld.atlas;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.net.PlanetDetailPayload;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBufferPool;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;

public final class PlanetDetail {
    private record Level(int divisor, int grid) {
    }

    private static final Level[] LEVELS = {new Level(4, 384), new Level(2, 384)};
    private static final int CHECK_INTERVAL = 20;

    private record Patch(int centreX, int centreZ) {
    }

    private static final class PlayerState {
        final Patch[] sent = new Patch[LEVELS.length];
        String dimension = "";
        CompletableFuture<?> running;
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private static final java.util.concurrent.ForkJoinPool WORKERS = new java.util.concurrent.ForkJoinPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2), pool -> {
                var thread = java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                thread.setName("SphereWorld planet detail " + thread.getPoolIndex());
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                return thread;
            }, null, false);
    private static int ticks;

    private PlanetDetail() {
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % CHECK_INTERVAL != 0) return;

        if (dev.sphereworld.compat.VoxyPregen.active()) return;
        STATES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(player, PlanetDetailPayload.TYPE)) continue;
            ServerLevel level = player.level();
            PlanetGeometry geometry = Planets.of(level);
            PlanetAtlas atlas = geometry == null ? null : PlanetAtlases.get(level.dimension().identifier());
            if (atlas == null) continue;
            PlayerState state = STATES.computeIfAbsent(player.getUUID(), id -> new PlayerState());
            String dimension = level.dimension().identifier().toString();
            if (!state.dimension.equals(dimension)) {
                java.util.Arrays.fill(state.sent, null);
                state.dimension = dimension;
            }
            if (state.running != null && !state.running.isDone()) continue;

            for (int i = 0; i < LEVELS.length; i++) {
                int cell = Math.max(1, atlas.cellSize() / LEVELS[i].divisor);
                int half = cell * LEVELS[i].grid / 2;
                Patch sent = state.sent[i];
                int px = geometry.canonical((int) Math.floor(player.getX()));
                int pz = geometry.canonical((int) Math.floor(player.getZ()));
                if (sent != null && Math.abs(geometry.delta(sent.centreX, px)) < half / 3 && Math.abs(geometry.delta(sent.centreZ, pz)) < half / 3) {
                    continue;
                }
                int snap = cell * 16;
                Patch patch = new Patch(geometry.canonical(Math.floorDiv(px, snap) * snap), geometry.canonical(Math.floorDiv(pz, snap) * snap));
                state.sent[i] = patch;
                int index = i;
                state.running = CompletableFuture
                        .supplyAsync(() -> compute(level, geometry, atlas, index, cell, patch), WORKERS)
                        .thenAcceptAsync(payload -> {
                            if (!player.hasDisconnected() && player.level() == level) ServerPlayNetworking.send(player, payload);
                        }, server)
                        .exceptionally(error -> {
                            SphereWorld.LOGGER.warn("Could not compute the planet's detail round {}", player.getName().getString(), error);
                            return null;
                        });
                break;
            }
        }
    }

    private static PlanetDetailPayload compute(ServerLevel level, PlanetGeometry geometry, PlanetAtlas atlas, int index, int cell, Patch patch) {
        long start = System.nanoTime();
        int grid = LEVELS[index].grid;
        int half = cell * grid / 2;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int minY = level.getMinY();
        int seaLevel = atlas.seaLevel();
        short[] heights = new short[grid * grid];
        int[] colors = new int[grid * grid];
        ThreadLocal<BiomeResolver> resolvers = ThreadLocal.withInitial(() -> generator.getBiomeSource().createUncachedResolver(randomState));
        IntStream.range(0, grid).parallel().forEach(row -> {
            BiomeResolver resolver = resolvers.get();
            int z = patch.centreZ - half + row * cell + cell / 2;
            int x0 = patch.centreX - half + cell / 2;
            int[] tops = surfaceRow(generator, randomState, level, x0, z, cell, grid);
            for (int col = 0; col < grid; col++) {
                int x = geometry.canonical(x0 + col * cell);
                int i = row * grid + col;
                int h = tops[col];
                if (h <= minY + 1) {
                    heights[i] = PlanetAtlas.VOID;
                    continue;
                }
                int biomeY = QuartPos.fromBlock(Math.max(h, seaLevel));
                Holder<Biome> biome = resolver.getNoiseBiome(QuartPos.fromBlock(x), biomeY, QuartPos.fromBlock(geometry.canonical(z)));
                AtlasPalette.Sample sample = AtlasPalette.estimate(biome, h, seaLevel, x, z);
                heights[i] = sample.height();
                colors[i] = sample.color() & 0xFFFFFF;
            }
        });
        SphereWorld.LOGGER.debug("Planet detail level {} ({}x{} cells of {} blocks) round {} {} in {} ms",
                index, grid, grid, cell, patch.centreX, patch.centreZ, (System.nanoTime() - start) / 1_000_000);
        return new PlanetDetailPayload(level.dimension().identifier(), index, patch.centreX, patch.centreZ, cell, grid, heights, colors);
    }

    private static final int STEP_Y = 8;

    private static int[] surfaceRow(ChunkGenerator generator, RandomState randomState, ServerLevel level, int x0, int z, int step, int count) {
        int[] tops = new int[count];
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            for (int i = 0; i < count; i++) {
                tops[i] = generator.getBaseHeight(x0 + i * step, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            }
            return tops;
        }
        NoiseSettings settings = noise.generatorSettings().value().noiseSettings();
        int minY = settings.minY();
        int samples = settings.height() / STEP_Y + 1;
        DensityFunction finalDensity = noise.generatorSettings().value().noiseRouter().finalDensity();
        DensityBufferPool pool = randomState.acquireDensityBufferPool();
        try {
            SamplerContext context = SamplerContext.builder().useBufferArena(pool).build();
            DensityVolume volume = new DensityVolume(count, samples, 1, x0, minY, z, step, STEP_Y, 1);
            try (ScopedDensityBuffer buffer = context.acquireBuffer(volume)) {
                randomState.getSampler(finalDensity).sampleVolume(context, buffer, volume);
                for (int i = 0; i < count; i++) {
                    tops[i] = minY;
                    for (int y = samples - 2; y >= 0; y--) {
                        float below = buffer.get(volume.indexUnchecked(i, y, 0));
                        if (below <= 0.0F) continue;
                        float above = buffer.get(volume.indexUnchecked(i, y + 1, 0));

                        double t = above >= 0.0F ? 1.0 : below / (double) (below - above);
                        tops[i] = minY + (int) Math.floor((y + t) * STEP_Y) + 1;
                        break;
                    }
                }
            }
        } finally {
            randomState.releaseDensityBufferPool(pool);
        }
        return tops;
    }
}
