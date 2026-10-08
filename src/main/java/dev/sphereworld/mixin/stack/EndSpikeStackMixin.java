package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndSpikeFeature.class)
public abstract class EndSpikeStackMixin {
    @Inject(method = "getSpikesForLevel", at = @At("RETURN"), cancellable = true)
    private static void sphereworld$endLayerSpikes(WorldGenLevel level, CallbackInfoReturnable<List<EndSpikeFeature.EndSpike>> cir) {
        if (!(level instanceof Level real) || !StackedWorld.is(real)) return;
        int offset = StackBand.END.offset();
        cir.setReturnValue(cir.getReturnValue().stream()
                .map(spike -> new EndSpikeFeature.EndSpike(spike.getCenterX(), spike.getCenterZ(), spike.getRadius(), spike.getHeight() + offset, spike.isGuarded()))
                .toList());
    }
}
