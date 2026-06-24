package lumaris.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class FriendsCommand(private val partySystem: Party) : CommandExecutor {

    private val friendsMap = mutableMapOf<UUID, MutableList<UUID>>()
    private val incomingRequests = mutableMapOf<UUID, UUID>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use friends commands!")
            return true
        }
        val player: Player = sender
        val playerUUID = player.uniqueId

        val friends = friendsMap.computeIfAbsent(playerUUID) { mutableListOf() }

        when (command.name.lowercase()) {
            "viewfriends" -> {
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
            "acceptfriend" -> {
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
            "declinefriend" -> {
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

        if (args.isEmpty()) {
            player.sendMessage("§cUsage: /${command.name.lowercase()} [Username]")
            return true
        }
        val targetName = args[0]
        val targetPlayer = Bukkit.getPlayer(targetName)

        when (command.name.lowercase()) {
            "addfriend" -> {
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
                targetPlayer.sendMessage("§eType §a/acceptfriend §eor §c/declinefriend")
                return true
            }
            "removefriend" -> {
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
            "invitefriend" -> {
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
        }
        return false
    }
}