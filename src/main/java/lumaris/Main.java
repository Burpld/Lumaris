package lumaris;

import lumaris.battlebox.ArenaManager;
import lumaris.battlebox.BattleBoxDeathListener;
import lumaris.battlebox.CenterBlockListener;
import lumaris.command.*;
import lumaris.kit.KitAbilities;
import lumaris.kit.KitSelector;
import lumaris.listener.BattleboxItems;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        PluginContext.INSTANCE.setInstance(this);
        ArenaManager.INSTANCE.load(this);

        Party partySystem = new Party(this);
        SpawnBattleItem spawnBattleItem = new SpawnBattleItem(this);
        FriendsCommand friendSystem = new FriendsCommand();
        BattleBoxSetup battleBoxSetup = new BattleBoxSetup(this);

        getServer().getPluginManager().registerEvents(partySystem, this);
        getServer().getPluginManager().registerEvents(new BattleboxItems(this), this);
        getServer().getPluginManager().registerEvents(KitSelector.INSTANCE, this);
        getServer().getPluginManager().registerEvents(new KitAbilities(this), this);
        getServer().getPluginManager().registerEvents(new CenterBlockListener(), this);
        getServer().getPluginManager().registerEvents(new BattleBoxDeathListener(), this);
        getLogger().info("Lumaris 1.21.11 Systems Enabled!");

        PluginCommand hubCommand = getCommand("hub");
        PluginCommand adminHubCommand = getCommand("adminhub");
        PluginCommand spawnNPCCommand = getCommand("spawnnpc");
        PluginCommand partyCommand = getCommand("party");
        PluginCommand partyChatCommand = getCommand("partychat");
        PluginCommand dolphinCommand = getCommand("dolphinfun");
        PluginCommand spawnBattleItemCommand = getCommand("spawnbattleitem");
        PluginCommand friendCommand = getCommand("friend");
        PluginCommand battleBoxCommand = getCommand("battlebox");

        if (hubCommand != null) {
            hubCommand.setExecutor(new Hub());
        }
        else {
            getLogger().warning("Hub is not a valid command");
        }

        if (adminHubCommand != null) {
            adminHubCommand.setExecutor(new AdminHub());
        }
        else {
            getLogger().warning("Admin Hub is not a valid command");
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

        if (dolphinCommand != null) {
            dolphinCommand.setExecutor(new Dolphin());
        }
        else {
            getLogger().warning("Dolphin Command is not a valid command");
        }

        if (spawnBattleItemCommand != null) {
            spawnBattleItemCommand.setExecutor(spawnBattleItem);
            spawnBattleItemCommand.setTabCompleter(spawnBattleItem);
        }
        else {
            getLogger().warning("Spawn Battle Item is not a valid command");
        }

        if (friendCommand != null) {
            friendCommand.setExecutor(friendSystem);
            friendCommand.setTabCompleter(friendSystem);
        }
        else {
            getLogger().warning("Friend Command is not a valid command");
        }

        if (battleBoxCommand != null) {
            battleBoxCommand.setExecutor(battleBoxSetup);
            battleBoxCommand.setTabCompleter(battleBoxSetup);
        }
        else {
            getLogger().warning("Battle Box Command is not a valid command");
        }
    }
}
