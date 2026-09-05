package lumaris.battlebox

import lumaris.GameManager
import lumaris.Global
import lumaris.MapSelector
import lumaris.Main
import lumaris.TeamColour
import lumaris.kit.KitSelector
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Owns the full Battle Box round/match loop for one running game:
 *   start() -> startRound() -> tick() every second -> endRound() -> either
 *   startRound() again, or endMatch() once a team hits the target score.
 *
 * One BattleBoxGame wraps one GameManager (which already has the balanced
 * teams from TeamGenerator).
 */
class BattleBoxGame(private val plugin: Main, val gameManager: GameManager) {

    companion object {
        val activeGames = mutableListOf<BattleBoxGame>()

        /** Finds the Battle Box game a given player is currently part of, if any. */
        fun findGameFor(uuid: UUID): BattleBoxGame? {
            return activeGames.find { it.gameManager.teamGenerator.teamMap.containsKey(uuid) }
        }
    }

    val map = MapSelector.getRandomMap()
    private val arena = ArenaManager.getArena(map)

    // blockIndex (1-9) -> team that owns it, or null if unclaimed
    val blockOwners: MutableMap<Int, TeamColour> = mutableMapOf()

    val teamScores: MutableMap<TeamColour, Int> = TeamColour.entries.associateWith { 0 }.toMutableMap()

    private var secondsElapsed = 0
    private var task: BukkitTask? = null
    private var timerBar: BossBar? = null

    fun start() {
        if (!arena.isComplete()) {
            broadcast("\u00A7cThe map ${map.name} isn't fully configured yet (${arena.missingPieces()}). Ask an admin to run /battlebox setcenter and /battlebox setspawn for it.")
            gameManager.endGame()
            return
        }

        activeGames.add(this)
        broadcast("\u00A7b\u00A7lBattle Box \u00A77- Map: \u00A7f${map.name}")

        startRound()
    }

    private fun startRound() {
        secondsElapsed = 0
        blockOwners.clear()

        // Teleport players to their team spawn and re-gear them for the fresh round.
        for ((uuid, team) in gameManager.teamGenerator.teamMap) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            arena.teamSpawns[team]?.let { player.teleport(it) }

            val chosenKit = lumaris.kit.Kit.of(player)
            if (chosenKit != null) {
                chosenKit.applyTo(player)
            } else {
                KitSelector.open(player)
            }
            player.health = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
        }

        for ((_, location) in arena.centerBlocks) {
            location.block.type = Material.WHITE_STAINED_GLASS
        }

        timerBar?.removeAll()
        timerBar = Bukkit.createBossBar("\u00A7bBattle Box \u00A77- \u00A7f${formatTime(Global.BATTLEBOX_ROUND_SECONDS)}", BarColor.BLUE, BarStyle.SOLID)
        for (uuid in gameManager.teamGenerator.teamMap.keys) {
            Bukkit.getPlayer(uuid)?.let { timerBar?.addPlayer(it) }
        }

        broadcast("\u00A7eNew round! Center blocks unlock in ${Global.BATTLEBOX_CAPTURE_GRACE_SECONDS} seconds.")

        task = object : BukkitRunnable() {
            override fun run() {
                tick()
            }
        }.runTaskTimer(plugin, 20L, 20L)
    }

    private fun tick() {
        secondsElapsed++
        val remaining = Global.BATTLEBOX_ROUND_SECONDS - secondsElapsed

        timerBar?.progress = (remaining.toDouble() / Global.BATTLEBOX_ROUND_SECONDS).coerceIn(0.0, 1.0)
        timerBar?.setTitle("\u00A7bBattle Box \u00A77- \u00A7f${formatTime(remaining)}")

        if (secondsElapsed == Global.BATTLEBOX_CAPTURE_GRACE_SECONDS) {
            broadcast("\u00A7a\u00A7lThe center blocks are unlocked!")
        }

        if (remaining <= 0) {
            endRound(fullyCaptured = false)
        }
    }

    /** Called by CenterBlockListener whenever a block gets claimed. */
    fun isCaptureAllowed(): Boolean = secondsElapsed >= Global.BATTLEBOX_CAPTURE_GRACE_SECONDS

    fun onBlockClaimed(index: Int, team: TeamColour) {
        blockOwners[index] = team
        if (blockOwners.size == 9 && blockOwners.values.all { it == team }) {
            addScore(team, Global.BATTLEBOX_CAPTURE_POINTS)
            broadcast("\u00A7b\u00A7l${team.minecraftName} \u00A7fcaptured the center for \u00A7e+${Global.BATTLEBOX_CAPTURE_POINTS} points\u00A7f!")
            endRound(fullyCaptured = true)
        }
    }

    fun onKill(killerTeam: TeamColour) {
        addScore(killerTeam, Global.BATTLEBOX_KILL_POINTS)
    }

    private fun addScore(team: TeamColour, amount: Int) {
        teamScores[team] = (teamScores[team] ?: 0) + amount
        broadcastScores()

        val winner = teamScores.entries.find { it.value >= Global.BATTLEBOX_TARGET_SCORE }
        if (winner != null) {
            endMatch(winner.key)
        }
    }

    private fun endRound(fullyCaptured: Boolean) {
        task?.cancel()
        task = null

        if (!fullyCaptured) {
            broadcast("\u00A77Time's up! No team captured the center this round.")
        }

        // If the match hasn't already ended (endMatch cancels activeGames membership), start the next round.
        if (activeGames.contains(this)) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable { startRound() }, 60L) // 3 second breather
        }
    }

    private fun endMatch(winner: TeamColour) {
        task?.cancel()
        task = null
        timerBar?.removeAll()
        activeGames.remove(this)

        broadcast("\u00A7e\u00A7l${winner.minecraftName} \u00A7fwins the match! Final score: " +
                teamScores.entries.joinToString(", ") { "${it.key.minecraftName}: ${it.value}" })

        for (uuid in gameManager.teamGenerator.teamMap.keys) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.teleport(org.bukkit.Location(
                Bukkit.getWorld(Global.WORLD_NAME),
                Global.SPAWN_X, Global.SPAWN_Y, Global.SPAWN_Z,
                Global.SPAWN_YAW, Global.SPAWN_PITCH
            ))
        }

        gameManager.endGame()
    }

    private fun broadcastScores() {
        broadcast(teamScores.entries.joinToString("  \u00A77|  ") { "${it.key.minecraftName} \u00A7f${it.value}" })
    }

    private fun broadcast(message: String) {
        for (uuid in gameManager.teamGenerator.teamMap.keys) {
            Bukkit.getPlayer(uuid)?.sendMessage(message)
        }
    }

    private fun formatTime(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
    }
}
