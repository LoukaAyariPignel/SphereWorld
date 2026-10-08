package dev.sphereworld.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public record PeriodicPositionalRandom(PositionalRandomFactory delegate, int period)
        implements PositionalRandomFactory {
    @Override
    public RandomSource at(int x, int y, int z) {
        return delegate.at(Math.floorMod(x, period), y, Math.floorMod(z, period));
    }

    @Override
    public RandomSource fromHashOf(String name) {
        return delegate.fromHashOf(name);
    }

    @Override
    public RandomSource fromSeed(long seed) {
        return delegate.fromSeed(seed);
    }

    @Override
    public void parityConfigString(StringBuilder sb) {
        delegate.parityConfigString(sb);
    }
}
