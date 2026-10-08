package dev.sphereworld.mixin.level;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.PlanetLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
abstract class LevelMixin implements PlanetLevel {
    @Unique private volatile @Nullable PlanetGeometry sphereworld$geometry;

    @Override
    public @Nullable PlanetGeometry sphereworld$geometry() {
        return sphereworld$geometry;
    }

    @Override
    public void sphereworld$setGeometry(@Nullable PlanetGeometry geometry) {
        sphereworld$geometry = geometry;
    }
}
