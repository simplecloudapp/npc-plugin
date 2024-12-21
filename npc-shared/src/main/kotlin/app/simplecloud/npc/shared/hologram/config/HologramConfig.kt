package app.simplecloud.npc.shared.hologram.config

import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.TextDisplay
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class HologramConfig(
    val text: String = "",
    val billboard: Billboard = Billboard.FIXED,
    val viewRange: Float? = null,
    val shadowRadius: Float? = null,
    val displayHeight: Float? = null,
    val displayWidth: Float? = null,
    val interpolationDelay: Int? = null,
    val alignment: TextDisplay.TextAlignment = TextDisplay.TextAlignment.CENTER,
    val lineWidth: Int? = null,
    val shadow: Boolean? = null,
)