package lumaris.listener

import lumaris.GameManager
import lumaris.Main
import lumaris.TeamColour
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class BattleboxItems(private val plugin: Main) : Listener {

    private val specialIdKey = NamespacedKey(plugin, "special_item_id")
    private val radius = 5.0 // Radius X
    private val cooldowns = mutableMapOf<UUID, Long>()
    private val cooldownTime = 30 * 1000 // 30 seconds in milliseconds

    init {
        // Task to display the radius circle while holding the item
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    val item = player.inventory.itemInMainHand
                    if (item.type != Material.FIREWORK_STAR) continue

                    val meta = item.itemMeta ?: continue
                    val specialId = meta.persistentDataContainer.get(specialIdKey, PersistentDataType.STRING)

                    if (specialId == "regeneration_circle") {
                        displayCircle(player, radius, false)
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L) // Runs every 5 ticks (0.25s)
    }

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        // Check for right click action
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        
        // Ensure the item is a Firework Star
        if (item.type != Material.FIREWORK_STAR) return

        val meta = item.itemMeta ?: return
        val specialId = meta.persistentDataContainer.get(specialIdKey, PersistentDataType.STRING)

        // Check if the Firework Star has the special ID
        if (specialId == "regeneration_circle") {
            // Check cooldown
            val now = System.currentTimeMillis()
            val expiration = cooldowns[player.uniqueId] ?: 0L
            
            if (now < expiration) {
                val remaining = (expiration - now) / 1000
                player.sendMessage("§c§l(!) §cYou must wait $remaining seconds before using this again!")
                event.isCancelled = true
                return
            }

            // Find the game this player is in
            val game = findGameForPlayer(player.uniqueId) ?: return
            val clickerTeam = game.teamGenerator.teamMap[player.uniqueId] ?: return

            // 1. Set Cooldown
            cooldowns[player.uniqueId] = now + cooldownTime
            player.setCooldown(Material.FIREWORK_STAR, 30 * 20) // Visual cooldown (ticks)

            // 2. Display "burst" circle around the player
            displayCircle(player, radius, true)

            // 3. Loop through team map to find teammates and give regeneration
            applyRegenerationToTeam(player, game.teamGenerator.teamMap, clickerTeam, radius)
            
            // Consume one of the item
            if (item.amount > 1) {
                item.amount -= 1
            } else {
                player.inventory.setItemInMainHand(null)
            }
        }
    }

    private fun findGameForPlayer(uuid: UUID): GameManager? {
        return GameManager.runningGames.find { it.teamGenerator.teamMap.containsKey(uuid) }
    }

    /**
     * Displays a circle of particles around the player.
     * @param isBurst If true, uses more particles and different effect for activation feedback.
     */
    private fun displayCircle(player: Player, radius: Double, isBurst: Boolean) {
        val location = player.location
        val particlesCount = if (isBurst) 120 else 70
        val particleType = if (isBurst) Particle.HAPPY_VILLAGER else Particle.COMPOSTER
        
        for (i in 0 until particlesCount) {
            val angle = 2 * Math.PI * i / particlesCount
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            
            // Use player's ground Y, but offset slightly to avoid Z-fighting with blocks
            val particleLocation = location.clone().add(x, 0.1, z)
            
            if (isBurst) {
                // Everyone sees the burst
                player.world.spawnParticle(particleType, particleLocation, 1, 0.0, 0.0, 0.0, 0.0)
            } else {
                // Only the player holding it (and nearby) see the idle radius to reduce lag
                player.world.spawnParticle(particleType, particleLocation, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    /**
     * Applies regeneration to teammates of the clicker within the specified radius.
     */
    private fun applyRegenerationToTeam(clicker: Player, teamMap: Map<UUID, TeamColour>, clickerTeam: TeamColour, radius: Double) {
        for ((uuid, team) in teamMap) {
            // Check if player is on the same team
            if (team == clickerTeam) {
                val teammate = Bukkit.getPlayer(uuid) ?: continue
                
                // Check if teammate is in the same world and within the radius
                if (teammate.world == clicker.world && teammate.location.distance(clicker.location) <= radius) {
                    // Give Regeneration II for 5 seconds (100 ticks)
                    teammate.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 100, 1))
                    teammate.sendMessage("§a§l(!) §aYou received regeneration from ${clicker.name}'s item!")
                }
            }
        }
    }
}