package app.simplecloud.npc.provider.fancynpcs

import app.simplecloud.npc.core.config.GlowColors
import de.oliver.fancynpcs.api.NpcData
import net.kyori.adventure.text.format.NamedTextColor

internal object FancyGlowColor {

    fun apply(data: NpcData, colorName: String?) {
        val name = colorName?.lowercase()?.takeIf(GlowColors.NAMES::contains) ?: "white"
        NamedTextColor.NAMES.value(name)?.let(data::setGlowingColor)
    }
}
