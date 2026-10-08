package dev.sphereworld.atlas;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

public final class AtlasBuildProgress {
    public static final List<String> PHASES = List.of("continents", "erosion", "ridges", "climate", "biomes", "relief", "surface");

    public static final int RELIEF = PHASES.indexOf("relief");

    public record Live(PlanetAtlas atlas, int[] display, byte[] stageOf, AtomicInteger phase, AtomicInteger done) {
        public float progress() {
            return done.get() / (float) display.length;
        }

        public float overall() {
            return (phase.get() + progress()) / PHASES.size();
        }
    }

    private static volatile @Nullable Live current;

    private AtlasBuildProgress() {
    }

    public static @Nullable Live current() {
        return current;
    }

    static void publish(Live live) {
        current = live;
    }

    static void finish(Live live) {
        if (current == live) current = null;
    }
}
