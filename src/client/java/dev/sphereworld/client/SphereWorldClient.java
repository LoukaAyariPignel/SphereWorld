package dev.sphereworld.client;

import dev.sphereworld.client.render.ClientAtlases;
import dev.sphereworld.client.render.PlanetLodRenderer;
import dev.sphereworld.net.PlanetAtlasPayload;
import dev.sphereworld.net.PlanetSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class SphereWorldClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PlanetSyncPayload.TYPE, (payload, context) -> {
            ClientPlanets.accept(payload);
            ClientPlanets.bind(context.client().level);
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking.registerGlobalReceiver(PlanetSyncPayload.TYPE,
                (payload, context) -> ClientPlanets.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(PlanetAtlasPayload.TYPE, (payload, context) -> ClientAtlases.accept(payload.atlas()));
        ClientPlayNetworking.registerGlobalReceiver(dev.sphereworld.net.PlanetAtlasUpdatePayload.TYPE, (payload, context) -> ClientAtlases.update(payload));
        ClientPlayNetworking.registerGlobalReceiver(dev.sphereworld.net.PlanetDetailPayload.TYPE,
                (payload, context) -> dev.sphereworld.client.render.DetailMeshes.accept(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPlanets.clear();
            ClientAtlases.clear();
            dev.sphereworld.client.render.DetailMeshes.clear();
            dev.sphereworld.client.render.RemotePlanetRenderer.clear();
            PlanetLodRenderer.close();
        });
        PlanetLodRenderer.init();
        dev.sphereworld.client.dev.SecondScreen.install();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                dev.sphereworld.client.render.IrisLodSupport.tick(dev.sphereworld.planet.Planets.of(client.level));
            }
        });
    }
}
