package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import dev.sphereworld.worldgen.stacked.StackedHeightmaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class StackedHeightmapMixin {
    private StackedHeightmapMixin() {
    }

    @Mixin(Level.class)
    public abstract static class LevelMixin {
        @Unique
        private @Nullable Boolean sphereworld$stacked;

        @Unique
        private boolean sphereworld$isStacked() {
            Boolean stacked = this.sphereworld$stacked;
            if (stacked == null) this.sphereworld$stacked = stacked = StackedAmbience.isStackedType((Level) (Object) this);
            return stacked;
        }

        @Inject(method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I", at = @At("RETURN"), cancellable = true)
        private void sphereworld$overworldSurface(Heightmap.Types type, int x, int z, CallbackInfoReturnable<Integer> cir) {
            if (cir.getReturnValueI() <= StackBand.OVERWORLD.worldMaxY() + 1 || !this.sphereworld$isStacked()) return;
            LevelChunk chunk = ((Level) (Object) this).getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
            cir.setReturnValue(((StackedHeightmaps.Chunk) chunk).sphereworld$overworldHeight(type, x & 15, z & 15) + 1);
        }

        public BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) {
            Level self = (Level) (Object) this;
            int x = pos.getX();
            int z = pos.getZ();
            if (pos.getY() > StackBand.OVERWORLD.worldMaxY() && this.sphereworld$isStacked()) {
                int cx = SectionPos.blockToSectionCoord(x);
                int cz = SectionPos.blockToSectionCoord(z);
                int y = self.hasChunk(cx, cz) ? self.getChunk(cx, cz).getHeight(type, x & 15, z & 15) + 1 : StackBand.END.worldMinY();
                return new BlockPos(x, y, z);
            }
            return new BlockPos(x, self.getHeight(type, x, z), z);
        }
    }

    @Mixin(LevelChunk.class)
    public abstract static class ChunkMixin implements StackedHeightmaps.Chunk {
        @Shadow
        @Final
        Level level;

        @Unique
        private volatile @Nullable ProtoChunk sphereworld$overworld;

        @Override
        public int sphereworld$overworldHeight(Heightmap.Types type, int localX, int localZ) {
            ProtoChunk view = this.sphereworld$overworld;
            if (view == null) {
                this.sphereworld$overworld = view = StackedHeightmaps.overworldView((LevelChunk) (Object) this, this.level.palettedContainerFactory());
            }
            return view.getHeight(type, localX, localZ);
        }

        @Inject(method = "setBlockState", at = @At("RETURN"))
        private void sphereworld$updateOverworld(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
            ProtoChunk view = this.sphereworld$overworld;
            if (view == null || cir.getReturnValue() == null || !StackBand.OVERWORLD.containsWorldY(pos.getY())) return;
            for (Heightmap.Types type : StackedHeightmaps.types()) {
                view.getOrCreateHeightmapUnprimed(type).update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
            }
        }

        @Inject(method = "replaceWithPacketData", at = @At("TAIL"))
        private void sphereworld$reloaded(CallbackInfo ci) {
            this.sphereworld$overworld = null;
        }
    }
}
