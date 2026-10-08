# SphereWorld

**Walk straight in any direction and you come back where you started.**

SphereWorld turns your world into a finite, round planet. The ground curves away to a real horizon, the
whole globe hangs in the sky when you fly high enough, and there is no edge and no wall: the world wraps
around seamlessly in every direction.

> ⚠️ **Alpha.** SphereWorld changes world generation, rendering and networking. Back up your worlds and
> start with a fresh one. Existing worlds cannot be converted into planets.

---

## ✨ Features

### 🌍 A real planet
- **A finite world that wraps around**: walk 4,096 blocks east (on the default size) and you are back
  home. No border, no visible seam, no loading wall.
- **Curved terrain**: the ground bends with the planet's true radius. From high up you see the whole
  globe, with oceans, continents, mountains and the night side.
- **Every biome on every planet**: the generator picks the part of the vanilla climate map that fits all
  biomes on your planet, so even a small world has deserts, jungles, mushroom islands and ice spikes.
- **Planet sizes from 2,048 to 65,536 blocks** around, chosen when you create the world.

### 🧱 Nether, Overworld and End in one world
- The three dimensions are **stacked in a single column**: the Nether lies under the Overworld's floor
  and the End floats above its sky.
- **Dig down into the Nether** and see the sky through your hole. **Build up into the End.** Nether and
  End portals still work, as shortcuts between the layers.
- Each layer has its own ambience: Nether fog, End sky and music, the full **Ender Dragon fight**, End
  gateways and the dragon's respawn ritual.
- The Overworld's deepslate runs down into the Nether's roof like vanilla bedrock, for a natural seam.
- Advancements, compasses, maps, `/locate` and the eye of ender all understand the layers.

### 🔁 Seamless everywhere
Everything works across the planet's seam, in survival, creative and multiplayer: walking, boats and
horses, mobs and pathfinding, combat, knockback and explosions, projectiles, redstone, pistons and
minecarts, sculk, villagers and raids, light, sounds and particles.

### 🔭 The distant planet
- Beyond your render distance, a **live map of the whole planet** fills in the rest of the globe, updated
  as chunks are generated and as players build.
- **Three levels of detail**: 2-block cells near you, 4-block cells further away, then the whole planet.
- It follows the day and night cycle like the terrain around you.

### 🗺️ The planet map
Press the **Planet map** key (M on a QWERTY keyboard, rebindable in Controls) to see the whole globe,
centred on you, with your position and the world spawn. Drag to turn it, scroll to zoom.

### 🖥️ A loading screen worth watching
When you create a world, watch the planet take shape stage by stage: continents, erosion, ridges,
climate, biomes, relief and surface. Then the view lands on your spawn.

---

## 🧩 Compatibility

### ✅ Voxy
SphereWorld is fully compatible with **Voxy**, and the two are made for each other: Voxy's far terrain is
bent around the planet like everything else, so you can see the real terrain all the way to the other
side of the globe.

In single player, SphereWorld can **generate the whole planet for Voxy** when you first open a new world.
The loading screen shows the planet filling in, with the number of chunks done and the time left. Press
**Escape** at any time to play right away; the rest is generated the next time you open the world. The
Customize screen gives an estimate for each size: about 20 minutes for the default 4,096-block planet on a
recent PC, four times longer each time the size doubles. Once the whole planet is in Voxy, SphereWorld
leaves the distant terrain to Voxy.

Tested with Voxy 0.2.20 beta.

### ✅ Shader packs (Iris)
Planet curvature works with shader packs through **Iris**. SphereWorld was tested with
**Complementary Reimagined r5.9.3**: the terrain, the sky, the clouds and the distant planet all follow the
curve. Other packs should work too, since the curvature is applied to every pack the same way.

The finer levels of detail of the distant planet are not drawn while a shader pack is on; the whole-planet
map still is. Above the Overworld layer (in the End layer and in space), the pack's clouds are hidden so the
globe stays clear.

### ✅ Other mods
| Mod | Status |
|---|---|
| **Sodium** | Supported (curved terrain) |
| **Sodium Extra** | Supported, its coordinates show your real position on the planet |
| **ModernFix** | Supported |

Coordinates shown in F3 and copied with F3+C always stay inside the planet (no "X: -4097" on a
4,096-block planet).

---

## 📦 Installation

1. Install **Fabric Loader** and **Fabric API** for Minecraft **26.3** (Java 25).
2. Put SphereWorld in your `mods` folder.
3. Create a new world and pick the **Planet** world type. Use **Customize** to choose the size.

### Servers
SphereWorld is required **on the server and on every client**. Set `level-type=sphereworld\:planet` in
`server.properties` before the server creates its world. The planet map is computed when the server
starts (about 20 seconds on the default size); players who join meanwhile watch it being built.

---

## ⚠️ Known limitations
- A fresh world is needed: existing worlds are not converted.
- Players without the mod cannot play properly on a planet server.
- Generating the whole planet for Voxy is only available in single player.
- The world border is not curved.
- A small planet is strongly curved: on the default size (4,096 blocks around) the horizon is about
  115 blocks away at eye level. Pick a bigger planet for a flatter look.
- Planets need more memory than a flat world, especially with Voxy and a large render distance.

---

## 🐞 Bug reports
Please include your `latest.log`, your mod list and, if you can, a screenshot with F3 open.
