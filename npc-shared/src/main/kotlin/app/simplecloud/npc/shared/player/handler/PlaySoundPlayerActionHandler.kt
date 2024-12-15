package app.simplecloud.npc.shared.player.handler

import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.player.PlayerActionHandler
import app.simplecloud.npc.shared.player.PlayerActionOptions
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class PlaySoundPlayerActionHandler : PlayerActionHandler {

    override fun handle(player: Player, optionProvider: OptionProvider) {
        val soundName = optionProvider.getOption(PlayerActionOptions.PLAY_SOUND)
        val soundVolume = optionProvider.getOption(PlayerActionOptions.SOUND_VOLUME)
        val soundPitch = optionProvider.getOption(PlayerActionOptions.SOUND_PITCH)
        val soundSource = optionProvider.getOption(PlayerActionOptions.SOUND_SOURCE)
        val sound = Sound.sound(Key.key(soundName), Sound.Source.valueOf(soundSource), soundVolume, soundPitch)
        player.playSound(sound)
    }

    override fun getSuggestions() = mapOf(
        "play.sound" to org.bukkit.Sound.values().map { it.key.key },
        "sound.source" to Sound.Source.values().map { it.name.lowercase() }
    )

    override fun getOptions() = listOf(
        PlayerActionOptions.PLAY_SOUND,
        PlayerActionOptions.SOUND_PITCH,
        PlayerActionOptions.SOUND_VOLUME,
        PlayerActionOptions.SOUND_SOURCE
    )

}