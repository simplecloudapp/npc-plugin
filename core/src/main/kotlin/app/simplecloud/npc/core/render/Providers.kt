package app.simplecloud.npc.core.render

object Providers {
    const val STANDALONE = "standalone"
    const val MANNEQUIN = "mannequin"
    const val CITIZENS = "citizens"
    const val FANCYNPCS = "fancynpcs"
    const val ZNPCSPLUS = "znpcsplus"
    const val MYTHICMOBS = "mythicmobs"

    val OWN: Set<String> = setOf(STANDALONE, MANNEQUIN)

    fun displayName(provider: String): String = when (provider.lowercase()) {
        STANDALONE -> "Standalone"
        MANNEQUIN -> "Mannequin"
        CITIZENS -> "Citizens"
        FANCYNPCS -> "FancyNpcs"
        ZNPCSPLUS -> "ZNPCsPlus"
        MYTHICMOBS -> "MythicMobs"
        else -> provider
    }
}
