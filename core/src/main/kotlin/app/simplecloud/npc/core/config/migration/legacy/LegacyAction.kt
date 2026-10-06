package app.simplecloud.npc.core.config.migration.legacy

import app.simplecloud.npc.core.render.Providers

enum class LegacyAction {
    RUN_COMMAND,
    RUN_CONSOLE_COMMAND,
    CONNECT_TO_SERVER,
    TRANSFER_TO_SERVER,
    QUICK_JOIN,
    OPEN_INVENTORY,
}

object LegacyActionOptions {
    const val EXECUTE_COMMAND_NAME = "command.name"
    const val CONNECT_TO_SERVER_NAME = "server.name"
    const val TRANSFER_SERVER_IP = "server.ip"
    const val TRANSFER_SERVER_PORT = "server.port"
    const val TRANSFER_SERVER_DEFAULT_PORT = "25565"
    const val GROUP_NAME = "group.name"
    const val INVENTORY_NAME = "inventory.name"
}

object LegacyPlayerActionOptions {
    const val SEND_MESSAGE = "send.message"
    const val SEND_TITLE = "send.title"
    const val SEND_SUBTITLE = "send.subtitle"
    const val PLAY_SOUND = "play.sound"
}

enum class LegacyProviderType(val commandName: String) {
    CITIZENS(Providers.CITIZENS),
    FANCY_NPCS(Providers.FANCYNPCS),
    MYTHIC_MOBS(Providers.MYTHICMOBS),
    ZNPCS_PLUS(Providers.ZNPCSPLUS);

    companion object {
        fun getOrNull(value: String): LegacyProviderType? = entries.firstOrNull {
            it.commandName.equals(value, true) || it.name.equals(value, true)
        }
    }
}
