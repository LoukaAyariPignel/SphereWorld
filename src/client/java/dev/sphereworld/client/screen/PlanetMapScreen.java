package dev.sphereworld.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.atlas.PlanetAtlas;
import dev.sphereworld.client.render.ClientAtlases;
import dev.sphereworld.client.render.PlanetLoadingView;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PlanetMapScreen extends Screen {
    private static final Identifier TEXTURE = SphereWorld.id("planet_map");
    private static final int MAX_RESOLUTION = 512;
    private static final double MAX_EXTENT = 1.15;
    private static final double MIN_EXTENT = 0.18;
    private static final long REFRESH_MILLIS = 2000;

    private final KeyMapping key;
    private final PlanetGeometry geometry;
    private final PlanetAtlas atlas;
    private double lon;
    private double lat;
    private double extent = MAX_EXTENT;
    private @Nullable DynamicTexture texture;
    private int resolution;
    private boolean dirty = true;
    private long painted;

    private PlanetMapScreen(KeyMapping key, PlanetGeometry geometry, PlanetAtlas atlas, Vec3 player) {
        super(Component.translatable("sphereworld.map.title"));
        this.key = key;
        this.geometry = geometry;
        this.atlas = atlas;
        this.lon = lonOf(player.x);
        this.lat = latOf(player.z);
    }

    public static void open(Minecraft client, KeyMapping key) {
        if (client.level == null || client.player == null) return;
        PlanetGeometry geometry = Planets.of(client.level);
        PlanetAtlas atlas = geometry == null ? null : ClientAtlases.get(client.level.dimension().identifier());
        if (atlas == null) return;
        client.gui.setScreen(new PlanetMapScreen(key, geometry, atlas, client.player.position()));
    }

    private double lonOf(double x) {
        return 2.0 * Math.PI * geometry.canonical(x) / atlas.circumference();
    }

    private double latOf(double z) {
        return -Math.PI * geometry.canonical(z) / atlas.circumference();
    }

    private int square() {
        return Math.max(64, Math.min(width - 32, height - 64));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        PlanetLoadingView.extractBackground(graphics, width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int square = square();
        int guiScale = (int) Math.max(1, Math.round(minecraft.getWindow().getGuiScale()));
        int wanted = Math.min(MAX_RESOLUTION, square * guiScale);
        if (texture == null || resolution != wanted) {
            if (texture != null) minecraft.getTextureManager().release(TEXTURE);
            resolution = wanted;
            texture = new DynamicTexture(() -> "SphereWorld planet map", resolution, resolution, true) {
                {
                    this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                }
            };
            minecraft.getTextureManager().register(TEXTURE, texture);
            dirty = true;
        }
        if (dirty || Util.getMillis() - painted > REFRESH_MILLIS) {
            PlanetLoadingView.paint(texture.getPixels(), resolution, atlas, null, lon, lat, extent, null, null);
            texture.upload();
            dirty = false;
            painted = Util.getMillis();
        }
        int left = (width - square) / 2;
        int top = 26;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0, 0, square, square, resolution, resolution, resolution, resolution);

        int centreX = width / 2;
        int centreY = top + square / 2;
        double radius = square / (2.0 * extent);
        graphics.enableScissor(left, top, left + square, top + square);
        if (minecraft.level != null) {
            BlockPos spawn = minecraft.level.getRespawnData().pos();
            marker(graphics, lonOf(spawn.getX() + 0.5), latOf(spawn.getZ() + 0.5), centreX, centreY, radius, 0xFFFFC94A, false);
        }
        if (minecraft.player != null) {
            marker(graphics, lonOf(minecraft.player.getX()), latOf(minecraft.player.getZ()), centreX, centreY, radius, 0xFFFFFFFF, true);
        }
        graphics.disableScissor();

        graphics.centeredText(font, title, centreX, 9, 0xFFFFFFFF);
        info(graphics, centreX, top + square + 6);
    }

    private void info(GuiGraphicsExtractor graphics, int centreX, int y) {
        if (minecraft.player == null || minecraft.level == null) return;
        Vec3 shown = new Vec3(geometry.canonical(minecraft.player.getX()), minecraft.player.getY(), geometry.canonical(minecraft.player.getZ()));
        Component position = Component.translatable("sphereworld.map.position",
                Mth.floor(shown.x), Mth.floor(shown.y), Mth.floor(shown.z));
        if (StackedAmbience.isStackedType(minecraft.level)) {
            StackBand band = StackBand.at(Mth.floor(shown.y));
            position = position.copy().append(" · ").append(Component.translatable("sphereworld.customize.layer." + band.name().toLowerCase(Locale.ROOT)));
        }
        graphics.centeredText(font, position, centreX, y, 0xFFFFFFFF);

        BlockPos spawn = minecraft.level.getRespawnData().pos();
        double dx = geometry.delta(shown.x, spawn.getX() + 0.5);
        double dz = geometry.delta(shown.z, spawn.getZ() + 0.5);
        Component around = Component.translatable("sphereworld.map.around",
                PlanetCustomizeScreen.grouped(geometry.circumference()),
                PlanetCustomizeScreen.grouped(Math.round(Math.sqrt(dx * dx + dz * dz))));
        graphics.centeredText(font, around.copy().withStyle(ChatFormatting.GRAY), centreX, y + 11, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("sphereworld.map.hint", key.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.DARK_GRAY), centreX, y + 22, 0xFFFFFFFF);
    }

    private void marker(GuiGraphicsExtractor graphics, double pointLon, double pointLat, int centreX, int centreY, double radius,
                        int color, boolean pulse) {
        double cosLat = Math.cos(pointLat);
        double x2 = cosLat * Math.sin(pointLon);
        double y1 = Math.sin(pointLat);
        double z2 = cosLat * Math.cos(pointLon);
        double nx = x2 * Math.cos(lon) - z2 * Math.sin(lon);
        double z1 = x2 * Math.sin(lon) + z2 * Math.cos(lon);
        double ny = y1 * Math.cos(lat) - z1 * Math.sin(lat);
        double nz = y1 * Math.sin(lat) + z1 * Math.cos(lat);
        if (nz <= 0.02) return;
        int x = centreX + (int) Math.round(nx * radius);
        int y = centreY - (int) Math.round(ny * radius);
        if (pulse) {
            int ring = 4 + (int) ((Util.getMillis() / 120) % 6);
            graphics.outline(x - ring, y - ring, ring * 2 + 1, ring * 2 + 1, (Math.max(0, 200 - ring * 30) << 24) | (color & 0xFFFFFF));
        }
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFF101010);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double radius = square() / (2.0 * extent);
        lon -= dx / radius;
        lat = Math.clamp(lat + dy / radius, -Math.PI / 2, Math.PI / 2);
        dirty = true;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        extent = Math.clamp(extent * Math.pow(0.85, scrollY), MIN_EXTENT, MAX_EXTENT);
        dirty = true;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (key.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (texture != null) minecraft.getTextureManager().release(TEXTURE);
        texture = null;
        super.removed();
    }
}
