package dev.dackoro.adventuretime;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class GrassCursePlugin extends JavaPlugin {

    public static final String GRASS_SWORD_ID = "Grass_Sword";

    private static final Set<UUID> CURSED_PLAYERS = new HashSet<>();
    private static GrassCursePlugin instance;

    private Config<CurseConfig> curseConfig;

    public GrassCursePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.curseConfig = withConfig("grass_curse", CurseConfig.CODEC);
    }

    public static GrassCursePlugin get() {
        return instance;
    }

    public static boolean isCursed(UUID uuid) {
        return CURSED_PLAYERS.contains(uuid);
    }

    /**
     * True if the stack is the Grass Sword in any state (drawn base or sheathed state).
     */
    public static boolean isGrassSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (GRASS_SWORD_ID.equals(stack.getItemId())) {
            return true;
        }
        Item base = Item.getAssetMap().getAsset(GRASS_SWORD_ID);
        return base != null && base.getStateForItem(stack.getItemId()) != null;
    }

    /**
     * True if the stack is the Grass Sword in the sheathed state.
     */
    public static boolean isGrassSwordSheathed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (GRASS_SWORD_ID.equals(stack.getItemId())) {
            return false;
        }
        Item base = Item.getAssetMap().getAsset(GRASS_SWORD_ID);
        return base != null && "Cursed_Sheathed".equals(base.getStateForItem(stack.getItemId()));
    }

    public static void setCursed(UUID uuid, boolean cursed) {
        boolean changed;
        if (cursed) {
            changed = CURSED_PLAYERS.add(uuid);
        } else {
            changed = CURSED_PLAYERS.remove(uuid);
        }
        if (changed && instance != null) {
            instance.persistCursed();
        }
    }

    private void persistCursed() {
        CurseConfig config = curseConfig.get();
        config.setCursedPlayers(CURSED_PLAYERS.stream().map(UUID::toString).toArray(String[]::new));
        curseConfig.save();
    }

    @Override
    protected void setup() {
        CurseConfig config = curseConfig.load().join();
        if (config.getCursedPlayers() != null) {
            for (String id : config.getCursedPlayers()) {
                try {
                    CURSED_PLAYERS.add(UUID.fromString(id));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed ids
                }
            }
        }
        curseConfig.save();

        getEntityStoreRegistry().registerSystem(new CurseTickSystem());
        getEntityStoreRegistry().registerSystem(new CurseDropSystem());
        getEntityStoreRegistry().registerSystem(new CurseDropRequestSystem());

        getLogger().at(Level.INFO).log("Adventure Time - Grass Sword curse plugin loaded "
                + "(persisted cursed players: " + CURSED_PLAYERS.size() + ")");
    }
}
