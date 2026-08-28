package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Cancels the drop request of a Grass Sword for cursed players outside Creative.
 */
public class CurseDropRequestSystem extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

    public CurseDropRequestSystem() {
        super(DropItemEvent.PlayerRequest.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, DropItemEvent.PlayerRequest event) {
        if (!CurseDropSystem.isSurvivalPlayer(chunk, store, entityIndex)) {
            return;
        }
        ItemStack stack = itemInSlot(store, chunk.getReferenceTo(entityIndex),
                event.getInventorySectionId(), event.getSlotId());
        if (GrassCursePlugin.isGrassSword(stack)) {
            event.setCancelled(true);
        }
    }

    private ItemStack itemInSlot(Store<EntityStore> store, Ref<EntityStore> ref, int sectionId, short slotId) {
        ComponentType<EntityStore, ? extends InventoryComponent> type = sectionComponentType(sectionId);
        if (type == null) {
            return null;
        }
        InventoryComponent component = store.getComponent(ref, type);
        if (component == null) {
            return null;
        }
        return component.getInventory().getItemStack(slotId);
    }

    private ComponentType<EntityStore, ? extends InventoryComponent> sectionComponentType(int sectionId) {
        switch (sectionId) {
            case InventoryComponent.HOTBAR_SECTION_ID:
                return InventoryComponent.Hotbar.getComponentType();
            case InventoryComponent.STORAGE_SECTION_ID:
                return InventoryComponent.Storage.getComponentType();
            case InventoryComponent.ARMOR_SECTION_ID:
                return InventoryComponent.Armor.getComponentType();
            case InventoryComponent.UTILITY_SECTION_ID:
                return InventoryComponent.Utility.getComponentType();
            case InventoryComponent.TOOLS_SECTION_ID:
                return InventoryComponent.Tool.getComponentType();
            case InventoryComponent.BACKPACK_SECTION_ID:
                return InventoryComponent.Backpack.getComponentType();
            default:
                return null;
        }
    }
}
