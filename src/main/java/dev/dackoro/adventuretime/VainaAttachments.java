package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the Grass Sword sheath on the player's forearm without using an armor or
 * cosmetic slot, by injecting a custom {@link ModelAttachment} directly into the
 * player's {@link Model} - the same low-level mechanism the native cosmetic system
 * itself uses.
 * <p>
 * Hytale's own {@code CosmeticRegistry} does not accept cosmetics added by mods (confirmed:
 * registering a custom {@code Gloves.json} left the registry size unchanged), so this can't be
 * wired through the normal cosmetic pipeline. Instead this class rebuilds a fresh {@link Model}
 * on every toggle: every native skin part ({@code bodyCharacteristic}, {@code haircut}, {@code
 * gloves}, etc.) is re-resolved straight from {@link PlayerSkin} via {@link CosmeticRegistry} +
 * {@link ModelUtils}, and the vaina attachment is appended on top only while sheathed.
 * <p>
 * Rebuilding from {@code PlayerSkin} (not from the current {@code Model}'s attachment list) is
 * the important part: {@code Model.getAttachments()} only holds <em>extra</em> attachments, not
 * the full skin, so reusing it silently drops the player's actual skin - this was tried and
 * confirmed broken (see {@code pending.md}). Because we always rebuild from the source of truth,
 * the model self-heals on every call and never persists a corrupted skin.
 * <p>
 * Core rebuild logic ported from GoodWitchLalya/LalyanCosmeticCore (AGPL-3.0),
 * {@code AttachmentsRegistry#rebuildSkinWithCosmetics} / {@code #restoreSkinWithOverrides}
 * (https://github.com/GoodWitchLalya/LalyanCosmeticCore), trimmed down to this mod's single
 * hardcoded attachment - we don't need Lalyan's cosmetic registry, menu, or per-slot override
 * bookkeeping since we never take over a native slot.
 */
final class VainaAttachments {

    private static final String VAINA_MODEL = "Resources/Grass_Sword_Sheath/grass-sword-arm.blockymodel";
    private static final String VAINA_TEXTURE = "Resources/Grass_Sword_Sheath/grass-sword-arm.png";

    private VainaAttachments() {
    }

    /**
     * Rebuilds the player's Model, with the vaina attachment present iff {@code sheathed}.
     * Call with a non-null {@code buffer} from inside an ECS system (e.g. AutoSheatheSystem);
     * pass {@code buffer = null} when called directly from a command handler (Store.replaceComponent
     * outside a system, CommandBuffer.replaceComponent inside one - mixing them throws
     * "Store is currently processing!").
     */
    static void setSheathed(Store<EntityStore> store, CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, PlayerRef playerRef, boolean sheathed) {
        ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
        PlayerSkinComponent skinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (playerRef == null || modelComponent == null || skinComponent == null) {
            return;
        }
        Model model = modelComponent.getModel();
        PlayerSkin skin = skinComponent.getPlayerSkin();
        if (model == null || skin == null) {
            return;
        }

        List<ModelAttachment> attachments = new ArrayList<>();
        restoreSkin(skin, attachments);

        if (sheathed) {
            attachments.add(new ModelAttachment(VAINA_MODEL, VAINA_TEXTURE, "", "", 1));
        }

        Model newModel = new Model(
                playerRef.getUsername() + "_VainaModel",
                model.getScale(),
                model.getRandomAttachmentIds(),
                attachments.toArray(new ModelAttachment[0]),
                model.getBoundingBox(),
                model.getModel(),
                model.getTexture(),
                model.getGradientSet(),
                model.getGradientId(),
                model.getEyeHeight(),
                model.getCrouchOffset(),
                model.getSittingOffset(),
                model.getSleepingOffset(),
                model.getAnimationSetMap(),
                model.getCamera(),
                model.getLight(),
                model.getParticles(),
                model.getTrails(),
                model.getPhysicsValues(),
                model.getDetailBoxes(),
                model.getPhobia(),
                model.getPhobiaModelAssetId()
        );

        if (buffer != null) {
            buffer.replaceComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
        } else {
            store.replaceComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
        }
    }

    /**
     * Re-derives every native skin-part attachment straight from {@code PlayerSkin}, mirroring
     * LalyanCosmeticCore's {@code restoreSkinWithOverrides} minus the override map (we never
     * claim a native slot, so every part is always restored as-is).
     */
    private static void restoreSkin(PlayerSkin skin, List<ModelAttachment> attachments) {
        CosmeticRegistry registry = CosmeticsModule.get().getRegistry();
        String[] bodyCharacteristicParts = skin.bodyCharacteristic.split("\\.");
        String gradientId = bodyCharacteristicParts[1];

        var bodyCharacteristic = registry.getBodyCharacteristics().get(bodyCharacteristicParts[0]);
        if (bodyCharacteristic != null) {
            attachments.add(ModelUtils.resolveAttachment(bodyCharacteristic, bodyCharacteristicParts, gradientId));
        }

        if (skin.facialHair != null) {
            String[] parts = skin.facialHair.split("\\.");
            var part = registry.getFacialHairs().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.ears != null) {
            String[] parts = skin.ears.split("\\.");
            var part = registry.getEars().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.eyebrows != null) {
            String[] parts = skin.eyebrows.split("\\.");
            var part = registry.getEyebrows().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.eyes != null) {
            String[] parts = skin.eyes.split("\\.");
            var part = registry.getEyes().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.face != null) {
            String[] parts = skin.face.split("\\.");
            var part = registry.getFaces().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.mouth != null) {
            String[] parts = skin.mouth.split("\\.");
            var part = registry.getMouths().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.haircut != null) {
            String[] parts = skin.haircut.split("\\.");
            var part = registry.getHaircuts().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.cape != null) {
            String[] parts = skin.cape.split("\\.");
            var part = registry.getCapes().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.faceAccessory != null) {
            String[] parts = skin.faceAccessory.split("\\.");
            var part = registry.getFaceAccessories().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.gloves != null) {
            String[] parts = skin.gloves.split("\\.");
            var part = registry.getGloves().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.headAccessory != null) {
            String[] parts = skin.headAccessory.split("\\.");
            var part = registry.getHeadAccessories().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.overpants != null) {
            String[] parts = skin.overpants.split("\\.");
            var part = registry.getOverpants().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.overtop != null) {
            String[] parts = skin.overtop.split("\\.");
            var part = registry.getOvertops().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.pants != null) {
            String[] parts = skin.pants.split("\\.");
            var part = registry.getPants().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.shoes != null) {
            String[] parts = skin.shoes.split("\\.");
            var part = registry.getShoes().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.undertop != null) {
            String[] parts = skin.undertop.split("\\.");
            var part = registry.getUndertops().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.underwear != null) {
            String[] parts = skin.underwear.split("\\.");
            var part = registry.getUnderwear().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.earAccessory != null) {
            String[] parts = skin.earAccessory.split("\\.");
            var part = registry.getEarAccessories().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }

        if (skin.skinFeature != null) {
            String[] parts = skin.skinFeature.split("\\.");
            var part = registry.getSkinFeatures().get(parts[0]);
            if (part != null) {
                attachments.add(ModelUtils.resolveAttachment(part, parts, gradientId));
            }
        }
    }
}
