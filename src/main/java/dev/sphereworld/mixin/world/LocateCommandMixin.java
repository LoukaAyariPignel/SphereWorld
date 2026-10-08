package dev.sphereworld.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LocateCommand.class)
abstract class LocateCommandMixin {
    @ModifyVariable(method = "showLocateResult(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/core/BlockPos;Lcom/mojang/datafixers/util/Pair;Ljava/lang/String;ZLjava/lang/String;Ljava/time/Duration;)I",
            at = @At("STORE"), ordinal = 1)
    private static BlockPos sphereworld$nearestResult(BlockPos found, @Local(argsOnly = true) CommandSourceStack source,
                                                      @Local(argsOnly = true) BlockPos sourcePos) {
        PlanetGeometry g = Planets.of(source.getLevel());
        return g == null ? found : PlanetWrap.nearestImage(g, found, Vec3.atCenterOf(sourcePos));
    }
}
