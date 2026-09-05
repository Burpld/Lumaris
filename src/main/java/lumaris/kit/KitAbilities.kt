package lumaris.kit

import lumaris.Main
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.Event.Result
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Tracks which Tanks currently have Guardian's Call active, so
 * BattleBoxListeners.kt's damage handler can redirect 25% of a teammate's
 * incoming damage onto them. Read via TankGuard.isGuarding(uuid).
 */
object TankGuard {
    private val guardingUntil = mutableMapOf<UUID, Long>()

    fun activate(player: Player, durationMillis: Long) {
        guardingUntil[player.uniqueId] = System.currentTimeMillis() + durationMillis
    }

    fun isGuarding(uuid: UUID): Boolean {
        val end = guardingUntil[uuid] ?: return false
        return System.currentTimeMillis() < end
    }
}

/**
 * Handles the "use ability" interaction for kits that need one:
 * - Tank's Guardian's Call (right-click the Shield item): for 5s, redirects
 *   25% of teammates' incoming damage onto the Tank, and gives the Tank
 *   Resistance III for the same duration.
 * - Speedster's Dash (right-click the Sugar item): a short forward burst.
 * - Mage's Arcane Bolt (thrown Snowball): on hit, extra damage + knockback
 *   beyond a normal snowball.
 *
 * Abilities are on a per-kit cooldown, shown as a countdown on the player's
 * XP bar/level (purely visual - XP/levels aren't otherwise used here).
 */
class KitAbilities(private val plugin: Main) : Listener {
    private val cooldownEndsAt = mutableMapOf<UUID, Long>()

    private val cooldownSecondsByKit = mapOf(
        Kit.TANK.name to 15,
        Kit.SPEEDSTER.name to 6,
        Kit.MAGE.name to 6
    )

    private fun isOnCooldown(player: Player): Boolean {
        val end = cooldownEndsAt[player.uniqueId] ?: return false
        return System.currentTimeMillis() < end
    }

    /** Starts the cooldown and animates it on the player's XP bar/level until it ends. */
    private fun startCooldown(player: Player, seconds: Int) {
        val end = System.currentTimeMillis() + (seconds * 1000L)
        cooldownEndsAt[player.uniqueId] = end

        object : BukkitRunnable() {
            override fun run() {
                val remainingMillis = end - System.currentTimeMillis()
                if (remainingMillis <= 0 || !player.isOnline) {
                    player.level = 0
                    player.exp = 0f
                    cancel()
                    return
                }
                val remainingSeconds = (remainingMillis / 1000.0).toInt() + 1
                player.level = remainingSeconds
                player.exp = (remainingMillis % 1000L / 1000.0).toFloat()
            }
        }.runTaskTimer(plugin, 0L, 2L)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        val meta = item.itemMeta ?: return
        val abilityKit = meta.persistentDataContainer.get(Kit.KIT_ABILITY_KEY, PersistentDataType.STRING) ?: return
        if (!event.action.isRightClick) return

        val player = event.player
        val cooldownSeconds = cooldownSecondsByKit[abilityKit] ?: 6

        when (abilityKit) {
            Kit.TANK.name -> {
                if (isOnCooldown(player)) {
                    player.sendMessage("\u00A7cGuardian's Call is still on cooldown.")
                    event.setUseItemInHand(Result.DENY)
                    return
                }

                TankGuard.activate(player, 5000L)
                player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 5 * 20, 2, true, true))
                player.world.spawnParticle(org.bukkit.Particle.END_ROD, player.location.add(0.0, 1.0, 0.0), 30, 0.5, 0.7, 0.5, 0.02)
                player.sendMessage("\u00A7bGuardian's Call active! You're absorbing 25% of your team's damage for 5 seconds.")
                startCooldown(player, cooldownSeconds)
                event.setUseItemInHand(Result.DENY)
            }

            Kit.SPEEDSTER.name -> {
                if (isOnCooldown(player)) {
                    player.sendMessage("\u00A7cDash is still on cooldown.")
                    event.setUseItemInHand(Result.DENY)
                    return
                }

                val direction = player.location.direction.normalize()
                player.velocity = direction.multiply(1.8).setY(0.3)
                player.world.spawnParticle(org.bukkit.Particle.CLOUD, player.location, 20, 0.3, 0.1, 0.3, 0.02)
                startCooldown(player, cooldownSeconds)
                event.setUseItemInHand(Result.DENY)
            }

            Kit.MAGE.name -> {
                // The throw itself is handled by vanilla (it's a real Snowball item),
                // we just gate it with a cooldown before the throw is allowed.
                if (isOnCooldown(player)) {
                    player.sendMessage("\u00A7cArcane Bolt is still on cooldown.")
                    event.setUseItemInHand(Result.DENY)
                    return
                }
                startCooldown(player, cooldownSeconds)
                // Not denying here - the snowball throw proceeds normally.
            }
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val snowball = event.entity as? Snowball ?: return
        val shooter = snowball.shooter as? Player ?: return
        val hitEntity = event.hitEntity as? Player ?: return

        // Only apply the bonus if the shooter currently has the Mage kit equipped -
        // Snowball entities don't carry the source ItemStack's persistent data.
        if (Kit.of(shooter) != Kit.MAGE) return

        hitEntity.damage(3.0, shooter)
        val knockback: Vector = hitEntity.location.toVector()
            .subtract(shooter.location.toVector())
            .normalize()
            .multiply(1.2)
            .setY(0.4)
        hitEntity.velocity = knockback
        hitEntity.world.spawnParticle(org.bukkit.Particle.WITCH, hitEntity.location.add(0.0, 1.0, 0.0), 15, 0.3, 0.3, 0.3, 0.02)
    }
}
