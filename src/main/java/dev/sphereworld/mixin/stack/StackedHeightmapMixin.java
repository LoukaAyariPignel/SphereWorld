package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import dev.sphereworld.worldgen.stacked.StackedHeightmaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
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

        @ModifyReturnValue(method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I", at = @At("RETURN"))
        private int sphereworld$overworldSurface(int original, @Local(argsOnly = true) Heightmap.Types type,
                                                 @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int z) {
            if (original <= StackBand.OVERWORLD.worldMaxY() + 1 || !this.sphereworld$isStacked()) return original;
            LevelChunk chunk = ((Level) (Object) this).getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
            return ((StackedHeightmaps.Chunk) chunk).sphereworld$overworldHeight(type, x & 15, z & 15) + 1;
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
        @Unique
        private static final int SPHEREWORLD$UNKNOWN = Integer.MIN_VALUE;

        @Unique
        private static final int SPHEREWORLD$TYPES = Heightmap.Types.values().length;

        @Unique
        private volatile int @Nullable [] sphereworld$overworld;

        @Override
        public int sphereworld$overworldHeight(Heightmap.Types type, int localX, int localZ) {
            int[] heights = this.sphereworld$overworld;
            if (heights == null) {
                heights = new int[SPHEREWORLD$TYPES << 8];
                java.util.Arrays.fill(heights, SPHEREWORLD$UNKNOWN);
                this.sphereworld$overworld = heights;
            }
            int x = localX & 15;
            int z = localZ & 15;
            int index = type.ordinal() << 8 | z << 4 | x;
            int height = heights[index];
            if (height == SPHEREWORLD$UNKNOWN) {
                height = StackedHeightmaps.overworldHeight((LevelChunk) (Object) this, type, x, z);
                heights[index] = height;
            }
            return height;
        }

        @Inject(method = "setBlockState", at = @At("RETURN"))
        private void sphereworld$updateOverworld(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
            int[] heights = this.sphereworld$overworld;
            if (heights == null || cir.getReturnValue() == null || !StackBand.OVERWORLD.containsWorldY(pos.getY())) return;
            int column = (pos.getZ() & 15) << 4 | (pos.getX() & 15);
            for (int type = 0; type < SPHEREWORLD$TYPES; type++) heights[type << 8 | column] = SPHEREWORLD$UNKNOWN;
        }

        @Inject(method = "replaceWithPacketData", at = @At("TAIL"))
        private void sphereworld$reloaded(CallbackInfo ci) {
            this.sphereworld$overworld = null;
        }
    }
}
