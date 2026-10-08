package dev.sphereworld.mixin.world;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.worldgen.PlanetRandomState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGeneratorStructureState.class)
abstract class StrongholdRingsMixin {
    @Shadow @Final private RandomState randomState;

    @Inject(method = "generateRingPositions", at = @At("RETURN"), cancellable = true)
    private void sphereworld$keepOnPlanet(CallbackInfoReturnable<CompletableFuture<List<ChunkPos>>> cir) {
        PlanetGeometry g = ((PlanetRandomState) (Object) randomState).sphereworld$planet();
        if (g == null) return;
        cir.setReturnValue(cir.getReturnValue().thenApply(positions -> {
            List<ChunkPos> kept = new ArrayList<>();
            for (ChunkPos pos : positions) {
                if (g.isCanonicalChunk(pos.x()) && g.isCanonicalChunk(pos.z())) kept.add(pos);
            }
            if (kept.isEmpty() && !positions.isEmpty()) {
                ChunkPos first = positions.getFirst();
                kept.add(new ChunkPos(g.canonicalChunk(first.x()), g.canonicalChunk(first.z())));
            }
            return kept;
        }));
    }
}
