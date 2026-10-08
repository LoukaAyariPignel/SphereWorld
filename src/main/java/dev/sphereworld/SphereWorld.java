package dev.sphereworld;

import dev.sphereworld.command.PlanetCommand;
import dev.sphereworld.dev.DevServerScript;
import dev.sphereworld.worldgen.PlanetChunkGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import dev.sphereworld.atlas.PlanetAtlases;
import dev.sphereworld.net.PlanetAtlasPayload;
import dev.sphereworld.net.PlanetSyncPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SphereWorld implements ModInitializer {
    public static final String MOD_ID = "sphereworld";
    public static final Logger LOGGER = LoggerFactory.getLogger("SphereWorld");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("planet"), PlanetChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("stacked"), dev.sphereworld.worldgen.stacked.StackedChunkGenerator.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PlanetCommand.register(dispatcher));
        PayloadTypeRegistry.clientboundPlay().register(PlanetSyncPayload.TYPE, PlanetSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundConfiguration().register(PlanetSyncPayload.TYPE, PlanetSyncPayload.CODEC);

        net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking.canSend(handler, PlanetSyncPayload.TYPE)) {
                net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking.send(handler, PlanetSyncPayload.of(server));
            }
        });
        PayloadTypeRegistry.clientboundPlay().registerLarge(PlanetAtlasPayload.TYPE, PlanetAtlasPayload.CODEC, PlanetAtlasPayload.MAX_SIZE);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayNetworking.send(handler.player, PlanetSyncPayload.of(server));
            PlanetAtlases.sendAll(handler.player);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(PlanetAtlases::start);
        dev.sphereworld.compat.VoxyPregen.init();
        dev.sphereworld.worldgen.stacked.StackedLayerTracker.init();
        ServerLifecycleEvents.SERVER_STOPPING.register(PlanetAtlases::stop);
        PayloadTypeRegistry.clientboundPlay().register(dev.sphereworld.net.PlanetAtlasUpdatePayload.TYPE, dev.sphereworld.net.PlanetAtlasUpdatePayload.CODEC);

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> PlanetAtlases.markDirty(level, chunk.getPos()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(PlanetAtlases::tick);

        PayloadTypeRegistry.clientboundPlay().registerLarge(dev.sphereworld.net.PlanetDetailPayload.TYPE,
                dev.sphereworld.net.PlanetDetailPayload.CODEC, dev.sphereworld.net.PlanetDetailPayload.MAX_SIZE);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(dev.sphereworld.atlas.PlanetDetail::tick);
        PayloadTypeRegistry.serverboundPlay().register(dev.sphereworld.net.PlanetViewPayload.TYPE, dev.sphereworld.net.PlanetViewPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(dev.sphereworld.net.PlanetViewPayload.TYPE,
                (payload, context) -> dev.sphereworld.atlas.PlanetDetail.view(context.player(), payload));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> dev.sphereworld.atlas.PlanetDetail.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> dev.sphereworld.atlas.PlanetDetail.clear());
        DevServerScript.install();
    }
}
