package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingApproachPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DragonLandingApproachPhase.class)
public abstract class DragonLandingApproachStackMixin {
    @ModifyExpressionValue(method = "findNewTarget", at = @At(value = "CONSTANT", args = "doubleValue=105.0"))
    private double sphereworld$approachHeight(double y, @Local(argsOnly = true) ServerLevel level) {
        return StackedWorld.is(level) ? y + StackBand.END.offset() : y;
    }
}
