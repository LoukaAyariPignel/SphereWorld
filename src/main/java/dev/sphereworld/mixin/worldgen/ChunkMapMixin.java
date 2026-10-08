package dev.sphereworld.mixin.worldgen;

import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.PlanetLevel;
import dev.sphereworld.planet.PlanetServer;
import dev.sphereworld.planet.Planets;
import dev.sphereworld.stack.StackTravel;
import dev.sphereworld.worldgen.PlanetRandomState;
import dev.sphereworld.wrap.PlanetTrackingView;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
abstract class ChunkMapMixin {
    @Shadow private RandomState randomState;
    @Shadow @Final private net.minecraft.world.level.chunk.ChunkGeneratorStructureState chunkGeneratorState;
    @Shadow @Final private ServerLevel level;

    @Shadow
    private int getPlayerViewDistance(ServerPlayer player) {
        throw new AssertionError();
    }

    @Shadow
    private void markChunkPendingToSend(ServerPlayer player, ChunkPos pos) {
        throw new AssertionError();
    }

    @Shadow private static void dropChunk(ServerPlayer player, ChunkPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;createState(Lnet/minecraft/core/HolderLookup;Lnet/minecraft/world/level/levelgen/RandomState;J)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;"))
    private void sphereworld$attachPlanet(CallbackInfo ci, @Local(argsOnly = true) ServerLevel level,
                                          @Local(argsOnly = true) net.minecraft.world.level.TicketStorage tickets,
                                          @Local(argsOnly = true) net.minecraft.world.level.chunk.ChunkGenerator generator) {
        PlanetGeometry geometry = PlanetServer.geometry(level.getServer(), level.dimension());
        PlanetRandomState state = (PlanetRandomState) (Object) randomState;
        if (geometry != null) {
            dev.sphereworld.worldgen.ClimateWindow window =
                    generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise
                            ? dev.sphereworld.worldgen.BiomeGuarantee.prepare(level, geometry, noise)
                            : dev.sphereworld.worldgen.ClimateWindow.VANILLA;
            state.sphereworld$attachPlanet(geometry, window);
            ((PlanetLevel) level).sphereworld$setGeometry(geometry);
        }
        state.sphereworld$setBoundaries(StackTravel.boundaries(level.getServer(), level.dimension()));
        PlanetWrap.CONSTRUCTING.set(geometry);
        ((dev.sphereworld.wrap.PlanetTickets) tickets).sphereworld$setGeometry(geometry);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sphereworld$constructed(CallbackInfo ci, @Local(argsOnly = true) net.minecraft.world.level.chunk.ChunkGenerator generator) {
        if (generator instanceof dev.sphereworld.worldgen.stacked.StackedChunkGenerator stacked) {
            stacked.initBands(level, randomState, chunkGeneratorState, Planets.of(level));
        }
        PlanetWrap.CONSTRUCTING.remove();
    }

    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void sphereworld$planetTracking(ServerPlayer player, CallbackInfo ci) {
        PlanetGeometry g = Planets.of(level);
        if (g == null) return;
        ci.cancel();
        ChunkPos center = player.chunkPosition();
        int viewDistance = Math.min(getPlayerViewDistance(player), g.halfChunks() - 2);
        ChunkTrackingView previous = player.getChunkTrackingView();
        if (previous instanceof PlanetTrackingView view && view.center().equals(center) && view.viewDistance() == viewDistance) {
            return;
        }
        if (player.level() != level) return;
        PlanetTrackingView next = new PlanetTrackingView(center, viewDistance, g);
        if (!(previous instanceof PlanetTrackingView view && view.center().equals(center))) {
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(center.x(), center.z()));
        }
        PlanetTrackingView.difference(previous, next, pos -> markChunkPendingToSend(player, pos), pos -> dropChunk(player, pos));
        player.setChunkTrackingView(next);
    }

    @Inject(method = "scheduleGenerationTask", at = @At("HEAD"))
    private void sphereworld$periodicNeighbourhood(CallbackInfoReturnable<?> cir) {
        PlanetGeometry g = Planets.of(level);
        dev.sphereworld.wrap.PeriodicCache.CONSTRUCTING.set(g == null ? 0 : g.chunks());
    }

    @Inject(method = "scheduleGenerationTask", at = @At("RETURN"))
    private void sphereworld$periodicNeighbourhoodDone(CallbackInfoReturnable<?> cir) {
        dev.sphereworld.wrap.PeriodicCache.CONSTRUCTING.set(0);
    }

    @ModifyVariable(method = {"getUpdatingChunkIfPresent", "getVisibleChunkIfPresent", "acquireGeneration"},
            at = @At("HEAD"), argsOnly = true)
    private long sphereworld$canonicalHolder(long key) {
        PlanetGeometry g = Planets.of(level);
        if (g == null) return key;
        int x = ChunkPos.getX(key);
        int z = ChunkPos.getZ(key);
        if (g.isCanonicalChunk(x) && g.isCanonicalChunk(z)) return key;
        return ChunkPos.pack(g.canonicalChunk(x), g.canonicalChunk(z));
    }

    @ModifyVariable(method = "isChunkTracked", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sphereworld$trackedX(int x) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? x : g.canonicalChunk(x);
    }

    @ModifyVariable(method = "isChunkTracked", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int sphereworld$trackedZ(int z) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? z : g.canonicalChunk(z);
    }
}
