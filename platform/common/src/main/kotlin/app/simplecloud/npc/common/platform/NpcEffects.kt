package app.simplecloud.npc.common.platform

import app.simplecloud.npc.core.config.NpcAnimation
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

interface NpcEffects {
    fun animate(npc: NpcConfig, player: NpcPlayer, animation: NpcAnimation)
    fun speak(npc: NpcConfig, player: NpcPlayer, text: Component)

    companion object {
        val NONE = object : NpcEffects {
            override fun animate(npc: NpcConfig, player: NpcPlayer, animation: NpcAnimation) = Unit
            override fun speak(npc: NpcConfig, player: NpcPlayer, text: Component) = Unit
        }
    }
}
