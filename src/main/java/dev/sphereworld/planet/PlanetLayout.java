package dev.sphereworld.planet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class PlanetLayout {
    public static final List<Integer> OVERWORLD_SIZES = List.of(2048, 4096, 8192, 16384, 32768, 65536);
    public static final int OVERWORLD_MIN_Y = -64;
    public static final int OVERWORLD_SEA = 63;
    public static final int OVERWORLD_TOP = 320;
    public static final int NETHER_SEA = 32;
    public static final int NETHER_ROOF_TOP = 128;
    public static final int END_REFERENCE_Y = 0;

    private PlanetLayout() {
    }

    public static double radius(int circumference) {
        return circumference / (2.0 * Math.PI);
    }

    private static int roundedCircumference(double radius) {
        return Math.max(PlanetGeometry.MIN_CIRCUMFERENCE, (int) Math.round(2 * Math.PI * radius / 32.0) * 32);
    }

    public static int netherCircumference(int overworld) {
        double roofRadius = radius(overworld) + (OVERWORLD_MIN_Y - OVERWORLD_SEA);
        return roundedCircumference(roofRadius - (NETHER_ROOF_TOP - NETHER_SEA));
    }

    public static int coreRadius(int overworld) {
        return (int) Math.round(radius(netherCircumference(overworld)) - NETHER_SEA);
    }

    public static boolean netherFits(int overworld) {
        return radius(overworld) + (OVERWORLD_MIN_Y - OVERWORLD_SEA) - (NETHER_ROOF_TOP - NETHER_SEA) - NETHER_SEA >= 16;
    }

    public static int endCircumference(int overworld) {
        return roundedCircumference(radius(overworld) + (OVERWORLD_TOP - OVERWORLD_SEA) - END_REFERENCE_Y);
    }

    public static PlanetConfig derive(PlanetConfig config) {
        Integer overworld = config.circumference(Level.OVERWORLD);
        if (overworld == null) return config;
        Map<ResourceKey<Level>, Integer> planets = new LinkedHashMap<>(config.planets());
        List<ResourceKey<Level>> stack = new ArrayList<>(config.stack());
        if (Level.NETHER.equals(config.below(Level.OVERWORLD)) && netherFits(overworld)) {
            planets.put(Level.NETHER, netherCircumference(overworld));
        }
        if (Level.END.equals(config.above(Level.OVERWORLD))) {
            planets.put(Level.END, endCircumference(overworld));
        }
        return new PlanetConfig(planets, stack, config.openBoundaries());
    }

    public static boolean isDerived(PlanetConfig config, ResourceKey<Level> dimension) {
        Integer overworld = config.circumference(Level.OVERWORLD);
        if (overworld == null) return false;
        return Level.NETHER.equals(dimension) && Level.NETHER.equals(config.below(Level.OVERWORLD)) && netherFits(overworld)
                || Level.END.equals(dimension) && Level.END.equals(config.above(Level.OVERWORLD));
    }
}
