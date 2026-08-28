package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * A dropped Grass Sword can be picked back up while still in its sheathed item
 * state (e.g. after a Creative-mode drop). Sheathed is only a meaningful state
 * while the curse's own toggle logic is tracking it in inventory; once it's back
 * in a player's hand there's no vaina model applied for it (that's driven by the
 * player's Model, not the item), so a picked-up sheathed sword would otherwise
 * get stuck showing the sheathed icon forever with no vaina to match. Normalize
 * it back to drawn on pickup - the usual auto-sheathe logic takes over from there.
 */
public class CursePickupSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    public CursePickupSystem() {
        super(InteractivelyPickupItemEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, InteractivelyPickupItemEvent event) {
        ItemStack stack = event.getItemStack();
        if (GrassCursePlugin.isGrassSwordSheathed(stack)) {
            event.setItemStack(GrassSwordToggle.toDrawn(stack));
        }
    }
}
