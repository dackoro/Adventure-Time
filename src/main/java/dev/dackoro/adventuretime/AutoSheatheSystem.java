package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventorySetActiveSlotEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Auto-sheathes and draws the cursed Grass Sword when the active hotbar slot
 * changes: selecting the sword draws it, selecting anything else sheathes it.
 */
public class AutoSheatheSystem extends EntityEventSystem<EntityStore, InventorySetActiveSlotEvent> {

    public AutoSheatheSystem() {
        super(InventorySetActiveSlotEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, InventorySetActiveSlotEvent event) {
        if (event.getInventorySectionId() != InventoryComponent.HOTBAR_SECTION_ID) {
            return;
        }
        Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !GrassCursePlugin.isCursed(playerRef.getUuid())) {
            return;
        }
        GrassSwordToggle.autoHandleActiveSlot(store, buffer, ref, playerRef, event.getNewSlot());
    }
}
