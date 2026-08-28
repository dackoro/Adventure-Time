package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.GameMode;

/**
 * Cancels the actual drop of a Grass Sword for cursed players outside Creative.
 */
public class CurseDropSystem extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {

    public CurseDropSystem() {
        super(DropItemEvent.Drop.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, DropItemEvent.Drop event) {
        ItemStack stack = event.getItemStack();
        if (!GrassCursePlugin.isGrassSword(stack)) {
            return;
        }
        if (!isSurvivalPlayer(chunk, store, entityIndex)) {
            return;
        }
        event.setCancelled(true);
        PlayerRef playerRef = store.getComponent(chunk.getReferenceTo(entityIndex), PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw("The Grass Sword refuses to leave your hand.").color("#7ac94f"));
        }
    }

    static boolean isSurvivalPlayer(ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, int index) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !GrassCursePlugin.isCursed(playerRef.getUuid())) {
            return false;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        return player == null || player.getGameMode() != GameMode.Creative;
    }
}
