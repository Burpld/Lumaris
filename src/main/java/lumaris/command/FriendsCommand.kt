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
    private val incomingRequests = mutableMapOf<UUID, MutableList<UUID>>()

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use friends commands!")
            return listOf()
        }

        if (args.size == 1) {
            return listOf("list", "add", "remove", "partyfriend", "accept", "decline").filter { it.startsWith(args[0], true) }
        }

        if (args.size == 2) {
            val cmd = args[0].lowercase()

            if (cmd == "add" || cmd == "remove") {
                return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], true) }
            }

            if (cmd == "partyfriend" || cmd == "pf") {
                return Bukkit.getOnlinePlayers().map { it.name }.filter {
                    val list = friendsMap[sender.uniqueId]
                    val player = Bukkit.getPlayer(it)

                    if (list.isNullOrEmpty() || player == null) {
                        return@filter false
                    }


                    return@filter list.contains(player.uniqueId)
                }.filter { it.startsWith(args[1], true) }
            }

            if (cmd == "accept" || cmd == "decline") {
                val requests = incomingRequests[sender.uniqueId] ?: return listOf()
                return requests.mapNotNull { Bukkit.getOfflinePlayer(it).name }.filter { it.startsWith(args[1], true) }
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

        if (args.isEmpty()) {
            player.sendMessage("§cUsage: /friend [list|add|remove|partyfriend|accept|decline] [Username]")
            return true
        }

        when(args[0].lowercase()) {
            "list" -> {
                if (friends.isEmpty()) {
                    player.sendMessage("§eYou don't have any friends added yet. Use /friend add [Name]!")
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

        if (args.size < 2) {
            player.sendMessage("§cUsage: /friend ${args[0].lowercase()} [Username]")
            return true
        }

        val targetName = args[1]
        val targetPlayer = Bukkit.getPlayer(targetName)

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
                val targetRequests = incomingRequests[targetPlayer.uniqueId]
                if (targetRequests != null && targetRequests.contains(playerUUID)) {
                    player.sendMessage("§eYou have already sent a pending request to ${targetPlayer.name}!")
                    return true
                }
                incomingRequests.computeIfAbsent(targetPlayer.uniqueId) { mutableListOf() }.add(playerUUID)
                player.sendMessage("§aFriend request sent to ${targetPlayer.name}!")
                targetPlayer.sendMessage("§9=== Pending Friend Request ===")
                targetPlayer.sendMessage("§6${player.name} §awants to be your friend!")
                targetPlayer.sendMessage("§eType §a/friend accept ${player.name} §eor §c/friend decline ${player.name}")
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
                val targetUUID = targetPlayer?.uniqueId ?: Bukkit.getOfflinePlayer(targetName).uniqueId
                val targetRequests = incomingRequests[playerUUID]
                if (targetRequests == null || !targetRequests.contains(targetUUID)) {
                    player.sendMessage("§cYou do not have a pending friend invitation from $targetName.")
                    return true
                }
                val senderName = Bukkit.getOfflinePlayer(targetUUID).name ?: "A player"
                friends.add(targetUUID)
                friendsMap.computeIfAbsent(targetUUID) { mutableListOf() }.add(playerUUID)
                targetRequests.remove(targetUUID)
                if (targetRequests.isEmpty()) {
                    incomingRequests.remove(playerUUID)
                }
                player.sendMessage("§aYou accepted $senderName's friend request!")
                Bukkit.getPlayer(targetUUID)?.sendMessage("§a${player.name} accepted your friend request!")
                return true
            }
            "decline" -> {
                val targetUUID = targetPlayer?.uniqueId ?: Bukkit.getOfflinePlayer(targetName).uniqueId
                val targetRequests = incomingRequests[playerUUID]
                if (targetRequests == null || !targetRequests.contains(targetUUID)) {
                    player.sendMessage("§cYou do not have a pending friend invitation from $targetName.")
                    return true
                }
                val senderName = Bukkit.getOfflinePlayer(targetUUID).name ?: "A player"
                targetRequests.remove(targetUUID)
                if (targetRequests.isEmpty()) {
                    incomingRequests.remove(playerUUID)
                }
                player.sendMessage("§eYou declined $senderName's friend request.")
                Bukkit.getPlayer(targetUUID)?.sendMessage("§c${player.name} declined your friend request.")
                return true
            }
        }

        return false
    }
}
