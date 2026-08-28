package dev.dackoro.adventuretime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Persists the list of cursed player UUIDs and the sheathe command name to the
 * plugin's config file.
 */
public class CurseConfig {

    public static final BuilderCodec<CurseConfig> CODEC = BuilderCodec.builder(CurseConfig.class, CurseConfig::new)
            .append(new KeyedCodec<>("CursedPlayers", Codec.STRING_ARRAY),
                    CurseConfig::setCursedPlayers,
                    CurseConfig::getCursedPlayers)
            .add()
            .append(new KeyedCodec<>("SheatheCommand", Codec.STRING),
                    CurseConfig::setSheatheCommand,
                    CurseConfig::getSheatheCommand)
            .add()
            .append(new KeyedCodec<>("AbilitySlot", Codec.INTEGER),
                    CurseConfig::setAbilitySlot,
                    CurseConfig::getAbilitySlot)
            .add()
            .build();

    private String[] cursedPlayers = new String[0];
    private String sheatheCommand = "grass";
    private int abilitySlot = 0;

    public CurseConfig() {
    }

    public String[] getCursedPlayers() {
        return cursedPlayers;
    }

    public void setCursedPlayers(String[] cursedPlayers) {
        this.cursedPlayers = cursedPlayers;
    }

    public String getSheatheCommand() {
        return sheatheCommand;
    }

    public void setSheatheCommand(String sheatheCommand) {
        this.sheatheCommand = sheatheCommand;
    }

    public int getAbilitySlot() {
        return abilitySlot;
    }

    public void setAbilitySlot(int abilitySlot) {
        this.abilitySlot = abilitySlot;
    }
}
