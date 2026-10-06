package app.simplecloud.npc.common.editor

import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig.SoundOptions
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

sealed interface PromptResult {
    data object Accepted : PromptResult
    data class Rejected(val reason: Component) : PromptResult

    companion object {
        fun rejectInvalidMiniMessage(text: String): Rejected? =
            if (runCatching { Msg.miniMessage.deserialize(text) }.isSuccess) null
            else Rejected(Component.text("That is not valid MiniMessage."))
    }
}

enum class PromptFormat { PLAIN, MINI_MESSAGE }

class PromptPlaceholder(val token: String, val description: String)

class PromptExtra(
    val label: String,
    val hint: String,
    val submit: (String) -> PromptResult,
    val next: () -> Unit,
)

data class Prompt(
    val title: String = "Input",
    val instruction: String = "",
    val instructionArgs: List<Any?> = emptyList(),
    val cancelKeyword: String = "cancel",
    val current: String? = null,
    val format: PromptFormat = PromptFormat.PLAIN,
    val placeholders: List<PromptPlaceholder> = emptyList(),
    val context: String? = null,
    val onClear: (() -> Unit)? = null,
    val extra: PromptExtra? = null,
    val onCancel: () -> Unit = {},
    val onSubmit: (String) -> PromptResult,
)

class ChatInputPrompts {

    private class Pending(val player: NpcPlayer, val prompt: Prompt) {
        @Volatile
        var staged: String? = null
    }

    private val pending = ConcurrentHashMap<UUID, Pending>()

    fun start(player: NpcPlayer, prompt: Prompt) {
        pending.remove(player.uniqueId)?.let { previous -> runCatching(previous.prompt.onCancel) }
        val entry = Pending(player, prompt)
        pending[player.uniqueId] = entry

        player.sendMessage(PromptCard.open(prompt, controls(entry)))
        player.sendActionBar(PromptCard.hud(prompt, staged = false))
        player.playSound(OPEN_SOUND, OPEN_TONE)
    }

    fun isPending(uuid: UUID): Boolean = pending.containsKey(uuid)

    fun cancel(uuid: UUID) {
        pending.remove(uuid)?.let { entry ->
            entry.player.sendActionBar(Component.empty())
            runCatching(entry.prompt.onCancel)
        }
    }

    fun refreshHud() {
        pending.values.forEach { entry ->
            entry.player.sendActionBar(PromptCard.hud(entry.prompt, entry.staged != null))
        }
    }

    fun submit(uuid: UUID, text: String) {
        val entry = pending[uuid] ?: return
        val prompt = entry.prompt
        val typed = text.trim()

        if (typed.equals(prompt.cancelKeyword, ignoreCase = true)) {
            cancel(uuid)
            return
        }
        if (prompt.format == PromptFormat.MINI_MESSAGE) {
            PromptResult.rejectInvalidMiniMessage(typed)?.let { rejected ->
                reject(entry, rejected.reason)
                return
            }
            entry.staged = typed
            entry.player.sendMessage(PromptCard.preview(prompt, typed, controls(entry)))
            entry.player.sendActionBar(PromptCard.hud(prompt, staged = true))
            entry.player.playSound(PREVIEW_SOUND, PREVIEW_TONE)
            return
        }

        apply(entry, typed)
    }

    private fun apply(
        entry: Pending,
        text: String,
        submit: (String) -> PromptResult = entry.prompt.onSubmit,
        then: () -> Unit = {},
    ) {
        val uuid = entry.player.uniqueId
        if (pending[uuid] !== entry) return

        val result = try {
            submit(text)
        } catch (exception: Exception) {
            pending.remove(uuid, entry)
            logger.log(Level.WARNING, "Chat prompt handler failed", exception)
            entry.player.sendMessage(PromptCard.failed())
            entry.player.sendActionBar(Component.empty())
            runCatching(entry.prompt.onCancel)
            return
        }

        when (result) {
            is PromptResult.Accepted -> {
                pending.remove(uuid, entry)
                entry.player.sendActionBar(Component.empty())
                entry.player.playSound(SUCCESS_SOUND, SUCCESS_TONE)
                then()
            }

            is PromptResult.Rejected -> reject(entry, result.reason)
        }
    }

    private fun reject(entry: Pending, reason: Component) {
        entry.staged = null
        entry.player.sendMessage(PromptCard.rejected(entry.prompt, reason, controls(entry)))
        entry.player.playSound(ERROR_SOUND, ERROR_TONE)
    }

    private fun controls(entry: Pending): PromptCard.Controls {
        val uuid = entry.player.uniqueId
        fun active(): Boolean = pending[uuid] === entry

        return PromptCard.Controls(
            cancel = { if (active()) cancel(uuid) },
            clear = entry.prompt.onClear?.let { onClear ->
                {
                    if (active()) {
                        pending.remove(uuid, entry)
                        entry.player.sendActionBar(Component.empty())
                        entry.player.playSound(SUCCESS_SOUND, SUCCESS_TONE)
                        onClear()
                    }
                }
            },
            apply = { text -> if (active() && entry.staged == text) apply(entry, text) },
            extra = entry.prompt.extra?.let { extra ->
                { text: String -> if (active() && entry.staged == text) apply(entry, text, extra.submit, extra.next) }
            },
        )
    }

    private companion object {
        private val logger = NpcLog.logger

        private const val OPEN_SOUND = "block.note_block.pling"
        private const val PREVIEW_SOUND = "block.note_block.hat"
        private const val SUCCESS_SOUND = "entity.experience_orb.pickup"
        private const val ERROR_SOUND = "block.note_block.bass"

        private val OPEN_TONE = SoundOptions(volume = 0.6, pitch = 1.6)
        private val PREVIEW_TONE = SoundOptions(volume = 0.6, pitch = 1.2)
        private val SUCCESS_TONE = SoundOptions(volume = 0.5, pitch = 1.4)
        private val ERROR_TONE = SoundOptions(volume = 0.7, pitch = 0.8)
    }
}
