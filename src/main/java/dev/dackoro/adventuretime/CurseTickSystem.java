package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.GameMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs once per second per player:
 * - marks the player as cursed when they own a Grass Sword and applies the curse effect,
 * - re-grants a Grass Sword if a cursed player is missing it (survival),
 * - clears the curse if a cursed player got rid of it while in Creative,
 * - keeps a DENY drop filter on the slot holding the Grass Sword so it can never be
 *   dropped (manually or on death), preventing duplicates.
 */
public class CurseTickSystem extends EntityTickingSystem<EntityStore> {

    private static final AtomicInteger TICK_COUNTER = new AtomicInteger();
    private static final String CURSE_EFFECT_ID = "Grass_Sword_Curse";
    private static final Map<UUID, Map<ItemContainer, Short>> PROTECTED_SLOTS = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void tick(float delta, int entityIndex, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        if (TICK_COUNTER.incrementAndGet() % 20 != 0) {
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();

        CombinedItemContainer inventory = InventoryComponent.getCombined(
                store, ref, InventoryComponent.HOTBAR_STORAGE_BACKPACK);
        int count = inventory.countItemStacks(GrassCursePlugin::isGrassSword);

        if (count >= 1) {
            if (!GrassCursePlugin.isCursed(uuid)) {
                GrassCursePlugin.setCursed(uuid, true);
                playerRef.sendMessage(Message.raw("The Grass Sword has bound itself to you...").color("#7ac94f"));
            }
            applyCurseEffect(store, ref);
            updateDropProtection(store, ref, uuid, true);
            return;
        }

        if (!GrassCursePlugin.isCursed(uuid)) {
            updateDropProtection(store, ref, uuid, false);
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && player.getGameMode() == GameMode.Creative) {
            GrassCursePlugin.setCursed(uuid, false);
            removeCurseEffect(store, ref);
            updateDropProtection(store, ref, uuid, false);
            playerRef.sendMessage(Message.raw("The Grass Sword releases you from its curse.").color("#7ac94f"));
        } else {
            Player.giveItem(new ItemStack(GrassCursePlugin.GRASS_SWORD_ID), ref, store);
            updateDropProtection(store, ref, uuid, true);
            playerRef.sendMessage(Message.raw("The Grass Sword returns to you.").color("#7ac94f"));
        }
    }

    private void updateDropProtection(Store<EntityStore> store, Ref<EntityStore> ref, UUID uuid, boolean protect) {
        Map<ItemContainer, Short> protectedSlots = PROTECTED_SLOTS.get(uuid);
        if (protectedSlots != null) {
            for (Map.Entry<ItemContainer, Short> entry : protectedSlots.entrySet()) {
                entry.getKey().setSlotFilter(FilterActionType.DROP, entry.getValue(), SlotFilter.ALLOW);
            }
            PROTECTED_SLOTS.remove(uuid);
        }
        if (!protect) {
            return;
        }
        for (InventoryComponent section : sections(store, ref)) {
            if (section == null) {
                continue;
            }
            ItemContainer inv = section.getInventory();
            if (inv == null) {
                continue;
            }
            for (short slot = 0; slot < inv.getCapacity(); slot++) {
                ItemStack stack = inv.getItemStack(slot);
                if (GrassCursePlugin.isGrassSword(stack)) {
                    inv.setSlotFilter(FilterActionType.DROP, slot, SlotFilter.DENY);
                    Map<ItemContainer, Short> newProtection = new HashMap<>();
                    newProtection.put(inv, slot);
                    PROTECTED_SLOTS.put(uuid, newProtection);
                    return;
                }
            }
        }
    }

    private InventoryComponent[] sections(Store<EntityStore> store, Ref<EntityStore> ref) {
        return new InventoryComponent[]{
                store.getComponent(ref, InventoryComponent.Hotbar.getComponentType()),
                store.getComponent(ref, InventoryComponent.Storage.getComponentType()),
                store.getComponent(ref, InventoryComponent.Backpack.getComponentType())
        };
    }

    private void applyCurseEffect(Store<EntityStore> store, Ref<EntityStore> ref) {
        EffectControllerComponent effects = store.getComponent(ref, EffectControllerComponent.getComponentType());
        EntityEffect curse = EntityEffect.getAssetMap().getAsset(CURSE_EFFECT_ID);
        if (effects != null && curse != null && !effects.hasEffect(curse)) {
            effects.addInfiniteEffect(ref, EntityEffect.getAssetMap().getIndex(CURSE_EFFECT_ID), curse, store);
        }
    }

    private void removeCurseEffect(Store<EntityStore> store, Ref<EntityStore> ref) {
        EffectControllerComponent effects = store.getComponent(ref, EffectControllerComponent.getComponentType());
        EntityEffect curse = EntityEffect.getAssetMap().getAsset(CURSE_EFFECT_ID);
        if (effects != null && curse != null) {
            effects.removeEffect(ref, EntityEffect.getAssetMap().getIndex(CURSE_EFFECT_ID), store);
        }
    }
}
