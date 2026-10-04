package app.simplecloud.npc.common.editor.npc

import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.location.NpcLocation
import java.util.Locale

object NpcFormat {

    val CLICK_ROWS = listOf(
        PlayerInteraction.LEFT_CLICK,
        PlayerInteraction.SHIFT_LEFT_CLICK,
        PlayerInteraction.RIGHT_CLICK,
        PlayerInteraction.SHIFT_RIGHT_CLICK,
    )

    fun displayName(config: NpcConfig): String =
        config.entity.customName?.takeIf { it.isNotBlank() }?.let(Msg.miniMessage::stripTags) ?: config.id

    fun plain(text: String): String = Msg.miniMessage.escapeTags(text)

    fun joinState(joinState: String): String = joinState.lowercase()

    fun isDefaultJoinState(joinState: String): Boolean = joinState.equals(NpcConfig.DEFAULT_JOIN_STATE, true)

    fun actionJoinStates(config: NpcConfig): List<String> =
        (listOf(NpcConfig.DEFAULT_JOIN_STATE) + config.joinStates + config.actions.map { it.joinState })
            .map { it.lowercase() }
            .distinct()

    fun hologramJoinStates(config: NpcConfig): List<String> =
        (listOf(NpcConfig.DEFAULT_JOIN_STATE) + config.hologram.layouts.map { it.joinState })
            .map { it.lowercase() }
            .distinct()

    fun cellName(interaction: PlayerInteraction, joinState: String): String =
        "${Clicks.short(interaction)} · ${joinState(joinState)}"

    fun seconds(ticks: Int, decimals: Int = 1): String = String.format(Locale.ROOT, "%.${decimals}fs", ticks / 20.0)

    fun pacing(title: NpcConfig.TitleConfiguration): String =
        "<bd><val>${seconds(title.fadeIn)} <bd>in · " +
            "<val>${seconds(title.stay)} <bd>hold · " +
            "<val>${seconds(title.fadeOut)} <bd>out"

    fun messageLines(message: String?): List<String> = message?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    fun fieldLines(action: NpcConfig.ActionConfiguration): List<String> = buildList {
        action.openInventory?.let { add("<bd>Menu <val>${plain(it)}") }
        action.sendToServer?.let { add("<bd>Server <val>${plain(it)}") }
        action.teleport?.let { add("<bd>Teleport <val>${position(it)}") }
        action.transferToServer?.let { add("<bd>Transfer <val>${plain(it)}") }
        action.sendMessage?.let { add("<bd>Chat <val>${messageLines(it).size} lines") }
        action.sendTitle?.let { add("<bd>Title <val>${Ui.quote(it.title.ifBlank { it.subtitle })}") }
        action.playSound?.let { add("<bd>Sound <val>${plain(it)}") }
        action.executeCommand?.let { add("<bd>Command <val>/${plain(it)}") }
    }

    fun soundSettings(options: NpcConfig.SoundOptions): String =
        "<hnt>${String.format(Locale.ROOT, "%.1f", options.volume)} vol · " +
            "${String.format(Locale.ROOT, "%.2f", options.pitch)} pitch"

    fun position(location: NpcLocation, separator: String = " "): String =
        "${location.world}$separator${location.x.toInt()} ${location.y.toInt()} ${location.z.toInt()}"

    fun skinLabel(skin: NpcConfig.SkinConfiguration): String =
        skin.sourcePlayer ?: if (skin.texture != null) "custom" else "default"

    fun glowLine(entity: NpcConfig.NpcEntityConfiguration): String =
        if (entity.glowing) "<bd>Glow <on>on <hnt>· <val>${glowColor(entity)}"
        else "<bd>Glow <off>off"

    fun glowColor(entity: NpcConfig.NpcEntityConfiguration): String = entity.glowColor ?: DEFAULT_GLOW_COLOR

    class GlowColor(val teamName: String, val label: String, val icon: String)

    val GLOW_COLORS = listOf(
        GlowColor("white", "White", "WHITE_WOOL"),
        GlowColor("gray", "Gray", "LIGHT_GRAY_WOOL"),
        GlowColor("dark_gray", "Dark Gray", "GRAY_WOOL"),
        GlowColor("black", "Black", "BLACK_WOOL"),
        GlowColor("red", "Red", "RED_WOOL"),
        GlowColor("dark_red", "Dark Red", "NETHER_WART_BLOCK"),
        GlowColor("gold", "Gold", "ORANGE_WOOL"),
        GlowColor("yellow", "Yellow", "YELLOW_WOOL"),
        GlowColor("green", "Green", "LIME_WOOL"),
        GlowColor("dark_green", "Dark Green", "GREEN_WOOL"),
        GlowColor("aqua", "Aqua", "CYAN_WOOL"),
        GlowColor("dark_aqua", "Dark Aqua", "WARPED_WART_BLOCK"),
        GlowColor("blue", "Blue", "LIGHT_BLUE_WOOL"),
        GlowColor("dark_blue", "Dark Blue", "BLUE_WOOL"),
        GlowColor("light_purple", "Light Purple", "MAGENTA_WOOL"),
        GlowColor("dark_purple", "Purple", "PURPLE_WOOL"),
    )

    fun glowIcon(teamName: String): String =
        (GLOW_COLORS.firstOrNull { it.teamName.equals(teamName, true) } ?: GLOW_COLORS.first()).icon

    private const val DEFAULT_GLOW_COLOR = "white"
}
