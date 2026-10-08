package dev.sphereworld.mixin.wrap;

import dev.sphereworld.wrap.PlanetLight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLightEngine.class)
abstract class BlockLightEngineMixin {
    @Redirect(method = {"propagateIncrease", "propagateDecrease"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;offset(JLnet/minecraft/core/Direction;)J"))
    private long sphereworld$wrapNeighbour(long node, Direction direction) {
        return PlanetLight.wrap(((PlanetLight) this).sphereworld$geometry(), BlockPos.offset(node, direction));
    }
}
