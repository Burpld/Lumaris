package lumaris

object MapSelector {
    @JvmStatic
    fun getRandomMap(): GameMaps {
        return GameMaps.entries.random()
    }
}

enum class GameMaps(val id : Int) {
    ISLAND(1),
    VOLCANO(2),
    NEON_CITY(3)
}