package lumaris

import lumaris.Global.MAX_TEAM_SIZE
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Generates the teams for the game
 */
class TeamGenerator(private val queueList: MutableList<UUID>, private val partyMap: HashMap<UUID, UUID>) {
    val teamMap = mutableMapOf<UUID, TeamColour>()

    fun assignTeams() {
        val assigned = mutableSetOf<UUID>()
        val totalPlayers = queueList.size
        val numTeams = TeamColour.entries.size
        val idealTeamSize = (totalPlayers + numTeams - 1) / numTeams

        for (queuedPlayer in queueList) {
            if (queuedPlayer in assigned) continue

            val partyLeader = partyMap[queuedPlayer]
            if (partyLeader != null) {
                // Get all party members that are in the queue and not yet assigned
                val partyMembers = queueList.filter { partyMap[it] == partyLeader && it !in assigned }
                var currentTeam = getTeamWithFewerPlayers()

                for (member in partyMembers) {
                    // If adding this player unbalances the team or exceeds MAX_TEAM_SIZE, move to the next best team
                    if (getTeamSize(currentTeam) >= idealTeamSize || getTeamSize(currentTeam) >= MAX_TEAM_SIZE) {
                        currentTeam = getTeamWithFewerPlayers(currentTeam)
                    }

                    teamMap[member] = currentTeam
                    assigned.add(member)
                }
            }
            else {
                val bestTeam = getTeamWithFewerPlayers()
                teamMap[queuedPlayer] = bestTeam
                assigned.add(queuedPlayer)
            }
        }
    }

    fun messageTeam(player: Player?) {
        if (player == null || teamMap[player.uniqueId] == null) return

        val playerTeam = teamMap[player.uniqueId]!!

        player.sendMessage("§l§aYou are on team: ${playerTeam.minecraftName}.")
    }

    private fun getTeamWithFewerPlayers(exclude: TeamColour? = null): TeamColour {
        return TeamColour.entries
            .filter { it != exclude }
            .minByOrNull { getTeamSize(it) }
            ?: TeamColour.entries.first()
    }

    private fun getTeamSize(team: TeamColour): Int {
        return teamMap.values.count { it == team }
    }

    fun isTeamFull(team: TeamColour): Boolean {
        return getTeamSize(team) >= MAX_TEAM_SIZE
    }
}

enum class TeamColour(val minecraftName: String) {
    RED("§cRED"),
    BLUE("§9BLUE"),
    YELLOW("§eYELLOW"),
    LIME("§qLIME")
}