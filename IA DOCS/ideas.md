# Ideas / Backlog

Ideas que no entraron en la iteración actual. Se priorizan cuando toque.

## Inmediatas (próxima iteración)

- **Glow de firma** por espada: `ItemAppearanceConditions` con `SignatureEnergy [100,100]` + `ModelVFXId: "Sword_Signature_Status"` y partículas por espada (verde/plata/violeta/oro/blanco).
- **Sonidos de impacto diferenciados** por espada (hoy todas usan `SFX_Sword_T2_Impact`). Reutilizar tiers stock T1/T2/T3 o añadir `.ogg` propios.
- **Trails — wiring pendiente**: el campo `Trails` a nivel de item **NO es válido** (rompe el decode del item: "Failed to decode asset"). Los assets `Server/Entity/Trails/*.json` cargan bien pero no están conectados a nada. Hay que engancharlos por **interacción** (Effects de los swings): investigar cómo lo referencia una espada stock (p.ej. `Medium_Sword_Basic`) y luego añadir el campo correcto al item o a la interacción.

## Fase 3 — Combos + hitboxes custom

- Cadenas de interacción propias por espada (Root → Charging → Chaining → Simple) con `ItemAnimationId` y timing propios.
- `Selector` custom por golpe para hitboxes más grandes/precisas.
- Trails/partículas atados a cada golpe de la cadena.

## Grass maldita (requiere plugin Java)

- Item no dropeable / siempre en inventario: slot filters `REMOVE`/`DROP` + `SlotFilter.DENY`; re-añadir al morir/reconectar.
- Tecla custom para envainar/desplegar: `Ability2`/`Ability3` o keybind del plugin.
- Modelo envainado en el brazo: item de armadura slot `Hands` con el modelo de la hoja plegada en el antebrazo.
- Cambio de icono por estado: item `State` variants (`Cursed_Sheathed` / `Cursed_Drawn`) con su `Icon`/`Model`/`Animation`.
- Al desplegar, reemplaza cualquier espada en la mano (mover `getItemInHand()` → storage, meter Grass en slot activo).
- Animación de recoger/desplegar: `.blockyanim` por estado (+ confirmar one-shot al cambiar de estado).

## Futuro

- Prefab + boss (elemental de hierba / "rey árbol enfadado") que dropee un upgrade de la Grass.
- Partículas de impacto custom por espada (verificar IDs stock primero: `Impact_Sword_Basic`, `Block_Break_Grass`, etc.).
- "Grass blade extend": animar la hoja de la Grass en Blockbench (que crezca en la firma, como en la serie).
