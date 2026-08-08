package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.createAtNamespacedKey
import app.simplecloud.npc.shared.hologramLineNamespacedKey
import app.simplecloud.npc.shared.hologramNamespacedKey
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType

/**
 * @author Niklas Nieberler
 */

class HologramEditor(
    private val npcId: String,
    private val location: Location,
    private val lineIndex: Int,
) {
    val textDisplay = createTextDisplay()

    /**
     * Sets a custom name to the text display
     * @param name the custom name
     * @return this editor instance
     */
    fun withCustomName(name: Component?): HologramEditor {
        this.textDisplay.text(name)
        return this
    }

    private fun createTextDisplay(): TextDisplay {
        val textDisplay = this.location.world.spawn(this.location, TextDisplay::class.java)
        textDisplay.billboard = Billboard.CENTER

        val persistentDataContainer = textDisplay.persistentDataContainer
        persistentDataContainer.set(hologramNamespacedKey, PersistentDataType.STRING, this.npcId)
        persistentDataContainer.set(createAtNamespacedKey, PersistentDataType.LONG, System.currentTimeMillis())
        persistentDataContainer.set(hologramLineNamespacedKey, PersistentDataType.INTEGER, this.lineIndex)
        return textDisplay
    }

}
