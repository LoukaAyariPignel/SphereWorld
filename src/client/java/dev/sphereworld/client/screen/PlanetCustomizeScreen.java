package dev.sphereworld.client.screen;

import dev.sphereworld.planet.PlanetConfig;
import dev.sphereworld.worldgen.PlanetChunkGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.core.Holder;
import dev.sphereworld.planet.PlanetLayout;
import org.jspecify.annotations.Nullable;

public final class PlanetCustomizeScreen extends Screen {
    private static final List<Integer> SIZES = List.of(256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536);
    private static final int ROW_HEIGHT = 24;

    private final CreateWorldScreen parent;
    private final WorldCreationContext context;

    private final List<Row> rows = new ArrayList<>();
    private boolean openBoundaries;
    private Component geometryNote = Component.empty();
    private int page;

    public PlanetCustomizeScreen(CreateWorldScreen parent, WorldCreationContext context) {
        super(Component.translatable("sphereworld.customize.title"));
        this.parent = parent;
        this.context = context;
        PlanetConfig config = context.selectedDimensions().overworld() instanceof PlanetChunkGenerator planet
                ? planet.planetConfig() : PlanetConfig.defaults();
        this.openBoundaries = config.openBoundaries();

        Map<ResourceKey<Level>, LevelStem> stems = new LinkedHashMap<>();
        context.selectedDimensions().dimensions().forEach((key, stem) -> stems.put(levelKey(key), stem));
        for (LevelStem stem : context.datapackDimensions()) {
            context.datapackDimensions().getResourceKey(stem).ifPresent(key -> stems.putIfAbsent(levelKey(key), stem));
        }

        Set<ResourceKey<Level>> ordered = new LinkedHashSet<>();
        List<ResourceKey<Level>> stack = new ArrayList<>(config.stack());
        java.util.Collections.reverse(stack);
        for (ResourceKey<Level> key : stack) {
            if (stems.containsKey(key)) ordered.add(key);
        }
        ordered.addAll(stems.keySet());
        for (ResourceKey<Level> key : ordered) {
            LevelStem stem = stems.get(key);
            boolean noise = stem.generator() instanceof NoiseBasedChunkGenerator;
            Integer size = noise ? config.circumference(key) : null;
            rows.add(new Row(key, noise, size, config.stack().contains(key)));
        }
    }

    private static ResourceKey<Level> levelKey(ResourceKey<LevelStem> stem) {
        return ResourceKey.create(Registries.DIMENSION, stem.identifier());
    }

    private int rowsPerPage() {
        return Math.max(1, (height - 64 - 56) / ROW_HEIGHT);
    }

    @Override
    protected void init() {
        clearWidgets();
        int perPage = rowsPerPage();
        int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int left = width / 2 - 200;
        int y = 64;
        PlanetConfig preview = PlanetLayout.derive(buildConfig());
        for (int i = page * perPage; i < Math.min(rows.size(), (page + 1) * perPage); i++) {
            Row row = rows.get(i);
            int index = i;

            boolean overworld = row.key.equals(Level.OVERWORLD);
            boolean derived = PlanetLayout.isDerived(preview, row.key);
            List<Integer> values = overworld ? PlanetLayout.OVERWORLD_SIZES : sizesWithFlat();
            Integer current = derived ? preview.circumference(row.key) : row.size;
            int initial = current == null ? (overworld ? 4096 : 0) : current;
            if (!values.contains(initial)) {
                values = new ArrayList<>(values);
                values.add(initial);
            }
            CycleButton<Integer> size = CycleButton.<Integer>builder(value -> value == 0
                            ? Component.translatable("sphereworld.customize.flat")
                            : derived ? Component.translatable("sphereworld.customize.auto", String.format(Locale.ROOT, "%,d", value))
                            : Component.literal(String.format(Locale.ROOT, "%,d", value)), initial)
                    .withValues(values)
                    .create(left + 140, y, 110, 20, Component.translatable("sphereworld.customize.size"),
                            (button, value) -> {
                                row.size = value == 0 ? null : value;
                                if (overworld) rebuildWidgets();
                            });
            size.active = row.noise && !derived;
            String tooltip = !row.noise ? "sphereworld.customize.size.unsupported"
                    : derived ? "sphereworld.customize.size.derived"
                    : overworld ? "sphereworld.customize.size.overworld" : "sphereworld.customize.size.tooltip";
            size.setTooltip(Tooltip.create(Component.translatable(tooltip)));
            addRenderableWidget(size);

            CycleButton<Boolean> stacked = CycleButton.onOffBuilder(row.stacked)
                    .create(left + 254, y, 96, 20, Component.translatable("sphereworld.customize.stacked"),
                            (button, value) -> {
                                row.stacked = value;
                                sortRows();
                                rebuildWidgets();
                            });
            stacked.setTooltip(Tooltip.create(Component.translatable("sphereworld.customize.stacked.tooltip")));
            addRenderableWidget(stacked);

            Button up = Button.builder(Component.literal("▲"), b -> move(index, -1)).bounds(left + 354, y, 20, 20).build();
            Button down = Button.builder(Component.literal("▼"), b -> move(index, 1)).bounds(left + 378, y, 20, 20).build();
            up.active = row.stacked && index > 0 && rows.get(index - 1).stacked;
            down.active = row.stacked && index + 1 < rows.size() && rows.get(index + 1).stacked;
            addRenderableWidget(up);
            addRenderableWidget(down);
            y += ROW_HEIGHT;
        }

        int footer = height - 52;
        Integer overworldSize = preview.circumference(Level.OVERWORLD);
        geometryNote = overworldSize == null ? Component.empty()
                : Level.NETHER.equals(preview.below(Level.OVERWORLD)) && PlanetLayout.netherFits(overworldSize)
                ? Component.translatable("sphereworld.customize.geometry.exact", PlanetLayout.coreRadius(overworldSize))
                : Component.translatable("sphereworld.customize.geometry.gap");
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                page = Math.max(0, page - 1);
                rebuildWidgets();
            }).bounds(width / 2 - 200, footer, 20, 20).build()).active = page > 0;
            addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                page = Math.min(pages - 1, page + 1);
                rebuildWidgets();
            }).bounds(width / 2 + 180, footer, 20, 20).build()).active = page < pages - 1;
        }
        CycleButton<Boolean> open = CycleButton.onOffBuilder(openBoundaries)
                .create(width / 2 - 155, footer, 310, 20, Component.translatable("sphereworld.customize.open_boundaries"),
                        (button, value) -> openBoundaries = value);
        open.setTooltip(Tooltip.create(Component.translatable("sphereworld.customize.open_boundaries.tooltip")));
        addRenderableWidget(open);

        addRenderableWidget(Button.builder(Component.translatable("sphereworld.customize.reset"), b -> reset())
                .bounds(width / 2 - 155, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> apply())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(width / 2 + 55, height - 28, 100, 20).build());
    }

    private static List<Integer> sizesWithFlat() {
        List<Integer> values = new ArrayList<>();
        values.add(0);
        values.addAll(SIZES);
        return values;
    }

    private void move(int index, int direction) {
        int other = index + direction;
        if (other < 0 || other >= rows.size()) return;
        Row moved = rows.remove(index);
        rows.add(other, moved);
        rebuildWidgets();
    }

    private void sortRows() {
        List<Row> stacked = rows.stream().filter(r -> r.stacked).toList();
        List<Row> others = rows.stream().filter(r -> !r.stacked).toList();
        rows.clear();
        rows.addAll(stacked);
        rows.addAll(others);
    }

    private void reset() {
        PlanetConfig defaults = PlanetConfig.defaults();
        List<ResourceKey<Level>> stack = new ArrayList<>(defaults.stack());
        java.util.Collections.reverse(stack);
        rows.sort((a, b) -> Integer.compare(rank(stack, a.key), rank(stack, b.key)));
        for (Row row : rows) {
            row.size = row.noise ? defaults.circumference(row.key) : null;
            row.stacked = defaults.stack().contains(row.key);
        }
        openBoundaries = defaults.openBoundaries();
        rebuildWidgets();
    }

    private static int rank(List<ResourceKey<Level>> stack, ResourceKey<Level> key) {
        int index = stack.indexOf(key);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private PlanetConfig buildConfig() {
        Map<ResourceKey<Level>, Integer> planets = new LinkedHashMap<>();
        List<ResourceKey<Level>> stack = new ArrayList<>();
        for (Row row : rows) {
            if (row.size != null) planets.put(row.key, row.size);
            if (row.stacked) stack.add(row.key);
        }
        java.util.Collections.reverse(stack);
        return new PlanetConfig(planets, stack, openBoundaries);
    }

    private void apply() {
        PlanetConfig config = PlanetLayout.derive(buildConfig());
        parent.getUiState().updateDimensions((registryAccess, dimensions) -> {
            LevelStem stem = dimensions.dimensions().get(LevelStem.OVERWORLD);
            if (stem == null || !(stem.generator() instanceof NoiseBasedChunkGenerator noise)) return dimensions;
            if (noise instanceof dev.sphereworld.worldgen.stacked.StackedChunkGenerator stacked) {
                Map<ResourceKey<LevelStem>, LevelStem> map = new LinkedHashMap<>(dimensions.dimensions());
                map.put(LevelStem.OVERWORLD, new LevelStem(stem.type(), stacked.withConfig(config)));
                return new WorldDimensions(map);
            }
            var dimensionTypes = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE);
            var noiseSettings = registryAccess.lookupOrThrow(Registries.NOISE_SETTINGS);

            Holder<DimensionType> type = dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
            Holder<NoiseGeneratorSettings> settings = noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
            Map<ResourceKey<LevelStem>, LevelStem> map = new LinkedHashMap<>(dimensions.dimensions());
            map.put(LevelStem.OVERWORLD, new LevelStem(type,
                    new PlanetChunkGenerator(noise.getBiomeSource(), settings, config)));
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
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("sphereworld.customize.hint").withStyle(ChatFormatting.GRAY),
                width / 2, 28, 0xFFFFFFFF);
        int left = width / 2 - 200;
        graphics.text(font, Component.translatable("sphereworld.customize.column.dimension")
                .withStyle(ChatFormatting.UNDERLINE), left, 50, 0xFFFFFFFF);
        int perPage = rowsPerPage();
        int y = 64;
        int stackedCount = (int) rows.stream().filter(r -> r.stacked).count();
        for (int i = page * perPage; i < Math.min(rows.size(), (page + 1) * perPage); i++) {
            Row row = rows.get(i);
            Component name = displayName(row.key);
            if (row.stacked) {
                int layer = stackedCount - 1 - i;
                String tag = layer == 0 ? "sphereworld.customize.layer.centre"
                        : i == 0 ? "sphereworld.customize.layer.outer" : "sphereworld.customize.layer.middle";
                graphics.text(font, name, left, y + 3, 0xFFFFFFFF);
                graphics.text(font, Component.translatable(tag).withStyle(ChatFormatting.AQUA), left, y + 12, 0xFFFFFFFF);
            } else {
                graphics.text(font, name.copy().withStyle(ChatFormatting.GRAY), left, y + 6, 0xFFFFFFFF);
            }
            y += ROW_HEIGHT;
        }
        graphics.centeredText(font, geometryNote.copy().withStyle(ChatFormatting.GRAY), width / 2, height - 76, 0xFFFFFFFF);
        if (stackedCount == 1) {
            graphics.centeredText(font, Component.translatable("sphereworld.customize.single_layer")
                    .withStyle(ChatFormatting.YELLOW), width / 2, height - 64, 0xFFFFFFFF);
        }
    }

    static Component displayName(ResourceKey<Level> key) {
        Identifier id = key.identifier();
        String translationKey = id.toLanguageKey("dimension");
        if (Language.getInstance().has(translationKey)) return Component.translatable(translationKey);
        String pretty = String.join(" ", Arrays.stream(id.getPath().split("[_/]"))
                .filter(s -> !s.isEmpty())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1)).toList());
        return id.getNamespace().equals("minecraft") ? Component.literal(pretty)
                : Component.literal(pretty + " (" + id.getNamespace() + ")");
    }

    private static final class Row {
        final ResourceKey<Level> key;
        final boolean noise;
        @Nullable Integer size;
        boolean stacked;

        Row(ResourceKey<Level> key, boolean noise, @Nullable Integer size, boolean stacked) {
            this.key = key;
            this.noise = noise;
            this.size = size;
            this.stacked = stacked;
        }
    }
}
