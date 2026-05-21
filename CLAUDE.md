# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Hytale mod "Adventure time" by Dackoro (`manifest.json`: Group `Dackoro`, v0.0.1) that adds 5 Adventure Time–themed swords (Finn, Grass, Night, Scarlet, Tree). The repo is content-only: JSON definitions plus asset files (`.blockymodel`, `.blockyanim`, `.png`, `.ogg`) loaded by the Hytale modding runtime.

There is no source code, build system, package manager, linter, or test runner. Do not invent one. Iteration is done by loading the mod folder in the Hytale Model Tool / game client.

## Repository layout

- `manifest.json` — mod metadata. `Group` + `Name` form the mod's namespace; all asset paths in JSON are relative to this root.
- `Common/` — assets shipped to both client and server.
  - `Common/Resources/<Weapon>/` — per-weapon `.blockymodel` + texture `.png`, plus optional `.blockyanim` (e.g. Night_Sword has `eye_animation.blockyanim`, `look_around.blockyanim`).
  - `Common/Icons/ItemsGenerated/` — inventory icons referenced by item JSON `Icon` field.
  - `Common/Sounds/Weapons/<Weapon>/` — raw `.ogg` audio files.
- `Server/` — server-side definitions.
  - `Server/Item/Items/*.json` — one file per weapon. `Parent` (e.g. `Template_Weapon_Sword`) inherits from a base template; the file overrides `Model`, `Texture`, `Icon`, `Quality`, `Recipe`, `InteractionVars` (per-swing damage + sounds), `MaxDurability`, `Particles`, etc.
  - `Server/Item/Interactions/...` — interaction graph. Folder hierarchy mirrors Hytale's stock layout (e.g. `Weapons/Sword/Attacks/Primary/Thrust`) so `Parent` IDs resolve.
  - `Server/Audio/SoundEvents/SFX/Weapons/Sword/...` — sound-event JSONs that wrap raw `.ogg`s with `Volume`, pitch randomization, `AudioCategory`, and a `Parent` attenuation profile (e.g. `SFX_Attn_Moderate`). Referenced by ID (filename minus `.json`) from item `DamageEffects.WorldSoundEventId` / `LocalSoundEventId` and the top-level `SoundEventId`.

## Conventions

- **IDs vs paths.** `Parent`, `WorldSoundEventId`, `LocalSoundEventId`, `SoundEventId`, `ItemId`, `SystemId`, `EntityStatId` are string IDs resolved by the engine — either the basename of a JSON file in this mod, or a stock engine ID (e.g. `Template_Weapon_Sword`, `Ingredient_Fibre`, `Block_Break_Grass`, `SFX_Attn_Moderate`). In contrast, `Model`, `Texture`, `Icon`, and sound-event `Files` are filesystem paths relative to the mod root.
- **Folder placement of a JSON does not affect its ID** (only its filename does). Mirroring Hytale's stock hierarchy is for discoverability, not resolution.
- **Adding a new weapon.** Drop assets in `Common/Resources/<Name>/` and an icon in `Common/Icons/ItemsGenerated/`; add `.ogg`s under `Common/Sounds/Weapons/<Name>/` plus sound-event JSONs under `Server/Audio/SoundEvents/...`; then create `Server/Item/Items/<Name>.json` modeled on an existing one. `Grass_Sword.json` is the most fully-featured reference (recipe, all swing variants, signature attacks, particles).
- **Load-bearing typo.** `Server/Audio/SoundEvents/SFX/Weapons/Sword/Impacts/SXF_Grass_Sword_Equip.json` is misspelled (`SXF` not `SFX`) and `Grass_Sword.json` references that exact ID via `SoundEventId`. Renaming requires updating every reference.

## Workflow

This repo uses git worktrees so prod and dev versions of the mod can be loaded into Hytale simultaneously.

- `main` branch — what is published on CurseForge. Lives at `C:\HytaleModding\Adventure time\`. Manifest `Name`: `Adventure time`.
- `dev` branch — experimentation. Lives at `C:\HytaleModding\Adventure time-dev\` (a `git worktree`). Manifest `Name`: `Adventure time DEV` (distinct identity so Hytale lists both).

Both folders are symlinked into `%APPDATA%\Hytale\UserData\Mods\` so edits are picked up on next world load. To set up the symlinks (one-time, requires admin PowerShell):

```powershell
.\scripts\install-symlinks.ps1
```

**Day-to-day**: edit JSON / assets in `C:\HytaleModding\Adventure time-dev\`, commit on the `dev` branch.

**Promote a release** (from the main worktree):

```powershell
.\scripts\promote.ps1 -Version 0.0.3 -Message "fix: update for Hytale 2026.04 build"
```

This merges `dev → main`, bumps `manifest.json` `Version`, commits, tags `v0.0.3`, and pushes to `origin`.

**Publish to CurseForge + GitHub** (from main, after promote):

```powershell
.\scripts\publish.ps1
```

Requires:
- `.cf-config.json` at repo root (copy from `.cf-config.example.json`, fill in `ProjectId`). Gitignored.
- `CURSEFORGE_API_TOKEN` env var (or `.cf-token` file at repo root, gitignored). Generate at CurseForge → profile → API Tokens.
- `gh` CLI authenticated for the GitHub release step (skip with `-SkipGitHub` if missing).

`build-zip.ps1` is invoked by `publish.ps1` but can also be run standalone to just produce `dist/<Name>-<Version>.zip`.

## Mod icon

Hytale auto-discovers a `icon-256.png` at the **mod root** (same level as `manifest.json`) and shows it in the in-game mod list. No manifest field references it.

- File: `icon-256.png` at repo root. 256×256 PNG.
- To replace: drop a new 256×256 PNG at the repo root with the same name.

## ServerVersion compatibility

Hytale validates `manifest.json` `ServerVersion` by exact string equality (since Update 3). Every Hytale build update breaks the mod unless `ServerVersion` is updated. Workflow:

1. Note the new build string from Hytale's start screen (e.g. `2026.03.26-89796e57b`).
2. On `dev` branch: update `ServerVersion` in `manifest.json`, smoke-test in game.
3. `promote.ps1` + `publish.ps1` with patch-version bump.

Alternative: declare a semver range like `"2026.*"` in `ServerVersion` IF users install [ModsVersionRange](https://www.curseforge.com/hytale/mods/mods-version-range-fix) (+ MixinTale). Not the default — adds dependencies for end users.

## What not to do

- Don't add a build system, `package.json`, or test framework — this is content-only.
- Don't relocate or rename JSON files without updating every `Parent` / `*Id` reference that points at them; IDs are filename-based.
