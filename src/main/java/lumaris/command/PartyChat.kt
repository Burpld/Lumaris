package lumaris.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PartyChat(private val party: Party) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) return true

        var message = ""

        for (arg in args) {
            message += "$arg "
        }

        val senderPlayer = party.getPartyMapValue(sender.uniqueId)

        for (online in Bukkit.getOnlinePlayers()) {
            val onlinePlayer = party.getPartyMapValue(online.uniqueId)

            if (onlinePlayer != null &&
                senderPlayer != null &&
                senderPlayer == onlinePlayer) {
                online.sendMessage("§d<Party>§r ${sender.name}: " + message);
            }
        }

        return false
    }
}