package app.simplecloud.npc.core.config

enum class NpcPose(val heightFactor: Double) {
    STANDING(1.0),
    SNEAKING(0.83),
    SWIMMING(0.33),
    SLEEPING(0.33),
}
