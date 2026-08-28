package dev.dackoro.adventuretime;

import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture;

/**
 * Resolves a player's skin-part strings (e.g. "haircut.Braids.3") into a Hytale
 * ModelAttachment, the same way the native cosmetic system does.
 * <p>
 * Ported from GoodWitchLalya/LalyanCosmeticCore (AGPL-3.0), {@code util/ModelUtils.java}
 * (https://github.com/GoodWitchLalya/LalyanCosmeticCore), so {@link VainaAttachments} can
 * rebuild a player's full vanilla skin from {@code PlayerSkin} without going through the
 * native {@code CosmeticRegistry} (which does not accept mod-added cosmetics).
 */
final class ModelUtils {

    private ModelUtils() {
    }

    static ModelAttachment fromPlayerSkinPart(PlayerSkinPart part, String gradientId) {
        return new ModelAttachment(
                part.getModel(),
                part.getGreyscaleTexture(),
                part.getGradientSet(),
                gradientId,
                1
        );
    }

    static ModelAttachment resolveAttachment(PlayerSkinPart part, String[] parts, String defaultGradientId) {
        String p1 = parts.length > 1 ? parts[1] : null;
        String p2 = parts.length > 2 ? parts[2] : null;

        if (p1 != null && part.getVariants() != null && part.getVariants().containsKey(p1)) {
            PlayerSkinPart.Variant variant = part.getVariants().get(p1);

            if (p2 != null && variant.getTextures() != null && variant.getTextures().containsKey(p2)) {
                PlayerSkinPartTexture texture = variant.getTextures().get(p2);
                return new ModelAttachment(
                        variant.getModel(),
                        texture.getTexture(),
                        part.getGradientSet(),
                        defaultGradientId,
                        1
                );
            }

            return new ModelAttachment(
                    variant.getModel(),
                    variant.getGreyscaleTexture(),
                    part.getGradientSet(),
                    p2 != null ? p2 : defaultGradientId,
                    1
            );
        }

        if (p2 != null && part.getVariants() != null && part.getVariants().containsKey(p2)) {
            PlayerSkinPart.Variant variant = part.getVariants().get(p2);

            if (p1 != null && variant.getTextures() != null && variant.getTextures().containsKey(p1)) {
                PlayerSkinPartTexture texture = variant.getTextures().get(p1);
                return new ModelAttachment(
                        variant.getModel(),
                        texture.getTexture(),
                        part.getGradientSet(),
                        defaultGradientId,
                        1
                );
            }

            return new ModelAttachment(
                    variant.getModel(),
                    variant.getGreyscaleTexture(),
                    part.getGradientSet(),
                    p1,
                    1
            );
        }

        if (p1 != null && part.getTextures() != null && part.getTextures().containsKey(p1)) {
            PlayerSkinPartTexture texture = part.getTextures().get(p1);
            return new ModelAttachment(
                    part.getModel(),
                    texture.getTexture(),
                    part.getGradientSet(),
                    p2 != null ? p2 : defaultGradientId,
                    1
            );
        }

        if (p2 != null && part.getTextures() != null && part.getTextures().containsKey(p2)) {
            PlayerSkinPartTexture texture = part.getTextures().get(p2);
            return new ModelAttachment(
                    part.getModel(),
                    texture.getTexture(),
                    part.getGradientSet(),
                    p1,
                    1
            );
        }

        return fromPlayerSkinPart(part, p1 != null ? p1 : defaultGradientId);
    }
}
