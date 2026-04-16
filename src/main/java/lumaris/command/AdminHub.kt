package lumaris.command

import lumaris.Global.SPAWN_PITCH
import lumaris.Global.SPAWN_YAW
import lumaris.Global.WORLD_NAME
import lumaris.Main
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class AdminHub(private val plugin: Main) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player || !sender.isOp) return true

        if (args.isNotEmpty() && args[0].lowercase() == "testitem") {
            val item = ItemStack(Material.FIREWORK_STAR)
            val meta = item.itemMeta ?: return true
            
            meta.displayName(Component.text("§a§lRegen Star"))
            meta.persistentDataContainer.set(
                NamespacedKey(plugin, "special_item_id"),
                PersistentDataType.STRING,
                "regeneration_circle"
            )
            
            item.itemMeta = meta
            sender.inventory.addItem(item)
            sender.sendMessage("§a§l(!) §aGave you a Regen Star!")
            return true
        }

        val world = Bukkit.getWorld(WORLD_NAME) ?: return true

        sender.teleport(Location(world, 0.0, 0.0, 0.0, SPAWN_YAW, SPAWN_PITCH))
        sender.sendMessage("§aTeleported to Hub!")

        return true
    }
}