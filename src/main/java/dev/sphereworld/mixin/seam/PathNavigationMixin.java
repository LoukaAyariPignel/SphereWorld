package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PathNavigation.class)
abstract class PathNavigationMixin {
    @Shadow @Final protected Mob mob;

    @ModifyVariable(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At("HEAD"), argsOnly = true)
    private Set<BlockPos> sphereworld$nearestTargets(Set<BlockPos> targets) {
        PlanetGeometry g = PlanetWrap.server(mob.level());
        if (g == null) return targets;
        Set<BlockPos> mapped = new LinkedHashSet<>(targets.size());
        for (BlockPos target : targets) mapped.add(PlanetWrap.nearestImage(g, target, mob.position()));
        return mapped;
    }
}
