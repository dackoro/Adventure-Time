# AGENTS.md

Guidance for AI agents (and contributors) working on this repository.

## Project overview

Hytale mod "Adventure time" by Dackoro — 5 Adventure Time-themed swords (Finn, Grass, Night, Scarlet, Tree). Content-only repo: JSON definitions plus asset files (`.blockymodel`, `.blockyanim`, `.png`, `.ogg`) loaded by the Hytale modding runtime. No source code, build system, or tests; iteration happens by loading the mod folder in the game client.

See `CLAUDE.md` for the full project layout, conventions, and dev/main workflow. This file focuses on external reference documentation.

## Documentation & reference sources

Curated list of official and community resources used to build and maintain this mod.

### Official

| Resource | URL | Notes |
|---|---|---|
| Hytale Docs (stable) | https://docs.hytale.com | Official server API Javadoc (Hytale Server API). |
| Hytale Docs (pre-release) | https://pre-release.docs.hytale.com | Docs for the current pre-release patchline. |
| Hytale news / patch notes | https://hytale.com/news | Update patch notes (incl. Update 6 pre-release notes, updated weekly). |
| Hytale Modding strategy | https://hytale.com/news/2025/11/hytale-modding-strategy-and-status | Official modding architecture overview ("server-side mods"). |
| Hytale Modding + CurseForge | https://hytale.curseforge.com | Official creation tools landing page + Mod Author Discord. |
| CurseForge Hytale authors KB | https://support.curseforge.com/en/support/solutions/folders/9000202192 | Packs, plugins, NPCs, worldgen tutorials. |
| CurseForge blog — Hytale mods guide | https://blog.curseforge.com/how-to-create-hytale-mods/ | Packs vs Plugins, publishing flow. |
| CurseForge REST API | https://docs.curseforge.com/rest-api | Upload API (Hytale quirk: omit `gameVersions`). |

### Community / unofficial

| Resource | URL | Notes |
|---|---|---|
| doctale.dev | https://doctale.dev | Full modding docs (assets + Java plugins) based on decompiled server analysis. Includes Update 1→4 changelogs. |
| HytaleModding | https://hytalemodding.dev | #1 community resource: guides, docs, wiki, plugin template. |
| HytaleModding plugin template | https://github.com/HytaleModding/plugin-template | Official Java 25 plugin template (ScaffoldIt Gradle plugin). |
| hytale-docs.com | https://hytale-docs.com/docs/modding/overview | Modding overview & strategy ("one clean client for all servers"). |
| hytale.game | https://hytale.game | Guides: first Java plugin, installing mods client & server. |
| Hytale Shared Source | https://github.com/HytaleModding | Shared-source server, network protocol and assets (since June 2026). |
| Community wikis | https://www.hytalewiki.pro · https://www.hytale-wiki.fun · https://hytaleversions.io | Mod lists, patch note archives, changelogs. |
| News / changelog aggregators | https://hytalecharts.com · https://shrine-orbis.driphacker.dev | Weekly pre-release Update 6 changelog (community-curated). |

### Key facts learned so far

- **Architecture**: Hytale mods run server-side. There is no client plugin API; the client streams assets/sync from the server ("one clean client for all servers").
- **Content mods** (this project) are JSON + `.blockymodel`/`.blockyanim`/`.png`/`.ogg`, no Java needed.
- **Code mods** are Java 25 plugins (Maven `https://maven.hytale.com/release`, dep `com.hypixel.hytale:Server:+`), packaged as `.jar` in a server's `plugins/` folder.
- **`ServerVersion` in `manifest.json`**: since Update 5 (2026-05-25) it accepts semver ranges; current value is `">=0.5.0"`.
- **Mod Browser** (in-game mod marketplace, shipped in Update 6 Part 12, 2026-08-13) is backed by CurseForge: creator name/avatar come from the CurseForge account, mod thumbnail from the project logo, detail images from the project gallery.
