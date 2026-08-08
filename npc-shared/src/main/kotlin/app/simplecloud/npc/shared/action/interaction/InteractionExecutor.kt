package app.simplecloud.npc.shared.action.interaction

import app.simplecloud.npc.shared.action.TargetConnectionService
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.hologram.JoinStateHelper
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.sync
import app.simplecloud.npc.shared.utils.PlayerConnectionHelper
import app.simplecloud.plugin.api.shared.extension.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.time.Duration

class InteractionExecutor(
    private val namespace: NpcNamespace,
) {
    fun execute(
        id: String,
        player: Player,
        playerInteraction: PlayerInteraction,
        optionProvider: OptionProvider = OptionProvider(),
    ) {
        val config = namespace.npcRepository.find(id) ?: return
        val interaction = if (player.isSneaking) {
            PlayerInteraction.getOrNull("SHIFT_$playerInteraction") ?: playerInteraction
        } else {
            playerInteraction
        }

        CoroutineScope(Dispatchers.IO).launch {
            val joinState = JoinStateHelper.getJoinState(config)
            val actions = config.actionsFor(interaction, joinState)
            actions.forEach { executeAction(config, it, player) }
        }
    }

    private suspend fun executeAction(config: NpcConfig, action: NpcConfig.ActionConfiguration, player: Player) {
        if (action.joinTarget) {
            var destination: String? = null
            for (target in config.targetServers) {
                destination = TargetConnectionService.findDestination(target)
                if (destination != null) break
            }
            destination?.let { sync { PlayerConnectionHelper.sendPlayerToServer(player, it) } }
        }

        sync {
            action.playSound?.let {
                player.playSound(Sound.sound(Key.key(it), Sound.Source.MASTER, 1F, 1F))
            }
            action.executeCommand?.let { player.performCommand(resolvePlayerPlaceholders(it, player)) }
            action.sendMessage?.let { player.sendMessage(text(resolvePlayerPlaceholders(it, player))) }
            action.teleport?.let { teleport(player, it) }
            action.sendTitle?.let { showTitle(player, it) }
            action.sendToServer?.let { PlayerConnectionHelper.sendPlayerToServer(player, it) }
            action.transferToServer?.let { transfer(player, it) }
        }
    }

    private fun teleport(player: Player, config: NpcConfig.TeleportConfiguration) {
        val world = Bukkit.getWorld(config.world) ?: return
        player.teleport(Location(world, config.x, config.y, config.z, config.yaw, config.pitch))
    }

    private fun showTitle(player: Player, config: NpcConfig.TitleConfiguration) {
        val times = Title.Times.times(
            Duration.ofMillis(config.fadeIn * 50L),
            Duration.ofMillis(config.stay * 50L),
            Duration.ofMillis(config.fadeOut * 50L),
        )
        player.showTitle(Title.title(text(config.title), text(config.subtitle), times))
    }

    private fun transfer(player: Player, address: String) {
        val host = address.substringBefore(':')
        val port = address.substringAfter(':', "25565").toIntOrNull() ?: return
        player.transfer(host, port)
    }

    private fun resolvePlayerPlaceholders(value: String, player: Player): String {
        return value
            .replace("<playername>", player.name)
            .replace("<playeruuid>", player.uniqueId.toString())
    }
}
