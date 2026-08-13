# Workflow — Adventure Time mod

Notas internas para retomar contexto rápido cuando Claude vuelva a una sesión nueva.
Este archivo está gitignored — no se publica.

## Estructura de carpetas

```
C:\HytaleModding\
├── Adventure time\          worktree de "main" (branch estable, lo que va a CurseForge)
└── Adventure time-dev\      worktree de "dev" (branch experimental)

%APPDATA%\Hytale\UserData\Mods\
├── Adventure time\          symlink → C:\HytaleModding\Adventure time\
└── Adventure time DEV\      symlink → C:\HytaleModding\Adventure time-dev\
```

- Ambas carpetas en `C:\HytaleModding` son el MISMO repo git con dos branches checkeadas a la vez (worktrees). Cambias dev sin tocar main.
- Hytale lee mods desde `%APPDATA%\Hytale\UserData\Mods\`. Los symlinks evitan tener que copiar después de cada edición.
- `manifest.json` usa `Group:Name` como ID único. Por eso dev se llama `"Adventure time DEV"` (distinto de prod) — así Hytale los lista como dos mods separados y se pueden activar/desactivar independientemente.

## Scripts (en `scripts/`)

| Script | Qué hace | Cuándo |
|---|---|---|
| `build-zip.ps1` | Empaqueta `dist/<Name>-<Version>.zip` con manifest.json en raíz, sin scripts/.git/etc | Lo llama publish.ps1 |
| `promote.ps1 -Version X.Y.Z -Message "..."` | Merge dev→main, bump Version, commit, tag, push | Cuando dev está listo para release |
| `publish.ps1 [-SkipGitHub] [-ReleaseType release\|beta\|alpha]` | Build zip + upload a CurseForge + (opcional) GitHub release | Después de promote |
| `install-symlinks.ps1` | Reemplaza copia en %APPDATA% por symlinks. **Requiere PS admin.** | Una sola vez |

## Workflow día a día

### 1. Iterar en dev
```powershell
cd "C:\HytaleModding\Adventure time-dev"
# editar JSONs
git add -A
git commit -m "feat: ..."
git push origin dev   # opcional
```
En Hytale: activar "Adventure time DEV" en la lista de mods, probar.

### 2. Promover a main
```powershell
cd "C:\HytaleModding\Adventure time"
.\scripts\promote.ps1 -Version 0.0.4 -Message "feat: ..."
```
Mergea dev → main, bumpea Version en manifest.json a 0.0.4, commit + tag v0.0.4 + push.

### 3. Publicar a CurseForge
```powershell
.\scripts\publish.ps1 -SkipGitHub
# sin -SkipGitHub si gh CLI está instalado (crea GitHub release con zip adjunto)
```

### 4. Caso especial: nuevo build de Hytale rompe el mod
1. En dev, editar `manifest.json` → cambiar `ServerVersion` al nuevo string (visto en pantalla de inicio de Hytale)
2. Smoke test → si carga → promote + publish con bump de patch

### 5. Descartar dev
```powershell
cd "C:\HytaleModding\Adventure time-dev"
git reset --hard origin/main
```

## Setup faltante (una sola vez)

1. **Symlinks** — PowerShell como **Administrador**:
   ```powershell
   cd "C:\HytaleModding\Adventure time"
   .\scripts\install-symlinks.ps1
   ```

2. **gh CLI** (opcional):
   ```powershell
   winget install --id GitHub.cli
   gh auth login   # cuenta dackoro
   ```

## Credenciales / config

- **Git remote**: `github-hytale:dackoro/Adventure-Time.git` — usa SSH alias del `~/.ssh/config` que apunta al key `id_ed25519_github_hytale` y autentica como `dackoro`.
- **Git user.email** (local repo): `dackoro89@gmail.com` (vía `git config --local`)
- **CurseForge ProjectId**: 1448331
- **CurseForge API token**: en `.cf-token` (gitignored) o env var `CURSEFORGE_API_TOKEN`. Generado en https://authors.curseforge.com/account/api-tokens
- **CurseForge API base URL**: `https://authors-old.curseforge.com/api` (eternal/hypixel también funcionan)

## Quirks de Hytale + CurseForge que descubrimos

- **`ServerVersion` en manifest.json**: validado por string EXACTO desde Update 3. Cada build nuevo de Hytale rompe el mod. Alternativa: usar ModsVersionRange (requiere que los usuarios instalen otro mod), no la default.
- **`gameVersions` en upload API**: la doc dice required pero Hytale lo RECHAZA. Hay que omitir el campo entero. ServerVersion controla compat.
- **Endpoint upload**: `POST https://authors-old.curseforge.com/api/projects/<id>/upload-file`. `hytale.curseforge.com/api` da 404. Header: `X-Api-Token`.
- **Icono**: PNG 256×256 en la raíz como `icon-256.png`. NO se referencia desde manifest, Hytale auto-descubre. Webp en subcarpeta no funciona.
- **PowerShell 5.1**: no tiene `Invoke-RestMethod -Form`. `publish.ps1` construye multipart/form-data a mano.
- **Em-dashes (—) en scripts PS1**: PowerShell 5.1 los puede malinterpretar como bytes y romper el parser. Usar guión normal `-`.

## Restricciones que aprendimos

- `promote.ps1` falla si la working tree está dirty o no estás en branch main. Hay que commitear antes.
- El dev worktree mantiene `Name: "Adventure time DEV"` en manifest. Al promover, dev → main hace conflict en esa línea. PENDIENTE: hacer que promote.ps1 force el Name correcto en main post-merge (por ahora hay que resolverlo a mano o evitar promover sin trabajo real en dev).
- `IncludesAssetPack` está en `false`. Si los modelos/texturas no cargan, este es el primer sospechoso a cambiar.

## Estado actual del proyecto (v0.0.3)

- Branch main al día con icono, scripts, LICENSE, ServerVersion=2026.03.26-89796e57b
- v0.0.3 publicado a CurseForge (fileId 8123536), en moderation queue
- 3 alphas de prueba quedaron en CurseForge durante debugging — usuario los borra a mano vía web UI
- Tag `v0.0.3` pusheado a GitHub
