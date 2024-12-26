package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.createAtNamespacedKey
import app.simplecloud.npc.shared.hologramNamespacedKey
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType

/**
 * @author Niklas Nieberler
 */

data class HologramEditor(
    private val npcId: String,
    private val location: Location,
) {

    private var yCloneLocation = 0.3
    private var nextLineHologram: HologramEditor? = null

    val textDisplay = createTextDisplay()
    val id = textDisplay.uniqueId

    /**
     * Sets a custom name to the text display
     * @param name the custom name
     * @return this editor instance
     */
    fun withCustomName(name: Component?): HologramEditor {
        this.textDisplay.text(name)
        return this
    }

    /**
     * Creates a new hologram line
     * @return new hologram instance
     */
    fun withNextLine(): HologramEditor {
        if (this.nextLineHologram == null)
            return HologramEditor(this.npcId, this.location.clone().add(0.0, this.yCloneLocation, 0.0))
        return this.nextLineHologram!!
    }

    /**
     * Destroys the text display
     */
    fun destroy() {
        this.textDisplay.remove()
    }

    private fun createTextDisplay(): TextDisplay {
        val textDisplay = this.location.world.spawn(this.location, TextDisplay::class.java)
        textDisplay.billboard = Billboard.CENTER

        val persistentDataContainer = textDisplay.persistentDataContainer
        persistentDataContainer.set(hologramNamespacedKey, PersistentDataType.STRING, this.npcId)
        persistentDataContainer.set(createAtNamespacedKey, PersistentDataType.LONG, System.currentTimeMillis())
        return textDisplay
    }

}