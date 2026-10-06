package app.simplecloud.npc.common.editor.npc.picker

import app.simplecloud.npc.common.editor.core.PickerOption
import app.simplecloud.npc.common.editor.core.PickerOptions
import app.simplecloud.npc.common.editor.core.PickerResult
import app.simplecloud.npc.common.editor.core.SoundTuning
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.SoundField
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

class PickSoundSource(override val purpose: PickerPurpose.PickSound) : PickerSource {
    private val field = purpose.field

    override val title = "Pick Sound"
    override val filteredNoun = "sounds"

    override val typeHint: List<String> = PickerOptions.SOUND_TYPE_HINT

    private fun currentSound(config: NpcConfig): String? = when (field) {
        SoundField.Push -> config.pushback.sound
        is SoundField.Action -> config.findAction(field.interaction, field.joinState)?.playSound
    }

    private fun soundOptions(config: NpcConfig): NpcConfig.SoundOptions = when (field) {
        SoundField.Push -> config.pushback.soundOptions
        is SoundField.Action -> config.findAction(field.interaction, field.joinState)?.playSoundOptions
            ?: SoundTuning.DEFAULTS
    }

    private fun setSound(config: NpcConfig, sound: String) = when (field) {
        SoundField.Push -> config.pushback.sound = sound
        is SoundField.Action -> config.actionOrCreate(field.interaction, field.joinState).playSound = sound
    }

    private fun setSoundOptions(config: NpcConfig, options: NpcConfig.SoundOptions) = when (field) {
        SoundField.Push -> config.pushback.soundOptions = options
        is SoundField.Action -> config.actionOrCreate(field.interaction, field.joinState).playSoundOptions = options
    }

    override fun options(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): List<PickerOption> {
        val current = currentSound(config)
        val filter = (context.session(player).pickerResult as? PickerResult.Filtered)?.typed

        return PickerOptions.sounds(context.catalogs.sounds(), filter, current)
    }

    override fun resolveTyped(context: NpcEditorContext, config: NpcConfig, typed: String): PickerResult? =
        PickerOptions.search(context.catalogs.sounds(), typed, "sound key")

    override fun apply(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, value: String) {
        val canonical = PickerOptions.canonical(context.catalogs.sounds(), value)
        val saved = context.change(player, config.id) { fresh -> fresh.also { setSound(it, canonical) } }
        if (!saved) return

        if (context.session(player).pickerResult is PickerResult.Filtered) {
            context.render(player, NpcEditorScreen.Picker(config.id, purpose))
        } else {
            val result = PickerResult.Added(canonical, "Tune volume and pitch below, or go Back.")
            PickerMenuBuilder.showResult(context, player, config, purpose, result)
        }
    }

    override fun onRightClick(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, option: PickerOption) {
        player.playSound(option.value, soundOptions(config))
    }

    override fun extras(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, pane: Pane) {
        val screen = NpcEditorScreen.Picker(config.id, purpose)

        SoundTuning.place(
            pane,
            player,
            currentSound(config),
            soundOptions(config),
            requireSound = field is SoundField.Action,
        ) { updated ->
            context.commit(player, config.id, screen) { fresh ->
                if (field is SoundField.Action && currentSound(fresh) == null) return@commit null
                setSoundOptions(fresh, updated(soundOptions(fresh)))
                fresh
            }
        }
    }
}
