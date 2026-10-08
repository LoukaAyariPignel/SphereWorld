package dev.sphereworld.client.compat.voxy;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

public final class VoxyShaderPatch {
    private static final Pattern VERSION = Pattern.compile("^\\s*#version[^\\n]*\\n");

    private VoxyShaderPatch() {
    }

    public static String patch(String id, String source) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        if (g == null || source == null || source.contains("sw_curve")) return source;
        String patched = source;
        patched = replace(id, patched, "ivec3 baseSection = (getLoDPosition(sPos)<<lodLevel) - baseSectionPos;",
                "ivec3 baseSection = sw_wrapSec((getLoDPosition(sPos)<<lodLevel) - baseSectionPos);");
        patched = replace(id, patched, "vec4 pos = MVP * vec4(point, 1.0f);",
                "vec4 pos = MVP * vec4(sw_curve(point - cameraSubPos, float(baseSectionPos.y<<5) + cameraSubPos.y) + cameraSubPos, 1.0f);");
        patched = replace(id, patched, "vec3 nPos = vec3(((node.pos<<node.lodLevel)-camSecPos)<<5)-camSubSecPos;",
                "vec3 nPos = vec3(sw_wrapSec((node.pos<<node.lodLevel)-camSecPos)<<5)-camSubSecPos;");
        patched = screenspace(id, patched);
        patched = replace(id, patched, "ivec3 pos = (((ipos<<detail)-baseSectionPos)<<5);",
                "ivec3 pos = sw_wrapSec((ipos<<detail)-baseSectionPos)<<5;");
        patched = replace(id, patched, "gl_Position = MVP * vec4(vec3(pos)+offset*(1<<detail),1);", """
                vec3 swLocal = vec3(pos)+offset*(1<<detail);
                    float swSpan = float(max(size.x, size.z)+2) * float(1<<detail);
                    swLocal.y += (float((gl_VertexID>>2)&1)*2.0f-1.0f) * (swSpan*swSpan/(8.0f*SW_R) + 1.0f);
                    gl_Position = MVP * vec4(sw_curve(swLocal - cameraSubPos, float(baseSectionPos.y<<5) + cameraSubPos.y) + cameraSubPos, 1);""");
        patched = replace(id, patched, "ivec3 relative = ipos-(baseSectionPos>>detail);", """
                ivec3 relative = sw_wrapSec((ipos<<detail)-baseSectionPos)>>detail;
                        bool swFar = (max(abs(relative.x), abs(relative.z))<<detail) > 8;""");
        if (patched.contains("bool swFar")) {
            patched = replace(id, patched, "(relative.y>-1))", "(relative.y>-1 || swFar))");
            patched = replace(id, patched, "(relative.y<1 ))", "(relative.y<1  || swFar))");
        }
        patched = replace(id, patched, "uvec3 rel = abs(extractPosition(meta)-(baseSectionPos>>detail));",
                "uvec3 rel = uvec3(abs(sw_wrapSec((extractPosition(meta)<<detail)-baseSectionPos)>>detail));");
        if (patched.equals(source)) return source;
        SphereWorld.LOGGER.debug("Planet curvature for Voxy shader {}", id);
        return insertHelpers(patched, g);
    }

    private static String screenspace(String id, String source) {
        int start = source.indexOf("vec3 basePos = vec3(((node.pos<<node.lodLevel)-camSecPos)<<5)-camSubSecPos;");
        String endMarker = "vec4 P111 = Axis[1] + P101;";
        int end = source.indexOf(endMarker, Math.max(start, 0));
        if (start < 0 || end < 0) return source;
        String bent = """
                float swSize = float(32<<node.lodLevel);
                    vec3 swFlat = vec3(sw_wrapSec((node.pos<<node.lodLevel)-camSecPos)<<5)-camSubSecPos;
                    float swCamY = float(camSecPos.y<<5)+camSubSecPos.y;
                    vec3 swMin = vec3(1e30f);
                    vec3 swMax = vec3(-1e30f);
                    for (int swI = 0; swI < 8; swI++) {
                        vec3 swC = sw_curve(swFlat + vec3(swI&1, (swI>>1)&1, (swI>>2)&1)*swSize, swCamY);
                        swMin = min(swMin, swC);
                        swMax = max(swMax, swC);
                    }
                    float swSag = swSize*swSize/(8.0f*SW_R) + 1.0f;
                    swMin -= vec3(swSag);
                    swMax += vec3(swSag);
                    vec3 basePos = swMin;
                    vec3 swExtent = swMax - swMin;
                    _frustumCulled = false;
                    for (int swP = 0; swP < 5; swP++) {
                        vec4 swPlane = frustum.planes[swP];
                        if (dot(swPlane.xyz, basePos + mix(swExtent, vec3(0), lessThan(swPlane.xyz, vec3(0)))) < -swPlane.w) _frustumCulled = true;
                    }
                    if (_frustumCulled) {
                        return;
                    }
                    vec4 P000 = MVP * vec4(basePos, 1);
                    mat3x4 Axis = mat3x4(MVP);
                    vec4 P100 = Axis[0]*swExtent.x + P000;
                    vec4 P001 = Axis[2]*swExtent.z + P000;
                    vec4 P101 = Axis[2]*swExtent.z + P100;
                    vec4 P010 = Axis[1]*swExtent.y + P000;
                    vec4 P110 = Axis[1]*swExtent.y + P100;
                    vec4 P011 = Axis[1]*swExtent.y + P001;
                    vec4 P111 = Axis[1]*swExtent.y + P101;""";
        return source.substring(0, start) + bent + source.substring(end + endMarker.length());
    }

    private static String replace(String id, String source, String target, String replacement) {
        return source.contains(target) ? source.replace(target, replacement) : source;
    }

    private static String insertHelpers(String source, PlanetGeometry g) {
        String helpers = String.format(Locale.ROOT, """
                #define SW_R %1$.4f
                #define SW_SURFACE %2$d.0
                #define SW_PERIOD %3$d
                int sw_wrap1(int v) { return v - SW_PERIOD * int(floor(float(v) / float(SW_PERIOD) + 0.5)); }
                ivec3 sw_wrapSec(ivec3 s) { return ivec3(sw_wrap1(s.x), s.y, sw_wrap1(s.z)); }
                vec3 sw_curve(vec3 pos, float cameraY) {
                    float dist = length(pos.xz);
                    if (dist < 0.0001) return pos;
                    float cameraRadius = max(SW_R + cameraY - SW_SURFACE, 1.0);
                    float effective = mix(cameraRadius, SW_R, smoothstep(0.0, 128.0, length(pos)));
                    float theta = min(dist / effective, 3.14159265);
                    float halfSin = sin(theta * 0.5);
                    float horizontal = (cameraRadius + pos.y) * sin(theta);
                    float vertical = pos.y * cos(theta) - 2.0 * cameraRadius * halfSin * halfSin;
                    vec2 dir = pos.xz / dist;
                    return vec3(dir.x * horizontal, vertical, dir.y * horizontal);
                }
                """, g.radius(), g.surfaceY(), g.circumference() / 32);
        int at = insertionPoint(source);
        return source.substring(0, at) + helpers + source.substring(at);
    }

    private static int insertionPoint(String source) {
        String[] lines = source.split("\n", -1);
        int lastExtension = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("#extension")) lastExtension = i;
        }
        if (lastExtension < 0) {
            Matcher version = VERSION.matcher(source);
            return version.find() ? version.end() : 0;
        }
        int depth = 0;
        int line = 0;
        for (; line <= lastExtension; line++) depth += conditionalDelta(lines[line]);
        while (depth > 0 && line < lines.length) depth += conditionalDelta(lines[line++]);
        int offset = 0;
        for (int i = 0; i < line; i++) offset += lines[i].length() + 1;
        return Math.min(offset, source.length());
    }

    private static int conditionalDelta(String line) {
        String t = line.trim();
        if (t.startsWith("#if")) return 1;
        if (t.startsWith("#endif")) return -1;
        return 0;
    }

    static @Nullable PlanetGeometry planet() {
        return Planets.of(Minecraft.getInstance().level);
    }
}
