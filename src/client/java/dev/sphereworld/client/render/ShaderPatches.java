package dev.sphereworld.client.render;

import dev.sphereworld.SphereWorld;
import net.minecraft.resources.Identifier;

public final class ShaderPatches {
    private static final Identifier SODIUM_TERRAIN = Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque");
    private static final Identifier SODIUM_GLOBALS = Identifier.fromNamespaceAndPath("sodium", "globals.glsl");
    private static final String SODIUM_PROJECTION = "gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);";

    private static final String SODIUM_GLOBALS_FIELDS = """
                bool u_UseRGSS;
                vec4 u_SpherePlanet;
                vec4 u_SphereCamera;
            """;

    private static final String SODIUM_CURVE = """

            vec3 sphereworld_curve(vec3 pos) {
                if (u_SpherePlanet.z < 0.5) {
                    return pos;
                }
                float dist = length(pos.xz);
                if (dist < 0.0001) {
                    return pos;
                }
                float radius = u_SpherePlanet.x;
                float cameraRadius = max(radius + u_SphereCamera.x - u_SpherePlanet.y, 1.0);
                float effective = mix(cameraRadius, radius, smoothstep(0.0, 128.0, length(pos)));
                float theta = min(dist / effective, 3.14159265);
                float vertexRadius = cameraRadius + pos.y;
                float halfSin = sin(theta * 0.5);
                float horizontal = vertexRadius * sin(theta);
                float vertical = pos.y * cos(theta) - 2.0 * cameraRadius * halfSin * halfSin;
                vec2 dir = pos.xz / dist;
                return vec3(dir.x * horizontal, vertical, dir.y * horizontal);
            }
            """;

    private ShaderPatches() {
    }

    private static boolean disabled() {
        return Boolean.getBoolean("sphereworld.noShaderPatch");
    }

    public static String patchShader(Identifier id, String source) {
        if (disabled() || !id.equals(SODIUM_TERRAIN)) return source;
        if (!source.contains(SODIUM_PROJECTION)) {
            SphereWorld.LOGGER.warn("Sodium terrain shader changed; planet curvature is disabled with Sodium");
            return source;
        }
        SphereWorld.LOGGER.info("Patched Sodium terrain shader for planet curvature");
        return source.replace(SODIUM_PROJECTION, "position = sphereworld_curve(position);\n    " + SODIUM_PROJECTION);
    }

    public static String patchInclude(Identifier id, String source) {
        if (disabled() || !id.equals(SODIUM_GLOBALS)) return source;
        int end = source.lastIndexOf("};");
        if (!source.contains("bool u_UseRGSS;") || end < 0) {
            SphereWorld.LOGGER.warn("Sodium globals changed; planet curvature is disabled with Sodium");
            return source;
        }
        String withFields = source.replaceFirst("[ \\t]*bool u_UseRGSS;\\r?\\n?", java.util.regex.Matcher.quoteReplacement(SODIUM_GLOBALS_FIELDS));
        return withFields + SODIUM_CURVE;
    }
}
