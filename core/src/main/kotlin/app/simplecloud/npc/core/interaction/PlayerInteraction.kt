package app.simplecloud.npc.core.interaction

enum class PlayerInteraction {
    RIGHT_CLICK,
    LEFT_CLICK,
    SHIFT_RIGHT_CLICK,
    SHIFT_LEFT_CLICK;

    fun shifted(): PlayerInteraction = when (this) {
        RIGHT_CLICK -> SHIFT_RIGHT_CLICK
        LEFT_CLICK -> SHIFT_LEFT_CLICK
        SHIFT_RIGHT_CLICK, SHIFT_LEFT_CLICK -> this
    }

    companion object {
        fun getOrNull(name: String): PlayerInteraction? = entries.firstOrNull { it.name.equals(name, true) }
    }
}
