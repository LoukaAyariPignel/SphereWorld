package dev.sphereworld.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

public final class PlanetCommand {
    private PlanetCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sphereworld")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("info").executes(PlanetCommand::info))
                .then(Commands.literal("seamcheck").executes(PlanetCommand::seamCheck))
                .then(Commands.literal("atlasmap").executes(PlanetCommand::atlasMap))
                .then(Commands.literal("probe")
                        .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                .executes(context -> {
                                    ServerLevel level = context.getSource().getLevel();
                                    BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(context, "pos");
                                    String text = String.format("probe %s: %s block light %d sky light %d",
                                            pos.toShortString(), level.getBlockState(pos).getBlock().getName().getString(),
                                            level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos),
                                            level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos));
                                    context.getSource().sendSuccess(() -> Component.literal(text), false);
                                    return 1;
                                })))
                .then(Commands.literal("column")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> column(context, IntegerArgumentType.getInteger(context, "x"),
                                                IntegerArgumentType.getInteger(context, "z"))))))
                .then(Commands.literal("map")
                        .then(Commands.argument("centerX", IntegerArgumentType.integer())
                                .then(Commands.argument("centerZ", IntegerArgumentType.integer())
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(16, 8192))
                                                .then(Commands.argument("blocksPerPixel", IntegerArgumentType.integer(1, 64))
                                                        .executes(PlanetCommand::map)))))));
    }

    private static int column(CommandContext<CommandSourceStack> context, int x, int z) {
        ServerLevel level = context.getSource().getLevel();
        StringBuilder text = new StringBuilder("column " + x + " " + z + ":");
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxY(), z);
        String run = null;
        int runTop = level.getMaxY();
        for (int y = level.getMaxY(); y >= level.getMinY() - 1; y--) {
            String here;
            if (y < level.getMinY()) {
                here = null;
            } else {
                pos.setY(y);
                String block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
                String biome = level.getBiome(pos).unwrapKey().map(key -> key.identifier().getPath()).orElse("?");
                here = block + " (" + biome + ")";
            }
            if (run != null && !run.equals(here)) {
                text.append("\n  ").append(runTop).append("..").append(y + 1).append(" ").append(run);
                runTop = y;
            }
            if (run == null) runTop = y;
            run = here;
        }
        String result = text.toString();
        dev.sphereworld.SphereWorld.LOGGER.info(result);
        context.getSource().sendSuccess(() -> Component.literal(result), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        PlanetGeometry geometry = Planets.of(level);
        if (geometry == null) {
            context.getSource().sendFailure(Component.literal("This dimension is not a planet."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(String.format(
                "Planet %s: circumference %d blocks, radius %.0f blocks, coordinates wrap at ±%d",
                level.dimension().identifier(), geometry.circumference(), geometry.radius(), geometry.half())), false);
        return geometry.circumference();
    }

    private static int seamCheck(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        PlanetGeometry g = Planets.of(level);
        if (g == null) {
            context.getSource().sendFailure(Component.literal("This dimension is not a planet."));
            return 0;
        }
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        int periodicMismatches = 0;
        int samples = 0;
        int maxSeamJump = 0;
        int maxInteriorJump = 0;
        int bigSeam = 0;
        int bigInterior = 0;
        java.util.Random random = new java.util.Random(1);
        for (int i = 0; i < 1500; i++) {
            int x = random.nextInt(g.circumference()) - g.half();
            int z = random.nextInt(g.circumference()) - g.half();
            int a = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int b = generator.getBaseHeight(x + g.circumference(), z - g.circumference(), Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            samples++;
            if (a != b) {
                periodicMismatches++;
                if (generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise) {
                    var fd = noise.generatorSettings().value().noiseRouter().finalDensity();
                    var ctx = net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build();
                    var sampler = randomState.getSampler(fd);
                    int minY = -64;
                    var v1 = new net.minecraft.world.level.levelgen.densityfunction.DensityVolume(1, 384, 1, x, minY, z);
                    var v2 = new net.minecraft.world.level.levelgen.densityfunction.DensityVolume(1, 384, 1, x + g.circumference(), minY, z - g.circumference());
                    var b1 = net.minecraft.world.level.levelgen.densityfunction.DensityBuffer.createUnpooled(384);
                    var b2 = net.minecraft.world.level.levelgen.densityfunction.DensityBuffer.createUnpooled(384);
                    sampler.sampleVolume(ctx, b1, v1);
                    var ctx2 = net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build();
                    sampler.sampleVolume(ctx2, b2, v2);
                    int firstDiff = -999;
                    for (int y = 383; y >= 0; y--) {
                        if (Math.abs(b1.get(y) - b2.get(y)) > 1e-4) { firstDiff = y + minY; break; }
                    }
                    float p1 = sampler.sampleValue(ctx, x, a, z);
                    int fdY = firstDiff;
                    String msg = String.format("mismatch x=%d z=%d h=%d vs %d, volume diff topmost y=%d point=%.3f vol=%.3f/%.3f",
                            x, z, a, b, fdY, p1, b1.get(Math.max(0, a + 64)), b2.get(Math.max(0, a + 64)));
                    context.getSource().sendSuccess(() -> Component.literal(msg), false);
                }
            }

            int s1 = generator.getBaseHeight(g.half() - 1, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int s2 = generator.getBaseHeight(-g.half(), z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int s3 = generator.getBaseHeight(x, g.half() - 1, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int s4 = generator.getBaseHeight(x, -g.half(), Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            maxSeamJump = Math.max(maxSeamJump, Math.max(Math.abs(s1 - s2), Math.abs(s3 - s4)));
            int i1 = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int i2 = generator.getBaseHeight(x + 1, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int i3 = generator.getBaseHeight(z, x, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int i4 = generator.getBaseHeight(z, x + 1, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            maxInteriorJump = Math.max(maxInteriorJump, Math.max(Math.abs(i1 - i2), Math.abs(i3 - i4)));
            if (Math.abs(s1 - s2) > 10 || Math.abs(s3 - s4) > 10) bigSeam++;
            if (Math.abs(i1 - i2) > 10 || Math.abs(i3 - i4) > 10) bigInterior++;
        }
        if (generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise) {
            var router = noise.generatorSettings().value().noiseRouter();
            java.util.Map<String, net.minecraft.world.level.levelgen.densityfunction.DensityFunction> functions = new java.util.LinkedHashMap<>();
            functions.put("temperature", router.temperature());
            functions.put("vegetation", router.vegetation());
            functions.put("continents", router.continents());
            functions.put("erosion", router.erosion());
            functions.put("depth", router.depth());
            functions.put("ridges", router.ridges());
            functions.put("surface", router.chunkSurfaceLevel());
            functions.put("final", router.finalDensity());
            StringBuilder report = new StringBuilder("router periodicity:");
            for (var entry : functions.entrySet()) {
                int bad = 0;
                java.util.Random r = new java.util.Random(2);
                for (int i = 0; i < 300; i++) {
                    int x = r.nextInt(g.circumference()) - g.half();
                    int y = r.nextInt(200) - 40;
                    int z = r.nextInt(g.circumference()) - g.half();
                    float a = randomState.sampleBlockValueUncached(entry.getValue(), x, y, z);
                    float b = randomState.sampleBlockValueUncached(entry.getValue(), x + g.circumference(), y, z - g.circumference());
                    if (Math.abs(a - b) > 1.0E-4F) bad++;
                }
                float seamMax = 0;
                float interiorMax = 0;
                for (int i = 0; i < 300; i++) {
                    int y = r.nextInt(200) - 40;
                    int z = r.nextInt(g.circumference()) - g.half();
                    int x = r.nextInt(g.circumference() - 2) - g.half();
                    seamMax = Math.max(seamMax, Math.abs(randomState.sampleBlockValueUncached(entry.getValue(), g.half() - 1, y, z)
                            - randomState.sampleBlockValueUncached(entry.getValue(), -g.half(), y, z)));
                    interiorMax = Math.max(interiorMax, Math.abs(randomState.sampleBlockValueUncached(entry.getValue(), x, y, z)
                            - randomState.sampleBlockValueUncached(entry.getValue(), x + 1, y, z)));
                }
                report.append(' ').append(entry.getKey()).append('=').append(bad)
                        .append(String.format("[seam %.3f int %.3f]", seamMax, interiorMax));
            }
            String text = report.toString();
            context.getSource().sendSuccess(() -> Component.literal(text), false);
        }
        int bs = bigSeam;
        int bi = bigInterior;
        context.getSource().sendSuccess(() -> Component.literal("steps >10: seam " + bs + " interior " + bi), false);
        int mismatches = periodicMismatches;
        int seam = maxSeamJump;
        int interior = maxInteriorJump;
        int total = samples;
        context.getSource().sendSuccess(() -> Component.literal(String.format(
                "seamcheck: periodic mismatches %d/%d, max seam step %d, max interior step %d",
                mismatches, total, seam, interior)), false);
        return mismatches == 0 ? 1 : 0;
    }

    private static int atlasMap(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        PlanetGeometry g = Planets.of(level);
        if (g == null) {
            context.getSource().sendFailure(Component.literal("This dimension is not a planet."));
            return 0;
        }
        var live = dev.sphereworld.atlas.PlanetAtlases.get(level.dimension().identifier());
        var atlas = live != null ? live.copy() : dev.sphereworld.atlas.PlanetAtlas.compute(level, g);
        var biomes = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        int n = atlas.size();
        BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int i = row * n + col;
                int h = atlas.heights()[i];
                var biome = biomes.byId(atlas.biomes()[i]);
                String name = biome == null ? "?" : String.valueOf(biomes.getKey(biome));
                names.add(name);
                int rgb;
                if (h == dev.sphereworld.atlas.PlanetAtlas.VOID) {
                    rgb = 0x000000;
                } else {
                    int north = atlas.heights()[Math.floorMod(row - 1, n) * n + col];
                    int shade = north == dev.sphereworld.atlas.PlanetAtlas.VOID ? 0 : Math.max(-30, Math.min(30, (h - north) * 6));
                    int c = atlas.colors()[i];
                    int r = Math.max(0, Math.min(255, ((c >> 16) & 255) + shade));
                    int gg = Math.max(0, Math.min(255, ((c >> 8) & 255) + shade));
                    int b = Math.max(0, Math.min(255, (c & 255) + shade));
                    rgb = (r << 16) | (gg << 8) | b;
                }
                image.setRGB(col, row, rgb);
            }
        }
        Path out = context.getSource().getServer().getServerDirectory().resolve("sphereworld-maps")
                .resolve("atlas_" + level.dimension().identifier().getPath() + ".png");
        try {
            Files.createDirectories(out.getParent());
            ImageIO.write(image, "png", out.toFile());
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal("Could not write map: " + e.getMessage()));
            return 0;
        }
        String list = names.size() + " surface biomes: " + String.join(", ", names);
        context.getSource().sendSuccess(() -> Component.literal("Wrote " + out.toAbsolutePath() + "\n" + list), false);
        return names.size();
    }

    private static int map(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        int centerX = IntegerArgumentType.getInteger(context, "centerX");
        int centerZ = IntegerArgumentType.getInteger(context, "centerZ");
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int step = IntegerArgumentType.getInteger(context, "blocksPerPixel");
        int size = 2 * radius / step;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] previousRow = new int[size];
        for (int pz = 0; pz < size; pz++) {
            int z = centerZ - radius + pz * step;
            for (int px = 0; px < size; px++) {
                int x = centerX - radius + px * step;
                ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                pos.set(x, top, z);
                MapColor color = chunk.getBlockState(pos).getMapColor(level, pos);
                MapColor.Brightness brightness = MapColor.Brightness.NORMAL;
                if (pz > 0) {
                    brightness = top > previousRow[px] ? MapColor.Brightness.HIGH
                            : top < previousRow[px] ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                }
                previousRow[px] = top;
                int argb = color.calculateARGBColor(brightness);
                image.setRGB(px, pz, argb & 0xFFFFFF);
            }
        }
        Path out = context.getSource().getServer().getServerDirectory().resolve("sphereworld-maps")
                .resolve(String.format("map_%s_%d_%d_r%d.png", level.dimension().identifier().getPath(), centerX, centerZ, radius));
        try {
            Files.createDirectories(out.getParent());
            ImageIO.write(image, "png", out.toFile());
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal("Could not write map: " + e.getMessage()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Wrote " + out.toAbsolutePath()), false);
        return 1;
    }
}
