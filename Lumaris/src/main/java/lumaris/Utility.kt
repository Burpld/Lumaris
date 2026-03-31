package lumaris

import org.bukkit.entity.Player
import kotlin.math.atan2
import kotlin.math.roundToInt

object Utility {
    @JvmStatic
    fun calculateEffectiveScore(player: Player): Int {
        return atan2(player.ping.toDouble(), player.yaw.toDouble()).roundToInt()
    }
}