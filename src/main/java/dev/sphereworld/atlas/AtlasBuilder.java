package dev.sphereworld.atlas;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetGeometry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

final class AtlasBuilder {
    private static final int TILE = 8;

    private AtlasBuilder() {
    }

    static PlanetAtlas build(ServerLevel level, PlanetGeometry geometry, long minStageMillis) {
        long start = System.nanoTime();
        int size = PlanetAtlas.sizeFor(geometry);
        int cell = geometry.circumference() / size;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        int cells = size * size;
        short[] heights = new short[cells];
        int[] colors = new int[cells];
        int[] biomes = new int[cells];
        int minY = level.getMinY();
        int seaLevel = generator.getSeaLevel();
        int quartSea = QuartPos.fromBlock(seaLevel);
        PlanetAtlas atlas = new PlanetAtlas(level.dimension().identifier(), geometry.circumference(), size, seaLevel, heights, colors, biomes);
        AtlasBuildProgress.Live live = new AtlasBuildProgress.Live(atlas, new int[cells], new byte[cells], new AtomicInteger(), new AtomicInteger());
        AtlasBuildProgress.publish(live);
        int[] tiles = tileOrder(size, level.getSeed());

        @SuppressWarnings("unchecked")
        Holder<Biome>[] biomeHolders = new Holder[cells];
        int[] top = new int[cells];
        try {
            boolean hasClimate = generator.getBiomeSource() instanceof net.minecraft.world.level.biome.MultiNoiseBiomeSource;
            for (int stage = hasClimate ? 0 : AtlasBuildProgress.PHASES.indexOf("biomes"); stage < AtlasBuildProgress.PHASES.size(); stage++) {
                live.phase().set(stage);
                live.done().set(0);
                String name = AtlasBuildProgress.PHASES.get(stage);
                runStage(live, stage, tiles, size, minStageMillis, () -> {
                    Climate.Sampler climate = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
                    BiomeResolver resolver = generator.getBiomeSource().createUncachedResolver(randomState);
                    return (row, col, index) -> {
                        int x = geometry.minBlock() + col * cell + cell / 2;
                        int z = geometry.minBlock() + row * cell + cell / 2;
                        switch (name) {
                            case "continents", "erosion", "ridges", "climate" -> {
                                Climate.TargetPoint point = climate.sample(QuartPos.fromBlock(x), quartSea, QuartPos.fromBlock(z));
                                return climateColor(name, point);
                            }
                            case "biomes" -> {
                                Holder<Biome> biome = resolver.getNoiseBiome(QuartPos.fromBlock(x), quartSea, QuartPos.fromBlock(z));
                                biomeHolders[index] = biome;
                                return biomeColor(biome, x, z);
                            }
                            case "relief" -> {
                                int h = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                                top[index] = h;
                                boolean empty = h <= minY + 1;
                                heights[index] = empty ? PlanetAtlas.VOID : (short) Math.max(h, seaLevel);
                                return empty ? 0 : reliefColor(h, seaLevel);
                            }
                            default -> {
                                int h = top[index];
                                if (h <= minY + 1) return 0;
                                Holder<Biome> biome = biomeHolders[index];
                                if (h > seaLevel) {
                                    biome = resolver.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(h), QuartPos.fromBlock(z));
                                }
                                biomes[index] = biomeRegistry.getId(biome.value());
                                AtlasPalette.Sample sample = AtlasPalette.estimate(biome, h, seaLevel, x, z);
                                heights[index] = sample.height();
                                colors[index] = sample.color();
                                return sample.color();
                            }
                        }
                    };
                });
            }
        } finally {
            AtlasBuildProgress.finish(live);
        }
        SphereWorld.LOGGER.info("Planet atlas for {} ({}x{} cells of {} blocks) built in {} ms",
                level.dimension().identifier(), size, size, cell, (System.nanoTime() - start) / 1_000_000);
        return atlas;
    }

    @FunctionalInterface
    private interface CellStage {
        int colour(int row, int col, int index);
    }

    private static void runStage(AtlasBuildProgress.Live live, int stage, int[] tiles, int size, long minMillis,
                                 java.util.function.Supplier<CellStage> perWorker) {
        AtomicInteger next = new AtomicInteger();
        int tilesPerSide = (size + TILE - 1) / TILE;
        long begin = System.currentTimeMillis();
        int workers = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        IntStream.range(0, workers).parallel().forEach(worker -> {
            CellStage work = perWorker.get();
            for (int t; (t = next.getAndIncrement()) < tiles.length; ) {
                int tileCol = tiles[t] % tilesPerSide;
                int tileRow = tiles[t] / tilesPerSide;
                for (int row = tileRow * TILE; row < Math.min(size, tileRow * TILE + TILE); row++) {
                    for (int col = tileCol * TILE; col < Math.min(size, tileCol * TILE + TILE); col++) {
                        int index = row * size + col;
                        live.display()[index] = work.colour(row, col, index);
                        live.stageOf()[index] = (byte) (stage + 1);
                        live.done().incrementAndGet();
                    }
                }
                if (minMillis > 0) {
                    long due = begin + minMillis * (t + 1) / tiles.length;
                    for (long now; (now = System.currentTimeMillis()) < due; ) {
                        try {
                            Thread.sleep(Math.min(20, due - now));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        });
    }

    private static int[] tileOrder(int size, long seed) {
        int n = (size + TILE - 1) / TILE;
        int centre = (size / 2) / TILE;
        Random random = new Random(seed);
        double[] key = new double[n * n];
        Integer[] order = new Integer[n * n];
        for (int i = 0; i < n * n; i++) {
            int dx = Math.abs(i % n - centre);
            int dz = Math.abs(i / n - centre);
            dx = Math.min(dx, n - dx);
            dz = Math.min(dz, n - dz);
            key[i] = Math.sqrt(dx * dx + dz * dz) + random.nextDouble() * 3.0;
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble(i -> key[i]));
        int[] tiles = new int[n * n];
        for (int i = 0; i < tiles.length; i++) tiles[i] = order[i];
        return tiles;
    }

    private static int climateColor(String stage, Climate.TargetPoint point) {
        switch (stage) {
            case "continents" -> {
                float c = Climate.unquantizeCoord(point.continentalness());
                if (c < -1.05F) return 0x5A3D8C;
                if (c < -0.455F) return 0x10235E;
                if (c < -0.19F) return 0x1F4FA8;
                if (c < -0.11F) return 0xE6D58E;
                return lerp(0x7FB25A, 0x2F5A26, Math.clamp((c + 0.11F) / 1.1F, 0.0F, 1.0F));
            }
            case "erosion" -> {
                float e = Math.clamp((Climate.unquantizeCoord(point.erosion()) + 1.0F) / 2.0F, 0.0F, 1.0F);
                return lerp(0x4A2E1E, 0xE9DCC0, e);
            }
            case "ridges" -> {
                float w = Climate.unquantizeCoord(point.weirdness());
                float pv = 1.0F - Math.abs(3.0F * Math.abs(w) - 2.0F);
                float t = Math.clamp((pv + 1.0F) / 2.0F, 0.0F, 1.0F);
                return t < 0.5F ? lerp(0x1B3B6F, 0x7A8C7A, t * 2.0F) : lerp(0x7A8C7A, 0xF2F2F2, (t - 0.5F) * 2.0F);
            }
            default -> {
                float temp = Math.clamp((Climate.unquantizeCoord(point.temperature()) + 1.0F) / 2.0F, 0.0F, 1.0F);
                float humid = Math.clamp((Climate.unquantizeCoord(point.humidity()) + 1.0F) / 2.0F, 0.0F, 1.0F);
                int dry = lerp(0x4F7BE0, 0xE8643A, temp);
                return lerp(dry, lerp(0x9BE0E6, 0x2E9E44, temp), humid * 0.6F);
            }
        }
    }

    private static int biomeColor(Holder<Biome> biome, int x, int z) {
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            return AtlasPalette.estimate(biome, 0, 1, x, z).color();
        }
        return AtlasPalette.estimate(biome, 1, 0, x, z).color();
    }

    private static int reliefColor(int h, int sea) {
        if (h < sea) return lerp(0x5C8ED6, 0x0B1E4A, Math.clamp((sea - h) / 50.0F, 0.0F, 1.0F));
        float a = (h - sea) / 160.0F;
        if (a < 0.25F) return lerp(0x3E8A3A, 0xB8B060, a / 0.25F);
        if (a < 0.55F) return lerp(0xB8B060, 0x8B5A2B, (a - 0.25F) / 0.30F);
        if (a < 0.85F) return lerp(0x8B5A2B, 0x8C8C8C, (a - 0.55F) / 0.30F);
        return lerp(0x8C8C8C, 0xFFFFFF, Math.clamp((a - 0.85F) / 0.3F, 0.0F, 1.0F));
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
