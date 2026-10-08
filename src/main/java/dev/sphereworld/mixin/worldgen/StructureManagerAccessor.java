package dev.sphereworld.mixin.worldgen;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureManager.class)
public interface StructureManagerAccessor {
    @Accessor("level")
    LevelAccessor sphereworld$level();

    @Accessor("worldOptions")
    WorldOptions sphereworld$worldOptions();

    @Accessor("structureCheck")
    StructureCheck sphereworld$structureCheck();
}
