package app.simplecloud.npc.common.inventory.view

import app.simplecloud.npc.common.text.substitute
import app.simplecloud.npc.core.config.NpcConfig

object EntryActions {
    fun substituted(
        action: NpcConfig.ActionConfiguration,
        substitutions: Map<String, String>,
    ): NpcConfig.ActionConfiguration {
        if (substitutions.isEmpty()) return action

        fun apply(value: String): String = value.substitute(substitutions)

        return action.copy(
            playSound = action.playSound?.let(::apply),
            executeCommand = action.executeCommand?.let(::apply),
            sendMessage = action.sendMessage?.let(::apply),
            sendToServer = action.sendToServer?.let(::apply),
            transferToServer = action.transferToServer?.let(::apply),
            openInventory = action.openInventory?.let(::apply),
            actionBar = action.actionBar?.let(::apply),
            denyMessage = action.denyMessage?.let(::apply),
            sendTitle = action.sendTitle?.let { it.copy(title = apply(it.title), subtitle = apply(it.subtitle)) },
        )
    }
}
