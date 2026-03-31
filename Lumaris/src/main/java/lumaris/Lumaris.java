package lumaris;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class Lumaris extends JavaPlugin {

    // ------------------- SPAWN -------------------
    private final String WORLD_NAME = "world";
    private final double X = -97.5;
    private final double Y = 178;
    private final double Z = -224.5;
    private final float YAW = -180;
    private final float PITCH = 1;

    // ------------------- PARTY -------------------
    private final HashMap<Player, Player> partyMap = new HashMap<>();
    private final HashMap<Player, Player> inviteMap = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Lumaris enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Lumaris disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        // ------------------- SPAWN COMMAND -------------------
        if (command.getName().equalsIgnoreCase("spawn")) {
            World world = Bukkit.getWorld(WORLD_NAME);
            if (world == null) {
                player.sendMessage("§cSpawn world not found!");
                return true;
            }
            Location spawnLocation = new Location(world, X, Y, Z, YAW, PITCH);
            player.teleport(spawnLocation);
            player.sendMessage("§aTeleported to spawn!");
            return true;
        }

        // ------------------- PARTY COMMAND -------------------
        if (command.getName().equalsIgnoreCase("party")) {

            if (args.length == 0) {
                player.sendMessage("§eUsage: /party create|invite|accept|leave|disband|chat <message>");
                return true;
            }

            String sub = args[0].toLowerCase();
            switch (sub) {

                case "create":
                    if (partyMap.containsKey(player)) {
                        player.sendMessage("§cYou are already in a party!");
                        return true;
                    }
                    partyMap.put(player, player);
                    player.sendMessage("§aParty created! You are the leader.");
                    break;

                case "invite":
                    if (args.length < 2) {
                        player.sendMessage("§eUsage: /party invite <player>");
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null || !target.isOnline()) {
                        player.sendMessage("§cPlayer not found.");
                        return true;
                    }
                    if (!partyMap.containsKey(player) || !partyMap.get(player).equals(player)) {
                        player.sendMessage("§cOnly party leaders can invite.");
                        return true;
                    }
                    if (partyMap.containsKey(target)) {
                        player.sendMessage("§cThat player is already in a party.");
                        return true;
                    }
                    inviteMap.put(target, player);
                    target.sendMessage("§aYou have been invited to " + player.getName() + "'s party! Use /party accept to join.");
                    player.sendMessage("§aInvitation sent to " + target.getName());
                    break;

                case "accept":
                    if (!inviteMap.containsKey(player)) {
                        player.sendMessage("§cYou have no party invitations.");
                        return true;
                    }
                    Player leader = inviteMap.remove(player);
                    partyMap.put(player, leader);
                    player.sendMessage("§aJoined " + leader.getName() + "'s party!");
                    leader.sendMessage("§a" + player.getName() + " joined your party!");
                    break;

                case "leave":
                    if (!partyMap.containsKey(player)) {
                        player.sendMessage("§cYou are not in a party.");
                        return true;
                    }
                    Player currentLeader = partyMap.get(player);
                    partyMap.remove(player);
                    player.sendMessage("§aYou left the party.");
                    if (!player.equals(currentLeader)) {
                        currentLeader.sendMessage("§e" + player.getName() + " left your party.");
                    } else {
                        Set<Player> members = new HashSet<>();
                        for (var entry : partyMap.entrySet()) {
                            if (entry.getValue().equals(player)) members.add(entry.getKey());
                        }
                        for (Player m : members) {
                            partyMap.remove(m);
                            m.sendMessage("§cThe party has been disbanded because the leader left.");
                        }
                        player.sendMessage("§aYou disbanded your party.");
                    }
                    break;

                case "disband":
                    if (!partyMap.containsKey(player) || !partyMap.get(player).equals(player)) {
                        player.sendMessage("§cOnly party leaders can disband.");
                        return true;
                    }
                    Set<Player> partyMembers = new HashSet<>();
                    for (var entry : partyMap.entrySet()) {
                        if (entry.getValue().equals(player)) partyMembers.add(entry.getKey());
                    }
                    for (Player m : partyMembers) {
                        partyMap.remove(m);
                        m.sendMessage("§cYour party has been disbanded.");
                    }
                    player.sendMessage("§aYou disbanded your party.");
                    break;

                case "chat":
                    if (!partyMap.containsKey(player)) {
                        player.sendMessage("§cYou are not in a party.");
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage("§eUsage: /party chat <message>");
                        return true;
                    }
                    StringBuilder message = new StringBuilder();
                    for (int i = 1; i < args.length; i++) message.append(args[i]).append(" ");
                    String msg = message.toString().trim();
                    Player leaderForChat = partyMap.get(player);
                    for (var entry : partyMap.entrySet()) {
                        if (entry.getValue().equals(leaderForChat)) {
                            entry.getKey().sendMessage("§b[Party] " + player.getName() + ": " + msg);
                        }
                    }
                    break;

                default:
                    player.sendMessage("§cUnknown party command.");
            }
            return true;
        }

        return false; // Command is not spawn or party
    }
}