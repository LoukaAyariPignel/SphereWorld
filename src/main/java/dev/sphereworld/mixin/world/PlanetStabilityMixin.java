package dev.sphereworld.mixin.world;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Lifecycle;
import dev.sphereworld.worldgen.PlanetChunkGenerator;
import dev.sphereworld.worldgen.stacked.StackedChunkGenerator;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldDimensions.class)
public abstract class PlanetStabilityMixin {
    @Shadow
    @Final
    private java.util.Map<ResourceKey<LevelStem>, LevelStem> dimensions;

    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void sphereworld$planetIsStable(ResourceKey<LevelStem> key, LevelStem dimension, CallbackInfoReturnable<Lifecycle> cir) {
        if (key == LevelStem.OVERWORLD && dimension.generator() instanceof PlanetChunkGenerator) cir.setReturnValue(Lifecycle.stable());
    }

    @ModifyExpressionValue(method = "bake", at = @At(value = "INVOKE", target = "Ljava/util/Set;containsAll(Ljava/util/Collection;)Z"))
    private boolean sphereworld$stackedHasEveryDimension(boolean all) {
        LevelStem overworld = this.dimensions.get(LevelStem.OVERWORLD);
        return all || overworld != null && overworld.generator() instanceof StackedChunkGenerator;
    }
}
