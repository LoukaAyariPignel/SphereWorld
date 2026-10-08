package dev.sphereworld.client.dev;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.lwjgl.PointerBuffer;

public final class SecondScreen {
    private SecondScreen() {
    }

    public static void install() {
        if (!Boolean.getBoolean("sphereworld.secondScreen")) return;
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                PointerBuffer monitors = (PointerBuffer) glfw.getMethod("glfwGetMonitors").invoke(null);
                if (monitors == null || monitors.limit() < 2) return;
                long primary = (long) glfw.getMethod("glfwGetPrimaryMonitor").invoke(null);
                Method monitorPos = glfw.getMethod("glfwGetMonitorPos", long.class, int[].class, int[].class);
                Method workArea = glfw.getMethod("glfwGetMonitorWorkarea", long.class, int[].class, int[].class, int[].class, int[].class);
                Method setPos = glfw.getMethod("glfwSetWindowPos", long.class, int.class, int.class);
                for (int i = 0; i < monitors.limit(); i++) {
                    long monitor = monitors.get(i);
                    if (monitor == primary) continue;
                    int[] x = new int[1];
                    int[] y = new int[1];
                    int[] w = new int[1];
                    int[] h = new int[1];
                    workArea.invoke(null, monitor, x, y, w, h);
                    if (w[0] == 0) monitorPos.invoke(null, monitor, x, y);
                    int width = client.getWindow().getWidth();
                    int height = client.getWindow().getHeight();
                    int left = x[0] + Math.max(0, (w[0] - width) / 2);
                    int top = y[0] + Math.max(30, (h[0] - height) / 2);
                    setPos.invoke(null, client.getWindow().handle(), left, top);
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        });
    }
}
