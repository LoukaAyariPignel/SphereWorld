package dev.sphereworld.client;

import dev.sphereworld.client.render.ClientAtlases;
import dev.sphereworld.client.render.PlanetLodRenderer;
import dev.sphereworld.net.PlanetAtlasPayload;
import dev.sphereworld.net.PlanetSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class SphereWorldClient implements ClientModInitializer {
    private static final boolean VOXY = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("voxy");
    private static final net.minecraft.client.KeyMapping PLANET_MAP = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
            new net.minecraft.client.KeyMapping("key.sphereworld.planet_map",
                    com.mojang.blaze3d.platform.InputConstants.KEY_M,
                    net.minecraft.client.KeyMapping.Category.register(dev.sphereworld.SphereWorld.id("planet"))));
    private static @org.jspecify.annotations.Nullable Boolean sentDetail;
    private static boolean mapHintShown;
    private static final net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId MAP_HINT =
            new net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId(8000L);

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
            sentDetail = null;
            ClientPlanets.clear();
            ClientAtlases.clear();
            dev.sphereworld.client.render.DetailMeshes.clear();
            dev.sphereworld.client.render.RemotePlanetRenderer.clear();
            PlanetLodRenderer.close();
        });
        PlanetLodRenderer.init();
        dev.sphereworld.client.dev.SecondScreen.install();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PLANET_MAP.consumeClick()) dev.sphereworld.client.screen.PlanetMapScreen.open(client, PLANET_MAP);
            if (!mapHintShown && client.level != null && client.gui.screen() == null
                    && ClientAtlases.get(client.level.dimension().identifier()) != null) {
                mapHintShown = true;
                net.minecraft.client.gui.components.toasts.SystemToast.add(client.gui.toastManager(), MAP_HINT,
                        net.minecraft.network.chat.Component.translatable("sphereworld.map.title"),
                        net.minecraft.network.chat.Component.translatable("sphereworld.map.toast", PLANET_MAP.getTranslatedKeyMessage()));
            }
            if (client.level != null) {
                dev.sphereworld.client.render.IrisLodSupport.tick(dev.sphereworld.planet.Planets.of(client.level));
                dev.sphereworld.client.render.AtlasMeshes.applyChanges();
                boolean detail = !dev.sphereworld.client.render.IrisLodSupport.shaderPackInUse();
                if (!Boolean.valueOf(detail).equals(sentDetail) && ClientPlayNetworking.canSend(dev.sphereworld.net.PlanetViewPayload.TYPE)) {
                    ClientPlayNetworking.send(new dev.sphereworld.net.PlanetViewPayload(detail, VOXY));
                    sentDetail = detail;
                }
            }
        });
    }
}
