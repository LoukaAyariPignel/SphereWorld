package dev.sphereworld.client.screen;

import dev.sphereworld.planet.PlanetConfig;
import dev.sphereworld.planet.PlanetLayout;
import dev.sphereworld.worldgen.PlanetChunkGenerator;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedChunkGenerator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class PlanetCustomizeScreen extends Screen {
    private static final int DEFAULT_SIZE = 4096;
    private static final double WALK_SPEED = 4.317;
    private static final double VOXY_CHUNKS_PER_SECOND = 30.0;
    private static final boolean VOXY = FabricLoader.getInstance().isModLoaded("voxy");

    private record Layer(StackBand band, String name, ItemStack icon, int color) {
    }

    private static final List<Layer> LAYERS = List.of(
            new Layer(StackBand.END, "sphereworld.customize.layer.end", new ItemStack(Items.END_STONE), 0xFFD9D49B),
            new Layer(StackBand.OVERWORLD, "sphereworld.customize.layer.overworld", new ItemStack(Items.GRASS_BLOCK), 0xFF6DA544),
            new Layer(StackBand.NETHER, "sphereworld.customize.layer.nether", new ItemStack(Items.NETHERRACK), 0xFF8E3A36));

    private final CreateWorldScreen parent;
    private final PlanetConfig config;
    private int size;

    public PlanetCustomizeScreen(CreateWorldScreen parent, WorldCreationContext context) {
        super(Component.translatable("sphereworld.customize.title"));
        this.parent = parent;
        this.config = context.selectedDimensions().overworld() instanceof PlanetChunkGenerator planet
                ? planet.planetConfig() : PlanetConfig.defaults();
        Integer current = config.circumference(Level.OVERWORLD);
        this.size = current != null && PlanetLayout.OVERWORLD_SIZES.contains(current) ? current : DEFAULT_SIZE;
    }

    @Override
    protected void init() {
        clearWidgets();
        CycleButton<Integer> sizeButton = CycleButton.<Integer>builder(
                        value -> Component.translatable("sphereworld.customize.size.value", grouped(value)), size)
                .withValues(PlanetLayout.OVERWORLD_SIZES)
                .create(width / 2 - 110, 44, 220, 20, Component.translatable("sphereworld.customize.size"),
                        (button, value) -> size = value);
        sizeButton.setTooltip(Tooltip.create(Component.translatable("sphereworld.customize.size.tooltip")));
        addRenderableWidget(sizeButton);

        addRenderableWidget(Button.builder(Component.translatable("sphereworld.customize.reset"), b -> {
            size = DEFAULT_SIZE;
            rebuildWidgets();
        }).bounds(width / 2 - 155, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> apply())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(width / 2 + 55, height - 28, 100, 20).build());
    }

    private PlanetConfig chosenConfig() {
        Map<ResourceKey<Level>, Integer> planets = new LinkedHashMap<>(config.planets());
        planets.put(Level.OVERWORLD, size);
        return new PlanetConfig(planets, config.stack(), config.openBoundaries());
    }

    private void apply() {
        PlanetConfig chosen = chosenConfig();
        parent.getUiState().updateDimensions((registryAccess, dimensions) -> {
            LevelStem stem = dimensions.dimensions().get(LevelStem.OVERWORLD);
            if (stem == null || !(stem.generator() instanceof NoiseBasedChunkGenerator noise)) return dimensions;
            Map<ResourceKey<LevelStem>, LevelStem> map = new LinkedHashMap<>(dimensions.dimensions());
            if (noise instanceof StackedChunkGenerator stacked) {
                map.put(LevelStem.OVERWORLD, new LevelStem(stem.type(), stacked.withConfig(chosen)));
                return new WorldDimensions(map);
            }
            Holder<DimensionType> type = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD);
            Holder<NoiseGeneratorSettings> settings = registryAccess.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
            map.put(LevelStem.OVERWORLD, new LevelStem(type, new PlanetChunkGenerator(noise.getBiomeSource(), settings, PlanetLayout.derive(chosen))));
            return new WorldDimensions(map);
        });
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int centre = width / 2;
        graphics.centeredText(font, title, centre, 12, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("sphereworld.customize.hint").withStyle(ChatFormatting.GRAY), centre, 28, 0xFFFFFFFF);

        int y = 72;
        long radius = Math.round(PlanetLayout.radius(size));
        graphics.centeredText(font, Component.translatable("sphereworld.customize.info.radius", grouped(radius))
                .withStyle(ChatFormatting.GRAY), centre, y, 0xFFFFFFFF);
        y += 11;
        graphics.centeredText(font, Component.translatable("sphereworld.customize.info.walk", duration(size / WALK_SPEED))
                .withStyle(ChatFormatting.GRAY), centre, y, 0xFFFFFFFF);
        if (VOXY) {
            y += 11;
            double chunks = Math.pow(size / 16.0, 2);
            graphics.centeredText(font, Component.translatable("sphereworld.customize.info.voxy", duration(chunks / VOXY_CHUNKS_PER_SECOND))
                    .withStyle(ChatFormatting.GRAY), centre, y, 0xFFFFFFFF);
        }

        y += 20;
        int panelWidth = 220;
        int left = centre - panelWidth / 2;
        int rowHeight = 20;
        graphics.fill(left - 4, y - 4, left + panelWidth + 4, y + LAYERS.size() * rowHeight + 2, 0x66000000);
        for (Layer layer : LAYERS) {
            graphics.fill(left, y, left + 3, y + rowHeight - 2, layer.color);
            graphics.item(layer.icon, left + 7, y + 1);
            graphics.text(font, Component.translatable(layer.name), left + 28, y + 5, 0xFFFFFFFF);
            Component range = Component.translatable("sphereworld.customize.layer.range", layer.band.worldMinY(), layer.band.worldMaxY())
                    .withStyle(ChatFormatting.GRAY);
            graphics.text(font, range, left + panelWidth - font.width(range), y + 5, 0xFFFFFFFF);
            y += rowHeight;
        }
        y += 8;
        graphics.centeredText(font, Component.translatable("sphereworld.customize.layers.hint").withStyle(ChatFormatting.GRAY), centre, y, 0xFFFFFFFF);
    }

    static String grouped(long value) {
        String[] code = net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected().split("_");
        Locale locale = code.length == 2 ? Locale.of(code[0], code[1].toUpperCase(Locale.ROOT)) : Locale.ROOT;
        return String.format(locale, "%,d", value).replace('\u202F', ' ').replace('\u00A0', ' ');
    }

    private static Component duration(double seconds) {
        long minutes = Math.max(1, Math.round(seconds / 60.0));
        if (minutes < 60) return Component.translatable("sphereworld.loading.voxy.eta.minutes", minutes);
        long hours = minutes / 60;
        if (hours < 48) {
            long rest = minutes % 60;
            return rest == 0 || hours >= 10 ? Component.translatable("sphereworld.customize.hours", hours)
                    : Component.translatable("sphereworld.loading.voxy.eta.hours", hours, rest);
        }
        return Component.translatable("sphereworld.customize.days", Math.round(hours / 24.0));
    }
}
