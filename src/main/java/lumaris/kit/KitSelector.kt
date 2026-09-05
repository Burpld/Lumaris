package lumaris.kit

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

/**
 * Opens a GUI letting a player pick one of the 6 kits.
 * Call KitSelector.open(player) - typically right when a Battle Box round/match starts.
 */
object KitSelector : Listener {
    private const val TITLE = "\u00A78Choose your Kit"
    private val SLOT_KEY = NamespacedKey("lumaris", "kit_slot_choice")

    fun open(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 9, TITLE)

        Kit.entries.forEachIndexed { index, kit ->
            val item = ItemStack(kit.icon)
            val meta: ItemMeta = item.itemMeta
            meta.setDisplayName("\u00A7b\u00A7l${kit.displayName}")
            meta.lore = listOf("\u00A77${kit.description}")
            meta.persistentDataContainer.set(SLOT_KEY, PersistentDataType.STRING, kit.name)
            item.itemMeta = meta
            inv.setItem(index + 1, item) // slots 1-6 of a 9-slot row, centered-ish
        }

        player.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.view.title != TITLE) return
        event.isCancelled = true

        val clicked = event.currentItem ?: return
        val meta = clicked.itemMeta ?: return
        val kitName = meta.persistentDataContainer.get(SLOT_KEY, PersistentDataType.STRING) ?: return
        val kit = Kit.entries.find { it.name == kitName } ?: return

        val player = event.whoClicked as? Player ?: return
        kit.applyTo(player)
        player.closeInventory()
        player.sendMessage("\u00A7aYou selected the \u00A7b\u00A7l${kit.displayName}\u00A7a kit.")
    }
}
