package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetTicks;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelTicks.class)
abstract class LevelTicksMixin<T> implements PlanetTicks {
    @Unique private @Nullable PlanetGeometry sphereworld$geometry;

    @Override
    public void sphereworld$setGeometry(@Nullable PlanetGeometry geometry) {
        sphereworld$geometry = geometry;
    }

    @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    private ScheduledTick<T> sphereworld$canonicalTick(ScheduledTick<T> tick) {
        PlanetGeometry g = sphereworld$geometry;
        if (g == null) return tick;
        var pos = PlanetWrap.canonical(g, tick.pos());
        return pos == tick.pos() ? tick : new ScheduledTick<>(tick.type(), pos, tick.triggerTick(), tick.priority(), tick.subTickOrder());
    }
}
