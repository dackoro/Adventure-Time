package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ActiveSlotInventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * {@code InventorySetActiveSlotEvent} only fires when the active slot INDEX
 * changes (scroll wheel, number key) - not when a slot's contents change while it
 * stays active (dragging the Grass Sword into/out of the already-active slot via
 * the inventory UI). Re-running the same auto sheathe/draw check on every
 * inventory change makes moving the sword by hand behave the same as switching
 * to it normally.
 */
public class CurseInventoryChangeSystem extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

    public CurseInventoryChangeSystem() {
        super(InventoryChangeEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, InventoryChangeEvent event) {
        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !GrassCursePlugin.isCursed(playerRef.getUuid())) {
            return;
        }
        ActiveSlotInventoryComponent hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            return;
        }
        GrassSwordToggle.autoHandleActiveSlot(store, buffer, ref, playerRef, hotbar.getActiveSlot());
    }
}
