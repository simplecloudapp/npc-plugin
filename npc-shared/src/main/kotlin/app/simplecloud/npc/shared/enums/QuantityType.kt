package app.simplecloud.npc.shared.enums

/**
 * @author Niklas Nieberler
 */

enum class QuantityType(
    private val function: (Int) -> Boolean
) {

    MOST({ it >= 0 }),

    LEAST({ it <= 0 });

    fun invoke(quantity: Int): Boolean {
        return this.function(quantity)
    }

}