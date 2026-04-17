package lumaris.command

import lumaris.Global.SPAWN_PITCH
import lumaris.Global.SPAWN_YAW
import lumaris.Global.WORLD_NAME
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdminHub() : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player || !sender.isOp) return true

        val world = Bukkit.getWorld(WORLD_NAME) ?: return true

        sender.teleport(Location(world, 0.0, 0.0, 0.0, SPAWN_YAW, SPAWN_PITCH))
        sender.sendMessage("§aTeleported to Hub!")

        return true
    }
}