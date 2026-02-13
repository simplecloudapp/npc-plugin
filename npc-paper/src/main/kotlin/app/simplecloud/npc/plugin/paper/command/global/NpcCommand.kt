package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.npc.shared.enums.NpcType
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions

/**
 * @author Niklas Nieberler
 */

class NpcCommand(
    namespace: NpcNamespace
) : AbstractNpcCommand(
    namespace
) {

    @Command(commandName)
    @Permission("simplecloud.command.npc")
    fun execute(sender: CommandSourceStack) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        CommandMessages.sendHelpMessage(player)
    }

    @Command("$commandName apply <id>")
    @Permission("simplecloud.command.npc")
    fun executeApplyNpc(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "availableNpcIds") npcId: String
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        val npcManager = this.namespace.npcManager

        if (!this.namespace.existNpc(npcId)) {
            player.sendMessage(text("$PREFIX <#dc2626>Npc with id $npcId does not exist!"))
            return
        }

        if (npcManager.exist(npcId)) {
            player.sendMessage(text("$PREFIX <#dc2626>Npc with id $npcId already exist!"))
            return
        }

        val npcConfig = npcManager.create(npcId) ?: return
        player.sendMessage(text("$PREFIX <#ffffff>Npc with id $npcId has been <#a3e635>created."))
        CoroutineScope(Dispatchers.IO).launch {
            updateHolograms(npcConfig)
        }
    }

    @Command("$commandName <id> setType <type>")
    @Permission("simplecloud.command.npc")
    fun executeSetType(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("type", suggestions = "npcTypes") type: String
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player

        val npcType = try {
            NpcType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            player.sendMessage(text("$PREFIX <#dc2626>Invalid type! Please use ${NpcType.entries.joinToString(", ") { it.name.lowercase() }}"))
            return
        }

        invokeConfig(player, npcId) { config ->
            player.sendMessage(text("$PREFIX <#ffffff>Npc type has been set to <#a3e635>${npcType.name.lowercase()}."))
            config.copy(npcType = npcType)
        }
    }

    @Command("$commandName <id> setHologram <name>")
    @Permission("simplecloud.command.npc")
    fun executeSetHologram(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("name", suggestions = "hologramNames") name: String
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player

        invokeConfig(player, npcId) { config ->
            if (config.npcType == null) {
                player.sendMessage(text("$PREFIX <#dc2626>Please set the npc type first using /npc <id> setType <type>"))
                return@invokeConfig config
            }
            val exists = runBlocking {
                try {
                    when (config.npcType) {
                        NpcType.GROUP -> {
                            CloudService.cloudApi.group().getGroupByName(name).await()
                            true
                        }
                        NpcType.PERSISTENT -> {
                            CloudService.cloudApi.persistentServer().getPersistentServerByName(name).await()
                            true
                        }
                        null -> false
                    }
                } catch (e: Exception) {
                    false
                }
            }

            if (!exists) {
                val typeName = when (config.npcType) {
                    NpcType.GROUP -> "group"
                    NpcType.PERSISTENT -> "persistent server"
                    null -> "unknown"
                }
                player.sendMessage(text("$PREFIX <#dc2626>The $typeName with name $name does not exist!"))
                return@invokeConfig config
            }

            config.hologramConfiguration.placeholderName = name
            player.sendMessage(text("$PREFIX <#ffffff>Hologram name has been set to <#a3e635>$name."))
            CoroutineScope(Dispatchers.IO).launch { namespace.hologramManager.updateTextHologramByName(config, name) }
            config
        }
    }

    @Suggestions("npcTypes")
    fun suggestNpcTypes(): List<String> {
        return NpcType.entries.map { it.name.lowercase() }
    }

    @Suggestions("hologramNames")
    fun suggestHologramNames(sender: CommandSourceStack, @Argument("id") npcId: String): List<String> {
        val config = this.namespace.npcRepository.get(npcId) ?: return emptyList()
        return when (config.npcType) {
            NpcType.GROUP -> runBlocking {
                try {
                    CloudService.cloudApi.group().allGroups.await().map { it.name }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            NpcType.PERSISTENT -> runBlocking {
                try {
                    CloudService.cloudApi.persistentServer().allPersistentServers.await().map { it.name }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            null -> emptyList()
        }
    }

    @Suggestions("availableNpcIds")
    fun suggestAvailableNpcId(): List<String> {
        return this.namespace.findAllNpcs()
            .filter { !this.namespace.npcManager.exist(it) }
    }

    private fun updateHolograms(npcConfig: NpcConfig) {
        val hologramManager = this.namespace.hologramManager
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        if (hologramManager.getTextDisplays(npcConfig.id).isEmpty()) {
            coroutineScope.launch { hologramManager.updateHolograms(npcConfig) }
            return
        }
        val placeholderName = npcConfig.hologramConfiguration.placeholderName
        coroutineScope.launch { hologramManager.updateTextHologramByName(npcConfig, placeholderName) }
    }

    private fun invokeConfig(player: Player, npcId: String, function: (NpcConfig) -> NpcConfig) {
        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val newConfig = function(npcConfig)
        this.namespace.npcRepository.save("${newConfig.id}.yml", newConfig)
    }

}