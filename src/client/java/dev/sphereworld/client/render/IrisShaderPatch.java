package dev.sphereworld.client.render;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

public final class IrisShaderPatch {
    private static final Pattern MAIN = Pattern.compile("void\\s+main\\s*\\(\\s*(void)?\\s*\\)");
    private static final Pattern VERSION = Pattern.compile("^\\s*#version[^\\n]*\\n", Pattern.MULTILINE);
    private static final Pattern GET_VERTEX_POSITION = Pattern.compile("vec4\\s+getVertexPosition\\s*\\(\\s*\\)");
    private static final Pattern POSITION_INPUT = Pattern.compile("in\\s+vec3\\s+iris_Position\\s*;");
    private static final Pattern USES_POSITION = Pattern.compile("\\biris_Position\\b");

    private static volatile @Nullable PlanetGeometry compiledFor;

    private IrisShaderPatch() {
    }

    public static @Nullable PlanetGeometry compiledFor() {
        return compiledFor;
    }

    public static void markCompiled(@Nullable PlanetGeometry geometry) {
        compiledFor = geometry;
    }

    public static boolean applies(String programName) {
        String name = programName.toLowerCase(Locale.ROOT);
        return !(name.contains("sky") || name.contains("composite") || name.contains("final") || name.contains("deferred")
                || name.contains("prepare") || name.contains("begin") || name.contains("setup") || name.contains("dh_"));
    }

    public static boolean isShadow(String programName) {
        return programName.toLowerCase(Locale.ROOT).contains("shadow");
    }

    public static @Nullable String patch(String programName, String source) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        if (Boolean.getBoolean("sphereworld.noShaderPatch")) return null;
        compiledFor = g;
        if (g == null || !applies(programName) || source == null || source.contains("sphereworld_curve")) return null;
        Matcher version = VERSION.matcher(source);
        if (!version.find()) return null;
        dump(programName, source);

        String defines = String.format(Locale.ROOT,
                "#define SPHEREWORLD_RADIUS %.4f\n#define SPHEREWORLD_SURFACE %d.0\n", g.radius(), g.surfaceY());
        String uniforms = Pattern.compile("uniform\\s+vec3\\s+cameraPosition\\s*;").matcher(source).find()
                ? "" : "uniform vec3 cameraPosition;\n";
        String curve = uniforms + """
                vec3 sphereworld_curve(vec3 pos) {
                    float dist = length(pos.xz);
                    if (dist < 0.0001) return pos;
                    float radius = SPHEREWORLD_RADIUS;
                    float cameraRadius = max(radius + cameraPosition.y - SPHEREWORLD_SURFACE, 1.0);
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

        String body = source.substring(version.end());
        String header = source.substring(0, version.end()) + defines;
        String strategy;

        Matcher getter = GET_VERTEX_POSITION.matcher(body);
        Matcher input = POSITION_INPUT.matcher(body);
        Matcher main = MAIN.matcher(body);
        if (getter.find()) {
            int at = getter.start();
            body = body.substring(0, at) + "vec4 getVertexPosition();\nvec4 sphereworld_rawVertexPosition()"
                    + body.substring(getter.end())
                    + "\n" + curve
                    + "vec4 getVertexPosition() { vec4 p = sphereworld_rawVertexPosition(); return vec4(sphereworld_curve(p.xyz), p.w); }\n";
            strategy = "vertex position";
        } else if (input.find() && main.find()) {
            String offsetExpr = body.contains("iris_transforms.ModelOffset") ? "iris_transforms.ModelOffset" : "vec3(0.0)";
            String declaration = body.substring(input.start(), input.end());
            String rest = USES_POSITION.matcher(body.substring(input.end())).replaceAll("sphereworld_Position");
            body = body.substring(0, input.start()) + declaration + "\nvec3 sphereworld_Position;\n" + rest;
            main = MAIN.matcher(body);
            if (!main.find()) return null;
            body = body.substring(0, main.start()) + "void sphereworld_original_main()" + body.substring(main.end());
            body = body + "\n" + curve + """
                    void main() {
                        vec3 sphereworldOffset = %s;
                        sphereworld_Position = sphereworld_curve(iris_Position + sphereworldOffset) - sphereworldOffset;
                        sphereworld_original_main();
                    }
                    """.formatted(offsetExpr);
            strategy = "position attribute";
        } else if (main.find()) {
            body = reprojected(body, main, curve, isShadow(programName));
            strategy = "clip reprojection";
        } else {
            return null;
        }
        SphereWorld.LOGGER.debug("Curving shader-pack program {} ({})", programName, strategy);
        return header + body;
    }

    private static final Pattern CAMERA_UNIFORM = Pattern.compile("uniform\\s+vec3\\s+cameraPosition\\s*;");
    private static final Pattern ATM_FOG = Pattern.compile(
            "void\\s+DoAtmosphericFog\\s*\\(\\s*inout\\s+vec4\\s+\\w+\\s*,\\s*vec3\\s+(\\w+)[^)]*\\)\\s*\\{");

    public static @Nullable String patchFog(String source) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        if (g == null || source == null || Boolean.getBoolean("sphereworld.noShaderPatch")) return null;
        if (!CAMERA_UNIFORM.matcher(source).find()) return null;
        String clouds = patchClouds(g, source);
        if (clouds != null) source = clouds;
        Matcher fog = ATM_FOG.matcher(source);
        if (!fog.find()) return clouds;
        String pos = fog.group(1);
        String fix = String.format(Locale.ROOT, """

                    {
                        float sphereworldCameraRadius = max(%.4f + cameraPosition.y - %d.0, 1.0);
                        float sphereworldRadius = length(%s + vec3(0.0, sphereworldCameraRadius, 0.0));
                        %s.y = sphereworldRadius - sphereworldCameraRadius;
                    }
                """, g.radius(), g.surfaceY(), pos, pos);
        SphereWorld.LOGGER.debug("Planet altitude for the pack's atmospheric fog");
        return source.substring(0, fog.end()) + fix + source.substring(fog.end());
    }

    private static final Pattern CLOUD_NOISE = Pattern.compile(
            "bool\\s+GetCloudNoise\\s*\\(\\s*vec3\\s+(\\w+)\\s*,[^)]*\\)\\s*\\{");
    private static final Pattern VOLUMETRIC_CLOUDS = Pattern.compile(
            "vec4\\s+GetVolumetricClouds\\s*\\(\\s*int\\s+(\\w+)\\s*,[^)]*?vec3\\s+(\\w+)\\s*,\\s*vec3\\s+(\\w+)\\s*,[^)]*\\)\\s*\\{");
    private static final Pattern HIGHER_PLANE = Pattern.compile("float\\s+higherPlaneDistance\\s*=[^;]*;");
    private static final Pattern CLOUD_SHADING = Pattern.compile("\\(\\s*higherPlaneAltitude\\s*-\\s*(\\w+)\\.y\\s*\\)");

    private static @Nullable String patchClouds(PlanetGeometry g, String source) {
        Matcher noise = CLOUD_NOISE.matcher(source);
        Matcher volumetric = VOLUMETRIC_CLOUDS.matcher(source);
        if (!noise.find() || !volumetric.find() || noise.end() > volumetric.start()) return null;
        String altitudeFn = String.format(Locale.ROOT, """
                vec3 sphereworld_cloudCamera;
                float sphereworld_cameraRadius(float cameraY) { return max(%1$.4f + cameraY - %2$d.0, 1.0); }
                float sphereworld_altitude(vec3 worldPos) {
                    float cameraRadius = sphereworld_cameraRadius(sphereworld_cloudCamera.y);
                    return %2$d.0 + length(worldPos - sphereworld_cloudCamera + vec3(0.0, cameraRadius, 0.0)) - %1$.4f;
                }
                vec3 sphereworld_unbend(vec3 worldPos) {
                    float cameraRadius = sphereworld_cameraRadius(sphereworld_cloudCamera.y);
                    vec3 centred = worldPos - sphereworld_cloudCamera + vec3(0.0, cameraRadius, 0.0);
                    float horizontal = length(centred.xz);
                    float theta = atan(horizontal, centred.y);
                    float along = theta * mix(cameraRadius, %1$.4f, smoothstep(0.0, 128.0, length(worldPos - sphereworld_cloudCamera)));
                    vec2 flatXZ = horizontal > 0.0001 ? centred.xz / horizontal * along : vec2(0.0);
                    return vec3(sphereworld_cloudCamera.x + flatXZ.x, %2$d.0 + length(centred) - %1$.4f, sphereworld_cloudCamera.z + flatXZ.y);
                }
                float sphereworld_shellDistance(float altitude, vec3 direction) {
                    float cameraRadius = sphereworld_cameraRadius(sphereworld_cloudCamera.y);
                    float shellRadius = %1$.4f + altitude - %2$d.0;
                    float b = cameraRadius * direction.y;
                    float ground = b * b - (cameraRadius * cameraRadius - %1$.4f * %1$.4f);
                    if (cameraRadius > %1$.4f && ground > 0.0 && -b - sqrt(ground) > 0.0 && shellRadius > cameraRadius) return -1.0;
                    float disc = b * b - (cameraRadius * cameraRadius - shellRadius * shellRadius);
                    if (disc < 0.0) return -1.0;
                    if (cameraRadius < shellRadius) return -b + sqrt(disc);
                    float t = -b - sqrt(disc);
                    return t > 0.0 ? t : -1.0;
                }
                """, g.radius(), g.surfaceY());
        String samplePos = noise.group(1);
        String cameraPos = volumetric.group(2);
        String direction = volumetric.group(3);
        StringBuilder out = new StringBuilder(source);

        int volumetricBody = volumetric.end();
        Matcher plane = HIGHER_PLANE.matcher(source);
        boolean planes = plane.find(volumetricBody);
        Matcher shading = CLOUD_SHADING.matcher(source);
        java.util.List<int[]> shadingSpans = new java.util.ArrayList<>();
        java.util.List<String> shadingNames = new java.util.ArrayList<>();
        if (!planes) return null;
        int from = plane.end();
        while (shading.find(from)) {
            shadingSpans.add(new int[] {shading.start(), shading.end()});
            shadingNames.add(shading.group(1));
            from = shading.end();
        }
        for (int i = shadingSpans.size() - 1; i >= 0; i--) {
            int[] span = shadingSpans.get(i);
            out.replace(span[0], span[1], "(higherPlaneAltitude - clamp(sphereworld_altitude(" + shadingNames.get(i)
                    + "), lowerPlaneAltitude, higherPlaneAltitude))");
        }
        {
            out.insert(plane.end(), "\nlowerPlaneDistance = sphereworld_shellDistance(lowerPlaneAltitude, " + direction + ");\n"
                    + "higherPlaneDistance = sphereworld_shellDistance(higherPlaneAltitude, " + direction + ");\n");
        }
        out.insert(volumetricBody, "\nsphereworld_cloudCamera = " + cameraPos + ";\n");
        out.insert(noise.end(), "\n" + samplePos + " = sphereworld_unbend(" + samplePos + ");\n");
        out.insert(Math.min(noise.start(), volumetric.start()), altitudeFn);
        SphereWorld.LOGGER.debug("Planet shells for the pack's ray-marched clouds");
        dump("clouds-" + Integer.toHexString(source.hashCode()) + ".patched", out.toString());
        return out.toString();
    }

    private static String reprojected(String body, Matcher main, String curve, boolean shadow) {
        String proj = shadow ? "shadowProjection" : "gbufferProjection";
        String view = shadow ? "shadowModelView" : "gbufferModelView";
        StringBuilder uniforms = new StringBuilder();
        for (String name : new String[] {proj, proj + "Inverse", view, view + "Inverse"}) {
            if (!Pattern.compile("uniform\\s+mat4\\s+" + name + "\\s*;").matcher(body).find()) {
                uniforms.append("uniform mat4 ").append(name).append(";\n");
            }
        }
        String renamed = body.substring(0, main.start()) + "void sphereworld_original_main()" + body.substring(main.end());
        return renamed + "\n" + uniforms + curve + """
                void main() {
                    sphereworld_original_main();
                    if (abs(gl_Position.w) < 1.0) return;
                    vec4 sphereworldView = %sInverse * gl_Position;
                    sphereworldView /= sphereworldView.w;
                    vec3 sphereworldWorld = sphereworld_curve((%sInverse * vec4(sphereworldView.xyz, 1.0)).xyz);
                    gl_Position = %s * (%s * vec4(sphereworldWorld, 1.0));
                }
                """.formatted(proj, view, proj, view);
    }

    private static void dump(String programName, String source) {
        if (!Boolean.getBoolean("sphereworld.dumpShaders")) return;
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("sphereworld-shader-dump");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve(programName + ".vsh"), source);
        } catch (java.io.IOException ignored) {
        }
    }
}
