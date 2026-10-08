package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndPortalBlock.class)
public abstract class StackedCreditsMixin {
    @ModifyExpressionValue(method = "entityInside", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Level> sphereworld$endLayer(ResourceKey<Level> dimension, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) BlockPos pos) {
        return StackedAmbience.bandAt(level, Vec3.atCenterOf(pos)) == StackBand.END ? Level.END : dimension;
    }
}
