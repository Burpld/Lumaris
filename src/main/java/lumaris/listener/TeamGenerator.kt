package lumaris.listener

import lumaris.Global.MAX_TEAM_SIZE
import org.bukkit.event.Listener
import java.util.UUID

/**
 * Generates the teams for the game
 */
class TeamGenerator(private val queueList: MutableList<UUID>, private val partyMap: HashMap<UUID, UUID>) : Listener {
    private val teamMap = mutableMapOf<UUID, TeamColour>()

    fun assignTeams() {
        val assigned = mutableSetOf<UUID>()
        val totalPlayers = queueList.size
        val idealTeamSize = (totalPlayers + 1) / 2

        for (queuedPlayer in queueList) {
            if (queuedPlayer in assigned) continue

            val partyLeader = partyMap[queuedPlayer]
            if (partyLeader != null) {
                // Get all party members that are in the queue and not yet assigned
                val partyMembers = queueList.filter { partyMap[it] == partyLeader && it !in assigned }
                var currentTeam = getTeamWithFewerPlayers()

                for (member in partyMembers) {
                    // If adding this player unbalances the team or exceeds MAX_TEAM_SIZE
                    if (getTeamSize(currentTeam) >= idealTeamSize || getTeamSize(currentTeam) >= MAX_TEAM_SIZE) {
                        currentTeam = if (currentTeam == TeamColour.RED) TeamColour.BLUE else TeamColour.RED
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

    private fun getTeamWithFewerPlayers(): TeamColour {
        val redSize = getTeamSize(TeamColour.RED)
        val blueSize = getTeamSize(TeamColour.BLUE)
        return if (redSize <= blueSize) TeamColour.RED else TeamColour.BLUE
    }

    private fun getTeamSize(team: TeamColour): Int {
        return teamMap.values.count { it == team }
    }
}

enum class TeamColour {
    RED,
    BLUE,
    YELLOW,
    LIME
}