package app.simplecloud.npc.bukkit.interact

import app.simplecloud.npc.core.interaction.PlayerInteraction
import org.bukkit.entity.Player

typealias OnNpcInteract = (id: String, player: Player, interaction: PlayerInteraction) -> Unit
