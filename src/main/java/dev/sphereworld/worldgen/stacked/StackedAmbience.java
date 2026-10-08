package dev.sphereworld.worldgen.stacked;

import dev.sphereworld.mixin.stack.EnvironmentAttributeSystemAccessor;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public final class StackedAmbience {
    private static final Set<EnvironmentAttribute<?>> NETHER_KEEPS_SKY = Set.of(
            EnvironmentAttributes.SKY_COLOR,
            EnvironmentAttributes.SUNRISE_SUNSET_COLOR,
            EnvironmentAttributes.CLOUD_COLOR,
            EnvironmentAttributes.CLOUD_HEIGHT,
            EnvironmentAttributes.SUN_ANGLE,
            EnvironmentAttributes.MOON_ANGLE,
            EnvironmentAttributes.STAR_ANGLE,
            EnvironmentAttributes.MOON_PHASE,
            EnvironmentAttributes.STAR_BRIGHTNESS,
            EnvironmentAttributes.SKY_LIGHT_FACTOR,
            EnvironmentAttributes.SKY_FOG_END_DISTANCE,
            EnvironmentAttributes.CLOUD_FOG_END_DISTANCE);

    public interface Holder {
        void sphereworld$setBandSystems(List<EnvironmentAttributeSystem> systems);
    }

    private StackedAmbience() {
    }

    public static boolean isStackedType(Level level) {
        return level.dimensionTypeRegistration().unwrapKey().map(key -> key.identifier().equals(StackedWorld.DIMENSION_TYPE)).orElse(false);
    }

    public static StackBand bandAt(Level level, net.minecraft.world.phys.Vec3 pos) {
        return isStackedType(level) ? StackBand.at(Mth.floor(pos.y)) : null;
    }

    public static List<EnvironmentAttributeSystem> addLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        var types = level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
        EnvironmentAttributeSystem nether = bandSystem(types.getValueOrThrow(BuiltinDimensionTypes.NETHER), level);
        EnvironmentAttributeSystem end = bandSystem(types.getValueOrThrow(BuiltinDimensionTypes.END), level);
        for (EnvironmentAttribute<?> attribute : BuiltInRegistries.ENVIRONMENT_ATTRIBUTE) {
            addLayer(builder, attribute, nether, end);
        }
        return List.of(nether, end);
    }

    private static <V> void addLayer(EnvironmentAttributeSystem.Builder builder, EnvironmentAttribute<V> attribute,
            EnvironmentAttributeSystem nether, EnvironmentAttributeSystem end) {
        boolean netherKeepsSky = NETHER_KEEPS_SKY.contains(attribute);
        builder.addPositionalLayer(attribute, (value, pos, interpolator) -> {
            StackBand band = StackBand.at(Mth.floor(pos.y));
            if (band == StackBand.END) return end.getValue(attribute, pos, interpolator);
            if (band == StackBand.NETHER && !netherKeepsSky) return nether.getValue(attribute, pos, interpolator);
            return value;
        });
    }

    private static EnvironmentAttributeSystem bandSystem(DimensionType type, Level level) {
        EnvironmentAttributeSystem.Builder builder = EnvironmentAttributeSystem.builder();
        builder.addConstantLayer(type.attributes());
        EnvironmentAttributeSystemAccessor.sphereworld$addBiomeLayer(builder, level.registryAccess().lookupOrThrow(Registries.BIOME), level.getBiomeManager());
        type.timelines().forEach(timeline -> builder.addTimelineLayer(timeline, level.clockManager()));
        return builder.build();
    }

    public static DimensionType bandType(Level level, StackBand band) {
        var types = level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
        if (band == StackBand.NETHER) return types.getValueOrThrow(BuiltinDimensionTypes.NETHER);
        if (band == StackBand.END) return types.getValueOrThrow(BuiltinDimensionTypes.END);
        return level.dimensionType();
    }
}
