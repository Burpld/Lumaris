package lumaris.command

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class FriendsCommand(private val partySystem: Party) : CommandExecutor {

    private val friendsMap = mutableMapOf<UUID, MutableList<UUID>>()
    private val incomingRequests = mutableMapOf<UUID, UUID>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}Only players can use friends commands!")
            return true
        }
        val player: Player = sender
        val playerUUID = player.uniqueId

        val friends = friendsMap.computeIfAbsent(playerUUID) { mutableListOf() }

        when (label.lowercase()) {
            "viewfriends" -> {
                if (friends.isEmpty()) {
                    player.sendMessage("${ChatColor.YELLOW}You don't have any friends added yet. Use /addfriend [Name]!")
                    return true
                }
                player.sendMessage("${ChatColor.BLUE}=== Your Friends List ===")
                for (friendUUID in friends) {
                    val friendName = Bukkit.getOfflinePlayer(friendUUID).name
                    player.sendMessage("${ChatColor.GREEN}- ${friendName ?: "Unknown Player"}")
                }
                return true
            }
            "acceptfriend" -> {
                val senderUUID = incomingRequests[playerUUID]
                if (senderUUID == null) {
                    player.sendMessage("${ChatColor.RED}You do not have any pending friend invitations.")
                    return true
                }
                val senderName = Bukkit.getOfflinePlayer(senderUUID).name ?: "A player"
                friends.add(senderUUID)
                friendsMap.computeIfAbsent(senderUUID) { mutableListOf() }.add(playerUUID)
                incomingRequests.remove(playerUUID)
                player.sendMessage("${ChatColor.GREEN}You accepted $senderName's friend request!")
                Bukkit.getPlayer(senderUUID)?.sendMessage("${ChatColor.GREEN}${player.name} accepted your friend request!")
                return true
            }
            "declinefriend" -> {
                val senderUUID = incomingRequests[playerUUID]
                if (senderUUID == null) {
                    player.sendMessage("${ChatColor.RED}You do not have any pending friend invitations.")
                    return true
                }
                val senderName = Bukkit.getOfflinePlayer(senderUUID).name ?: "A player"
                incomingRequests.remove(playerUUID)
                player.sendMessage("${ChatColor.YELLOW}You declined $senderName's friend request.")
                Bukkit.getPlayer(senderUUID)?.sendMessage("${ChatColor.RED}${player.name} declined your friend request.")
                return true
            }
        }

        if (args.isEmpty()) {
            player.sendMessage("${ChatColor.RED}Usage: /$label [Username]")
            return true
        }
        val targetName = args[0]
        val targetPlayer = Bukkit.getPlayer(targetName)

        when (label.lowercase()) {
            "addfriend" -> {
                if (targetPlayer == null) {
                    player.sendMessage("${ChatColor.RED}Player '$targetName' is not online!")
                    return true
                }
                if (targetPlayer.uniqueId == playerUUID) {
                    player.sendMessage("${ChatColor.RED}You cannot add yourself as a friend!")
                    return true
                }
                if (friends.contains(targetPlayer.uniqueId)) {
                    player.sendMessage("${ChatColor.YELLOW}${targetPlayer.name} is already your friend!")
                    return true
                }
                if (incomingRequests[targetPlayer.uniqueId] == playerUUID) {
                    player.sendMessage("${ChatColor.YELLOW}You have already sent a pending request to ${targetPlayer.name}!")
                    return true
                }
                incomingRequests[targetPlayer.uniqueId] = playerUUID
                player.sendMessage("${ChatColor.GREEN}Friend request sent to ${targetPlayer.name}!")
                targetPlayer.sendMessage("${ChatColor.BLUE}=== Pending Friend Request ===")
                targetPlayer.sendMessage("${ChatColor.GOLD}${player.name} ${ChatColor.GREEN}wants to be your friend!")
                targetPlayer.sendMessage("${ChatColor.YELLOW}Type ${ChatColor.GREEN}/acceptfriend ${ChatColor.YELLOW}or ${ChatColor.RED}/declinefriend")
                return true
            }
            "removefriend" -> {
                val targetUUID = targetPlayer?.uniqueId ?: Bukkit.getOfflinePlayer(targetName).uniqueId
                if (!friends.contains(targetUUID)) {
                    player.sendMessage("${ChatColor.RED}$targetName is not on your friends list.")
                    return true
                }
                friends.remove(targetUUID)
                friendsMap[targetUUID]?.remove(playerUUID) // Remove it from their list too
                player.sendMessage("${ChatColor.GREEN}Removed $targetName from your friends list.")
                return true
            }
            "invitefriend" -> {
                if (targetPlayer == null) {
                    player.sendMessage("${ChatColor.RED}Player '$targetName' is not online!")
                    return true
                }
                if (!friends.contains(targetPlayer.uniqueId)) {
                    player.sendMessage("${ChatColor.RED}You can only invite players who are on your friends list!")
                    return true
                }
                player.performCommand("party invite ${targetPlayer.name}")
                player.sendMessage("${ChatColor.GREEN}Sent a party invitation to your friend, ${targetPlayer.name}!")
                return true
            }
        }
        return false
    }
}