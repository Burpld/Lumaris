package lumaris;

import lumaris.command.Hub;
import lumaris.command.Party;
import lumaris.command.PartyChat;
import lumaris.command.SpawnNPC;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        Party partySystem = new Party(this);

        getServer().getPluginManager().registerEvents(partySystem, this);
        getLogger().info("Lumaris 1.21.11 Systems Enabled!");

        PluginCommand hubCommand = getCommand("hub");
        PluginCommand spawnNPCCommand = getCommand("spawnnpc");
        PluginCommand partyCommand = getCommand("party");
        PluginCommand partyChatCommand = getCommand("partychat");

        if (hubCommand != null) {
            hubCommand.setExecutor(new Hub());
        }
        else {
            getLogger().warning("Hub is not a valid command");
        }

        if (spawnNPCCommand != null) {
            spawnNPCCommand.setExecutor(new SpawnNPC());
        }
        else {
            getLogger().warning("Spawn NPC is not a valid command");
        }

        if (partyCommand != null) {
            partyCommand.setExecutor(partySystem);
            partyCommand.setTabCompleter(partySystem);
        }
        else {
            getLogger().warning("Party is not a valid command");
        }

        if (partyChatCommand != null) {
            partyChatCommand.setExecutor(new PartyChat(partySystem));
        }
        else {
            getLogger().warning("Party chat is not a valid command");
        }
    }
}