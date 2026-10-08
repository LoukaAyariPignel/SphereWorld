package dev.sphereworld.worldgen.stacked;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetConfig;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.worldgen.ClimateWindow;
import dev.sphereworld.worldgen.PlanetChunkGenerator;
import dev.sphereworld.worldgen.PlanetRandomState;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.ticks.ProtoChunkTicks;
import org.jspecify.annotations.Nullable;

public final class StackedChunkGenerator extends PlanetChunkGenerator {
    public record Layer(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group(
                        BiomeSource.CODEC.fieldOf("biome_source").forGetter(Layer::biomeSource),
                        NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(Layer::settings))
                .apply(i, Layer::new));
    }

    public static final MapCodec<StackedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(StackedChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(StackedChunkGenerator::generatorSettings),
                    PlanetConfig.CODEC.fieldOf("sphereworld").forGetter(StackedChunkGenerator::planetConfig),
                    Layer.CODEC.fieldOf("nether").forGetter(g -> g.netherLayer),
                    Layer.CODEC.fieldOf("end").forGetter(g -> g.endLayer))
            .apply(i, i.stable(StackedChunkGenerator::new)));

    public record BandContext(StackBand band, NoiseBasedChunkGenerator generator, RandomState randomState,
                       ChunkGeneratorStructureState structureState, Set<Structure> structures,
                       java.util.function.Supplier<StructureManager> locateManagers) {
        public StructureManager locateManager() {
            return locateManagers.get();
        }
    }

    public static final ThreadLocal<BandContext> LOCATING = new ThreadLocal<>();

    private final Layer netherLayer;
    private final Layer endLayer;
    private final NoiseBasedChunkGenerator netherGenerator;
    private final NoiseBasedChunkGenerator endGenerator;
    private volatile @Nullable List<BandContext> bands;
    private volatile @Nullable PalettedContainerFactory containerFactory;
    private volatile @Nullable DensityFunction netherCaves;

    public StackedChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, PlanetConfig planetConfig,
                                 Layer nether, Layer end) {
        super(biomeSource, settings, planetConfig);
        this.netherLayer = nether;
        this.endLayer = end;
        this.netherGenerator = new NoiseBasedChunkGenerator(nether.biomeSource(), nether.settings());
        this.endGenerator = new NoiseBasedChunkGenerator(end.biomeSource(), end.settings());
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public StackedChunkGenerator withConfig(PlanetConfig config) {
        return new StackedChunkGenerator(getBiomeSource(), generatorSettings(), config, netherLayer, endLayer);
    }

    public NoiseBasedChunkGenerator generatorFor(StackBand band) {
        return band == StackBand.NETHER ? netherGenerator : band == StackBand.END ? endGenerator : this;
    }

    public void initBands(ServerLevel level, RandomState mainRandomState, ChunkGeneratorStructureState mainState,
                          @Nullable PlanetGeometry geometry) {
        long seed = level.getSeed();
        RegistryAccess registries = level.registryAccess();
        var noises = registries.lookupOrThrow(Registries.NOISE);
        var structureSets = registries.lookupOrThrow(Registries.STRUCTURE_SET);
        containerFactory = PalettedContainerFactory.create(registries);
        List<BandContext> list = new ArrayList<>();
        for (StackBand band : List.of(StackBand.NETHER, StackBand.OVERWORLD, StackBand.END)) {
            NoiseBasedChunkGenerator generator = generatorFor(band);
            RandomState randomState = band == StackBand.OVERWORLD ? mainRandomState
                    : RandomState.create(noises, seed, generator.generatorSettings().value());
            if (band != StackBand.OVERWORLD && geometry != null) {
                ((PlanetRandomState) (Object) randomState).sphereworld$attachPlanet(geometry, ClimateWindow.VANILLA);
            }

            ChunkGeneratorStructureState state;
            if (band == StackBand.OVERWORLD) {
                state = mainState;
            } else {
                state = generator.createState(structureSets, randomState, seed);
                state.ensureStructuresGenerated();
            }
            Set<Structure> structures = new java.util.HashSet<>();
            state.possibleStructureSets().forEach(set -> set.value().structures().forEach(entry -> structures.add(entry.structure().value())));
            java.util.function.Supplier<StructureManager> locateManager = com.google.common.base.Suppliers.memoize(() -> new StructureManager(
                    level, level.getServer().getWorldGenSettings().options(),
                    new StructureCheck(level.getChunkSource().chunkScanner(), registries, level.getServer().getStructureTemplateManager(),
                            level.dimension(), generator, randomState, band.heightAccessor(), generator.getBiomeSource(), seed,
                            level.getServer().getFixerUpper())));
            list.add(new BandContext(band, generator, randomState, state, structures, locateManager));
        }
        bands = List.copyOf(list);
        try {
            netherCaves = NetherCaves.shape(registries);
        } catch (RuntimeException e) {
            netherCaves = null;
            SphereWorld.LOGGER.warn("Overworld caves cannot reach the Nether: the cave shape could not be read", e);
        }
        SphereWorld.LOGGER.info("Stacked planet: Nether y {}..{}, Overworld y {}..{}, End y {}..{}",
                StackBand.NETHER.worldMinY(), StackBand.NETHER.worldMaxY(), StackBand.OVERWORLD.worldMinY(),
                StackBand.OVERWORLD.worldMaxY(), StackBand.END.worldMinY(), StackBand.END.worldMaxY());
    }

    private List<BandContext> bands() {
        List<BandContext> list = bands;
        if (list == null) throw new IllegalStateException("Stacked planet bands used before the level was set up");
        return list;
    }

    public @Nullable BandContext band(StackBand band) {
        List<BandContext> list = bands;
        if (list == null) return null;
        for (BandContext context : list) if (context.band() == band) return context;
        return null;
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
            ServerLevel level, HolderSet<Structure> wanted, BlockPos pos, int maxSearchRadius, boolean createReference) {
        Pair<BlockPos, Holder<Structure>> nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BandContext band : bands()) {
            List<Holder<Structure>> mine = new ArrayList<>();
            for (Holder<Structure> structure : wanted) if (band.structures().contains(structure.value())) mine.add(structure);
            if (mine.isEmpty()) continue;
            Pair<BlockPos, Holder<Structure>> found;
            if (band.generator() == this) {
                found = super.findNearestMapStructure(level, HolderSet.direct(mine), pos, maxSearchRadius, createReference);
            } else {
                LOCATING.set(band);
                try {
                    found = band.generator().findNearestMapStructure(level, HolderSet.direct(mine), pos, maxSearchRadius, createReference);
                } finally {
                    LOCATING.remove();
                }
                if (found != null) {
                    BlockPos at = found.getFirst();
                    found = Pair.of(at.atY(band.band().offset() + band.band().nativeMinY() + 64), found.getSecond());
                }
            }
            if (found == null) continue;
            double dx = found.getFirst().getX() - pos.getX();
            double dz = found.getFirst().getZ() - pos.getZ();
            double distance = dx * dx + dz * dz;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = found;
            }
        }
        return nearest;
    }

    private static final VarHandle VIEWS = MethodHandles.arrayElementVarHandle(ProtoChunk[].class);
    private static final EnumSet<Heightmap.Types> ALL_HEIGHTMAPS = EnumSet.allOf(Heightmap.Types.class);

    @Nullable ProtoChunk sharedView(ChunkAccess chunk, StackBand band) {
        if (chunk.getClass() != ProtoChunk.class) return null;
        ProtoChunk[] views = ((BandViews) chunk).sphereworld$bandViews();
        int slot = band.slot();
        ProtoChunk view = (ProtoChunk) VIEWS.getAcquire(views, slot);
        if (view == null) {
            ProtoChunk created = view(chunk, band);
            view = (ProtoChunk) VIEWS.compareAndExchangeRelease(views, slot, (ProtoChunk) null, created);
            if (view == null) view = created;
        }
        ChunkStatus status = chunk.getPersistedStatus();
        if (view.getPersistedStatus() != status) view.setPersistedStatus(status);
        return view;
    }

    ProtoChunk view(ChunkAccess chunk, StackBand band) {
        LevelChunkSection[] all = chunk.getSections();
        int first = band.firstWorldSectionIndex();
        LevelChunkSection[] sections = Arrays.copyOfRange(all, first, first + band.sectionCount());
        ProtoChunk view = new ProtoChunk(chunk.getPos(), UpgradeData.EMPTY, sections, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(),
                band.heightAccessor(), containerFactory, null);
        view.setPersistedStatus(chunk.getPersistedStatus());
        return view;
    }

    private static void copyPostProcessing(ProtoChunk view, ChunkAccess chunk, StackBand band) {
        ShortList[] lists = view.getPostProcessing();
        for (int i = 0; i < lists.length; i++) {
            if (lists[i] != null && !lists[i].isEmpty()) chunk.addPackedPostProcess(lists[i], band.firstWorldSectionIndex() + i);
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunk) {
        CompletableFuture<ChunkAccess> future = CompletableFuture.completedFuture(chunk);
        for (BandContext band : bands()) {
            future = future.thenCompose(ignored -> band.generator() == this
                    ? super.createBiomes(randomState, blender, structureManager, view(chunk, band.band()))
                    : band.generator().createBiomes(band.randomState(), Blender.empty(), structureManager, view(chunk, band.band())));
        }
        return future.thenApply(ignored -> chunk);
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
                                                       BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion,
                                                       Set<Holder<Biome>> possibleBiomes) {
        CompletableFuture<ChunkAccess> future = CompletableFuture.completedFuture(chunk);
        for (BandContext band : bands()) {
            future = future.thenCompose(ignored -> {
                StackBand stackBand = band.band();
                ProtoChunk shared = sharedView(chunk, stackBand);
                ProtoChunk view = shared != null ? shared : view(chunk, stackBand);
                BiomeManager bandBiomes = biomeManager.withDifferentSource(bandBiomes(chunk, biomeManager, stackBand));
                StructureManager bandStructures = BandStructures.manager(structureManager, band);
                Set<Holder<Biome>> bandPossible = band.generator().getBiomeSource().possibleBiomes();

                CompletableFuture<ChunkAccess> built = band.generator() == this
                        ? super.buildTerrain(view, blender, randomState, bandStructures, bandBiomes, null, bandPossible)
                        : band.generator().buildTerrain(view, Blender.empty(), band.randomState(), bandStructures, bandBiomes, null, bandPossible);
                return built.thenApply(done -> {
                    copyPostProcessing(view, chunk, stackBand);
                    return chunk;
                });
            });
        }
        return future.thenApply(built -> {
            ChunkAccess done = blendOverworldIntoNether(openBetweenBands(built), randomState);
            boolean carved = false;
            DensityFunction caves = netherCaves;
            BandContext overworld = band(StackBand.OVERWORLD);
            if (caves != null && overworld != null && planetConfig().cavesToNether()) {
                Beardifier beardifier = Beardifier.forStructuresInChunk(BandStructures.manager(structureManager, overworld), done.getPos());
                carved = NetherCaves.carve(done, randomState, caves, beardifier) > 0;
            }
            for (BandContext band : bands()) {
                ProtoChunk view = sharedView(done, band.band());
                if (view != null) Heightmap.primeHeightmaps(view, carved && band.band() != StackBand.END ? ALL_HEIGHTMAPS : ChunkStatus.FINAL_HEIGHTMAPS);
            }
            return done;
        });
    }

    private static BiomeResolver bandBiomes(ChunkAccess chunk, BiomeManager biomes, StackBand band) {
        ChunkPos pos = chunk.getPos();
        return (qx, qy, qz) -> {
            int y = band.worldQuart(qy);
            if (QuartPos.toSection(qx) == pos.x() && QuartPos.toSection(qz) == pos.z()) return chunk.getNoiseBiome(qx, y, qz);
            return biomes.getNoiseBiomeAtQuart(qx, y, qz);
        };
    }

    private static ChunkAccess openBetweenBands(ChunkAccess chunk) {
        int netherRoofBottom = StackBand.NETHER.worldMaxY() - 8;
        int overworldFloorTop = StackBand.OVERWORLD.worldMinY() + 8;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        for (int y = netherRoofBottom; y <= overworldFloorTop; y++) {
            BlockState fill = y < StackBand.OVERWORLD.worldMinY() ? Blocks.NETHERRACK.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    pos.set(x0 + dx, y, z0 + dz);
                    if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) chunk.setBlockState(pos, fill);
                }
            }
        }
        return chunk;
    }

    private static final int TRANSITION_DEPTH = 5;

    private static ChunkAccess blendOverworldIntoNether(ChunkAccess chunk, RandomState randomState) {
        var random = randomState.getOrCreateRandomFactory(SphereWorld.id("overworld_nether_transition"));
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        for (int row = 0; row < TRANSITION_DEPTH; row++) {
            int y = StackBand.NETHER.worldMaxY() - row;
            float chance = (TRANSITION_DEPTH - row) / (TRANSITION_DEPTH + 1.0F);
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    pos.set(x0 + dx, y, z0 + dz);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                    if (random.at(pos.getX(), y, pos.getZ()).nextFloat() < chance) chunk.setBlockState(pos, deepslate);
                }
            }
        }
        return chunk;
    }

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState state, StructureManager structureManager,
                                 ChunkAccess chunk, StructureTemplateManager templateManager, ResourceKey<Level> level) {
        for (BandContext band : bands()) {
            ProtoChunk view = view(chunk, band.band());
            if (band.generator() == this) {
                super.createStructures(registryAccess, state, structureManager, view, templateManager, level);
            } else {
                band.generator().createStructures(registryAccess, band.structureState(), structureManager, view, templateManager, level);
            }
            for (Map.Entry<Structure, net.minecraft.world.level.levelgen.structure.StructureStart> entry : view.getAllStarts().entrySet()) {
                chunk.setStartForStructure(entry.getKey(), BandStructures.toWorld(entry.getValue(), band.band()));
            }
        }
    }

    void setBlockInBand(ChunkAccess chunk, BlockPos pos, BlockState state) {
        chunk.setBlockState(pos, state);
        StackBand band = StackBand.at(pos.getY());
        ProtoChunk view = sharedView(chunk, band);
        if (view == null) return;
        int nativeY = pos.getY() - band.offset();
        for (Heightmap.Types type : ALL_HEIGHTMAPS) {
            if (view.hasPrimedHeightmap(type)) view.getOrCreateHeightmapUnprimed(type).update(pos.getX() & 15, nativeY, pos.getZ() & 15, state);
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        if (netherCaves != null && planetConfig().cavesToNether()) NetherCaves.seal(level, chunk, this);
        for (BandContext band : bands()) {
            BandAccess bandLevel = BandAccess.of(level, this, band.band());
            ChunkAccess view = bandLevel.view(chunk);
            StructureManager bandStructures = BandStructures.manager(structureManager, band);
            if (band.generator() == this) {
                super.applyBiomeDecoration(bandLevel.level(), view, bandStructures);
            } else {
                band.generator().applyBiomeDecoration(bandLevel.level(), view, bandStructures);
            }
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        for (BandContext band : bands()) {
            if (band.generator().generatorSettings().value().disableMobGeneration()) continue;
            BandAccess bandLevel = BandAccess.of(region, this, band.band());
            ChunkPos pos = region.getCenter();

            BlockPos source = pos.getWorldPosition().atY(band.band().worldMaxY());
            WorldgenRandom random = new WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed()));
            random.setDecorationSeed(region.getSeed(), pos.getMinBlockX(), pos.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(bandLevel.level(), source, pos, random);
        }
    }

    @Override
    public int getSpawnHeight(net.minecraft.world.level.LevelHeightAccessor heightAccessor) {
        return getSeaLevel() + 1;
    }

    PalettedContainerFactory containerFactory() {
        return containerFactory;
    }
}
