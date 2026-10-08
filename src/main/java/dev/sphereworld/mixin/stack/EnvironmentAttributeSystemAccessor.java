package dev.sphereworld.mixin.stack;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnvironmentAttributeSystem.class)
public interface EnvironmentAttributeSystemAccessor {
    @Invoker("addBiomeLayer")
    static void sphereworld$addBiomeLayer(EnvironmentAttributeSystem.Builder builder, HolderLookup<Biome> biomes, BiomeManager biomeManager) {
        throw new AssertionError();
    }
}
