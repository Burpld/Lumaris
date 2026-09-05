package lumaris.battlebox

import lumaris.TeamColour
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.entity.Player

/**
 * Handles right-clicking a center block to claim it for your team.
 */
class CenterBlockListener : Listener {

    private fun teamMaterial(team: TeamColour): Material {
        return when (team) {
            TeamColour.RED -> Material.RED_STAINED_GLASS
            TeamColour.BLUE -> Material.BLUE_STAINED_GLASS
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val player = event.player

        val game = BattleBoxGame.findGameFor(player.uniqueId) ?: return
        val team = game.gameManager.teamGenerator.teamMap[player.uniqueId] ?: return

        val arenaBlocks = ArenaManager.getArena(game.map).centerBlocks
        val index = arenaBlocks.entries.find { (_, loc) ->
            loc.block.x == clickedBlock.x && loc.block.y == clickedBlock.y && loc.block.z == clickedBlock.z && loc.world == clickedBlock.world
        }?.key ?: return

        event.isCancelled = true

        if (!game.isCaptureAllowed()) {
            player.sendMessage("\u00A7cThe center is still locked for a few more seconds.")
            return
        }

        if (game.blockOwners[index] == team) {
            player.sendMessage("\u00A77Your team already owns this block.")
            return
        }

        clickedBlock.type = teamMaterial(team)
        game.onBlockClaimed(index, team)
        player.sendMessage("\u00A7aYou claimed a center block for ${team.minecraftName}\u00A7a!")
    }
}

/**
 * - Blocks all PvP damage between teammates in an active Battle Box game (no friendly fire).
 * - Awards kill points to the killer's team when an enemy dies inside an active game.
 * - Prevents item/XP drops on death so kits aren't lost, and clears the "You died" drop mess.
 * - Sends players back to their team's spawn on respawn instead of the world spawn,
 *   and re-gears them with their chosen kit.
 */
class BattleBoxDeathListener : Listener {

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val damager = event.damager as? Player ?: return

        val game = BattleBoxGame.findGameFor(damager.uniqueId) ?: return
        if (BattleBoxGame.findGameFor(victim.uniqueId) != game) return

        val damagerTeam = game.gameManager.teamGenerator.teamMap[damager.uniqueId] ?: return
        val victimTeam = game.gameManager.teamGenerator.teamMap[victim.uniqueId] ?: return

        if (damagerTeam == victimTeam) {
            event.isCancelled = true
            return
        }

        // Guardian's Call: if a teammate (not the victim) currently has it active,
        // redirect 25% of this damage onto them instead.
        val guardian = game.gameManager.teamGenerator.teamMap.entries
            .filter { it.value == victimTeam && it.key != victim.uniqueId }
            .mapNotNull { org.bukkit.Bukkit.getPlayer(it.key) }
            .find { lumaris.kit.TankGuard.isGuarding(it.uniqueId) }

        if (guardian != null) {
            val redirected = event.damage * 0.25
            event.damage = event.damage * 0.75
            guardian.damage(redirected)
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val game = BattleBoxGame.findGameFor(victim.uniqueId) ?: return

        // Don't let kit items/XP spill onto the ground in the arena.
        event.drops.clear()
        event.droppedExp = 0

        val killer = victim.killer ?: return
        if (BattleBoxGame.findGameFor(killer.uniqueId) != game) return

        val killerTeam = game.gameManager.teamGenerator.teamMap[killer.uniqueId] ?: return
        val victimTeam = game.gameManager.teamGenerator.teamMap[victim.uniqueId] ?: return

        // No points for friendly fire deaths (shouldn't normally happen since damage
        // is already blocked above, but this is a safety net).
        if (killerTeam == victimTeam) return

        game.onKill(killerTeam)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val game = BattleBoxGame.findGameFor(player.uniqueId) ?: return
        val team = game.gameManager.teamGenerator.teamMap[player.uniqueId] ?: return

        val arena = ArenaManager.getArena(game.map)
        arena.teamSpawns[team]?.let { event.respawnLocation = it }

        // Re-gear them with their chosen kit one tick after the respawn actually happens,
        // since Bukkit resets health/inventory as part of respawn right after this event.
        org.bukkit.Bukkit.getScheduler().runTaskLater(lumaris.PluginContext.instance, Runnable {
            lumaris.kit.Kit.of(player)?.applyTo(player)
        }, 1L)
    }
}
