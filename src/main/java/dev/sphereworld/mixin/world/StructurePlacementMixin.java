package dev.sphereworld.mixin.world;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.worldgen.PlanetRandomState;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractSpreadingStructurePlacement.class)
abstract class StructurePlacementMixin {
    @ModifyVariable(method = "isStructureChunk", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sphereworld$canonicalX(int x, ChunkGeneratorStructureState state) {
        PlanetGeometry g = ((PlanetRandomState) (Object) state.randomState()).sphereworld$planet();
        return g == null ? x : g.canonicalChunk(x);
    }

    @ModifyVariable(method = "isStructureChunk", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int sphereworld$canonicalZ(int z, ChunkGeneratorStructureState state) {
        PlanetGeometry g = ((PlanetRandomState) (Object) state.randomState()).sphereworld$planet();
        return g == null ? z : g.canonicalChunk(z);
    }
}
