package app.simplecloud.npc.paper.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

object TitleSender {

    fun show(
        player: Player,
        title: Component,
        subtitle: Component,
        fadeInTicks: Int,
        stayTicks: Int,
        fadeOutTicks: Int,
    ) {
        val times = Title.Times.times(ticks(fadeInTicks), ticks(stayTicks), ticks(fadeOutTicks))
        player.showTitle(Title.title(title, subtitle, times))
    }

    private fun ticks(ticks: Int): Duration = Duration.ofMillis(ticks * 50L)
}
