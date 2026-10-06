package app.simplecloud.npc.common.action

import app.simplecloud.npc.common.inventory.source.InventoryOpenContext
import app.simplecloud.npc.common.inventory.view.InventoryOpener
import app.simplecloud.npc.common.platform.InteractionHooks
import app.simplecloud.npc.common.platform.NpcEffects
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.common.text.PlayerMessages
import app.simplecloud.npc.core.text.PlayerPlaceholders
import app.simplecloud.npc.common.utils.InputChecks
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.cloud.CloudLists
import app.simplecloud.npc.core.cloud.CloudOutageLog
import app.simplecloud.npc.core.cloud.CloudUnavailableException
import app.simplecloud.npc.core.cloud.JoinStateResolver
import app.simplecloud.npc.core.cloud.TargetResolver
import app.simplecloud.npc.core.config.ActionFields
import app.simplecloud.npc.core.config.JoinStrategy
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.repository.NpcRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import java.util.logging.Level

class InteractionExecutor(
    private val npcRepository: NpcRepository,
    private val inventoryOpener: InventoryOpener,
    private val effects: NpcEffects = NpcEffects.NONE,
    private val hooks: () -> InteractionHooks = { InteractionHooks.NONE },
    private val lists: CloudLists = CloudListCache,
) {

    private val interactionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cooldowns = ClickCooldowns()

    fun execute(id: String, player: NpcPlayer, playerInteraction: PlayerInteraction) {
        val config = npcRepository.find(id) ?: return

        interactionScope.launch {
            try {
                val joinState = JoinStateResolver.of(config)
                val actions = config.actionsFor(playerInteraction, joinState)
                if (!passesCooldown(player, "npc:$id:$playerInteraction", actions)) return@launch
                if (!hooks().interact(config.id, player, playerInteraction, joinState)) return@launch

                actions.forEach { executeAction(config.targetServers, it, player, config.joinStrategy, config) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Failed to handle interaction with NPC $id", exception)
            }
        }
    }

    fun passesCooldown(player: NpcPlayer, key: String, actions: List<NpcConfig.ActionConfiguration>): Boolean =
        cooldowns.tryAcquire(player.uniqueId, key, actions.maxOfOrNull { it.cooldown } ?: 0)

    fun shutdown() {
        interactionScope.cancel()
    }

    suspend fun executeAction(
        targetServers: List<String>,
        action: NpcConfig.ActionConfiguration,
        player: NpcPlayer,
        strategy: JoinStrategy = JoinStrategy.LEAST_PLAYERS,
        npc: NpcConfig? = null,
    ) {
        val permission = action.permission
        if (!permission.isNullOrBlank() && !player.hasPermission(permission)) {
            PlayerMessages.denied(player, action.denyMessage?.let { PlayerPlaceholders.resolve(it, player) })
            return
        }

        action.playSound?.let { sound ->
            runCatching { player.playSound(sound, action.playSoundOptions) }
                .onFailure { logFailure(ActionFields.PLAY_SOUND, it) }
        }

        if (npc != null) {
            action.npcAnimation?.let { animation ->
                runCatching { effects.animate(npc, player, animation) }
                    .onFailure { logFailure(ActionFields.NPC_ANIMATION, it) }
            }
            action.speech?.let { text ->
                runCatching { effects.speak(npc, player, render(text, player)) }
                    .onFailure { logFailure(ActionFields.SPEECH, it) }
            }
        }

        action.executeCommand?.let { command ->
            runCatching { player.performCommand(PlayerPlaceholders.resolve(command, player)) }
                .onFailure { logFailure(ActionFields.EXECUTE_COMMAND, it) }
        }

        action.sendMessage?.let { message ->
            runCatching { player.sendMessage(render(message, player)) }
                .onFailure { logFailure(ActionFields.SEND_MESSAGE, it) }
        }

        action.teleport?.let { location ->
            runCatching { player.teleport(location) }.onFailure { logFailure(ActionFields.TELEPORT, it) }
        }

        action.sendTitle?.let { config ->
            runCatching { showTitle(player, config) }.onFailure { logFailure(ActionFields.SEND_TITLE, it) }
        }

        action.actionBar?.let { text ->
            runCatching { player.sendActionBar(render(text, player)) }
                .onFailure { logFailure(ActionFields.ACTION_BAR, it) }
        }

        action.openInventory?.let { inventoryId ->
            runCatching { inventoryOpener.open(player, inventoryId, InventoryOpenContext(targetServers, strategy)) }
                .onFailure { logFailure(ActionFields.OPEN_INVENTORY, it) }
        }

        if (action.previousMenu) {
            runCatching { inventoryOpener.back(player) }.onFailure { logFailure(ActionFields.PREVIOUS_MENU, it) }
        } else if (action.closeMenu) {
            runCatching { player.closeInventory() }.onFailure { logFailure(ActionFields.CLOSE_MENU, it) }
        }

        switchServer(targetServers, action, player, strategy, npc?.id)
    }

    private suspend fun switchServer(
        targetServers: List<String>,
        action: NpcConfig.ActionConfiguration,
        player: NpcPlayer,
        strategy: JoinStrategy,
        npcId: String?,
    ) {
        val sendTo = action.sendToServer
        val transferTo = action.transferToServer
        val configured = listOfNotNull(
            ActionFields.JOIN_TARGET.takeIf { action.joinTarget },
            ActionFields.SEND_TO_SERVER.takeIf { sendTo != null },
            ActionFields.TRANSFER_TO_SERVER.takeIf { transferTo != null },
        )
        if (configured.isEmpty()) return
        if (configured.size > 1) {
            CloudOutageLog.warnThrottled(
                "NPC action sets ${configured.joinToString(", ")}; only '${configured.first()}' is used.",
                key = "action-switch:${configured.joinToString()}",
            )
        }

        when {
            action.joinTarget -> runCatching {
                val found = targetServers.firstNotNullOfOrNull { TargetResolver.findDestination(it, lists, strategy) }
                    ?: return@runCatching
                hooks().join(npcId, player, found)?.let(player::sendToServer)
            }.onFailure {
                if (it is CancellationException) throw it
                logFailure(ActionFields.JOIN_TARGET, it)
            }

            sendTo != null -> runCatching { player.sendToServer(sendTo) }
                .onFailure { logFailure(ActionFields.SEND_TO_SERVER, it) }

            transferTo != null -> runCatching { transfer(player, transferTo) }
                .onFailure { logFailure(ActionFields.TRANSFER_TO_SERVER, it) }
        }
    }

    private fun logFailure(actionType: String, throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        if (throwable is CloudUnavailableException) {
            CloudOutageLog.warnThrottled(
                "NPC action '$actionType' skipped: ${throwable.message}",
                key = "action:$actionType",
            )
            return
        }

        logger.log(Level.WARNING, "NPC action '$actionType' failed", throwable)
    }

    private fun render(text: String, player: NpcPlayer): Component =
        Msg.miniMessage.deserialize(PlayerPlaceholders.resolve(text, player))

    private fun showTitle(player: NpcPlayer, config: NpcConfig.TitleConfiguration) = player.showTitle(
        render(config.title, player),
        render(config.subtitle, player),
        config.fadeIn,
        config.stay,
        config.fadeOut,
    )

    private fun transfer(player: NpcPlayer, address: String) {
        val (host, port) = InputChecks.parseHostPort(address) ?: run {
            CloudOutageLog.warnThrottled(
                "NPC action '${ActionFields.TRANSFER_TO_SERVER}' has an unusable address '$address'.",
                key = "transfer:$address",
            )
            return
        }

        player.transferToServer(host, port)
    }

    private companion object {
        private val logger = NpcLog.logger
    }
}
