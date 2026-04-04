package lumaris.command

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.util.EulerAngle

class SpawnNPC : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player || !sender.isOp) return true

        spawnMannequin(sender.location)

        sender.sendMessage("§aBattle Box Mannequin spawned!")

        return true
    }

    private fun spawnMannequin(loc: Location) {
        val npc = loc.world.spawn(loc, ArmorStand::class.java)
        npc.setGravity(false)
        npc.isInvulnerable = true
        npc.setArms(true)
        npc.setBasePlate(false)
        npc.addScoreboardTag("battlebox")
        npc.isCustomNameVisible = true
        npc.customName(Component.text("§6§lBATTLE BOX §7[Click to Play]"))

        val head = ItemStack(Material.PLAYER_HEAD)
        val headMeta = head.itemMeta as SkullMeta?
        if (headMeta != null) {
            headMeta.owningPlayer = Bukkit.getOfflinePlayer("Burpld")
            head.setItemMeta(headMeta)
        }

        npc.equipment.setHelmet(head)
        npc.equipment.setChestplate(ItemStack(Material.NETHERITE_CHESTPLATE))
        npc.equipment.setLeggings(ItemStack(Material.NETHERITE_LEGGINGS))
        npc.equipment.setBoots(ItemStack(Material.NETHERITE_BOOTS))

        // 1.21.11 Official Spear
        npc.equipment.setItemInMainHand(ItemStack(Material.NETHERITE_SPEAR))
        npc.rightArmPose = EulerAngle(Math.toRadians(-90.0), 0.0, 0.0)
    }
}