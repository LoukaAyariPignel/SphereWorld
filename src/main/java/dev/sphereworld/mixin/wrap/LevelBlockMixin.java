package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
abstract class LevelBlockMixin {
    @ModifyVariable(method = {
            "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            "getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            "removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"},
            at = @At("HEAD"), argsOnly = true)
    private BlockPos sphereworld$canonicalPos(BlockPos pos) {
        PlanetGeometry g = PlanetWrap.server((Level) (Object) this);
        return g == null ? pos : PlanetWrap.canonical(g, pos);
    }
}
