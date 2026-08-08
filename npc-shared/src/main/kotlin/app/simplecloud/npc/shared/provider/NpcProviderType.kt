package app.simplecloud.npc.shared.provider

enum class NpcProviderType(val commandName: String) {
    CITIZENS("citizens"),
    FANCY_NPCS("fancynpcs"),
    MYTHIC_MOBS("mythicmobs"),
    ZNPCS_PLUS("znpcsplus");

    companion object {
        fun getOrNull(value: String): NpcProviderType? = entries.firstOrNull {
            it.commandName.equals(value, true) || it.name.equals(value, true)
        }
    }
}
