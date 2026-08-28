package dev.dackoro.adventuretime;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GrassCommand extends AbstractPlayerCommand {

    public GrassCommand(String name) {
        super(name, "Adventure Time Grass Sword commands");
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef playerRef, World world) {
        String input = context.getInputString();
        if (input != null) {
            String lower = input.toLowerCase();
            if (lower.contains("false")) {
                GrassSwordToggle.sheathe(store, ref, playerRef);
                return;
            }
            if (lower.contains("true")) {
                GrassSwordToggle.draw(store, ref, playerRef);
                return;
            }
        }
        GrassSwordToggle.toggle(store, ref, playerRef);
    }
}
