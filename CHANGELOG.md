# Changelog

## [0.0.5] - 2026-05-26

### Fixed
- Mod is now compatible with **Hytale Update 5**. Hytale changed the `ServerVersion` format from an exact build string to a semver range — the old format loaded with a warning and may stop working in a future update. The mod now declares `>=0.5.0`, which covers Update 5 and future patch releases without needing a new upload every Hytale build.

### Notes
- All Adventure Time swords (Finn, Grass, Night, Scarlet, Tree) remain craftable and functional as before. Stats and recipes unchanged.

---

## [0.0.4] - 2026-05-21

### Fixed
- Mod now loads on the latest Hytale build (`2026.03.26-89796e57b`). Previous versions were marked as incompatible due to a missing `ServerVersion` and stopped showing up in the mod list entirely.

### Added
- Mod icon in the in-game mod list (no more `?` placeholder).
- MIT license included with the project.

### Notes
- All Adventure Time swords (Finn, Grass, Night, Scarlet, Tree) remain craftable and functional as before. Stats and recipes unchanged.

---

## [0.0.3] - 2026-05-21

Internal release used to bootstrap CurseForge publishing automation. Same content as 0.0.4 from a user perspective. Use 0.0.4 instead.

---

## [0.0.1] - Initial release

- Five Adventure Time swords: Finn Sword, Grass Sword, Night Sword, Scarlet Sword, Tree Sword.
- Custom 3D models, textures, icons, and Grass Sword sound effects.
- Crafting recipes at the Arcane Bench.
