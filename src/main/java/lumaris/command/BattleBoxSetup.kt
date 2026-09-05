package lumaris.command

import lumaris.GameMaps
import lumaris.Main
import lumaris.TeamColour
import lumaris.battlebox.ArenaManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * /battlebox setcenter <map> <1-9>   - marks your current location as center block <index>
 * /battlebox setspawn <map> <RED|BLUE> - marks your current location as that team's spawn
 * /battlebox status <map>            - shows what's configured / missing for that map
 *
 * This exists so arenas can be configured entirely in-game, without editing any code.
 */
class BattleBoxSetup(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used in-game.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("Usage: /battlebox <setcenter|setspawn|status> <map> [index|team]")
            return true
        }

        when (args[0].lowercase()) {
            "setcenter" -> {
                if (args.size < 3) {
                    sender.sendMessage("Usage: /battlebox setcenter <map> <1-9>")
                    return true
                }
                val map = parseMap(args[1]) ?: run {
                    sender.sendMessage("Unknown map '${args[1]}'. Valid maps: ${GameMaps.entries.joinToString(", ") { it.name }}")
                    return true
                }
                val index = args[2].toIntOrNull()
                if (index == null || index !in 1..9) {
                    sender.sendMessage("Index must be a number from 1 to 9.")
                    return true
                }

                val arena = ArenaManager.getArena(map)
                arena.centerBlocks[index] = sender.location
                ArenaManager.save(plugin)

                sender.sendMessage("Set center block $index for ${map.name} to your current location.")
                sender.sendMessage("Progress: ${arena.missingPieces()}")
            }

            "setspawn" -> {
                if (args.size < 3) {
                    sender.sendMessage("Usage: /battlebox setspawn <map> <RED|BLUE>")
                    return true
                }
                val map = parseMap(args[1]) ?: run {
                    sender.sendMessage("Unknown map '${args[1]}'. Valid maps: ${GameMaps.entries.joinToString(", ") { it.name }}")
                    return true
                }
                val team = TeamColour.entries.find { it.name.equals(args[2], ignoreCase = true) }
                if (team == null) {
                    sender.sendMessage("Unknown team '${args[2]}'. Valid teams: ${TeamColour.entries.joinToString(", ") { it.name }}")
                    return true
                }

                val arena = ArenaManager.getArena(map)
                arena.teamSpawns[team] = sender.location
                ArenaManager.save(plugin)

                sender.sendMessage("Set ${team.name} spawn for ${map.name} to your current location.")
                sender.sendMessage("Progress: ${arena.missingPieces()}")
            }

            "status" -> {
                if (args.size < 2) {
                    sender.sendMessage("Usage: /battlebox status <map>")
                    return true
                }
                val map = parseMap(args[1]) ?: run {
                    sender.sendMessage("Unknown map '${args[1]}'. Valid maps: ${GameMaps.entries.joinToString(", ") { it.name }}")
                    return true
                }
                val arena = ArenaManager.getArena(map)
                if (arena.isComplete()) {
                    sender.sendMessage("${map.name} is fully configured and ready to play.")
                } else {
                    sender.sendMessage("${map.name} is missing: ${arena.missingPieces()}")
                }
            }

            else -> sender.sendMessage("Usage: /battlebox <setcenter|setspawn|status> <map> [index|team]")
        }

        return true
    }

    private fun parseMap(input: String): GameMaps? {
        return GameMaps.entries.find { it.name.equals(input, ignoreCase = true) }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        return when (args.size) {
            1 -> mutableListOf("setcenter", "setspawn", "status").filter { it.startsWith(args[0].lowercase()) }.toMutableList()
            2 -> GameMaps.entries.map { it.name }.filter { it.startsWith(args[1].uppercase()) }.toMutableList()
            3 -> when (args[0].lowercase()) {
                "setcenter" -> (1..9).map { it.toString() }.toMutableList()
                "setspawn" -> TeamColour.entries.map { it.name }.toMutableList()
                else -> mutableListOf()
            }
            else -> mutableListOf()
        }
    }
}
