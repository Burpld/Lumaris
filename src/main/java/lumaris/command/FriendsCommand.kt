package lumaris.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

class FriendsCommand : CommandExecutor, TabCompleter {
    private val friendsMap = mutableMapOf<UUID, MutableList<UUID>>()
    private val incomingRequests = mutableMapOf<UUID, UUID>()

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use friends commands!")
            return listOf()
        }

        if (args.size == 1) {
            return listOf("list", "add", "remove", "partyfriend", "accept", "decline")
        }

        if (args.size == 2) {
            val cmd = args[0].lowercase()

            if (cmd == "add" || cmd == "remove") {
                return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], true) }
            }

            if (cmd == "partyfriend" || cmd == "pf") {
                return Bukkit.getOnlinePlayers().map { it.name }.filter {
                    val list = friendsMap[sender.uniqueId]
                    val player = Bukkit.getPlayer(it)

                    if (list.isNullOrEmpty() || player == null) {
                        return@filter false
                    }


                    return@filter list.contains(player.uniqueId)
                }
            }
        }

        return listOf()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use friends commands!")
            return true
        }
        val player: Player = sender
        val playerUUID = player.uniqueId
        val friends = friendsMap.computeIfAbsent(playerUUID) { mutableListOf() }

        when(args[0].lowercase()) {
            "list" -> {
                if (friends.isEmpty()) {
                    player.sendMessage("§eYou don't have any friends added yet. Use /addfriend [Name]!")
                    return true
                }
                player.sendMessage("§9=== Your Friends List ===")
                for (friendUUID in friends) {
                    val friendName = Bukkit.getOfflinePlayer(friendUUID).name
                    player.sendMessage("§a- ${friendName ?: "Unknown Player"}")
                }
                return true
            }
        }

        if (args.isEmpty()) {
            player.sendMessage("§cUsage: /friend ${args[0].lowercase()} [Username]")
            return true
        }

        val targetName = args[1]
        val targetPlayer = Bukkit.getPlayer(targetName)

        // TODO: currently, each person can only have one pending invite, which can create problems
        when(args[0].lowercase()) {
            "add" -> {
                if (targetPlayer == null) {
                    player.sendMessage("§cPlayer '$targetName' is not online!")
                    return true
                }
                if (targetPlayer.uniqueId == playerUUID) {
                    player.sendMessage("§cYou cannot add yourself as a friend!")
                    return true
                }
                if (friends.contains(targetPlayer.uniqueId)) {
                    player.sendMessage("§e${targetPlayer.name} is already your friend!")
                    return true
                }
                if (incomingRequests[targetPlayer.uniqueId] == playerUUID) {
                    player.sendMessage("§eYou have already sent a pending request to ${targetPlayer.name}!")
                    return true
                }
                incomingRequests[targetPlayer.uniqueId] = playerUUID
                player.sendMessage("§aFriend request sent to ${targetPlayer.name}!")
                targetPlayer.sendMessage("§9=== Pending Friend Request ===")
                targetPlayer.sendMessage("§6${player.name} §awants to be your friend!")
                targetPlayer.sendMessage("§eType §a/friend accept §eor §c/friend decline")
                return true
            }
            "remove" -> {
                val targetUUID = targetPlayer?.uniqueId ?: Bukkit.getOfflinePlayer(targetName).uniqueId
                if (!friends.contains(targetUUID)) {
                    player.sendMessage("§c$targetName is not on your friends list.")
                    return true
                }
                friends.remove(targetUUID)
                friendsMap[targetUUID]?.remove(playerUUID)
                player.sendMessage("§aRemoved $targetName from your friends list.")
                return true
            }
            "partyfriend", "pf" -> {
                if (targetPlayer == null) {
                    player.sendMessage("§cPlayer '$targetName' is not online!")
                    return true
                }
                if (!friends.contains(targetPlayer.uniqueId)) {
                    player.sendMessage("§cYou can only invite players who are on your friends list!")
                    return true
                }
                player.performCommand("party invite ${targetPlayer.name}")
                player.sendMessage("§aSent a party invitation to your friend, ${targetPlayer.name}!")
                return true
            }
            "accept" -> {
                val senderUUID = incomingRequests[playerUUID]
                if (senderUUID == null) {
                    player.sendMessage("§cYou do not have any pending friend invitations.")
                    return true
                }
                val senderName = Bukkit.getOfflinePlayer(senderUUID).name ?: "A player"
                friends.add(senderUUID)
                friendsMap.computeIfAbsent(senderUUID) { mutableListOf() }.add(playerUUID)
                incomingRequests.remove(playerUUID)
                player.sendMessage("§aYou accepted $senderName's friend request!")
                Bukkit.getPlayer(senderUUID)?.sendMessage("§a${player.name} accepted your friend request!")
                return true
            }
            "decline" -> {
                val senderUUID = incomingRequests[playerUUID]
                if (senderUUID == null) {
                    player.sendMessage("§cYou do not have any pending friend invitations.")
                    return true
                }
                val senderName = Bukkit.getOfflinePlayer(senderUUID).name ?: "A player"
                incomingRequests.remove(playerUUID)
                player.sendMessage("§eYou declined $senderName's friend request.")
                Bukkit.getPlayer(senderUUID)?.sendMessage("§c${player.name} declined your friend request.")
                return true
            }
        }

        return false
    }
}
