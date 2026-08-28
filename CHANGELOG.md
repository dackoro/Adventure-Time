# Changelog

## [0.0.7]

### ✨ Grass Sword Maldita (nueva mecánica)
- **Infalible**: durabilidad infinita (`MaxDurability: 0`), no se daña ni se pierde con la muerte.
- **Inseparable**: no se puede soltar ni eliminar del inventario.
- **Reaparición garantizada**: si se pierde de algún modo, el sistema la re-grantea automáticamente (siempre hay ≥1 en el inventario).
- **Efecto de maldición**: estado `Grass_Sword_Curse` persistente con icono propio.

### 🛠️ Técnico
- El mod pasó de pack de contenido a **plugin Java 25** (Gradle wrapper, `Main: dev.dackoro.adventuretime.GrassCursePlugin`, `IncludesAssetPack: true`).
- Nuevos sonidos VFX de impacto/carga/ocultar para la espada.

### 🧪 Notas
- La vaina visual (modelo en el brazo al envainar) queda **aparcada** (el registro cosmético nativo no acepta modelos custom aún). Se retomará en una versión futura.
- Eliminado el estado "sheathed" → la espada muestra un solo icono.

---

## [0.0.6]

- Sonidos de impacto específicos por arma (Finn, Night, Tree).
- Trails por golpe y VFX de firma para todas las espadas.
- Balance de estadísticas por tier de calidad.