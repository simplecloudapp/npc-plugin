package app.simplecloud.npc.core.config

enum class EquipmentSlot {
    MAIN_HAND,
    OFF_HAND,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    ;

    companion object {
        private val HEAD_SUFFIXES = listOf("_HELMET", "_HEAD", "_SKULL", "CARVED_PUMPKIN", "TURTLE_HELMET")
        private val CHEST_SUFFIXES = listOf("_CHESTPLATE", "ELYTRA")

        fun suitedFor(material: String): EquipmentSlot {
            val name = material.uppercase()

            return when {
                HEAD_SUFFIXES.any(name::endsWith) -> HELMET
                CHEST_SUFFIXES.any(name::endsWith) -> CHESTPLATE
                name.endsWith("_LEGGINGS") -> LEGGINGS
                name.endsWith("_BOOTS") -> BOOTS
                name == "SHIELD" -> OFF_HAND
                else -> MAIN_HAND
            }
        }
    }
}
