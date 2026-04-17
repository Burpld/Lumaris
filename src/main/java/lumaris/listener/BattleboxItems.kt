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
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Handles special item interactions for Battlebox, specifically the "Regen Star".
 */
class BattleboxItems(plugin: Main) : Listener {
    // Key used to identify the special item via PersistentDataContainer
    private val specialIdKey = NamespacedKey(plugin, "special_item_id")
    
    // The radius for both the visual particle circle and the healing effect
    private val radius = 10.0
    
    // Tracks when each player's cooldown expires (UUID -> Timestamp in ms)
    private val cooldowns = mutableMapOf<UUID, Long>()
    
    // Active BossBars for players currently on cooldown (UUID -> BossBar instance)
    private val bossBars = mutableMapOf<UUID, BossBar>()
    
    // Duration of the cooldown (30 seconds)
    private val cooldownTime = 30 * 1000L

    init {
        // A single background task that manages all active BossBars and particle visuals
        object : BukkitRunnable() {
            var tickCount = 0L
            override fun run() {
                val now = System.currentTimeMillis()

                // 1. UPDATE BOSSBARS
                // Iterates through all active cooldown bars to update progress and titles
                val barIterator = bossBars.entries.iterator()
                while (barIterator.hasNext()) {
                    val entry = barIterator.next()
                    val uuid = entry.key
                    val bar = entry.value
                    val expiration = cooldowns[uuid] ?: 0L

                    val player = Bukkit.getPlayer(uuid)
                    
                    // Remove bar if player left or cooldown finished
                    if (player == null || now >= expiration) {
                        bar.removeAll()
                        barIterator.remove()
                        continue
                    }

                    // Update BossBar progress (starts full, drains to empty)
                    val remaining = expiration - now
                    val progress = (remaining.toDouble() / cooldownTime).coerceIn(0.0, 1.0)
                    bar.progress = progress
                    
                    // Update the title with remaining time formatted to 1 decimal place
                    val seconds = remaining / 1000.0
                    bar.setTitle("§a§lRegen Star Cooldown: §e${"%.1f".format(seconds)}s")
                }

                // 2. DISPLAY RADIUS CIRCLE
                // Only runs every 5 ticks (0.25s) to save performance while maintaining visibility
                if (tickCount % 5 == 0L) {
                    for (player in Bukkit.getOnlinePlayers()) {
                        val item = player.inventory.itemInMainHand
                        if (item.type != Material.FIREWORK_STAR) continue

                        val meta = item.itemMeta ?: continue
                        val specialId = meta.persistentDataContainer.get(specialIdKey, PersistentDataType.STRING)

                        // If holding the Regen Star, show the idle particle circle
                        if (specialId == "regeneration_circle") {
                            displayCircle(player, radius, false)
                        }
                    }
                }
                tickCount++
            }
        }.runTaskTimer(plugin, 0L, 1L) // Run every 1 tick for smooth BossBar progress
    }

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        // Validate that the player is right-clicking a Firework Star
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (item.type != Material.FIREWORK_STAR) return

        val meta = item.itemMeta ?: return
        val specialId = meta.persistentDataContainer.get(specialIdKey, PersistentDataType.STRING)

        // Verify the special ID matches our Regen Star
        if (specialId == "regeneration_circle") {
            val now = System.currentTimeMillis()
            val expiration = cooldowns[player.uniqueId] ?: 0L
            
            // COOLDOWN CHECK: Prevent use if the previous cooldown hasn't finished
            if (now < expiration) {
                val remaining = (expiration - now) / 1000
                player.sendMessage("§c§l(!) §cYou must wait $remaining seconds before using this again!")
                event.isCancelled = true // Prevent actual firework firing
                return
            }

            // GAME CONTEXT: Find the player's current game and team to identify teammates
            val game = findGameForPlayer(player.uniqueId)
            val clickerTeam = game?.teamGenerator?.teamMap?.get(player.uniqueId)

            // 1. LOGIC & VISUALS
            // Set the cooldown timestamp and trigger the vanilla item cooldown (shaded bar)
            cooldowns[player.uniqueId] = now + cooldownTime
            player.setCooldown(Material.FIREWORK_STAR, 600) // 30s * 20 ticks
            
            // Create or update the BossBar for the player
            val bossBar = bossBars.getOrPut(player.uniqueId) {
                Bukkit.createBossBar("§a§lRegen Star Cooldown", BarColor.GREEN, BarStyle.SOLID)
            }
            bossBar.addPlayer(player)
            bossBar.isVisible = true

            // Trigger the "Burst" visual (larger, brighter particles)
            displayCircle(player, radius, true)

            // 2. APPLY EFFECTS
            if (game != null && clickerTeam != null) {
                // Heal all teammates in the radius
                applyRegenerationToTeam(player, game.teamGenerator.teamMap, clickerTeam, radius)
            }
            else {
                // FALLBACK: If used outside a game (e.g., testing), just heal the user
                player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 60, 2))
                player.sendMessage("§a§l(!) §aApplied self-regeneration (Not in a game).")
            }
            
            event.isCancelled = true // Prevent default firework interactions
        }
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack

        val meta = item.itemMeta ?: return
        val specialId = meta.persistentDataContainer.get(specialIdKey, PersistentDataType.STRING)

        if (item.type == Material.FIREWORK_STAR || specialId == "regeneration_circle") {
            event.isCancelled = true;
        }
    }

    /**
     * Searches through all active games to find which one contains the specified player.
     */
    private fun findGameForPlayer(uuid: UUID): GameManager? {
        return GameManager.runningGames.find { it.teamGenerator.teamMap.containsKey(uuid) }
    }

    /**
     * Calculates and spawns a circular particle effect around the player.
     * @param isBurst If true, uses more particles and a brighter particle type for activation feedback.
     */
    private fun displayCircle(player: Player, radius: Double, isBurst: Boolean) {
        val location = player.location
        val particlesCount = if (isBurst) 120 else 70
        val particleType = if (isBurst) Particle.HAPPY_VILLAGER else Particle.COMPOSTER
        
        for (i in 0 until particlesCount) {
            val angle = 2 * Math.PI * i / particlesCount
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            
            // Spawn particles slightly above ground to ensure visibility
            val particleLocation = location.clone().add(x, 0.2, z)
            player.world.spawnParticle(particleType, particleLocation, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    /**
     * Iterates through the team map and applies Regeneration II to teammates within the radius.
     */
    private fun applyRegenerationToTeam(clicker: Player, teamMap: Map<UUID, TeamColour>, clickerTeam: TeamColour, radius: Double) {
        for ((uuid, team) in teamMap) {
            // Check if player is on the same team
            if (team == clickerTeam) {
                val teammate = Bukkit.getPlayer(uuid) ?: continue
                
                // Verify world and distance
                if (teammate.world == clicker.world && teammate.location.distance(clicker.location) <= radius) {
                    // Apply Regeneration II (level 1 = II) for 5 seconds
                    teammate.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 60, 2))
                    teammate.sendMessage("§a§l(!) §aYou received regeneration from ${clicker.name}'s item!")
                }
            }
        }
    }
}