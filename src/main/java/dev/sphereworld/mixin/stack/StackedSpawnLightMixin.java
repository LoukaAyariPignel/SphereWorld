package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Monster.class)
public abstract class StackedSpawnLightMixin {
    @ModifyExpressionValue(method = "isDarkEnoughToSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;dimensionType()Lnet/minecraft/world/level/dimension/DimensionType;"))
    private static DimensionType sphereworld$layerRules(DimensionType type, @Local(argsOnly = true) ServerLevelAccessor level,
            @Local(argsOnly = true) BlockPos pos) {
        StackBand band = StackedAmbience.bandAt(level.getLevel(), Vec3.atCenterOf(pos));
        return band == null || band == StackBand.OVERWORLD ? type : StackedAmbience.bandType(level.getLevel(), band);
    }
}
