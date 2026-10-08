# Modrinth listing

Fields to fill in on modrinth.com. The long description is in `description.md`: paste it as is into the
Description tab.

## Project

| Field | Value |
|---|---|
| Name | SphereWorld |
| URL (slug) | `sphereworld` |
| Summary | Turns your world into a finite, round planet with curved terrain, a real horizon and seamless wrap-around. Nether, Overworld and End stacked in one world. Compatible with Voxy and shader packs. |
| Project type | Mod |
| Client side | Required |
| Server side | Required |
| License | MIT |
| Source code | https://github.com/LoukaAyariPignel/SphereWorld |
| Issues | https://github.com/LoukaAyariPignel/SphereWorld/issues |

### Tags
- Categories: `worldgen`, `adventure`, `game-mechanics`
- Loader: Fabric

## First version

| Field | Value |
|---|---|
| File | `build/libs/sphereworld-0.1.0+mc26.3.jar` (not the `-sources.jar`) |
| Version number | `0.1.0+mc26.3` |
| Version name | SphereWorld 0.1.0 |
| Release channel | Alpha |
| Loaders | Fabric |
| Game versions | 26.3 |
| Dependencies | Fabric API: required. Sodium, Iris, Voxy: optional. |

### Changelog

```markdown
First public alpha.

- Finite round planets that wrap around in every direction, from 2,048 to 65,536 blocks around
- Curved terrain and a live map of the whole distant planet with three levels of detail
- Nether, Overworld and End stacked in one world, with the full Ender Dragon fight in the End layer
- Seamless mobs, combat, redstone, light and movement across the planet's seam (survival and multiplayer)
- Loading screen that shows the planet being generated
- Compatible with Voxy (whole-planet generation for Voxy in single player, with time left)
- Compatible with shader packs through Iris (tested with Complementary Reimagined r5.9.3)
- Compatible with Sodium and Sodium Extra
```

## Gallery

Files are in the game profile's `screenshots/gallery` folder, 1920×1080, HUD hidden. The first one is the
featured image.

| File | Title | Description |
|---|---|---|
| `01_globe_from_above.png` | The whole planet | The planet seen from high above, with the End's islands around it. |
| `02_globe_oblique.png` | A planet in space | The globe rising out of the dark, oceans and continents included. |
| `03_loading_screen.png` | Watch it being generated | The loading screen fills the planet in as it is generated for Voxy, with the time left. |
| `04_horizon_complementary.png` | A real horizon | Badlands curving away to the horizon (Complementary Reimagined r5.9.3). |
| `05_horizon_high_complementary.png` | The ground bends away | From higher up, the curve of the planet is clear (Complementary Reimagined r5.9.3). |
| `06_sunset_complementary.png` | Sunset under the End | The sun setting behind the End's floating islands (Complementary Reimagined r5.9.3). |
| `07_end_island_above_complementary.png` | The End floats in the sky | The main End island and its pillars, seen from the Overworld below (Complementary Reimagined r5.9.3). |
| `08_nether_hole_complementary.png` | The Nether lies under the Overworld | A hole dug from the surface: the sky seen from the Nether (Complementary Reimagined r5.9.3). |
| `09_horizon_high_no_shader.png` | Without shaders | The same view without a shader pack, with Sodium and Voxy. |

## Before publishing
- [x] GitHub repository and links filled in.
- [x] Icon: `icon.png` (512×512) in this folder, `icon_1000.png` for a larger version.
- [x] `contact` links in `fabric.mod.json`.
- [x] Build with `./gradlew build` (done: `build/libs/sphereworld-0.1.0+mc26.3.jar`).
- [ ] Upload `sphereworld-0.1.0+mc26.3.jar` to Modrinth.
- [x] Clean Fabric server with Fabric API and SphereWorld only: starts, creates the planet, builds its map.
- [ ] Clean Fabric client profile with Fabric API and SphereWorld only: create a planet world and walk around.
