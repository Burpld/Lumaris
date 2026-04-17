package lumaris.command

import lumaris.Main
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class SpawnBattleItem(private val plugin: Main) : CommandExecutor, TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String>? {
        if (args.size == 1) {
            return listOf("regenstar");
        }

        return null;
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player || !sender.isOp) return true

        val sub = args[0].lowercase()

        when (sub) {
            "regenstar" -> {
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
        }

        return false
    }
}