package dev.sphereworld.worldgen.stacked;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.blockscan.BlockMatcher;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import org.jspecify.annotations.Nullable;

final class RegionBandLevel implements WorldGenLevel, BandAccess {
    private static final Heightmap.Types[] HEIGHTMAPS = EnumSet.allOf(Heightmap.Types.class).toArray(Heightmap.Types[]::new);
    private static final int RADIUS = 1;
    private static final int SPAN = RADIUS * 2 + 1;

    private final WorldGenRegion real;
    private final StackedChunkGenerator generator;
    private final StackBand band;
    private final int offset;
    private final Long2ObjectOpenHashMap<ProtoChunk> views = new Long2ObjectOpenHashMap<>();
    private final ChunkAccess[] chunks = new ChunkAccess[SPAN * SPAN];
    private final int centerX;
    private final int centerZ;
    private @Nullable BiomeManager biomeManager;

    RegionBandLevel(WorldGenRegion real, StackedChunkGenerator generator, StackBand band) {
        this.real = real;
        this.generator = generator;
        this.band = band;
        this.offset = band.offset();
        ChunkPos center = real.getCenter();
        this.centerX = center.x();
        this.centerZ = center.z();
    }

    @Override
    public WorldGenLevel level() {
        return this;
    }

    @Override
    public ProtoChunk view(ChunkAccess chunk) {
        ProtoChunk shared = generator.sharedView(chunk, band);
        return shared != null ? shared : ownView(chunk);
    }

    private synchronized ProtoChunk ownView(ChunkAccess chunk) {
        long key = chunk.getPos().pack();
        ProtoChunk view = views.get(key);
        if (view == null) views.put(key, view = generator.view(chunk, band));
        return view;
    }

    private ChunkAccess chunkAt(int chunkX, int chunkZ) {
        int dx = chunkX - centerX + RADIUS;
        int dz = chunkZ - centerZ + RADIUS;
        if (dx < 0 || dx >= SPAN || dz < 0 || dz >= SPAN) return real.getChunk(chunkX, chunkZ);
        int index = dz * SPAN + dx;
        ChunkAccess chunk = chunks[index];
        if (chunk == null) chunks[index] = chunk = real.getChunk(chunkX, chunkZ);
        return chunk;
    }

    private ProtoChunk viewAt(int x, int z) {
        return view(chunkAt(x >> 4, z >> 4));
    }

    private BlockPos world(BlockPos pos) {
        return offset == 0 ? pos : band.toWorld(pos);
    }

    private Vec3 world(Vec3 pos) {
        return offset == 0 ? pos : pos.add(0, offset, 0);
    }

    private AABB world(AABB box) {
        return offset == 0 ? box : box.move(0, offset, 0);
    }

    @Override
    public long getSeed() {
        return real.getSeed();
    }

    @Override
    public ServerLevel getLevel() {
        return real.getLevel();
    }

    @Override
    public ChunkSource getChunkSource() {
        return real.getChunkSource();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        return real.getCurrentDifficultyAt(world(pos));
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        return real.getServer();
    }

    @Override
    public RandomSource getRandom() {
        return real.getRandom();
    }

    @Override
    public void playSound(@Nullable Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        real.playSound(except, world(pos), sound, source, volume, pitch);
    }

    @Override
    public void addParticle(ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd) {
        real.addParticle(particle, x, y, z, xd, yd, zd);
    }

    @Override
    public void levelEvent(@Nullable Entity source, int type, BlockPos pos, int data) {
        real.levelEvent(source, type, world(pos), data);
    }

    @Override
    public void gameEvent(Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context) {
        real.gameEvent(gameEvent, world(position), context);
    }

    @Override
    public long nextSubTickCount() {
        return real.nextSubTickCount();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return ticks(real.getBlockTicks());
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return ticks(real.getFluidTicks());
    }

    @Override
    public LevelData getLevelData() {
        return real.getLevelData();
    }

    @Override
    public RegistryAccess registryAccess() {
        return real.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return real.enabledFeatures();
    }

    @Override
    public DimensionType dimensionType() {
        return real.dimensionType();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return real.environmentAttributes();
    }

    @Override
    public int getSeaLevel() {
        return real.getSeaLevel();
    }

    @Override
    public int getSkyDarken() {
        return real.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        BiomeManager manager = biomeManager;
        if (manager == null) biomeManager = manager = real.getBiomeManager().withDifferentSource((qx, qy, qz) -> noiseBiome(qx, qy, qz));
        return manager;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        return noiseBiome(quartX, quartY, quartZ);
    }

    private Holder<Biome> noiseBiome(int quartX, int quartY, int quartZ) {
        int y = band.worldQuart(quartY);
        ChunkAccess chunk = real.getChunk(QuartPos.toSection(quartX), QuartPos.toSection(quartZ), ChunkStatus.BIOMES, false);
        return chunk != null ? chunk.getNoiseBiome(quartX, y, quartZ) : real.getUncachedNoiseBiome(quartX, y, quartZ);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
        return real.getUncachedNoiseBiome(quartX, band.worldQuart(quartY), quartZ);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return real.getLightEngine();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return real.getBlockEntity(world(pos));
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return chunkAt(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(world(pos));
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return chunkAt(pos.getX() >> 4, pos.getZ() >> 4).getFluidState(world(pos));
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ) {
        return view(chunkAt(chunkX, chunkZ));
    }

    @Override
    public @Nullable ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus targetStatus, boolean loadOrGenerate) {
        ChunkAccess chunk = real.getChunk(chunkX, chunkZ, targetStatus, loadOrGenerate);
        return chunk == null ? null : view(chunk);
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return real.hasChunk(chunkX, chunkZ);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return real.getWorldBorder();
    }

    @Override
    public boolean isClientSide() {
        return real.isClientSide();
    }

    @Override
    public List<? extends Player> players() {
        return real.players();
    }

    @Override
    public @Nullable Player getNearestPlayer(double x, double y, double z, double maxDist, @Nullable Predicate<Entity> predicate) {
        return real.getNearestPlayer(x, y, z, maxDist, predicate);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB bb, @Nullable Predicate<? super Entity> selector) {
        return real.getEntities(except, world(bb), selector);
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector) {
        return real.getEntities(type, world(bb), selector);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker, int updateLimit) {
        return real.destroyBlock(world(pos), dropResources, breaker, updateLimit);
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(getFluidState(pos));
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return predicate.test(getBlockState(pos));
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean movedByPiston) {
        return real.removeBlock(world(pos), movedByPiston);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int updateFlags, int updateLimit) {
        boolean placed = real.setBlock(world(pos), state, updateFlags, updateLimit);
        if (placed) {
            ProtoChunk view = viewAt(pos.getX(), pos.getZ());
            for (Heightmap.Types type : HEIGHTMAPS) {
                if (view.hasPrimedHeightmap(type)) view.getOrCreateHeightmapUnprimed(type).update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
            }
        }
        return placed;
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        entity.setPos(entity.getX(), entity.getY() + offset, entity.getZ());
        if (entity instanceof EndCrystal crystal && crystal.getBeamTarget() != null) {
            crystal.setBeamTarget(band.toWorld(crystal.getBeamTarget()));
        }
        return real.addFreshEntity(entity);
    }

    @Override
    public boolean ensureCanWrite(BlockPos pos) {
        return real.ensureCanWrite(world(pos));
    }

    @Override
    public BlockMatcher findBlocksIn(BlockPos from, BlockPos to) {
        return real.findBlocksIn(world(from), world(to));
    }

    @Override
    public void setCurrentlyGenerating(@Nullable Supplier<String> currentlyGenerating) {
        real.setCurrentlyGenerating(currentlyGenerating);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return viewAt(x, z).getHeight(type, x & 15, z & 15) + 1;
    }

    @Override
    public int getMinY() {
        return band.nativeMinY();
    }

    @Override
    public int getHeight() {
        return band.nativeHeight();
    }

    @Override
    public String toString() {
        return "BandLevel[" + band.name() + "] of " + real;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> LevelTickAccess<T> ticks(LevelTickAccess<T> realTicks) {
        if (offset == 0) return realTicks;
        LevelTickAccess raw = realTicks;
        return new LevelTickAccess<T>() {
            @Override
            public boolean willTickThisTick(BlockPos pos, T type) {
                return raw.willTickThisTick(band.toWorld(pos), type);
            }

            @Override
            public void schedule(ScheduledTick<T> tick) {
                raw.schedule(new ScheduledTick(tick.type(), band.toWorld(tick.pos()), tick.triggerTick(), tick.priority(), tick.subTickOrder()));
            }

            @Override
            public boolean hasScheduledTick(BlockPos pos, T type) {
                return raw.hasScheduledTick(band.toWorld(pos), type);
            }

            @Override
            public int count() {
                return raw.count();
            }
        };
    }
}
