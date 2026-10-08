package dev.sphereworld.worldgen.stacked;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;

final class BandLevel implements InvocationHandler {
    private static final EnumSet<Heightmap.Types> HEIGHTMAPS = EnumSet.allOf(Heightmap.Types.class);
    private static final Map<Class<?>, Map<Method, Boolean>> OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

    private final WorldGenLevel real;
    private final StackedChunkGenerator generator;
    private final StackBand band;
    private final WorldGenLevel proxy;
    private final Map<Long, ProtoChunk> views = new HashMap<>();

    private BandLevel(WorldGenLevel real, StackedChunkGenerator generator, StackBand band) {
        this.real = real;
        this.generator = generator;
        this.band = band;
        this.proxy = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(), new Class<?>[] {WorldGenLevel.class}, this);
    }

    static BandLevel of(WorldGenLevel real, StackedChunkGenerator generator, StackBand band) {
        return new BandLevel(real, generator, band);
    }

    WorldGenLevel level() {
        return proxy;
    }

    synchronized ProtoChunk view(ChunkAccess chunk) {
        return views.computeIfAbsent(chunk.getPos().pack(), key -> {
            ProtoChunk view = generator.view(chunk, band);
            Heightmap.primeHeightmaps(view, HEIGHTMAPS);
            return view;
        });
    }

    private ProtoChunk viewAt(int x, int z) {
        return view(real.getChunk(x >> 4, z >> 4));
    }

    @Override
    public Object invoke(Object self, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        int count = args == null ? 0 : args.length;
        switch (name) {
            case "getMinY" -> {
                if (count == 0) return band.nativeMinY();
            }
            case "getHeight" -> {
                if (count == 0) return band.nativeHeight();
                if (count == 3 && args[0] instanceof Heightmap.Types type) {
                    int x = (int) args[1];
                    int z = (int) args[2];
                    return viewAt(x, z).getHeight(type, x & 15, z & 15) + 1;
                }
            }
            case "getChunk" -> {
                Object chunk = invokeOverride(method, translate(args));
                return chunk instanceof ChunkAccess access ? view(access) : chunk;
            }
            case "getNoiseBiome", "getUncachedNoiseBiome" -> {
                if (count == 3) return invokeReal(method, new Object[] {args[0], band.worldQuart((int) args[1]), args[2]});
            }
            case "getBiomeManager" -> {
                if (count == 0) {
                    return real.getBiomeManager().withDifferentSource((qx, qy, qz) -> real.getNoiseBiome(qx, band.worldQuart(qy), qz));
                }
            }
            case "getBlockTicks", "getFluidTicks" -> {
                if (count == 0) return ticks((LevelTickAccess<?>) invokeReal(method, null));
            }
            case "addFreshEntity" -> {
                if (count == 1 && args[0] instanceof Entity entity) {
                    entity.setPos(entity.getX(), entity.getY() + band.offset(), entity.getZ());
                    if (entity instanceof EndCrystal crystal && crystal.getBeamTarget() != null) {
                        crystal.setBeamTarget(band.toWorld(crystal.getBeamTarget()));
                    }
                    return real.addFreshEntity(entity);
                }
            }
            case "setBlock" -> {
                if (count == 4 && args[0] instanceof BlockPos pos) {
                    BlockState state = (BlockState) args[1];
                    boolean placed = real.setBlock(band.toWorld(pos), state, (int) args[2], (int) args[3]);
                    if (placed) {
                        ProtoChunk view = viewAt(pos.getX(), pos.getZ());
                        for (Heightmap.Types type : HEIGHTMAPS) {
                            view.getOrCreateHeightmapUnprimed(type).update(pos.getX() & 15, pos.getY(), pos.getZ() & 15, state);
                        }
                    }
                    return placed;
                }
            }
            case "toString" -> {
                if (count == 0) return "BandLevel[" + band.name() + "] of " + real;
            }
            case "hashCode" -> {
                if (count == 0) return System.identityHashCode(self);
            }
            case "equals" -> {
                if (count == 1) return self == args[0];
            }
            default -> {
            }
        }
        if (method.isDefault() && !overridden(method)) return InvocationHandler.invokeDefault(self, method, args);
        return invokeOverride(method, translate(args));
    }

    private Object[] translate(Object[] args) {
        if (args == null || band.offset() == 0) return args;
        Object[] out = args.clone();
        for (int i = 0; i < out.length; i++) {
            Object arg = out[i];
            if (arg instanceof BlockPos pos) out[i] = band.toWorld(pos);
            else if (arg instanceof Vec3 vec) out[i] = vec.add(0, band.offset(), 0);
            else if (arg instanceof AABB box) out[i] = box.move(0, band.offset(), 0);
            else if (arg instanceof BoundingBox box) out[i] = box.moved(0, band.offset(), 0);
        }
        return out;
    }

    private boolean overridden(Method method) {
        return OVERRIDES.computeIfAbsent(real.getClass(), c -> new java.util.concurrent.ConcurrentHashMap<>())
                .computeIfAbsent(method, m -> {
                    try {
                        return !real.getClass().getMethod(m.getName(), m.getParameterTypes()).getDeclaringClass().isInterface();
                    } catch (NoSuchMethodException e) {
                        return false;
                    }
                });
    }

    private Object invokeOverride(Method method, Object[] args) throws Throwable {
        if (method.isDefault() && overridden(method)) {
            Method own = real.getClass().getMethod(method.getName(), method.getParameterTypes());
            try {
                return own.invoke(real, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
        return invokeReal(method, args);
    }

    private Object invokeReal(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(real, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LevelTickAccess<?> ticks(LevelTickAccess<?> realTicks) {
        if (band.offset() == 0) return realTicks;
        LevelTickAccess raw = realTicks;
        return new LevelTickAccess() {
            @Override
            public boolean willTickThisTick(BlockPos pos, Object type) {
                return raw.willTickThisTick(band.toWorld(pos), type);
            }

            @Override
            public void schedule(ScheduledTick tick) {
                raw.schedule(new ScheduledTick(tick.type(), band.toWorld(tick.pos()), tick.triggerTick(), tick.priority(), tick.subTickOrder()));
            }

            @Override
            public boolean hasScheduledTick(BlockPos pos, Object type) {
                return raw.hasScheduledTick(band.toWorld(pos), type);
            }

            @Override
            public int count() {
                return raw.count();
            }
        };
    }
}
