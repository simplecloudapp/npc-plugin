package app.simplecloud.npc.shared.enums

/**
 * @author Niklas Nieberler
 */

enum class QuantityType {

    MOST,

    LEAST;

    companion object {
        fun exist(name: String): Boolean {
            return entries.firstOrNull { it.name == name.uppercase() } != null
        }
    }

}