package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import org.bukkit.entity.TextDisplay

/**
 * @author Niklas Nieberler
 */

object HologramModifier {

    fun modify(configuration: HologramConfiguration, textDisplay: TextDisplay) {
        textDisplay.billboard = configuration.billboard
        textDisplay.alignment = configuration.alignment
        configuration.viewRange?.let { textDisplay.viewRange = it }
        configuration.shadowRadius?.let { textDisplay.shadowRadius = it }
        configuration.displayHeight?.let { textDisplay.displayHeight = it }
        configuration.displayWidth?.let { textDisplay.displayWidth = it }
        configuration.shadow?.let { textDisplay.isShadowed = it }
        configuration.lineWidth?.let { textDisplay.lineWidth = it }
    }

}