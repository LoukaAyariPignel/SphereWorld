package dev.sphereworld.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.mixin.worldgen.MultiNoiseBiomeSourceAccessor;
import dev.sphereworld.planet.PlanetGeometry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.storage.LevelResource;

public final class BiomeGuarantee {
    private static final int FORMAT = 2;
    private static final int[] SCALES = {1, 2, 3, 4, 6};
    private static final int[] CANDIDATES = {96, 48, 32, 32, 32};
    private static final int OFFSET_RANGE = 200_000;

    private BiomeGuarantee() {
    }

    public static Set<DensityFunction> climateFunctions(NoiseRouter router) {
        Set<DensityFunction> set = new HashSet<>();
        set.add(router.temperature());
        set.add(router.vegetation());
        return set;
    }

    public static ClimateWindow prepare(ServerLevel level, PlanetGeometry geometry, NoiseBasedChunkGenerator generator) {
        if (!(generator.getBiomeSource() instanceof MultiNoiseBiomeSource source)) return ClimateWindow.VANILLA;
        Path file = level.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve("sphereworld_climate_"
                + level.dimension().identifier().getNamespace() + "_" + level.dimension().identifier().getPath().replace('/', '_') + ".json");
        if (Files.isRegularFile(file)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (json.get("format").getAsInt() == FORMAT && json.get("circumference").getAsInt() == geometry.circumference()) {
                    return ClimateWindow.CODEC.parse(JsonOps.INSTANCE, json.get("window")).getOrThrow();
                }
            } catch (RuntimeException | IOException e) {
                SphereWorld.LOGGER.warn("Ignoring unreadable climate window file {}", file, e);
            }
        }
        long start = System.nanoTime();
        ClimateWindow window = choose(level, geometry, generator, source);
        try {
            JsonObject json = new JsonObject();
            json.addProperty("format", FORMAT);
            json.addProperty("circumference", geometry.circumference());
            json.add("window", ClimateWindow.CODEC.encodeStart(JsonOps.INSTANCE, window).getOrThrow());
            Files.createDirectories(file.getParent());
            Files.writeString(file, json.toString());
        } catch (IOException e) {
            SphereWorld.LOGGER.warn("Could not save climate window {}", file, e);
        }
        SphereWorld.LOGGER.info("Climate window for {}: offset {} {}, climate scale x{} (chosen in {} ms)",
                level.dimension().identifier(), window.offsetX(), window.offsetZ(), window.climateScale(),
                (System.nanoTime() - start) / 1_000_000);
        return window;
    }

    private static ClimateWindow choose(ServerLevel level, PlanetGeometry geometry, NoiseBasedChunkGenerator generator,
                                        MultiNoiseBiomeSource source) {
        Set<Holder<Biome>> possible = new LinkedHashSet<>();
        for (var entry : ((MultiNoiseBiomeSourceAccessor) source).sphereworld$parameters().values()) {
            possible.add(entry.getSecond());
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ level.dimension().identifier().hashCode());
        int[] ys = sampleHeights(level);
        ClimateWindow best = ClimateWindow.VANILLA;
        int bestCount = -1;
        Set<Holder<Biome>> bestMissing = possible;
        for (int s = 0; s < SCALES.length; s++) {
            int scale = SCALES[s];
            List<ClimateWindow> windows = new java.util.ArrayList<>();
            for (int candidate = 0; candidate < CANDIDATES[s]; candidate++) {
                windows.add(candidate == 0 && scale == 1 ? ClimateWindow.VANILLA
                        : new ClimateWindow(random.nextInt(2 * OFFSET_RANGE) - OFFSET_RANGE,
                        random.nextInt(2 * OFFSET_RANGE) - OFFSET_RANGE, scale));
            }

            List<Set<Holder<Biome>>> results = windows.parallelStream()
                    .map(window -> present(level, geometry, generator, source, window, ys))
                    .toList();
            for (int i = 0; i < windows.size(); i++) {
                Set<Holder<Biome>> present = results.get(i);
                int count = (int) possible.stream().filter(present::contains).count();
                if (count > bestCount) {
                    bestCount = count;
                    best = windows.get(i);
                    bestMissing = new HashSet<>(possible);
                    bestMissing.removeAll(present);
                }
                if (count == possible.size()) return windows.get(i);
            }
            SphereWorld.LOGGER.info("Climate scale x{}: best window still misses {}", scale,
                    bestMissing.stream().map(h -> h.unwrapKey().map(k -> k.identifier().getPath()).orElse("?")).toList());
        }
        SphereWorld.LOGGER.warn("No climate window holds every biome of {}; missing {}", level.dimension().identifier(),
                bestMissing.stream().map(h -> h.unwrapKey().map(k -> k.identifier().toString()).orElse("?")).toList());
        return best;
    }

    private static Set<Holder<Biome>> present(ServerLevel level, PlanetGeometry geometry, NoiseBasedChunkGenerator generator,
                                              MultiNoiseBiomeSource source, ClimateWindow window, int[] ys) {
        RandomState state = RandomState.create(level.registryAccess().lookupOrThrow(Registries.NOISE), level.getSeed(),
                generator.generatorSettings().value());
        ((PlanetRandomState) (Object) state).sphereworld$attachPlanet(geometry, window);
        Climate.Sampler sampler = state.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        int grid = Math.clamp(geometry.circumference() / 64, 24, 80);
        int step = geometry.circumference() / grid;
        Set<Holder<Biome>> present = new HashSet<>();
        for (int gz = 0; gz < grid; gz++) {
            for (int gx = 0; gx < grid; gx++) {
                int x = geometry.minBlock() + gx * step + step / 2;
                int z = geometry.minBlock() + gz * step + step / 2;
                for (int y : ys) {
                    present.add(source.getNoiseBiome(sampler.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z))));
                }
            }
        }
        return present;
    }

    private static int[] sampleHeights(ServerLevel level) {
        int min = level.getMinY() + 8;
        int max = Math.min(level.getMaxY(), level.getMinY() + level.dimensionType().logicalHeight()) - 8;
        int count = 8;
        int[] ys = new int[count];
        for (int i = 0; i < count; i++) ys[i] = min + (max - min) * i / (count - 1);
        return ys;
    }

    public static List<Holder<Biome>> listAll(MultiNoiseBiomeSource source) {
        return ((MultiNoiseBiomeSourceAccessor) source).sphereworld$parameters().values().stream().map(e -> e.getSecond()).distinct().toList();
    }
}
