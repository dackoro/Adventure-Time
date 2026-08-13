# Session 2026-05-21 — Setup completo + primer release publicado

## Punto de partida

- Mod existía en `C:\HytaleModding\Adventure time\` como carpeta suelta (sin git, sin scripts)
- Versión upstream en GitHub: `dackoro/Adventure-Time` v0.0.1, sin `ServerVersion`
- Hytale Update 3+ hizo `ServerVersion` un check de string exacto → mod salía como incompatible
- Update 4 hizo más estricto el validation → mod directamente no se listaba en Hytale
- Build actual de Hytale: `2026.03.26-89796e57b`
- El usuario quería: separar dev de prod, repo, auto-deploy, no entender por qué se rompió

## Cambios hechos en orden

### 1. Investigación de por qué falló
- Consulta a doctale.dev: `ServerVersion` ahora es string exacto, antes era semver. Update 4 quitó `SkipModValidationForVersion`.
- Descubrimos que el manifest del usuario no tenía `ServerVersion` para nada → mod marcado como roto → en Update 4 se oculta del listado.
- Conclusión: añadir `ServerVersion` + bump version.

### 2. Fix inicial al manifest
- `manifest.json`: añadido `"ServerVersion": "2026.03.26-89796e57b"`, version 0.0.1 → 0.0.2

### 3. Git + worktrees + GitHub
- `git init` en `C:\HytaleModding\Adventure time\`
- Branch main, worktree dev en `C:\HytaleModding\Adventure time-dev\` con `Name: "Adventure time DEV"` en manifest
- Merge con upstream `dackoro/Adventure-Time` vía `--allow-unrelated-histories` (preserva sus 4 commits originales en la historia)
- Reescritura de autoría de commits con `git filter-branch` para que todos sean `dackoro89@gmail.com` (matchea cuenta GitHub)
- Remote configurado vía alias SSH `github-hytale:dackoro/Adventure-Time.git` (usa `~/.ssh/id_ed25519_github_hytale`)
- Push de main + dev + tag v0.0.3 ✓

### 4. Icono
- Upstream tenía `Assets/icon.webp` en subcarpeta → Hytale no lo encontraba
- Convertido con ffmpeg a `icon-256.png` (256x256) en RAÍZ del repo → convención correcta (verificada inspeccionando zip de "Aures - Livestock Skins" que sí muestra icono)
- Manifest no necesita referenciar el icono — auto-discovery
- `Assets/` borrada después (cleanup)

### 5. Scripts del workflow

| Script | Qué hace |
|---|---|
| `build-zip.ps1` | Empaqueta dist/<Name>-<Version>.zip con manifest en raíz |
| `promote.ps1` | Merge dev→main, bump Version, commit, tag, push |
| `publish.ps1` | Build zip + sube a CurseForge + (opc) GitHub release |
| `install-symlinks.ps1` | Crea symlinks dev/prod en %APPDATA%, requiere admin |
| `.cf-config.example.json` | Template del config (tracked) |

### 6. CurseForge API — exploración larga

**Problema**: la doc oficial de CurseForge Upload API dice que `gameVersions` es required y manda usar el subdominio del juego.

**Descubrimientos por prueba y error**:
- `hytale.curseforge.com/api/projects/.../upload-file` → 404 (no existe esa ruta API)
- `minecraft.curseforge.com/api/game/versions` → funciona pero es para Minecraft
- `eternal.curseforge.com/api/game/versions` → devuelve UN solo versión (id 6952, name "1.0") supuestamente Hytale
- `hypixel.curseforge.com/api/game/versions` → mismo resultado
- POST a `eternal/hypixel/authors-old + /api/projects/.../upload-file` con gameVersions=6952 → "Invalid game version ID: 6952 belongs to an invalid game"
- Brute force con IDs 7000-13000 → todos "invalid game"
- POST sin el campo `gameVersions` → **funciona** (fileIds 8123532, 8123533, 8123534 todos quedaron como alpha de prueba en CF)

**Solución final**: el script omite `gameVersions` completamente. Endpoint: `authors-old.curseforge.com/api/projects/1448331/upload-file`. Per-build compat se hace solo vía `ServerVersion` en manifest.

### 7. PowerShell 5.1 issues
- `Invoke-RestMethod -Form` no existe en PS 5.1 → script construye multipart/form-data manualmente
- Em-dashes (`—`) en strings rompen el parser → reemplazados por guiones normales

### 8. Release v0.0.3 publicado
- `manifest.json` bumpeado a 0.0.3, tag `v0.0.3`, push a GitHub
- `publish.ps1 -SkipGitHub` → fileId 8123536, release type, changelog del mensaje del tag
- En CurseForge: "Under Review" (cola de moderation)

## Pendiente al cierre de sesión

- [ ] Usuario corre `install-symlinks.ps1` desde PS admin (task #5)
- [ ] Usuario borra 3 alphas de prueba en CurseForge web UI (fileIds 8123532-8123534)
- [ ] (Opcional) Usuario instala gh CLI para releases automáticos en GitHub
- [ ] CurseForge moderation review aprueba o pide cambios

## Lecciones para futuras sesiones

- Verificar siempre `Get-Command gh` antes de asumir gh CLI
- Para Hytale CurseForge: omitir `gameVersions` en metadata
- El user tiene aliases SSH custom (`github-personal`, `github-hytale`, etc) — preguntar/inspeccionar `~/.ssh/config` antes de configurar remotes
- El user tiene `dackoro89@gmail.com` para commits del proyecto Hytale
- Em-dashes en scripts PowerShell → no usar
- Para mods asset-only: icono PNG 256x256 en RAÍZ, llamado `icon-256.png`, no requiere mención en manifest
