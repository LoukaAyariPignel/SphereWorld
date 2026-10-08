package dev.sphereworld.compat;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.mixin.level.ChunkCacheInvoker;
import dev.sphereworld.mixin.level.ChunkMapInvoker;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

public final class VoxyPregen {
    private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, SphereWorld.id("voxy_pregen"),
            new TicketType(0L, TicketType.FLAG_LOADING));
    private static final String FILE = "sphereworld_voxy_pregen_rows.txt";

    private static final int MAX_VOXY_BACKLOG = 48 * 128;
    private static final double MAX_HEAP = 0.70;
    private static final int IN_FLIGHT = Math.max(16, Runtime.getRuntime().availableProcessors() * 4);

    public record Progress(long done, long total, int row, int rows) {
        public float fraction() {
            return total == 0 ? 1.0F : (float) done / total;
        }
    }

    private static volatile @Nullable Progress progress;
    private static volatile boolean waiting;
    private static volatile boolean skipRequested;
    private static @Nullable Run run;
    private static boolean wasFrozen;

    private VoxyPregen() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(VoxyPregen::prepare);
        ServerTickEvents.END_SERVER_TICK.register(VoxyPregen::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> stop(server, false));
    }

    public static boolean active() {
        return waiting || run != null;
    }

    public static void skip() {
        skipRequested = true;
    }

    public static @Nullable Progress progress() {
        return progress;
    }

    private static boolean wanted(MinecraftServer server) {
        return !server.isDedicatedServer() && FabricLoader.getInstance().isModLoaded("voxy")
                && !Boolean.getBoolean("sphereworld.noVoxyPregen");
    }

    private static void prepare(MinecraftServer server) {
        progress = null;
        skipRequested = false;
        if (!wanted(server) || Boolean.getBoolean("sphereworld.voxyPregenOnDemand")) return;
        begin(server);
    }

    public static void startNow(MinecraftServer server) {
        if (wanted(server)) begin(server);
    }

    private static void begin(MinecraftServer server) {
        PlanetGeometry geometry = Planets.of(server.overworld());
        if (geometry == null) return;
        int startRow = readRow(server);
        int rows = rowsFor(geometry);
        if (startRow >= rows) return;
        waiting = true;
        wasFrozen = server.tickRateManager().isFrozen();
        server.tickRateManager().setFrozen(true);
        long total = (long) geometry.chunks() * geometry.chunks();
        progress = new Progress((long) startRow * geometry.chunks(), total, startRow, rows);
        SphereWorld.LOGGER.info("Voxy present: generating the whole planet ({} chunks) before play, from row {}", total, startRow);
    }

    private static int rowsFor(PlanetGeometry geometry) {
        return Math.min(geometry.chunks(), Integer.getInteger("sphereworld.voxyPregenRows", Integer.MAX_VALUE));
    }

    private static void tick(MinecraftServer server) {
        if (!waiting && run == null) return;
        if (skipRequested) {
            skipRequested = false;
            stop(server, false);
            return;
        }
        if (run == null) {
            if (server.getPlayerList().getPlayerCount() == 0) return;
            ServerLevel level = server.overworld();
            PlanetGeometry geometry = Planets.of(level);
            if (geometry == null) {
                stop(server, false);
                return;
            }
            run = new Run(server, level, geometry, readRow(server));
            waiting = false;
        }
        if (run.step()) stop(server, true);
    }

    private static void stop(MinecraftServer server, boolean finished) {
        Run current = run;
        boolean started = waiting || current != null;
        waiting = false;
        run = null;
        progress = null;
        if (!started) return;
        if (!wasFrozen) server.tickRateManager().setFrozen(false);
        if (current == null) return;
        current.release();
        writeRow(server, finished ? current.rows : current.completedRow + 1);
        SphereWorld.LOGGER.info(finished ? "Voxy pregeneration finished" : "Voxy pregeneration paused at row {}", current.completedRow + 1);
    }

    private static final class Run {
        private final MinecraftServer server;
        private final ServerLevel level;
        private final PlanetGeometry geometry;
        private final int n;
        private final int rows;
        private final int maxLoaded;
        private final int spawnX;
        private final int spawnZ;
        private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
        private final List<Pending> pending = new ArrayList<>();
        private final @Nullable Method ingest;
        private int row;
        private int completedRow;
        private long done;
        private long lastSave = System.currentTimeMillis();
        private long lastLog;
        private long lastGc;
        private @Nullable Method voxyInstance;
        private @Nullable Method ingestService;
        private @Nullable Method taskCount;
        private boolean backlogUnknown;

        private record Pending(ChunkPos pos, int row, CompletableFuture<ChunkResult<LevelChunk>> future) {
        }

        Run(MinecraftServer server, ServerLevel level, PlanetGeometry geometry, int startRow) {
            this.server = server;
            this.level = level;
            this.geometry = geometry;
            this.n = geometry.chunks();
            this.rows = rowsFor(geometry);

            this.maxLoaded = n * 24 + 4000;
            BlockPos spawn = server.getRespawnData().pos();
            this.spawnX = geometry.canonicalChunk(SectionPos.blockToSectionCoord(spawn.getX()));
            this.spawnZ = geometry.canonicalChunk(SectionPos.blockToSectionCoord(spawn.getZ()));
            this.row = startRow;
            this.completedRow = startRow - 1;
            this.done = (long) startRow * n;
            this.ingest = voxyIngest();
        }

        boolean step() {
            long deadline = System.nanoTime() + 30_000_000L;
            if (!level.tickRateManager().runsNormally()) {
                ((ChunkCacheInvoker) level.getChunkSource()).sphereworld$ticketStorage().purgeStaleTickets(level.getChunkSource().chunkMap);
            }
            pending.removeIf(this::finish);

            int backlog = voxyBacklog();
            int loaded = level.getChunkSource().getLoadedChunksCount();
            Runtime runtime = Runtime.getRuntime();
            double heap = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
            boolean throttled = backlog > MAX_VOXY_BACKLOG || loaded > maxLoaded || heap > MAX_HEAP;
            if (heap > MAX_HEAP && System.currentTimeMillis() - lastGc > 10_000) {
                lastGc = System.currentTimeMillis();
                System.gc();
            }
            if (System.currentTimeMillis() - lastLog > 10_000) {
                lastLog = System.currentTimeMillis();
                SphereWorld.LOGGER.info("Voxy pregeneration: {} / {} chunks, row {} / {}, {} in flight, Voxy backlog {}, {} loaded, heap {}%{}",
                        done, (long) n * n, completedRow + 1, rows, pending.size(), backlog, loaded, Math.round(heap * 100),
                        throttled ? " (waiting)" : "");
            }
            while (!throttled && pending.size() < IN_FLIGHT && System.nanoTime() < deadline) {
                if (queue.isEmpty()) {
                    if (row >= rows) break;
                    fillRow(row++);
                    continue;
                }
                ChunkPos pos = queue.poll();
                level.getChunkSource().addTicketWithRadius(TICKET, pos, 0);
                ((ChunkCacheInvoker) level.getChunkSource()).sphereworld$runDistanceManagerUpdates();
                ChunkHolder holder = ((ChunkMapInvoker) level.getChunkSource().chunkMap).sphereworld$getVisibleChunkIfPresent(pos.pack());
                if (holder == null) {
                    level.getChunkSource().removeTicketWithRadius(TICKET, pos, 0);
                    done++;
                    continue;
                }
                pending.add(new Pending(pos, row - 1, holder.getFullChunkFuture()));
            }

            int lowestPending = queue.isEmpty() ? row : row - 1;
            for (Pending p : pending) lowestPending = Math.min(lowestPending, p.row);
            completedRow = Math.max(completedRow, lowestPending - 1);
            progress = new Progress(done, (long) n * n, Math.max(0, completedRow + 1), rows);
            if (System.currentTimeMillis() - lastSave > 10_000) {
                lastSave = System.currentTimeMillis();
                writeRow(server, completedRow + 1);
            }
            return row >= rows && queue.isEmpty() && pending.isEmpty();
        }

        private int voxyBacklog() {
            if (backlogUnknown || ingest == null) return 0;
            try {
                if (taskCount == null) {
                    voxyInstance = Class.forName("me.cortex.voxy.commonImpl.VoxyCommon").getMethod("getInstance");
                    ingestService = Class.forName("me.cortex.voxy.commonImpl.VoxyInstance").getMethod("getIngestService");
                    taskCount = Class.forName("me.cortex.voxy.common.world.service.VoxelIngestService").getMethod("getTaskCount");
                }
                Object instance = voxyInstance.invoke(null);
                if (instance == null) return 0;
                Object service = ingestService.invoke(instance);
                return service == null ? 0 : (int) taskCount.invoke(service);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                backlogUnknown = true;
                SphereWorld.LOGGER.warn("Could not read Voxy's ingest backlog; pregeneration is paced by memory only", e);
                return 0;
            }
        }

        private boolean finish(Pending p) {
            if (!p.future.isDone()) return false;
            ChunkResult<LevelChunk> result = p.future.getNow(null);
            if (result != null && ingest != null) {
                result.ifSuccess(chunk -> {
                    try {
                        ingest.invoke(null, chunk);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        SphereWorld.LOGGER.debug("Voxy did not take chunk {}", chunk.getPos(), e);
                    }
                });
            }
            level.getChunkSource().removeTicketWithRadius(TICKET, p.pos, 0);
            done++;
            return true;
        }

        private void fillRow(int r) {
            int z = geometry.canonicalChunk(spawnZ + r);
            queue.add(new ChunkPos(spawnX, z));
            for (int d = 1; d <= n / 2; d++) {
                queue.add(new ChunkPos(geometry.canonicalChunk(spawnX + d), z));
                if (d < n / 2) queue.add(new ChunkPos(geometry.canonicalChunk(spawnX - d), z));
            }
        }

        void release() {
            for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
                level.getChunkSource().removeTicketWithRadius(TICKET, it.next().pos, 0);
                it.remove();
            }
        }

        private static @Nullable Method voxyIngest() {
            try {
                return Class.forName("me.cortex.voxy.common.world.service.VoxelIngestService").getMethod("tryAutoIngestChunk", LevelChunk.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                SphereWorld.LOGGER.warn("Voxy's chunk ingest was not found; the planet is generated but Voxy will only see it when visited", e);
                return null;
            }
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE);
    }

    private static int readRow(MinecraftServer server) {
        try {
            Path path = file(server);
            return Files.exists(path) ? Integer.parseInt(Files.readString(path).trim()) : 0;
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    private static void writeRow(MinecraftServer server, int row) {
        try {
            Path path = file(server);
            Files.createDirectories(path.getParent());
            Files.writeString(path, Integer.toString(Math.max(0, row)));
        } catch (IOException e) {
            SphereWorld.LOGGER.warn("Could not save the Voxy pregeneration progress", e);
        }
    }
}
