package dev.sphereworld.mixin.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.CarverBiomeCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseBasedChunkGenerator.class)
abstract class NoiseBasedChunkGeneratorMixin {
    @Unique private final CarverBiomeCache sphereworld$carverBiomes = new CarverBiomeCache();

    @WrapOperation(method = "generateCarvers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;getBiomeGenerationSettingsForCarver(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/world/level/biome/BiomeGenerationSettings;"))
    private BiomeGenerationSettings sphereworld$cachedCarverBiome(NoiseBasedChunkGenerator generator, BiomeResolver resolver, ChunkPos sourcePos,
                                                                  Operation<BiomeGenerationSettings> original,
                                                                  @Local(argsOnly = true) RandomState randomState) {
        BiomeGenerationSettings cached = sphereworld$carverBiomes.get(randomState, sourcePos);
        if (cached != null) return cached;
        BiomeGenerationSettings settings = original.call(generator, resolver, sourcePos);
        sphereworld$carverBiomes.put(randomState, sourcePos, settings);
        return settings;
    }
}
