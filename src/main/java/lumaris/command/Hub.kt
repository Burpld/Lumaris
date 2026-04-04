package lumaris.command

import lumaris.Global.SPAWN_PITCH
import lumaris.Global.SPAWN_X
import lumaris.Global.SPAWN_Y
import lumaris.Global.SPAWN_YAW
import lumaris.Global.SPAWN_Z
import lumaris.Global.WORLD_NAME
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Hub : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player) return true

        val world = Bukkit.getWorld(WORLD_NAME) ?: return true

        sender.teleport(Location(world, SPAWN_X, SPAWN_Y, SPAWN_Z, SPAWN_YAW, SPAWN_PITCH))
        sender.sendMessage("§aTeleported to Hub!")

        return true
    }
}