package lumaris.battlebox

import lumaris.GameMaps
import lumaris.Main
import lumaris.TeamColour
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Holds the configuration for a single Battle Box arena (one per map):
 * - the 9 center capture-block locations (index 1-9)
 * - each team's spawn point
 *
 * This is intentionally NOT hardcoded in source. Admins configure it in-game
 * with /battlebox setcenter and /battlebox setspawn, and it's saved to
 * plugins/Lumaris/arenas.yml so it survives restarts.
 */
class BattleBoxArena(val map: GameMaps) {
    // index 1-9 -> location of that center block
    val centerBlocks: MutableMap<Int, Location> = mutableMapOf()

    // team -> spawn location
    val teamSpawns: MutableMap<TeamColour, Location> = mutableMapOf()

    /**
     * True once all 9 center blocks and both team spawns are configured.
     * The game will refuse to start on a map that isn't fully set up.
     */
    fun isComplete(): Boolean {
        return centerBlocks.size == 9 && TeamColour.entries.all { teamSpawns.containsKey(it) }
    }

    fun missingPieces(): String {
        val missingBlocks = (1..9).filter { !centerBlocks.containsKey(it) }
        val missingSpawns = TeamColour.entries.filter { !teamSpawns.containsKey(it) }

        val parts = mutableListOf<String>()
        if (missingBlocks.isNotEmpty()) parts.add("center blocks: ${missingBlocks.joinToString(", ")}")
        if (missingSpawns.isNotEmpty()) parts.add("spawns: ${missingSpawns.joinToString(", ") { it.name }}")

        return if (parts.isEmpty()) "Nothing missing." else parts.joinToString(" | ")
    }
}

/**
 * Loads and saves all BattleBoxArena configs to plugins/Lumaris/arenas.yml.
 * Call ArenaManager.load(plugin) once in onEnable(), and ArenaManager.save(plugin)
 * after every change made via /battlebox setcenter or /battlebox setspawn.
 */
object ArenaManager {
    private val arenas: MutableMap<GameMaps, BattleBoxArena> = mutableMapOf()

    fun getArena(map: GameMaps): BattleBoxArena {
        return arenas.getOrPut(map) { BattleBoxArena(map) }
    }

    private fun getFile(plugin: Main): File {
        return File(plugin.dataFolder, "arenas.yml")
    }

    fun load(plugin: Main) {
        val file = getFile(plugin)
        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)

        for (map in GameMaps.entries) {
            val section = config.getConfigurationSection(map.name) ?: continue
            val arena = getArena(map)

            val blocksSection = section.getConfigurationSection("centerBlocks")
            if (blocksSection != null) {
                for (key in blocksSection.getKeys(false)) {
                    val index = key.toIntOrNull() ?: continue
                    val loc = blocksSection.getLocation(key) ?: continue
                    arena.centerBlocks[index] = loc
                }
            }

            val spawnsSection = section.getConfigurationSection("teamSpawns")
            if (spawnsSection != null) {
                for (key in spawnsSection.getKeys(false)) {
                    val team = TeamColour.entries.find { it.name == key } ?: continue
                    val loc = spawnsSection.getLocation(key) ?: continue
                    arena.teamSpawns[team] = loc
                }
            }
        }
    }

    fun save(plugin: Main) {
        val config = YamlConfiguration()

        for ((map, arena) in arenas) {
            for ((index, loc) in arena.centerBlocks) {
                config.set("${map.name}.centerBlocks.$index", loc)
            }
            for ((team, loc) in arena.teamSpawns) {
                config.set("${map.name}.teamSpawns.${team.name}", loc)
            }
        }

        val file = getFile(plugin)
        file.parentFile?.mkdirs()
        config.save(file)
    }
}
