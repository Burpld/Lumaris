package lumaris

import java.util.UUID

class GameManager(private val queueList: MutableList<UUID>, private val partyMap: HashMap<UUID, UUID>) {
    companion object {
        @JvmStatic
        val runningGames = mutableListOf<GameManager>()
    }

    val teamGenerator = TeamGenerator(queueList, partyMap)

    fun endGame() {
        runningGames.remove(this)
    }
}