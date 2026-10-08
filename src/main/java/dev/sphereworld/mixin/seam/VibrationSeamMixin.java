package dev.sphereworld.mixin.seam;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VibrationSystem.Listener.class)
abstract class VibrationSeamMixin {
    @WrapOperation(method = "handleGameEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$Listener;isOccluded(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Z"))
    private static boolean sphereworld$occlusionNearby(Level level, Vec3 origin, Vec3 dest, Operation<Boolean> original) {
        return original.call(level, sphereworld$near(level, origin, dest), dest);
    }

    @ModifyVariable(method = "scheduleVibration", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 sphereworld$originNearby(Vec3 origin, @Local(argsOnly = true) ServerLevel level,
                                          @Local(argsOnly = true, ordinal = 1) Vec3 dest) {
        return sphereworld$near(level, origin, dest);
    }

    private static Vec3 sphereworld$near(Level level, Vec3 from, Vec3 to) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return from;
        return new Vec3(to.x + g.canonical(from.x - to.x), from.y, to.z + g.canonical(from.z - to.z));
    }
}
