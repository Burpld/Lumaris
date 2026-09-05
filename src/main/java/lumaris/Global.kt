package lumaris

object Global {
    // Spawn configs
    const val WORLD_NAME: String = "world"
    const val SPAWN_X: Double = -601.5
    const val SPAWN_Y: Double = 265.0
    const val SPAWN_Z: Double = -361.5
    const val SPAWN_YAW: Float = -180f
    const val SPAWN_PITCH: Float = 1f

    // Party Config
    const val MIN_PLAYERS_QUEUED: Int = 2
    const val MAX_PLAYERS_QUEUED: Int = 6

    // Game Config
    const val MAX_TEAM_SIZE: Int = 4

    // Battle Box Config
    const val BATTLEBOX_TARGET_SCORE: Int = 1500
    const val BATTLEBOX_ROUND_SECONDS: Int = 120
    const val BATTLEBOX_CAPTURE_GRACE_SECONDS: Int = 30
    const val BATTLEBOX_KILL_POINTS: Int = 100
    const val BATTLEBOX_CAPTURE_POINTS: Int = 300
}
