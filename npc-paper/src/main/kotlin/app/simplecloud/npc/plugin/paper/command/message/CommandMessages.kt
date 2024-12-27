package app.simplecloud.npc.plugin.paper.command.message

import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.shared.text
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.option.Option
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

object CommandMessages {

    fun sendOptionMessage(player: Player, npcConfig: NpcConfig, key: String, option: Option?) {
        if (option == null) {
            player.sendMessage(text("$PREFIX <#dc2626>Option $key does not exist!"))
            return
        }
        player.sendMessage(
            text("$PREFIX <#ffffff>Option of npc ${npcConfig.id}").appendNewline()
                .append(text("   <#a3a3a3>Key: <#38bdf8>${option.key}")).appendNewline()
                .append(text("   <#a3a3a3>Value: <#38bdf8>${option.value}"))
        )
    }

    fun sendNoActionCreated(player: Player, playerInteraction: String) {
        val interaction = PlayerInteraction.getOrNull(playerInteraction)?.name?.lowercase() ?: return
        player.sendMessage(text("$PREFIX <#dc2626>No action has been created! Please use /scnpc <npcId> interact $interaction setAction <action>"))
    }

    fun sendHelpMessage(player: Player) {
        val component = text("$PREFIX <#ffffff>Commands of NPC Plugin").appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> interact <type>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> interact <type> setAction <action>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> interact <type> setOption <key> <value>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> interact <type> getOption <key>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> interact <type> removeOption <key>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> setOption <key> <value>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> getOption <key>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> removeOption <key>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc <npcId> setHologramGroup <groupName>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc createInventory <name> <groupName>")).appendNewline()
            .append(text("   <#a3a3a3>/scnpc openInventory <name>"))
        player.sendMessage(component)
    }

}