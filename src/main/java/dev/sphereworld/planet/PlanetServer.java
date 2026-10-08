package dev.sphereworld.planet;

import dev.sphereworld.worldgen.PlanetChunkGenerator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.jspecify.annotations.Nullable;

public final class PlanetServer {
    private static final Map<MinecraftServer, Optional<PlanetConfig>> CACHE = new WeakHashMap<>();

    private PlanetServer() {
    }

    public static synchronized @Nullable PlanetConfig config(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, PlanetServer::read).orElse(null);
    }

    private static Optional<PlanetConfig> read(MinecraftServer server) {
        return server.registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM)
                .getOptional(LevelStem.OVERWORLD)
                .map(LevelStem::generator)
                .filter(PlanetChunkGenerator.class::isInstance)
                .map(generator -> ((PlanetChunkGenerator) generator).planetConfig());
    }

    public static @Nullable PlanetGeometry geometry(MinecraftServer server, ResourceKey<Level> dimension) {
        PlanetConfig config = config(server);
        if (config == null) return null;
        Integer circumference = config.circumference(dimension);
        if (circumference == null) return null;
        LevelStem stem = server.registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM)
                .getValue(ResourceKey.create(Registries.LEVEL_STEM, dimension.identifier()));
        int surfaceY = stem != null && stem.generator() instanceof NoiseBasedChunkGenerator noise
                ? noise.generatorSettings().value().seaLevel() : 64;
        return new PlanetGeometry(circumference, surfaceY);
    }
}
