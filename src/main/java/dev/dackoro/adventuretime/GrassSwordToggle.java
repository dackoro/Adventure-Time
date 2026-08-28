package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ActiveSlotInventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;

/**
 * Sheathes / draws the cursed Grass Sword by changing its state in place
 * (the sword never needs a free inventory slot).
 * - toggling: held drawn -> sheathed, held sheathed -> drawn, not held -> drawn.
 * - auto: selecting the sword draws it, selecting another item sheathes it.
 * When sheathed, a ModelAttachment (the vaina blockymodel) is appended to the
 * player's rendered Model so the sheath renders on the arm without occupying
 * an armor slot or a cosmetic slot; it is removed again when drawn.
 */
public class GrassSwordToggle {

    private static final String STATE_SHEATHED = "Cursed_Sheathed";
    private static final String STATE_DRAWN = "Cursed_Drawn";

    /**
     * Always draws the Grass Sword: if it is held but sheathed, it is drawn in
     * place; otherwise the sword is swapped from wherever it is into the active
     * hand slot. Never sheathes.
     */
    public static void invoke(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        ActiveSlotInventoryComponent hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        byte activeSlot = hotbar.getActiveSlot();
        ItemStack held = hotbar.getInventory().getItemStack(activeSlot);

        if (GrassCursePlugin.isGrassSword(held)) {
            if (GrassCursePlugin.isGrassSwordSheathed(held)) {
                hotbar.getInventory().setItemStackForSlot(activeSlot, held.withState(STATE_DRAWN));
                unequipVaina(store, null, ref, playerRef);
                playerRef.sendMessage(Message.raw("The Grass Sword is drawn.").color("#7ac94f"));
            }
            return;
        }

        Slot found = findGrassSword(store, ref, activeSlot);
        if (found == null) {
            playerRef.sendMessage(Message.raw("The Grass Sword is not in your inventory.").color("#ff5252"));
            return;
        }
        ItemContainer hotbarInv = hotbar.getInventory();
        ItemStack current = hotbarInv.getItemStack(activeSlot);
        ItemStack drawn = found.stack.withState(STATE_DRAWN);
        // swap: the hand item moves to the sword's slot, the drawn sword goes to the hand.
        found.container.setItemStackForSlot(found.slot, current);
        hotbarInv.setItemStackForSlot(activeSlot, drawn);
        unequipVaina(store, null, ref, playerRef);
        playerRef.sendMessage(Message.raw("The Grass Sword is drawn.").color("#7ac94f"));
    }

    public static void toggle(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        GrassCursePlugin.get().getLogger().at(Level.INFO).log("VAINA: TOGGLE command");
        ActiveSlotInventoryComponent hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        byte activeSlot = hotbar.getActiveSlot();
        ItemStack held = hotbar.getInventory().getItemStack(activeSlot);

        if (GrassCursePlugin.isGrassSword(held)) {
            if (GrassCursePlugin.isGrassSwordSheathed(held)) {
                hotbar.getInventory().setItemStackForSlot(activeSlot, held.withState(STATE_DRAWN));
                unequipVaina(store, null, ref, playerRef);
                playerRef.sendMessage(Message.raw("The Grass Sword is drawn.").color("#7ac94f"));
            } else {
                hotbar.getInventory().setItemStackForSlot(activeSlot, held.withState(STATE_SHEATHED));
                equipVaina(store, null, ref, playerRef);
                playerRef.sendMessage(Message.raw("The Grass Sword is sheathed.").color("#7ac94f"));
            }
            return;
        }

        Slot found = findGrassSword(store, ref, activeSlot);
        if (found == null) {
            playerRef.sendMessage(Message.raw("The Grass Sword is not in your inventory.").color("#ff5252"));
            return;
        }
        ItemContainer hotbarInv = hotbar.getInventory();
        ItemStack current = hotbarInv.getItemStack(activeSlot);
        ItemStack drawn = found.stack.withState(STATE_DRAWN);
        // swap: the hand item moves to the sword's slot, the drawn sword goes to the hand.
        found.container.setItemStackForSlot(found.slot, current);
        hotbarInv.setItemStackForSlot(activeSlot, drawn);
        unequipVaina(store, null, ref, playerRef);
        playerRef.sendMessage(Message.raw("The Grass Sword is drawn.").color("#7ac94f"));
    }

    /**
     * Forces the Grass Sword sheathed (state change in place) and applies the
     * vaina cosmetic.
     */
    public static void sheathe(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        ActiveSlotInventoryComponent hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        byte activeSlot = hotbar.getActiveSlot();
        ItemStack held = hotbar.getInventory().getItemStack(activeSlot);
        if (GrassCursePlugin.isGrassSword(held)) {
            if (!GrassCursePlugin.isGrassSwordSheathed(held)) {
                hotbar.getInventory().setItemStackForSlot(activeSlot, held.withState(STATE_SHEATHED));
                equipVaina(store, null, ref, playerRef);
                playerRef.sendMessage(Message.raw("The Grass Sword is sheathed.").color("#7ac94f"));
            }
            return;
        }
        Slot found = findGrassSword(store, ref, activeSlot);
        if (found != null && !GrassCursePlugin.isGrassSwordSheathed(found.stack)) {
            found.container.setItemStackForSlot(found.slot, found.stack.withState(STATE_SHEATHED));
            equipVaina(store, null, ref, playerRef);
            playerRef.sendMessage(Message.raw("The Grass Sword is sheathed.").color("#7ac94f"));
        }
    }

    /**
     * Forces the Grass Sword drawn (in the hand) and removes the vaina cosmetic.
     */
    public static void draw(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        ActiveSlotInventoryComponent hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        byte activeSlot = hotbar.getActiveSlot();
        ItemStack held = hotbar.getInventory().getItemStack(activeSlot);
        if (GrassCursePlugin.isGrassSword(held)) {
            if (GrassCursePlugin.isGrassSwordSheathed(held)) {
                hotbar.getInventory().setItemStackForSlot(activeSlot, held.withState(STATE_DRAWN));
                unequipVaina(store, null, ref, playerRef);
                playerRef.sendMessage(Message.raw("The Grass Sword is drawn.").color("#7ac94f"));
            }
            return;
        }
        Slot found = findGrassSword(store, ref, activeSlot);
        if (found == null) {
            playerRef.sendMessage(Message.raw("The Grass Sword is not in your inventory.").color("#ff5252"));
            return;
        }
        ItemContainer hotbarInv = hotbar.getInventory();
        ItemStack current = hotbarInv.getItemStack(activeSlot);
        ItemStack drawn = found.stack.withState(STATE_DRAWN);
        found.container.setItemStackForSlot(found.slot, current);
        hotbarInv.setItemStackForSlot(activeSlot, drawn);
        unequipVaina(store, null, ref, playerRef);
        playerRef.sendMessage(Message.raw("The Grass Sword is drawn.").color("#7ac94f"));
    }

    /**
     * Auto behaviour on active slot change (no command needed):
     * - selecting the Grass Sword draws it,
     * - selecting any other item sheathes a drawn Grass Sword.
     */
    public static void autoHandleActiveSlot(Store<EntityStore> store, CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, PlayerRef playerRef, byte newSlot) {
        ActiveSlotInventoryComponent hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        ItemStack active = hotbar.getInventory().getItemStack(newSlot);
        if (GrassCursePlugin.isGrassSword(active)) {
            if (GrassCursePlugin.isGrassSwordSheathed(active)) {
                hotbar.getInventory().setItemStackForSlot(newSlot, active.withState(STATE_DRAWN));
                unequipVaina(store, buffer, ref, playerRef);
            }
        } else {
            Slot found = findGrassSword(store, ref, newSlot);
            if (found != null && !GrassCursePlugin.isGrassSwordSheathed(found.stack)) {
                found.container.setItemStackForSlot(found.slot, found.stack.withState(STATE_SHEATHED));
                equipVaina(store, buffer, ref, playerRef);
            }
        }
    }

    private static void equipVaina(Store<EntityStore> store, CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, PlayerRef playerRef) {
        GrassCursePlugin.get().getLogger().at(Level.INFO).log("VAINA: vaina disabled (model attachment approach reverted)");
    }

    private static void unequipVaina(Store<EntityStore> store, CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, PlayerRef playerRef) {
    }

    private static Slot findGrassSword(Store<EntityStore> store, Ref<EntityStore> ref, byte skipSlot) {
        InventoryComponent hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        ItemContainer hotbar = hotbarComp.getInventory();
        for (short s = 0; s < hotbar.getCapacity(); s++) {
            if (s == skipSlot) {
                continue;
            }
            ItemStack stack = hotbar.getItemStack(s);
            if (GrassCursePlugin.isGrassSword(stack)) {
                return new Slot(hotbar, s, stack);
            }
        }
        ItemContainer storage = store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory();
        for (short s = 0; s < storage.getCapacity(); s++) {
            ItemStack stack = storage.getItemStack(s);
            if (GrassCursePlugin.isGrassSword(stack)) {
                return new Slot(storage, s, stack);
            }
        }
        InventoryComponent backpackComp = store.getComponent(ref, InventoryComponent.Backpack.getComponentType());
        if (backpackComp != null) {
            ItemContainer backpack = backpackComp.getInventory();
            for (short s = 0; s < backpack.getCapacity(); s++) {
                ItemStack stack = backpack.getItemStack(s);
                if (GrassCursePlugin.isGrassSword(stack)) {
                    return new Slot(backpack, s, stack);
                }
            }
        }
        return null;
    }

    private static final class Slot {
        final ItemContainer container;
        final short slot;
        final ItemStack stack;

        Slot(ItemContainer container, short slot, ItemStack stack) {
            this.container = container;
            this.slot = slot;
            this.stack = stack;
        }
    }
}