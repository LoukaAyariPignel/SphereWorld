package dev.sphereworld.dev;

import dev.sphereworld.SphereWorld;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public final class DevServerScript {
    private static final Deque<String> PENDING = new ArrayDeque<>();
    private static int waitTicks;
    private static boolean running;

    private DevServerScript() {
    }

    public static void install() {
        String script = System.getProperty("sphereworld.devCommands");
        String file = System.getProperty("sphereworld.devScript");
        if (file != null && !file.isBlank()) {
            try {
                script = String.join("|", java.nio.file.Files.readAllLines(java.nio.file.Path.of(file)));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
        if (script == null || script.isBlank()) return;
        PENDING.addAll(Arrays.stream(script.split(Pattern.quote("|"))).map(String::trim).filter(s -> !s.isEmpty()).toList());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> running = true);
        ServerTickEvents.END_SERVER_TICK.register(DevServerScript::tick);
    }

    private static void tick(MinecraftServer server) {
        if (!running) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        while (!PENDING.isEmpty()) {
            String command = PENDING.poll();
            if (command.startsWith("wait ")) {
                waitTicks = Integer.parseInt(command.substring(5).trim());
                return;
            }
            SphereWorld.LOGGER.info("[dev] /{}", command);
            long start = System.nanoTime();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            SphereWorld.LOGGER.info("[dev] done in {} ms", (System.nanoTime() - start) / 1_000_000);
        }
        running = false;
        if (Boolean.getBoolean("sphereworld.devStop")) {
            server.halt(false);
        }
    }
}
