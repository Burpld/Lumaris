package lumaris.command

import org.bukkit.ChatColor
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Dolphin
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class DolphinCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        // Ensure only players can run this
        if (sender !is Player || !sender.isOp) {
            sender.sendMessage("This command is for admins only!")
            return true
        }

        val loc = sender.location

        // 1. Visual/Audio effects (Safe: No block damage)
        loc.world.apply {
            playSound(loc, Sound.ENTITY_DOLPHIN_AMBIENT, 1.0f, 1.0f)
            spawnParticle(Particle.OMINOUS_SPAWNING, loc, 100, 1.5, 1.0, 1.5)
        }

        val amount = if (args.isEmpty()) {
            300
        }
        else {
            args[0].toInt()
        }

        // 2. Spawn temporary dolphins
        repeat(amount) {
            val dolphin = loc.world.spawnEntity(loc, EntityType.DOLPHIN) as? Dolphin ?: return@repeat

            dolphin.apply {
                // Ensure they don't stick around or save to the world
                isPersistent = false
                removeWhenFarAway = true
                setHasFish(true)
                isGlowing = true

                isInvulnerable = true

                addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 255))

                setBaby()

                // Toss them in random directions
                velocity = Vector(
                    (Math.random() - 0.5) * 1.5,
                    0.6,
                    (Math.random() - 0.5) * 1.5
                )
            }
        }

        sender.sendMessage("${ChatColor.AQUA}Dolphin surge initiated!")
        return true
    }
}