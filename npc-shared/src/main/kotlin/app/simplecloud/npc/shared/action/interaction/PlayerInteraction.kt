package app.simplecloud.npc.shared.action.interaction

/**
 * @author Niklas Nieberler
 */

enum class PlayerInteraction {

    RIGHT_CLICK,

    LEFT_CLICK;

    companion object {
        fun getOrNull(name: String): PlayerInteraction? {
            return entries.firstOrNull { it.name.equals(name, true) }
        }
    }

}