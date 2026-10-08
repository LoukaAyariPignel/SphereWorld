package dev.sphereworld.client.mixin;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.client.screen.PlanetCustomizeScreen;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldCreationUiState.class)
abstract class WorldCreationUiStateMixin {
    @Unique private static final ResourceKey<WorldPreset> PLANET =
            ResourceKey.create(Registries.WORLD_PRESET, SphereWorld.id("planet"));
    @Unique private static final PresetEditor PLANET_EDITOR = PlanetCustomizeScreen::new;

    @Redirect(method = {"getPresetEditor", "updatePresetLists"}, require = 1,
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object sphereworld$planetEditor(Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> editors, Object key) {
        if (key instanceof Optional<?> optional && optional.isPresent() && PLANET.equals(optional.get())) {
            return PLANET_EDITOR;
        }
        return editors.get(key);
    }
}
