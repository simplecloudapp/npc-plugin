package app.simplecloud.npc.bukkit.hologram

import app.simplecloud.npc.core.hologram.PersonalText
import net.kyori.adventure.text.Component
import org.bukkit.entity.TextDisplay
import java.util.Locale

data class HologramLineKey(val npcId: String, val index: Int) {
    companion object {
        fun of(npcId: String, index: Int) = HologramLineKey(npcId.lowercase(Locale.ROOT), index)
    }
}

class PersonalLine(val text: PersonalText, val refreshSeconds: Double)

interface ViewerHologramText {

    fun bind(display: TextDisplay, key: HologramLineKey, line: PersonalLine?, shared: Component)

    fun retain(npcId: String, lineCount: Int)

    fun unbind(npcId: String)

    fun unbindAll()

    companion object {
        val NONE = object : ViewerHologramText {
            override fun bind(display: TextDisplay, key: HologramLineKey, line: PersonalLine?, shared: Component) = Unit
            override fun retain(npcId: String, lineCount: Int) = Unit
            override fun unbind(npcId: String) = Unit
            override fun unbindAll() = Unit
        }
    }
}
