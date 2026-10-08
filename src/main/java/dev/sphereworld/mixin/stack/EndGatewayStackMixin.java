package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TheEndGatewayBlockEntity.class)
public abstract class EndGatewayStackMixin {
    @ModifyExpressionValue(method = "getPortalPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Level> sphereworld$endLayerIsTheEnd(ResourceKey<Level> dimension, @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) BlockPos entry) {
        return StackedWorld.is(level) && StackBand.END.containsWorldY(entry.getY()) ? Level.END : dimension;
    }

    @Inject(method = "isChunkEmpty", at = @At("HEAD"), cancellable = true)
    private static void sphereworld$endLayerEmpty(ServerLevel level, Vec3 xz, CallbackInfoReturnable<Boolean> cir) {
        if (!StackedWorld.is(level)) return;
        LevelChunk chunk = level.getChunk(Mth.floor(xz.x / 16.0), Mth.floor(xz.z / 16.0));
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = level.getSectionIndex(StackBand.END.worldMinY()); i < sections.length; i++) {
            if (!sections[i].hasOnlyAir()) {
                cir.setReturnValue(false);
                return;
            }
        }
        cir.setReturnValue(true);
    }

    @ModifyExpressionValue(method = "findValidSpawnInChunk", at = @At(value = "CONSTANT", args = "intValue=30"))
    private static int sphereworld$spawnSearchFloor(int y, @Local(argsOnly = true) LevelChunk chunk) {
        return StackedWorld.endY(chunk.getLevel(), y);
    }

    @ModifyExpressionValue(method = "findOrCreateValidTeleportPos", at = @At(value = "CONSTANT", args = "doubleValue=75.0"))
    private static double sphereworld$newIslandHeight(double y, @Local(argsOnly = true) ServerLevel level) {
        return StackedWorld.endY(level, (int) y);
    }
}
