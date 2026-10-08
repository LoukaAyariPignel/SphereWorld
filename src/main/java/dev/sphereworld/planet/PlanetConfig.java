package dev.sphereworld.planet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record PlanetConfig(Map<ResourceKey<Level>, Integer> planets, List<ResourceKey<Level>> stack,
                           boolean openBoundaries, boolean cavesToNether) {
    private static final Codec<ResourceKey<Level>> DIMENSION = ResourceKey.codec(Registries.DIMENSION);
    private static final Codec<Integer> CIRCUMFERENCE = Codec.INT.validate(value ->
            value >= PlanetGeometry.MIN_CIRCUMFERENCE && value <= PlanetGeometry.MAX_CIRCUMFERENCE && value % 32 == 0
                    ? DataResult.success(value)
                    : DataResult.error(() -> "Planet circumference must be a multiple of 32 in ["
                    + PlanetGeometry.MIN_CIRCUMFERENCE + ", " + PlanetGeometry.MAX_CIRCUMFERENCE + "]: " + value));

    public static final Codec<PlanetConfig> CODEC = RecordCodecBuilder.<PlanetConfig>create(i -> i.group(
                    Codec.unboundedMap(DIMENSION, CIRCUMFERENCE).fieldOf("planets").forGetter(PlanetConfig::planets),
                    DIMENSION.listOf().optionalFieldOf("stack", List.of()).forGetter(PlanetConfig::stack),
                    Codec.BOOL.optionalFieldOf("open_boundaries", true).forGetter(PlanetConfig::openBoundaries),
                    Codec.BOOL.optionalFieldOf("caves_to_nether", false).forGetter(PlanetConfig::cavesToNether))
            .apply(i, PlanetConfig::new))
            .validate(config -> config.stack().stream().distinct().count() == config.stack().size()
                    ? DataResult.success(config)
                    : DataResult.error(() -> "A dimension appears twice in the planet stack"));

    public PlanetConfig {
        planets = Map.copyOf(new LinkedHashMap<>(planets));
        stack = List.copyOf(stack);
    }

    public static PlanetConfig defaults() {
        Map<ResourceKey<Level>, Integer> planets = new LinkedHashMap<>();
        planets.put(Level.OVERWORLD, 4096);
        planets.put(Level.NETHER, PlanetLayout.netherCircumference(4096));
        planets.put(Level.END, PlanetLayout.endCircumference(4096));
        return new PlanetConfig(planets, List.of(Level.NETHER, Level.OVERWORLD, Level.END), true, false);
    }

    public @Nullable Integer circumference(ResourceKey<Level> dimension) {
        return planets.get(dimension);
    }

    public int layer(ResourceKey<Level> dimension) {
        return stack.indexOf(dimension);
    }

    public @Nullable ResourceKey<Level> below(ResourceKey<Level> dimension) {
        int layer = layer(dimension);
        return layer > 0 ? stack.get(layer - 1) : null;
    }

    public @Nullable ResourceKey<Level> above(ResourceKey<Level> dimension) {
        int layer = layer(dimension);
        return layer >= 0 && layer < stack.size() - 1 ? stack.get(layer + 1) : null;
    }
}
