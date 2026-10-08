package dev.sphereworld.test;

import dev.sphereworld.SphereWorld;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public final class SphereWorldClientTest implements FabricClientGameTest {
    private static final ResourceKey<WorldPreset> PLANET = ResourceKey.create(Registries.WORLD_PRESET, SphereWorld.id("planet"));

    @Override
    public void runTest(ClientGameTestContext context) {
        List<String> scenarios = Arrays.asList(System.getProperty("sphereworld.scenarios", "ui,world").split(","));
        if (scenarios.contains("ui")) customizeScreen(context);
        if (scenarios.contains("world")) planetWorld(context);
        if (scenarios.contains("seam")) seamWalk(context);
        if (scenarios.contains("view")) curvedView(context);
        if (scenarios.contains("mp")) multiplayerSeam(context);
        if (scenarios.contains("endview")) endView(context);
        if (scenarios.contains("packfar")) packFar(context);
        if (scenarios.contains("clouds")) clouds(context);
        if (scenarios.contains("lodupdate")) lodUpdate(context);
        if (scenarios.contains("savedworld")) {
            context.runOnClient(client -> client.options.renderDistance().set(Integer.getInteger("sphereworld.testRenderDistance", 12)));
            context.runOnClient(client -> client.createWorldOpenFlows().openWorld("SavedWorld", () -> {}));
            context.waitTicks(100);
            log("savedworld: screen " + context.computeOnClient(client -> client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName()));
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof net.minecraft.client.gui.screens.BackupConfirmScreen screen) {
                    for (var child : screen.children()) {
                        if (child instanceof net.minecraft.client.gui.components.Button button
                                && button.getMessage().getString().contains("Know")) {
                            button.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0));
                            return;
                        }
                    }
                }
            });
            context.waitFor(client -> client.level != null && client.player != null && client.gui.screen() == null, 3000);
            context.waitTicks(600);
            context.takeScreenshot("savedworld");
            context.runOnClient(client -> client.player.setXRot(90.0F));
            context.waitTicks(40);
            context.takeScreenshot("savedworld_down");
            if (Boolean.getBoolean("sphereworld.savedWorldNight")) {
                var server = context.computeOnClient(client -> client.getSingleplayerServer());
                server.execute(() -> {
                    var commands = server.getCommands();
                    var source = server.createCommandSourceStack();
                    commands.performPrefixedCommand(source, "gamemode spectator @a");
                    commands.performPrefixedCommand(source, "time set 14000");
                    commands.performPrefixedCommand(source, "gamerule advance_time false");
                    commands.performPrefixedCommand(source, "tp @a 0 250 0 0 45");
                });
                context.waitTicks(400);
                for (int yaw = 0; yaw < 360; yaw += 90) {
                    int y = yaw;
                    context.runOnClient(client -> client.player.setYRot(y));
                    context.waitTicks(60);
                    context.takeScreenshot("savedworld_night_" + yaw);
                }
            }
            context.runOnClient(client -> client.disconnectFromWorld(net.minecraft.network.chat.Component.empty()));
            context.waitTicks(40);
        }
        if (scenarios.contains("voxypregen")) {
            context.runOnClient(client -> client.options.renderDistance().set(6));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runCommand("gamemode spectator @a");
                world.getServer().runCommand("time set noon");
                world.getServer().runCommand("weather clear");
                world.getServer().runCommand("gamerule advance_time false");
                world.getServer().runCommand("chunky center 0 0");
                world.getServer().runCommand("chunky radius 640");
                world.getServer().runCommand("chunky start");
                context.waitTicks(20 * 150);
                world.getServer().runCommand("tp @a 0 300 0 0 35");
                context.waitTicks(400);
                context.takeScreenshot("voxy_pregen_high");
                world.getServer().runCommand("tp @a 0 120 0 45 8");
                context.waitTicks(300);
                context.takeScreenshot("voxy_pregen_ground");
            }
        }
        if (scenarios.contains("stacked")) {
            context.runOnClient(client -> client.options.renderDistance().set(10));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runOnServer(server -> System.out.println("World lifecycle: " + server.getWorldData().worldGenSettingsLifecycle()));
                world.getServer().runCommand("gamemode spectator @a");
                world.getServer().runCommand("time set noon");
                world.getServer().runCommand("weather clear");
                world.getServer().runCommand("gamerule advance_time false");
                world.getServer().runCommand("tp @a 40 120 40 0 -35");
                context.waitTicks(300);
                context.takeScreenshot("stacked_sky_from_ground");
                world.getServer().runCommand("fill 24 -110 24 39 -40 39 air");
                world.getServer().runCommand("fill 24 -39 24 39 30 39 air");
                world.getServer().runCommand("fill 24 31 24 39 140 39 air");
                world.getServer().runCommand("tp @a 32 -105 32 0 -90");
                context.waitTicks(300);
                context.takeScreenshot("stacked_nether_hole");
                world.getServer().runCommand("tp @a 32 -120 32 0 10");
                context.waitTicks(60);
                context.takeScreenshot("stacked_nether");
                world.getServer().runCommand("tp @a 300 420 300 0 50");
                context.waitTicks(300);
                context.takeScreenshot("stacked_from_end");
            }
        }
        if (scenarios.contains("layers")) {
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                var server = world.getServer();
                server.runCommand("gamemode survival @a");
                server.runCommand("effect give @a resistance infinite 255 true");
                server.runCommand("effect give @a fire_resistance infinite 255 true");
                server.runCommand("effect give @a slow_falling infinite 1 true");
                context.waitTicks(40);
                server.runCommand("fill 0 -101 0 4 -101 4 obsidian");
                server.runCommand("fill 0 -100 0 4 -97 4 air");
                server.runCommand("tp @a 2 -100 2");
                context.waitTicks(40);
                server.runCommand("execute if entity @a[advancements={minecraft:story/enter_the_nether=true}] run say PASS entering the Nether layer is entering the Nether");
                server.runCommand("fill 0 449 0 4 449 4 obsidian");
                server.runCommand("tp @a 2 450 2");
                context.waitTicks(40);
                server.runCommand("execute if entity @a[advancements={minecraft:story/enter_the_end=true}] run say PASS reaching the End layer is entering the End");
                server.runCommand("fill 0 -101 0 4 -101 4 obsidian");
                server.runCommand("tp @a 2 -100 2");
                context.waitTicks(20);
                server.runCommand("tp @a 2 -100 1000");
                context.waitTicks(20);
                server.runCommand("fill 0 199 1000 4 199 1004 stone");
                server.runCommand("tp @a 2 200 1002");
                context.waitTicks(40);
                server.runCommand("execute if entity @a[advancements={minecraft:nether/fast_travel=true}] run say PASS 1000 blocks in the Nether layer is Subspace Bubble");
            }
        }
        if (scenarios.contains("voxysoak")) {
            System.setProperty("sphereworld.voxyPregenOnDemand", "true");
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runOnServer(dev.sphereworld.compat.VoxyPregen::startNow);
                for (int minute = 0; minute < 6; minute++) {
                    context.waitTicks(1200);
                    world.getServer().runOnServer(server -> {
                        System.gc();
                        Runtime r = Runtime.getRuntime();
                        System.out.println("SOAK heap after GC: " + (r.totalMemory() - r.freeMemory()) / (1024 * 1024) + " MB of "
                                + r.maxMemory() / (1024 * 1024) + " MB, loaded " + server.overworld().getChunkSource().getLoadedChunksCount()
                                + ", progress " + dev.sphereworld.compat.VoxyPregen.progress());
                    });
                }
            }
            System.clearProperty("sphereworld.voxyPregenOnDemand");
        }
        if (scenarios.contains("voxyescape")) {
            Thread presser = new Thread(() -> {
                try {
                    while (dev.sphereworld.compat.VoxyPregen.progress() == null || dev.sphereworld.compat.VoxyPregen.progress().done() < 200) {
                        Thread.sleep(500);
                    }
                    net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                    client.execute(() -> {
                        var screen = client.gui.screen();
                        System.out.println("ESCAPE pressed on " + screen + ", progress " + dev.sphereworld.compat.VoxyPregen.progress()
                                + ", HUD hidden " + dev.sphereworld.client.render.PlanetLoadingView.active());
                        if (screen != null) screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 0, 0));
                    });
                } catch (InterruptedException ignored) {
                }
            }, "voxyescape-presser");
            presser.setDaemon(true);
            presser.start();
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runOnServer(server -> System.out.println("ESCAPE result: active=" + dev.sphereworld.compat.VoxyPregen.active()
                        + ", frozen=" + server.tickRateManager().isFrozen() + ", loaded " + server.overworld().getChunkSource().getLoadedChunksCount()));
                long before = world.getServer().computeOnServer(server -> server.overworld().getGameTime());
                context.waitTicks(40);
                long after = world.getServer().computeOnServer(server -> server.overworld().getGameTime());
                System.out.println((after > before ? "PASS" : "FAIL") + " the world runs after Escape (" + before + " -> " + after + ")");
            }
        }
        if (scenarios.contains("voxyfull")) {
            System.setProperty("sphereworld.captureLoading", "true");
            context.runOnClient(client -> client.options.renderDistance().set(6));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                System.clearProperty("sphereworld.captureLoading");
                world.getServer().runOnServer(server -> {
                    var file = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/sphereworld_voxy_pregen_rows.txt");
                    String saved;
                    try {
                        saved = java.nio.file.Files.readString(file).trim();
                    } catch (java.io.IOException e) {
                        saved = "missing";
                    }
                    System.out.println("Voxy pregen saved ring: " + saved + ", frozen=" + server.tickRateManager().isFrozen()
                            + ", active=" + dev.sphereworld.compat.VoxyPregen.active());
                });
                world.getServer().runCommand("time set noon");
                world.getServer().runCommand("tp @a 0 260 0 0 40");
                context.waitTicks(300);
                context.takeScreenshot("voxyfull_after");
            }
            System.clearProperty("sphereworld.captureLoading");
        }
        if (scenarios.contains("views")) {
            context.runOnClient(client -> client.options.renderDistance().set(Integer.getInteger("sphereworld.testRenderDistance", 12)));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                var server = world.getServer();
                server.runCommand("gamemode spectator @a");
                server.runCommand("weather clear");
                server.runCommand("time set 13000");
                server.runCommand("gamerule advance_time false");
                server.runCommand("tp @a 60 230 60 135 40");
                context.waitTicks(500);
                context.takeScreenshot("ub_dusk_above");
                server.runCommand("time set 6000");
                context.waitTicks(40);
                context.takeScreenshot("ub_noon_above");
                server.runCommand("tp @a 0.5 200 0.5 0 -70");
                context.waitTicks(300);
                context.takeScreenshot("ub_end_from_below");
                server.runCommand("tp @a -40 412 28 -125 25");
                context.waitTicks(300);
                context.takeScreenshot("ub_end_fight");
                server.runCommand("tp @a 24 397 -15 -60 10");
                context.waitTicks(100);
                context.takeScreenshot("ub_end_island");
                server.runCommand("tp @a 12 367 39 0 70");
                context.waitTicks(200);
                context.takeScreenshot("ub_end_down");

                server.runCommand("kill @e[type=ender_dragon]");
                context.waitTicks(300);
                server.runOnServer(s -> {
                    var level = s.overworld();
                    for (int y = 575; y > 320; y--) {
                        if (level.getBlockState(new net.minecraft.core.BlockPos(0, y, 0)).is(net.minecraft.world.level.block.Blocks.END_PORTAL)
                                || level.getBlockState(new net.minecraft.core.BlockPos(1, y, 0)).is(net.minecraft.world.level.block.Blocks.END_PORTAL)) {
                            BlockPosHolder.portal = new net.minecraft.core.BlockPos(0, y, 0);
                            break;
                        }
                    }
                    System.out.println("RITUAL portal at " + BlockPosHolder.portal);
                });
                net.minecraft.core.BlockPos portal = BlockPosHolder.portal == null ? new net.minecraft.core.BlockPos(0, 384, 0) : BlockPosHolder.portal;
                int py = portal.getY() + 1;
                server.runCommand("summon end_crystal 2.5 " + py + " 0.5");
                server.runCommand("summon end_crystal -1.5 " + py + " 0.5");
                server.runCommand("summon end_crystal 0.5 " + py + " 2.5");
                server.runCommand("summon end_crystal 0.5 " + py + " -1.5");
                server.runOnServer(s -> s.overworld().getDragonFight().tryRespawn());
                context.waitTicks(120);
                server.runCommand("execute as @e[type=end_crystal] run data get entity @s beam_target");
                server.runCommand("tp @a 12 367 39 0 70");
                context.waitTicks(40);
                context.takeScreenshot("ub_ritual_down");
                server.runCommand("tp @a 60 420 60 135 10");
                context.waitTicks(40);
                context.takeScreenshot("ub_ritual_side");
                server.runCommand("tp @a 30 200 30 135 -40");
                context.waitTicks(40);
                context.takeScreenshot("ub_ritual_from_overworld");

                context.runOnClient(client -> client.options.renderDistance().set(2));
                server.runCommand("tp @a 120 200 0.5 90 -50");
                context.waitTicks(200);
                context.takeScreenshot("ub_end_lod_from_below");
            }
        }
        if (scenarios.contains("lap")) {
            context.runOnClient(client -> client.options.renderDistance().set(Integer.getInteger("sphereworld.testRenderDistance", 12)));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                var server = world.getServer();
                server.runCommand("gamemode spectator @a");
                server.runCommand("weather clear");
                server.runCommand("time set 14500");
                server.runCommand("gamerule advance_time false");
                server.runCommand("tp @a 60 260 60 135 45");
                context.waitTicks(400);
                context.takeScreenshot("lap_before");
                int size = world.getServer().computeOnServer(s -> dev.sphereworld.planet.Planets.of(s.overworld()).circumference());
                int step = Integer.getInteger("sphereworld.lapStep", 128);
                for (int x = 60 - step; x > 60 - size; x -= step) {
                    server.runCommand("tp @a " + x + " 260 60 90 45");
                    context.waitTicks(Math.max(2, step / 8));
                }
                server.runCommand("tp @a 60 260 60 135 45");
                context.waitTicks(400);
                log("lap: client at " + context.computeOnClient(client -> client.player.position()));

                String shown = context.computeOnClient(client -> {
                    StringBuilder lines = new StringBuilder();
                    var collector = new net.minecraft.client.gui.components.debug.DebugScreenDisplayer() {
                        @Override public void addPriorityLine(String line) { lines.append(line).append(" | "); }
                        @Override public void addLine(String line) { lines.append(line).append(" | "); }
                        @Override public void addToGroup(net.minecraft.resources.Identifier group, java.util.Collection<String> group2) { group2.forEach(this::addLine); }
                        @Override public void addToGroup(net.minecraft.resources.Identifier group, String line) { addLine(line); }
                    };
                    new net.minecraft.client.gui.components.debug.DebugEntryPosition().display(collector, client.level, null, null);
                    try {
                        Object coords = Class.forName("me.flashyreese.mods.sodiumextra.client.gui.SodiumExtraDebugEntryCoords").getConstructor().newInstance();
                        ((net.minecraft.client.gui.components.debug.DebugScreenEntry) coords).display(collector, client.level, null, null);
                    } catch (ReflectiveOperationException e) {
                        lines.append("(no Sodium Extra)");
                    }
                    return lines.toString();
                });
                log("lap: shown " + shown);
                context.takeScreenshot("lap_after");
            }
        }
        if (scenarios.contains("endbottom")) {
            String seed = "6521071371443075479";
            String[] result = new String[2];
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(state -> {
                selectPlanet(state);
                state.setSeed(seed);
            }).create()) {
                result[0] = world.getServer().computeOnServer(s -> endBottoms(s.overworld(), StackedOffset.END));
            }
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(state -> state.setSeed(seed)).create()) {
                result[1] = world.getServer().computeOnServer(s -> endBottoms(s.getLevel(net.minecraft.world.level.Level.END), 0));
            }
            log("endbottom: planet " + result[0] + "; vanilla " + result[1]);
            log((result[0].equals(result[1]) ? "PASS" : "FAIL") + " the End layer's outer islands match the vanilla End");
        }
        if (scenarios.contains("snowcover")) {
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(state -> {
                selectPlanet(state);
                state.setSeed("6521071371443075479");
            }).create()) {
                String result = world.getServer().computeOnServer(s -> {
                    var level = s.overworld();
                    int snowy = 0, bare = 0;
                    StringBuilder map = new StringBuilder();
                    for (int cz = -12; cz < -2; cz++) {
                        map.append(" | ");
                        for (int cx = 26; cx < 46; cx++) {
                            var chunk = level.getChunk(cx, cz);
                            int x = cx * 16 + 8, z = cz * 16 + 8;
                            int y = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
                            var top = level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                            boolean cold = level.getBiome(new net.minecraft.core.BlockPos(x, y, z)).value().coldEnoughToSnow(new net.minecraft.core.BlockPos(x, y, z), level.getSeaLevel());
                            var above = level.getBlockState(new net.minecraft.core.BlockPos(x, y + 1, z));

                            boolean covered = !(top.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && !above.is(net.minecraft.world.level.block.Blocks.SNOW));
                            if (cold) {
                                if (covered) snowy++;
                                else bare++;
                            }
                            map.append(!cold ? '.' : covered ? '#' : 'X');
                        }
                    }
                    return snowy + " snowy chunks, " + bare + " bare" + map;
                });
                log("snowcover: " + result);
            }
        }
        if (scenarios.contains("crater")) {
            context.runOnClient(client -> client.options.renderDistance().set(Integer.getInteger("sphereworld.testRenderDistance", 12)));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runCommand("gamemode spectator @a");
                world.getServer().runCommand("time set noon");
                world.getServer().runCommand("gamerule advance_time false");
                world.getServer().runCommand("tp @a 0 300 0 0 90");
                context.waitTicks(400);
                context.takeScreenshot("crater_down");
                world.getServer().runCommand("tp @a 0 300 0 0 55");
                context.waitTicks(60);
                context.takeScreenshot("crater_oblique");
                world.getServer().runCommand("tp @a 700 320 700 0 90");
                context.waitTicks(400);
                context.takeScreenshot("crater_far_down");
                world.getServer().runCommand("tp @a 700 320 700 0 50");
                context.waitTicks(60);
                context.takeScreenshot("crater_far_oblique");
            }
        }
        if (scenarios.contains("curveall")) {
            context.runOnClient(client -> client.options.renderDistance().set(12));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                var server = world.getServer();
                server.runCommand("gamemode creative @a");
                server.runCommand("time set noon");
                server.runCommand("weather clear");
                server.runCommand("gamerule advance_time false");
                server.runCommand("gamerule spawn_mobs false");
                server.runCommand("fill 0 150 0 160 150 8 minecraft:stone");
                server.runCommand("fill 150 151 2 154 151 6 minecraft:end_portal");
                server.runCommand("fill 120 151 2 122 151 4 minecraft:end_portal");
                for (int x = 60; x <= 150; x += 30) {
                    server.runCommand("summon minecraft:cow " + x + " 151 6 {NoAI:1b}");
                }
                server.runCommand("summon minecraft:armor_stand 100 151 2 {Glowing:1b,NoGravity:1b}");
                server.runCommand("setblock 140 151 7 minecraft:beacon");
                server.runCommand("fill 139 150 6 141 150 8 minecraft:iron_block");
                server.runCommand("tp @a 0 156 4 -90 12");
                context.runOnClient(client -> client.debugEntries.toggleStatus(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES));
                context.waitTicks(200);
                context.takeScreenshot("curveall");
                context.runOnClient(client -> client.debugEntries.toggleStatus(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES));
            }
        }
        if (scenarios.contains("transition")) {
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                String rows = world.getServer().computeOnServer(s -> {
                    var level = s.overworld();
                    StringBuilder out = new StringBuilder();
                    for (int y = -62; y >= -72; y--) {
                        int deepslate = 0, netherrack = 0, other = 0;
                        for (int cx = 0; cx < 4; cx++) {
                            for (int cz = 0; cz < 4; cz++) {
                                var chunk = level.getChunk(cx, cz);
                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        var state = chunk.getBlockState(new net.minecraft.core.BlockPos(cx * 16 + x, y, cz * 16 + z));
                                        if (state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE)) deepslate++;
                                        else if (state.is(net.minecraft.world.level.block.Blocks.NETHERRACK)) netherrack++;
                                        else other++;
                                    }
                                }
                            }
                        }
                        out.append(String.format(java.util.Locale.ROOT, "%n  y %d: deepslate %d, netherrack %d, other %d", y, deepslate, netherrack, other));
                    }
                    return out.toString();
                });
                log("transition (4096 columns per row):" + rows);
            }
            context.runOnClient(client -> client.options.renderDistance().set(8));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runCommand("gamemode spectator @a");
                world.getServer().runCommand("fill 0 -80 0 15 -61 15 air replace minecraft:netherrack");
                world.getServer().runCommand("tp @a 8 -85 -12 0 -30");
                context.waitTicks(200);
                context.takeScreenshot("transition_roof");
            }
        }
        if (scenarios.contains("detail")) {
            context.runOnClient(client -> client.options.renderDistance().set(6));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runCommand("gamemode spectator @a");
                world.getServer().runCommand("time set noon");
                world.getServer().runCommand("weather clear");
                world.getServer().runCommand("gamerule advance_time false");
                world.getServer().runCommand("tp @a 0 200 0 0 25");
                context.waitTicks(600);
                context.takeScreenshot("detail_horizon");
                world.getServer().runCommand("tp @a 0 300 0 0 70");
                context.waitTicks(200);
                context.takeScreenshot("detail_down");
            }
        }
        if (scenarios.contains("survivalseam")) {
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                survivalWalk(context, world.getServer()::runCommand, "single player",
                        () -> world.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getX()));
            }
            try (var server = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).createServer();
                 var connection = server.connect()) {
                server.runCommand("op Tester");
                survivalWalk(context, server::runCommand, "dedicated server",
                        () -> server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).getX()));
            }
        }
        if (scenarios.contains("gallery")) {
            galleryShots(context, System.getProperty("sphereworld.galleryWorld", "Gallery"), System.getProperty("sphereworld.gallerySuffix", ""));
        }
        if (scenarios.contains("galleryload")) {
            context.getInput().resizeWindow(1920, 1080);
            System.setProperty("sphereworld.captureLoading", "true");
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                System.clearProperty("sphereworld.captureLoading");
            }
            System.clearProperty("sphereworld.captureLoading");
        }
        if (scenarios.contains("dragon")) {
            context.runOnClient(client -> client.options.renderDistance().set(10));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                world.getServer().runCommand("gamemode creative @a");
                world.getServer().runCommand("time set noon");
                world.getServer().runCommand("tp @a 0 420 60 180 10");
                world.getServer().runCommand("effect give @a resistance infinite 255 true");
                world.getServer().runCommand("gamemode spectator @a");
                context.waitTicks(400);
                world.getServer().runCommand("execute as @e[type=ender_dragon] run data get entity @s Pos");
                world.getServer().runCommand("execute if entity @e[type=ender_dragon,x=-200,dx=400,z=-200,dz=400,y=320,dy=255] run say PASS dragon flies in the End layer");
                context.takeScreenshot("dragon_fight");
                world.getServer().runCommand("kill @e[type=ender_dragon]");
                context.waitTicks(400);
                world.getServer().runCommand("sphereworld column 0 0");
                world.getServer().runCommand("execute if block 0 383 0 minecraft:bedrock run say bedrock podium column at 383");
                world.getServer().runCommand("tp @a 0 410 40 180 35");
                context.waitTicks(100);
                context.takeScreenshot("dragon_exit_portal");
            }
        }
        if (scenarios.contains("endcircle")) {
            context.runOnClient(client -> client.options.renderDistance().set(Integer.getInteger("sphereworld.testRenderDistance", 12)));
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet8192).create()) {
                world.getServer().runCommand("gamemode spectator @a");
                context.waitTicks(100);
                world.getServer().runCommand("execute in minecraft:the_end run tp @a 1539.75 88.55 2532.56 0 75");
                context.waitTicks(400);
                context.takeScreenshot("end_circle_user");
                world.getServer().runCommand("execute in minecraft:the_end run tp @a 1539.75 230 2532.56 0 90");
                context.waitTicks(400);
                context.takeScreenshot("end_circle_down");
                world.getServer().runCommand("execute in minecraft:the_end run tp @a 1539.75 230 2532.56 0 55");
                context.waitTicks(60);
                context.takeScreenshot("end_circle_oblique");
            }
        }
        if (scenarios.contains("loading")) {
            System.setProperty("sphereworld.captureLoading", "true");
            try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).create()) {
                context.waitTicks(20);
                context.takeScreenshot("loading_done");
                world.getServer().runOnServer(server -> {
                    double y = server.getPlayerList().getPlayers().get(0).getY();
                    System.out.println((y < 320 && y > -64 ? "PASS" : "FAIL") + " player spawned on the Overworld at y " + y);
                });
            }
            System.clearProperty("sphereworld.captureLoading");
        }
        if (scenarios.contains("flat")) {
            try (TestSingleplayerContext world = context.worldBuilder().create()) {
                world.getServer().runCommand("time set noon");
                context.waitTicks(200);
                context.takeScreenshot("flat_world");
            }
        }
        if (scenarios.contains("tint")) {
            groundShot(context, true);
        }
    }

    private static void groundShot(ClientGameTestContext context, boolean planet) {
        context.runOnClient(client -> client.options.renderDistance().set(12));
        var builder = context.worldBuilder();
        if (planet) builder.adjustSettings(SphereWorldClientTest::selectPlanet);
        try (TestSingleplayerContext world = builder.create()) {
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("tp @a 300 200 300");
            context.waitTicks(100);
            int y = world.getServer().computeOnServer(server -> server.overworld()
                    .getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 300, 300)) + 6;
            world.getServer().runCommand("tp @a 300 " + y + " 300 30 10");
            context.waitTicks(300);
            context.takeScreenshot(planet ? "tint_planet" : "tint_vanilla");
        }
    }

    private static void multiplayerSeam(ClientGameTestContext context) {
        try (var server = context.worldBuilder().adjustSettings(SphereWorldClientTest::selectPlanet).createServer();
             var connection = server.connect()) {
            server.runCommand("op Tester");
            server.runCommand("gamemode creative Tester");
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("forceload add 2032 -16 2047 16");
            server.runCommand("forceload add -2048 -16 -2032 16");
            server.runCommand("tp Tester 2040 200 0.5 -90 0");
            context.waitTicks(80);
            server.runCommand("fill 2030 199 -3 2047 199 3 minecraft:stone");
            server.runCommand("fill -2048 199 -3 -2035 199 3 minecraft:stone");
            server.runCommand("summon minecraft:villager -2045.5 200 0.5 {NoAI:1b,PersistenceRequired:1b,CustomName:'Ghost'}");
            server.runCommand("tp Tester 2044 200 0.5 -90 0");
            context.waitTicks(60);
            Double seenX = context.computeOnClient(client -> {
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity.hasCustomName() && entity.getCustomName().getString().equals("Ghost")) return entity.getX();
                }
                return null;
            });
            double playerX = context.computeOnClient(client -> client.player.getX());
            log("multiplayer: client x " + playerX + ", sees Ghost at " + seenX + " (expected about 2050.5)");
            context.takeScreenshot("mp_seam_view");
            float before = server.computeOnServer(s -> {
                for (var entity : s.overworld().getAllEntities()) {
                    if (entity instanceof net.minecraft.world.entity.LivingEntity living && entity.hasCustomName()
                            && entity.getCustomName().getString().equals("Ghost")) return living.getHealth();
                }
                return -1F;
            });
            context.runOnClient(client -> {
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity.hasCustomName() && entity.getCustomName().getString().equals("Ghost")) {
                        client.gameMode.attack(client.player, entity);
                    }
                }
            });
            context.waitTicks(1);
            String push = server.computeOnServer(s -> {
                for (var entity : s.overworld().getAllEntities()) {
                    if (entity.hasCustomName() && entity.getCustomName().getString().equals("Ghost")) {
                        return entity.getDeltaMovement().toString();
                    }
                }
                return "?";
            });
            log("multiplayer: Ghost knockback velocity " + push + " (x must be > 0: away from the attacker)");
            context.waitTicks(19);
            float after = server.computeOnServer(s -> {
                for (var entity : s.overworld().getAllEntities()) {
                    if (entity instanceof net.minecraft.world.entity.LivingEntity living && entity.hasCustomName()
                            && entity.getCustomName().getString().equals("Ghost")) return living.getHealth();
                }
                return -1F;
            });
            log("multiplayer: Ghost health " + before + " -> " + after + " after an attack across the seam");

            server.runCommand("tp Tester 2000 210 0.5 -90 0");
            server.runCommand("summon minecraft:tnt 2046.5 200 0.5 {fuse:0}");
            context.waitTicks(2);
            String blast = server.computeOnServer(s -> {
                for (var entity : s.overworld().getAllEntities()) {
                    if (entity instanceof net.minecraft.world.entity.LivingEntity living && entity.hasCustomName()
                            && entity.getCustomName().getString().equals("Ghost")) {
                        return "health " + living.getHealth() + ", velocity " + entity.getDeltaMovement();
                    }
                }
                return "Ghost gone";
            });
            log("multiplayer: after TNT across the seam: " + blast + " (health must drop, x velocity > 0)");
        }
    }

    private static void clouds(ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.renderDistance().set(12);
            client.options.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
        });
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(SphereWorldClientTest::selectPlanet)
                .create()) {
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("tp @a 0 150 0 0 -10");
            context.waitTicks(300);
            context.takeScreenshot("clouds_below");
            world.getServer().runCommand("tp @a 0 260 0 0 25");
            context.waitTicks(100);
            context.takeScreenshot("clouds_above");
        }
        context.runOnClient(client -> client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF));
    }

    private static void packFar(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.renderDistance().set(12));
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(SphereWorldClientTest::selectPlanet)
                .create()) {
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("tp @a 0 170 0 0 32");
            context.waitTicks(400);
            context.takeScreenshot("packfar_on");
            System.setProperty("sphereworld.noHorizonFar", "true");
            context.waitTicks(10);
            context.takeScreenshot("packfar_off");
            System.clearProperty("sphereworld.noHorizonFar");
        }
    }

    private static void lodUpdate(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.renderDistance().set(8));
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(SphereWorldClientTest::selectPlanet)
                .create()) {
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("tp @a 0 320 0 0 40");
            context.waitTicks(400);
            context.takeScreenshot("lod_before");
            world.getServer().runCommand("forceload add 200 260 360 420");
            context.waitTicks(200);
            world.getServer().runCommand("fill 200 260 260 360 260 420 red_wool");
            context.waitTicks(200);
            context.takeScreenshot("lod_after");
        }
    }

    private static void endView(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.renderDistance().set(12));
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(SphereWorldClientTest::selectPlanet)
                .create()) {
            world.getServer().runCommand("gamemode spectator @a");
            context.waitTicks(200);
            world.getServer().runCommand("execute in minecraft:the_end run tp @a 1500 120 1500 0 70");
            context.waitTicks(400);
            boolean atlas = context.computeOnClient(client -> dev.sphereworld.client.render.ClientAtlases
                    .get(net.minecraft.resources.Identifier.withDefaultNamespace("overworld")) != null);
            log("end view: overworld atlas on client " + atlas);
            context.takeScreenshot("end_view_down");
            world.getServer().runCommand("execute in minecraft:the_end run tp @a 2300 120 1500 0 70");
            context.waitTicks(200);
            context.takeScreenshot("end_view_moved");
            context.getInput().lookAt(0, 20);
            context.waitTicks(20);
            context.takeScreenshot("end_view_horizon");
        }
    }

    private static void curvedView(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.renderDistance().set(16));
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(SphereWorldClientTest::selectPlanet)
                .create()) {
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("tp @a 0 300 0 0 20");
            context.waitTicks(400);
            context.takeScreenshot("view_high_horizon");
            context.getInput().lookAt(0, 60);
            context.waitTicks(20);
            context.takeScreenshot("view_high_down");
            world.getServer().runCommand("execute positioned 0 0 0 run tp @a ~ ~ ~");
            world.getServer().runCommand("tp @a 0 110 0 45 5");
            context.waitTicks(200);
            context.takeScreenshot("view_ground");
        }
    }

    private static void log(String message) {
        SphereWorld.LOGGER.info("[test] {}", message);
    }

    private static void seamWalk(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(SphereWorldClientTest::selectPlanet)
                .create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("tp @a 2030 200 0.5");
            context.waitTicks(60);
            world.getServer().runCommand("fill 2020 199 -3 2075 199 3 minecraft:glass");
            world.getServer().runCommand("fill 2047 199 -3 2048 199 3 minecraft:gold_block");
            world.getServer().runCommand("tp @a 2030 200 0.5 -90 25");
            context.waitTicks(80);
            context.takeScreenshot("seam_before");
            double before = context.computeOnClient(client -> client.player.getX());
            context.getInput().holdKeyFor(options -> options.keyUp, 110);
            context.waitTicks(20);
            double clientX = context.computeOnClient(client -> client.player.getX());
            double serverX = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().getX());
            log(String.format("seam walk: client x %.2f -> %.2f, server x %.2f", before, clientX, serverX));
            boolean glassAhead = context.computeOnClient(client -> client.level.getBlockState(
                    net.minecraft.core.BlockPos.containing(client.player.getX() + 5, 199, 0)).is(net.minecraft.world.level.block.Blocks.GLASS));
            log("client sees glass ahead after the seam: " + glassAhead);
            context.takeScreenshot("seam_after");
            context.getInput().lookAt(90, 25);
            context.waitTicks(10);
            context.takeScreenshot("seam_look_back");
        }
    }

    static void selectPlanet8192(WorldCreationUiState state) {
        selectPlanet(state);
        state.updateDimensions((registryAccess, dimensions) -> {
            var stem = dimensions.dimensions().get(net.minecraft.world.level.dimension.LevelStem.OVERWORLD);
            if (!(stem.generator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise)) return dimensions;
            var planets = new java.util.LinkedHashMap<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Integer>();
            planets.put(net.minecraft.world.level.Level.OVERWORLD, 8192);
            var config = dev.sphereworld.planet.PlanetLayout.derive(new dev.sphereworld.planet.PlanetConfig(planets,
                    java.util.List.of(net.minecraft.world.level.Level.NETHER, net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.END), true));
            var map = new java.util.LinkedHashMap<>(dimensions.dimensions());
            map.put(net.minecraft.world.level.dimension.LevelStem.OVERWORLD, new net.minecraft.world.level.dimension.LevelStem(stem.type(),
                    new dev.sphereworld.worldgen.PlanetChunkGenerator(noise.getBiomeSource(), noise.generatorSettings(), config)));
            return new net.minecraft.world.level.levelgen.WorldDimensions(map);
        });
    }

    private static final class StackedOffset {
        static final int END = dev.sphereworld.worldgen.stacked.StackBand.END.offset();
    }

    private static String endBottoms(net.minecraft.server.level.ServerLevel level, int offset) {
        int[] histogram = new int[8];
        int columns = 0;
        for (int cx = -80; cx < -72; cx++) {
            for (int cz = -4; cz < 4; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (int x = 0; x < 16; x += 2) {
                    for (int z = 0; z < 16; z += 2) {
                        for (int y = offset; y < offset + 128; y++) {
                            var pos = new net.minecraft.core.BlockPos(cx * 16 + x, y, cz * 16 + z);
                            if (chunk.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.END_STONE)) {
                                histogram[Math.min(7, (y - offset) / 8)]++;
                                columns++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return columns + " island columns, lowest end stone by 8-block band from the floor: " + java.util.Arrays.toString(histogram);
    }

    private static void survivalWalk(ClientGameTestContext context, java.util.function.Consumer<String> command, String where,
            java.util.function.Supplier<Double> serverX) {
        command.accept("time set noon");
        command.accept("gamerule advance_time false");
        command.accept("forceload add 2016 -16 2047 16");
        command.accept("forceload add -2048 -16 -2016 16");
        command.accept("tp @a 2030.5 201 0.5 -90 0");
        context.waitTicks(60);
        command.accept("fill 2016 199 -2 2047 199 2 minecraft:stone");
        command.accept("fill -2048 199 -2 -2020 199 2 minecraft:stone");
        command.accept("fill 2016 200 -2 2047 203 2 minecraft:air");
        command.accept("fill -2048 200 -2 -2020 203 2 minecraft:air");
        command.accept("gamemode survival @a");
        command.accept("effect give @a resistance infinite 255 true");
        command.accept("tp @a 2030.5 200 0.5 -90 0");
        context.waitTicks(40);

        for (int i = 0; i < 140; i++) {
            context.runOnClient(client -> client.player.setPos(client.player.getX() + 0.215, client.player.getY(), client.player.getZ()));
            context.waitTick();
        }
        context.waitTicks(20);
        double clientX = context.computeOnClient(client -> client.player.getX());
        double x = serverX.get();
        boolean crossed = x < 0 && x > -2048 + 5;
        log(where + ": client x " + clientX + ", server x " + x);
        log((crossed ? "PASS" : "FAIL") + " survival walk over the seam (" + where + ")");
    }

    private static final class BlockPosHolder {
        static volatile net.minecraft.core.BlockPos portal;
    }

    private static void galleryShots(ClientGameTestContext context, String worldName, String suffix) {
        context.getInput().resizeWindow(1920, 1080);
        context.runOnClient(client -> {
            client.options.renderDistance().set(16);
            client.options.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
            client.options.gamma().set(1.0);
            client.options.fov().set(85);
        });
        context.runOnClient(client -> client.createWorldOpenFlows().openWorld(worldName, () -> {}));
        context.waitTicks(100);
        context.runOnClient(client -> {
            if (client.gui.screen() instanceof net.minecraft.client.gui.screens.BackupConfirmScreen screen) {
                for (var child : screen.children()) {
                    if (child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().contains("Know")) {
                        button.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0));
                        return;
                    }
                }
            }
        });
        context.waitFor(client -> client.level != null && client.player != null && client.gui.screen() == null, 6000);
        var server = context.computeOnClient(client -> client.getSingleplayerServer());
        java.util.function.Consumer<String> run = command -> context.runOnClient(client -> server.execute(() ->
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command)));
        run.accept("gamemode spectator @a");
        run.accept("weather clear");
        run.accept("gamerule advance_time false");
        context.runOnClient(client -> { if (!client.gui.hud.isHidden()) client.gui.hud.toggle(); });
        int[] spawn = new int[3];
        server.execute(() -> {
            var level = server.overworld();
            var pos = level.getRespawnData().pos();
            spawn[0] = pos.getX();
            spawn[1] = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            spawn[2] = pos.getZ();
        });
        context.waitTicks(20);
        int sx = spawn[0], sy = spawn[1], sz = spawn[2];
        String[][] shots = {
                {"globe_above", "6000", "-508.87 1150 272.77 0 90", "500"},
                {"globe_oblique", "6000", "-508.87 1000 272.77 30 40", "300"},
                {"horizon", "6000", "0.5 99 0.5 135 6", "400"},
                {"horizon_far", "6000", "0.5 164 0.5 45 18", "300"},
                {"sunset", "12600", "1500.5 240 1500.5 90 12", "500"},
                {"end_above", "6000", "60 260 60 135 -50", "300"},
        };
        for (String[] shot : shots) {
            run.accept("time set " + shot[1]);
            run.accept("tp @a " + shot[2]);
            context.waitTicks(Integer.parseInt(shot[3]));
            context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("gallery_" + shot[0] + suffix)
                    .withSize(1920, 1080).disableCounterPrefix());
        }
        int hx = sx + 160, hz = sz + 160;
        run.accept("tp @a " + hx + " 250 " + hz);
        context.waitTicks(100);
        int[] topHolder = new int[1];
        server.execute(() -> topHolder[0] = server.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, hx, hz));
        context.waitTicks(20);
        int top = topHolder[0];
        for (int y = -112; y <= top + 2; y += 40) {
            run.accept("fill " + (hx - 12) + " " + y + " " + (hz - 12) + " " + (hx + 12) + " " + Math.min(top + 2, y + 39) + " " + (hz + 12) + " air");
        }
        run.accept("time set 6000");
        run.accept("tp @a " + (hx - 9) + " -108 " + (hz - 9) + " -45 -62");
        context.waitTicks(300);
        context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("gallery_nether_hole" + suffix)
                .withSize(1920, 1080).disableCounterPrefix());
        context.runOnClient(client -> { if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
        context.runOnClient(client -> client.disconnectFromWorld(net.minecraft.network.chat.Component.empty()));
        context.waitTicks(60);
    }

    static void selectPlanet(WorldCreationUiState state) {
        state.getNormalPresetList().stream()
                .filter(entry -> entry.preset() != null && entry.preset().is(PLANET))
                .findFirst()
                .ifPresentOrElse(state::setWorldType, () -> {
                    throw new AssertionError("Planet world type is not offered in the world type list");
                });
    }

    private static void customizeScreen(ClientGameTestContext context) {
        context.runOnClient(client -> CreateWorldScreen.openFresh(client, () -> client.gui.setScreen(null)));
        context.waitForScreen(CreateWorldScreen.class);
        context.runOnClient(client -> {
            CreateWorldScreen screen = (CreateWorldScreen) client.gui.screen();
            selectPlanet(screen.getUiState());
        });
        context.waitTicks(2);
        context.takeScreenshot("sphereworld_create_world");
        context.runOnClient(client -> {
            CreateWorldScreen screen = (CreateWorldScreen) client.gui.screen();
            PresetEditor editor = screen.getUiState().getPresetEditor();
            if (editor == null) throw new AssertionError("Planet preset has no Customize editor");
            client.gui.setScreen(editor.createEditScreen(screen, screen.getUiState().getSettings()));
        });
        context.waitTicks(5);
        context.takeScreenshot("sphereworld_customize");
        context.clickScreenButton("gui.done");
        context.waitForScreen(CreateWorldScreen.class);
        context.runOnClient(client -> {
            CreateWorldScreen screen = (CreateWorldScreen) client.gui.screen();
            var stem = screen.getUiState().getSettings().selectedDimensions().dimensions().get(net.minecraft.world.level.dimension.LevelStem.OVERWORLD);
            if (!(stem.generator() instanceof dev.sphereworld.worldgen.stacked.StackedChunkGenerator)) {
                throw new AssertionError("Customize replaced the stacked world with " + stem.generator());
            }
            System.out.println("PASS customize keeps the stacked world");
        });
        context.runOnClient(client -> client.gui.setScreen(null));
        context.waitTicks(2);
    }

    private static void planetWorld(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(SphereWorldClientTest::selectPlanet)
                .create()) {
            world.getServer().runCommand("sphereworld info");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 0 200 0");
            context.waitTicks(100);
            context.getInput().lookAt(0, 20);
            context.waitTicks(40);
            context.takeScreenshot("sphereworld_overworld_high");
        }
    }
}
