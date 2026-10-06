package app.simplecloud.npc.core.config

import net.kyori.adventure.text.format.NamedTextColor

object GlowColors {
    val NAMES: List<String> = listOf(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white",
    )

    fun resolve(name: String?): NamedTextColor? = name?.let { NamedTextColor.NAMES.value(it.lowercase()) }
}
