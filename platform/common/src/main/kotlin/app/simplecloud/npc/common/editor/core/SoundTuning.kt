package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig.SoundOptions
import app.simplecloud.npc.core.platform.NpcPlayer

object SoundTuning {
    private const val VOLUME_SLOT = 29
    private const val PITCH_SLOT = 33
    private const val PREVIEW_SLOT = 42

    val DEFAULTS = SoundOptions()

    private val VOLUME = Stepper(
        label = "Volume",
        range = SoundOptions.VOLUME_RANGE,
        step = 0.1,
        default = DEFAULTS.volume,
        decimals = 1,
        hints = listOf("<hnt>Above 1.0 it only carries further."),
    )
    private val PITCH = Stepper(
        label = "Pitch",
        range = SoundOptions.PITCH_RANGE,
        step = 0.05,
        default = DEFAULTS.pitch,
        decimals = 2,
        hints = listOf("<hnt>Lower is deeper and slower."),
    )

    fun place(
        pane: Pane,
        player: NpcPlayer,
        sound: String?,
        options: SoundOptions,
        requireSound: Boolean = true,
        tune: ((SoundOptions) -> SoundOptions) -> Unit,
    ) {
        if (requireSound && sound == null) {
            val hint = listOf("<hnt>Pick a sound first.")
            pane[VOLUME_SLOT] = Ui.disabled(VOLUME.label, hint)
            pane[PITCH_SLOT] = Ui.disabled(PITCH.label, hint)
        } else {
            pane[VOLUME_SLOT] = VOLUME.element(options.volume) { value ->
                tune { it.copy(volume = value) }
            }
            pane[PITCH_SLOT] = PITCH.element(options.pitch) { value ->
                tune { it.copy(pitch = value) }
            }
        }
        pane.left(
            PREVIEW_SLOT,
            Ui.item(
                "BELL",
                "<ttl>Preview",
                listOf(
                    sound?.let { "<bd>Sound <val>$it" } ?: "<bd>Sound <off>none picked yet",
                    NpcFormat.soundSettings(options),
                    "<key>Left <hnt>Play it to me",
                ),
            ),
        ) { sound?.let { player.playSound(it, options) } }
    }
}
