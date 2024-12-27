package app.simplecloud.npc.shared.option

/**
 * @author Niklas Nieberler
 */

data class Option(
    val key: String = "",
    val value: Any = "",
) {

    companion object {
        fun <T> of(pair: Pair<String, Any>, value: T): Option {
            return Option(pair.first, value!!)
        }
    }

}