package lumaris;

import lumaris.command.*;
import lumaris.listener.BattleboxItems;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        Party partySystem = new Party(this);
        SpawnBattleItem spawnBattleItem = new SpawnBattleItem(this);
        FriendsCommand friendsCommand = new FriendsCommand(partySystem);

        getServer().getPluginManager().registerEvents(partySystem, this);
        getServer().getPluginManager().registerEvents(new BattleboxItems(this), this);
        getLogger().info("Lumaris 1.21.11 Systems Enabled!");

        PluginCommand hubCommand = getCommand("hub");
        PluginCommand adminHubCommand = getCommand("adminhub");
        PluginCommand spawnNPCCommand = getCommand("spawnnpc");
        PluginCommand partyCommand = getCommand("party");
        PluginCommand partyChatCommand = getCommand("partychat");
        PluginCommand dolphinCommand = getCommand("dolphinfun");
        PluginCommand spawnBattleItemCommand = getCommand("spawnbattleitem");
        PluginCommand viewFriendsCmd = getCommand("viewfriends");
        PluginCommand addFriendCmd = getCommand("addfriend");
        PluginCommand removeFriendCmd = getCommand("removefriend");
        PluginCommand inviteFriendCmd = getCommand("invitefriend");
        PluginCommand acceptFriendCmd = getCommand("acceptfriend");
        PluginCommand declineFriendCmd = getCommand("declinefriend");
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
        if (viewFriendsCmd != null) {
            viewFriendsCmd.setExecutor(friendsCommand);
            viewFriendsCmd.setTabCompleter(friendsCommand); // Added
        }
        if (addFriendCmd != null) {
            addFriendCmd.setExecutor(friendsCommand);
            addFriendCmd.setTabCompleter(friendsCommand); // Added
        }
        if (removeFriendCmd != null) {
            removeFriendCmd.setExecutor(friendsCommand);
            removeFriendCmd.setTabCompleter(friendsCommand); // Added
        }
        if (inviteFriendCmd != null) {
            inviteFriendCmd.setExecutor(friendsCommand);
            inviteFriendCmd.setTabCompleter(friendsCommand); // Added
        }
        if (acceptFriendCmd != null) {
            acceptFriendCmd.setExecutor(friendsCommand);
            acceptFriendCmd.setTabCompleter(friendsCommand); // Added
        }
        if (declineFriendCmd != null) {
            declineFriendCmd.setExecutor(friendsCommand);
            declineFriendCmd.setTabCompleter(friendsCommand); // Added
        }
    }
}