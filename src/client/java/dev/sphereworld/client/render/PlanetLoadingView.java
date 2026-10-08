package dev.sphereworld.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.atlas.AtlasBuildProgress;
import dev.sphereworld.atlas.PlanetAtlas;
import dev.sphereworld.atlas.PlanetAtlases;
import dev.sphereworld.compat.VoxyPregen;
import dev.sphereworld.planet.Planets;
import dev.sphereworld.worldgen.PlanetChunkGenerator;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class PlanetLoadingView {
    private static final Identifier TEXTURE = SphereWorld.id("loading_planet");
    private static final int MAX_RESOLUTION = 512;
    private static final double TILT = Math.toRadians(18.0);
    private static final double SECONDS_PER_TURN = 36.0;
    private static final double LANDING_SECONDS = 2.5;
    private static final double LANDING_ZOOM = 4.0;

    private static final double MARGIN = 1.1;

    private static final double LX = -0.55, LY = 0.45, LZ = 0.70;

    private static @Nullable DynamicTexture texture;
    private static int textureSize;
    private static long lastCapture;
    private static int captures;

    private static long landingStart;
    private static double spinAtLanding;

    private PlanetLoadingView() {
    }

    private static @Nullable PlanetAtlas atlas() {
        Minecraft client = Minecraft.getInstance();
        if (!client.hasSingleplayerServer()) return ClientAtlases.get(Level.OVERWORLD.identifier());
        AtlasBuildProgress.Live live = AtlasBuildProgress.current();
        if (live != null) return live.atlas();
        return PlanetAtlases.get(Level.OVERWORLD.identifier());
    }

    private static boolean planetWorld() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && (StackedAmbience.isStackedType(client.level) || Planets.of(client.level) != null)) return true;
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) return false;
        try {
            LevelStem overworld = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).getValue(LevelStem.OVERWORLD);
            return overworld != null && overworld.generator() instanceof PlanetChunkGenerator;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean active() {
        return atlas() != null || planetWorld();
    }

    public static boolean keyPressed(boolean escape) {
        if (!escape || VoxyPregen.progress() == null) return false;
        VoxyPregen.skip();
        return true;
    }

    public static void extractBackground(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0xFF04050B);

        java.util.Random random = new java.util.Random(1234L);
        long time = Util.getMillis();
        for (int i = 0; i < width * height / 900; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int base = 90 + random.nextInt(130);
            double twinkle = 0.75 + 0.25 * Math.sin(time / 700.0 + i * 1.7);
            int c = (int) (base * twinkle);
            graphics.fill(x, y, x + 1, y + 1, 0xFF000000 | (c << 16) | (c << 8) | Math.min(255, c + 20));
        }
    }

    public static void extract(GuiGraphicsExtractor graphics, Font font, int width, int height, float serverProgress) {
        PlanetAtlas atlas = atlas();
        if (atlas == null) {
            placeholder(graphics, font, width, height, serverProgress);
            return;
        }
        VoxyPregen.Progress pregen = VoxyPregen.progress();
        AtlasBuildProgress.Live live = AtlasBuildProgress.current();
        if (live != null && live.atlas() != atlas) live = null;
        boolean overworld = atlas.dimension().equals(Level.OVERWORLD.identifier());

        double spin = (Util.getMillis() / 1000.0) * 2.0 * Math.PI / SECONDS_PER_TURN;
        double lon = spin, lat = -TILT, zoom = 1.0;
        BlockPos spawn = spawn();
        if (pregen != null) {
            landingStart = 0;
        } else if (live == null && overworld && spawn != null) {
            if (landingStart == 0) {
                landingStart = Util.getMillis();
                spinAtLanding = spin;
            }
            double t = Math.min(1.0, (Util.getMillis() - landingStart) / 1000.0 / LANDING_SECONDS);
            double ease = t * t * (3 - 2 * t);
            double targetLon = 2.0 * Math.PI * spawn.getX() / atlas.circumference();
            double targetLat = -Math.PI * spawn.getZ() / atlas.circumference();
            double delta = Math.IEEEremainder(targetLon - spinAtLanding, 2.0 * Math.PI);
            lon = spinAtLanding + delta * ease;
            lat = -TILT + (targetLat + TILT) * ease;
            zoom = 1.0 + (LANDING_ZOOM - 1.0) * ease;
        } else if (live != null) {
            landingStart = 0;
        }

        int diameter = Math.max(32, (int) (Math.min(width, height) * 0.58));

        int square = (int) Math.min(diameter * zoom * MARGIN, Math.max(width, height) * 1.05);
        int guiScale = (int) Math.max(1, Math.round(Minecraft.getInstance().getWindow().getGuiScale()));
        int resolution = Math.min(MAX_RESOLUTION, square * guiScale);
        prepareTexture(resolution);
        double extent = square / (diameter * zoom);
        paint(texture.getPixels(), resolution, atlas, live, lon, lat, extent, pregen, spawn);
        texture.upload();

        int panel = live != null ? stagesPanelWidth(font) : 0;
        int centreX = panel > 0 && width - 2 * panel < diameter ? panel + (width - panel) / 2 : width / 2;
        int x = centreX - square / 2;
        int y = (height - square) / 2 - (zoom > 1.01 ? 0 : 10);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, square, square, resolution, resolution, resolution, resolution);

        int textY = Math.min(height - 22, (height + diameter) / 2 - 2);
        Component title;
        float progress;
        if (live != null) {
            String stage = AtlasBuildProgress.PHASES.get(Math.min(live.phase().get(), AtlasBuildProgress.PHASES.size() - 1));
            progress = live.progress();
            title = Component.translatable("sphereworld.loading.stage." + stage)
                    .append(" · ")
                    .append(Component.translatableWithFallback(dimensionKey(atlas), atlas.dimension().getPath()))
                    .append(" " + Math.round(progress * 100) + " %");
            stagesPanel(graphics, font, live, height);
        } else if (pregen != null) {
            progress = pregen.fraction();
            title = Component.translatable("sphereworld.loading.voxy", pregen.done(), pregen.total(), Math.round(progress * 100));
            graphics.centeredText(font, eta(pregen), centreX, Math.min(height - 19, textY + 18), 0xFFB8C4E0);
            graphics.centeredText(font, Component.translatable("sphereworld.loading.voxy.skip").withStyle(ChatFormatting.GRAY),
                    centreX, Math.min(height - 8, textY + 30), 0xFFFFFFFF);
        } else {
            progress = serverProgress;
            title = Component.translatable("sphereworld.loading.landing");
        }

        var lines = font.split(title, Math.max(40, width - 16));
        int titleY = textY - 10 * (lines.size() - 1);
        for (int i = 0; i < lines.size(); i++) {
            graphics.centeredText(font, lines.get(i), centreX, titleY + 10 * i, 0xFFFFFFFF);
        }
        int barX = centreX - 100;
        graphics.fill(barX, textY + 12, barX + 200, textY + 14, 0xFF202436);
        graphics.fill(barX, textY + 12, barX + Math.round(200 * Math.clamp(progress, 0.0F, 1.0F)), textY + 14, 0xFF6FB3FF);
        captureForTests();
    }

    private static final java.util.ArrayDeque<long[]> PREGEN_SAMPLES = new java.util.ArrayDeque<>();
    private static final long ETA_WINDOW_MS = 90_000;

    private static Component eta(VoxyPregen.Progress pregen) {
        long now = Util.getMillis();
        long[] last = PREGEN_SAMPLES.peekLast();
        if (last != null && pregen.done() < last[1]) PREGEN_SAMPLES.clear();
        if (last == null || now - last[0] >= 500) PREGEN_SAMPLES.addLast(new long[] {now, pregen.done()});
        while (PREGEN_SAMPLES.size() > 2 && now - PREGEN_SAMPLES.peekFirst()[0] > ETA_WINDOW_MS) PREGEN_SAMPLES.removeFirst();
        long[] first = PREGEN_SAMPLES.peekFirst();
        long elapsed = now - first[0];
        long made = pregen.done() - first[1];
        if (elapsed < 5_000 || made <= 0) return Component.translatable("sphereworld.loading.voxy.eta.estimating");
        long seconds = Math.round((pregen.total() - pregen.done()) * (elapsed / 1000.0) / made);
        Component time;
        if (seconds >= 3600) {
            time = Component.translatable("sphereworld.loading.voxy.eta.hours", seconds / 3600, String.format(java.util.Locale.ROOT, "%02d", (seconds % 3600) / 60));
        } else if (seconds >= 60) {
            time = Component.translatable("sphereworld.loading.voxy.eta.minutes", (seconds + 30) / 60);
        } else {
            time = Component.translatable("sphereworld.loading.voxy.eta.seconds", Math.max(1, seconds));
        }
        return Component.translatable("sphereworld.loading.voxy.eta", time);
    }

    private static void placeholder(GuiGraphicsExtractor graphics, Font font, int width, int height, float serverProgress) {
        int diameter = Math.max(32, (int) (Math.min(width, height) * 0.58));
        int guiScale = (int) Math.max(1, Math.round(Minecraft.getInstance().getWindow().getGuiScale()));
        int square = (int) (diameter * MARGIN);
        int resolution = Math.min(MAX_RESOLUTION, square * guiScale);
        prepareTexture(resolution);
        double pixel = 2.0 * MARGIN / resolution;
        double spin = (Util.getMillis() / 1000.0) * 2.0 * Math.PI / SECONDS_PER_TURN;
        double cosS = Math.cos(spin), sinS = Math.sin(spin);
        double cosT = Math.cos(TILT), sinT = Math.sin(TILT);
        NativeImage image = texture.getPixels();
        for (int py = 0; py < resolution; py++) {
            double rowY = (1.0 - (py + 0.5) / resolution * 2.0) * MARGIN;
            for (int px = 0; px < resolution; px++) {
                double ny = rowY;
                double nx = ((px + 0.5) / resolution * 2.0 - 1.0) * MARGIN;
                double r2 = nx * nx + ny * ny;
                double radius = Math.sqrt(r2);
                double coverage = coverage(radius, pixel);
                if (coverage <= 0.0) {
                    image.setPixel(px, py, glow(radius, 0.45));
                    continue;
                }
                if (radius >= 1.0) {
                    nx *= 0.9999 / radius;
                    ny *= 0.9999 / radius;
                    r2 = nx * nx + ny * ny;
                }
                double nz = Math.sqrt(1.0 - r2);
                double y1 = ny * cosT - nz * sinT;
                double z1 = ny * sinT + nz * cosT;
                double lon = Math.atan2(nx * cosS + z1 * sinS, -nx * sinS + z1 * cosS);
                double lat = Math.asin(Math.clamp(y1, -1.0, 1.0));
                long cell = (long) Math.floor((lon + Math.PI) * 12) * 131L + (long) Math.floor((lat + Math.PI) * 12) * 977L;
                double noise = ((cell * 2654435761L) >>> 28 & 15) / 15.0;
                double light = 0.25 + 0.75 * Math.max(0.0, nx * LX + ny * LY + nz * LZ);
                double rim = Math.pow(1.0 - nz, 3) * 0.7;
                double r = (24 + 12 * noise) * light * (1 - rim) + 120 * rim * light;
                double g = (24 + 10 * noise) * light * (1 - rim) + 170 * rim * light;
                double b = (34 + 12 * noise) * light * (1 - rim) + 255 * rim * light;
                image.setPixel(px, py, edge(0xFF000000 | ((int) Math.min(255, r) << 16) | ((int) Math.min(255, g) << 8) | (int) Math.min(255, b),
                        coverage, radius, 0.45));
            }
        }
        texture.upload();
        int x = (width - square) / 2;
        int y = (height - square) / 2 - 10;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, square, square, resolution, resolution, resolution, resolution);
        int textY = Math.min(height - 22, (height + diameter) / 2 - 2);
        graphics.centeredText(font, Component.translatable("sphereworld.loading.preparing"), width / 2, textY, 0xFFFFFFFF);
        int barX = width / 2 - 100;
        graphics.fill(barX, textY + 12, barX + 200, textY + 14, 0xFF202436);
        graphics.fill(barX, textY + 12, barX + Math.round(200 * Math.clamp(serverProgress, 0.0F, 1.0F)), textY + 14, 0xFF6FB3FF);
        captureForTests();
    }

    private static int stagesPanelWidth(Font font) {
        int widest = 0;
        for (String stage : AtlasBuildProgress.PHASES) {
            widest = Math.max(widest, font.width(Component.literal("✔ ").append(Component.translatable("sphereworld.loading.stage." + stage))));
        }
        return widest + 20;
    }

    private static void stagesPanel(GuiGraphicsExtractor graphics, Font font, AtlasBuildProgress.Live live, int height) {
        int current = live.phase().get();
        int y = Math.max(8, height / 2 - AtlasBuildProgress.PHASES.size() * 6);
        graphics.fill(4, y - 6, stagesPanelWidth(font) - 4, y + AtlasBuildProgress.PHASES.size() * 12 + 4, 0x99000000);
        for (int i = 0; i < AtlasBuildProgress.PHASES.size(); i++) {
            String key = "sphereworld.loading.stage." + AtlasBuildProgress.PHASES.get(i);
            int colour = i < current ? 0xFF8FD18F : i == current ? 0xFFFFFFFF : 0xFF6B7085;
            String mark = i < current ? "✔ " : i == current ? "▶ " : "· ";
            graphics.text(font, Component.literal(mark).append(Component.translatable(key)), 10, y + i * 12, colour);
        }
    }

    private static @Nullable BlockPos spawn() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return null;
        try {
            return server.getWorldData().overworldData().getRespawnData().pos();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String dimensionKey(PlanetAtlas atlas) {
        Identifier id = atlas.dimension();
        return "sphereworld.dimension." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    private static void prepareTexture(int resolution) {
        if (texture != null && textureSize == resolution) return;
        var manager = Minecraft.getInstance().getTextureManager();
        if (texture != null) manager.release(TEXTURE);
        texture = new DynamicTexture(() -> "SphereWorld loading planet", resolution, resolution, true) {
            {
                this.sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                        .getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.LINEAR);
            }
        };
        textureSize = resolution;
        manager.register(TEXTURE, texture);
    }

    public static void paint(NativeImage image, int s, PlanetAtlas atlas, AtlasBuildProgress.@Nullable Live live,
                              double lon0, double lat0, double extent,
                              VoxyPregen.@Nullable Progress pregen, @Nullable BlockPos spawn) {
        int circumference = atlas.circumference();
        int pregenRow = pregen == null ? Integer.MAX_VALUE : pregen.row();
        int spawnChunkZ = spawn == null ? 0 : Math.floorDiv(spawn.getZ(), 16);
        int chunksRound = Math.max(1, circumference / 16);
        int n = atlas.size();
        short[] heights = atlas.heights();
        int[] colours = live != null ? live.display() : atlas.colors();
        int sea = atlas.seaLevel();
        int cell = atlas.cellSize();
        int current = live == null ? Integer.MAX_VALUE : live.phase().get() + 1;
        int finalStage = AtlasBuildProgress.PHASES.size();
        double cosT = Math.cos(-lat0), sinT = Math.sin(-lat0);
        double cosA = Math.cos(lon0), sinA = Math.sin(lon0);
        double pulse = 0.6 + 0.4 * Math.sin(Util.getMillis() / 160.0);
        double hx = LX, hy = LY, hz = LZ + 1.0;
        double hl = Math.sqrt(hx * hx + hy * hy + hz * hz);
        hx /= hl;
        hy /= hl;
        hz /= hl;
        double pixel = 2.0 * extent / s;
        for (int py = 0; py < s; py++) {
            double rowY = (1.0 - (py + 0.5) / s * 2.0) * extent;
            for (int px = 0; px < s; px++) {
                double ny = rowY;
                double nx = ((px + 0.5) / s * 2.0 - 1.0) * extent;
                double r2 = nx * nx + ny * ny;
                double radius = Math.sqrt(r2);
                double coverage = coverage(radius, pixel);
                if (coverage <= 0.0) {
                    image.setPixel(px, py, glow(radius, 0.55));
                    continue;
                }
                if (radius >= 1.0) {
                    nx *= 0.9999 / radius;
                    ny *= 0.9999 / radius;
                    r2 = nx * nx + ny * ny;
                }
                double nz = Math.sqrt(1.0 - r2);

                double y1 = ny * cosT - nz * sinT;
                double z1 = ny * sinT + nz * cosT;
                double x2 = nx * cosA + z1 * sinA;
                double z2 = -nx * sinA + z1 * cosA;
                double lon = Math.atan2(x2, z2);
                double lat = Math.asin(Math.clamp(y1, -1.0, 1.0));
                int col = Math.floorMod((int) Math.floor((lon / (2.0 * Math.PI) + 0.5) * n), n);
                int row = Math.clamp((int) Math.floor((0.5 - lat / Math.PI) * n), 0, n - 1);
                int index = row * n + col;

                double light = Math.max(0.0, nx * LX + ny * LY + nz * LZ);
                double r, g, b;
                int stage = live == null ? finalStage : live.stageOf()[index];
                if (stage == 0) {
                    double noise = ((index * 2654435761L) >>> 28 & 15) / 15.0;
                    double lit = 0.35 + 0.65 * light;
                    r = (22 + 10 * noise) * lit;
                    g = (20 + 8 * noise) * lit;
                    b = (30 + 10 * noise) * lit;
                } else {
                    int h = heights[index];
                    boolean relief = stage > AtlasBuildProgress.RELIEF;
                    if (relief && h == PlanetAtlas.VOID) {
                        image.setPixel(px, py, coverage < 1.0 ? glow(radius, 0.55) : 0);
                        continue;
                    }
                    int colour = colours[index];
                    double shade = 1.0;
                    if (relief) {
                        int hEast = heights[row * n + (col + 1) % n];
                        int hSouth = heights[Math.min(n - 1, row + 1) * n + col];
                        double slope = ((hEast == PlanetAtlas.VOID ? h : hEast) - h + (hSouth == PlanetAtlas.VOID ? h : hSouth) - h) / (2.0 * cell);
                        shade = Math.clamp(1.0 - slope * 1.2, 0.6, 1.3);
                    }
                    double lit = (0.10 + 0.90 * light) * shade;
                    r = ((colour >> 16) & 255) * lit;
                    g = ((colour >> 8) & 255) * lit;
                    b = (colour & 255) * lit;
                    if (stage == finalStage && h <= sea) {
                        double spec = Math.pow(Math.max(0.0, nx * hx + ny * hy + nz * hz), 60) * 220;
                        r += spec;
                        g += spec;
                        b += spec;
                    }
                }
                if (live != null && stage < current && touchesStage(live.stageOf(), row, col, n, current)) {
                    boolean first = current == 1;
                    r += (first ? 230 : 140) * pulse;
                    g += (first ? 120 : 210) * pulse;
                    b += (first ? 30 : 255) * pulse;
                }
                if (pregen != null) {
                    int chunkZ = Math.floorDiv((int) Math.floor(((row + 0.5) / n - 0.5) * circumference), 16);
                    int band = Math.floorMod(chunkZ - spawnChunkZ, chunksRound);
                    if (band >= pregenRow) {
                        double dim = band == pregenRow ? 0.55 + 0.25 * pulse : 0.35;
                        double grey = (r + g + b) / 3.0;
                        r = (r * 0.3 + grey * 0.7) * dim;
                        g = (g * 0.3 + grey * 0.7) * dim;
                        b = (b * 0.3 + grey * 0.7) * dim + (band == pregenRow ? 60 * pulse : 0);
                    }
                }

                double rim = Math.pow(1.0 - nz, 3) * 0.7;
                r = r * (1 - rim) + 120 * rim * (0.3 + light);
                g = g * (1 - rim) + 170 * rim * (0.3 + light);
                b = b * (1 - rim) + 255 * rim * (0.3 + light);
                int ir = (int) Math.min(255, r), ig = (int) Math.min(255, g), ib = (int) Math.min(255, b);
                image.setPixel(px, py, edge(0xFF000000 | (ir << 16) | (ig << 8) | ib, coverage, radius, 0.55));
            }
        }
    }

    private static double coverage(double radius, double pixel) {
        return Math.clamp((1.0 - radius) / pixel + 0.5, 0.0, 1.0);
    }

    private static int glow(double radius, double strength) {
        if (radius >= 1.06) return 0;
        double glow = Math.pow(1.0 - Math.max(0.0, radius - 1.0) / 0.06, 2) * strength;
        return ((int) (glow * 255) << 24) | 0x78AAFF;
    }

    private static int edge(int colour, double coverage, double radius, double strength) {
        if (coverage >= 1.0) return colour;
        int glow = glow(radius, strength);
        double glowAlpha = ((glow >>> 24) / 255.0) * (1.0 - coverage);
        double alpha = coverage + glowAlpha;
        if (alpha <= 0.0) return 0;
        int out = (int) Math.round(alpha * 255) << 24;
        for (int shift = 0; shift <= 16; shift += 8) {
            double c = (((colour >> shift) & 255) * coverage + ((glow >> shift) & 255) * glowAlpha) / alpha;
            out |= Math.min(255, (int) Math.round(c)) << shift;
        }
        return out;
    }

    private static boolean touchesStage(byte[] stageOf, int row, int col, int n, int stage) {
        return stageOf[row * n + (col + 1) % n] == stage || stageOf[row * n + Math.floorMod(col - 1, n)] == stage
                || (row > 0 && stageOf[(row - 1) * n + col] == stage) || (row < n - 1 && stageOf[(row + 1) * n + col] == stage);
    }

    private static void captureForTests() {
        if (!Boolean.getBoolean("sphereworld.captureLoading") || Util.getMillis() - lastCapture < 1000) return;
        lastCapture = Util.getMillis();
        Minecraft client = Minecraft.getInstance();
        java.nio.file.Path out = client.gameDirectory.toPath().resolve("screenshots").resolve(String.format("loading_%03d.png", captures++));
        net.minecraft.client.Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
            try (image) {
                java.nio.file.Files.createDirectories(out.getParent());
                image.writeToFile(out);
            } catch (java.io.IOException e) {
                SphereWorld.LOGGER.warn("Could not save {}", out, e);
            }
        });
    }
}
