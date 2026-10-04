package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.platform.NpcPlayer

class ConfirmSpec<S : Any>(
    val title: String,
    val screen: S,
    val verb: String,
    val noun: String,
    val head: NpcItem,
    val fire: () -> Unit,
)

object ConfirmGate {
    private const val SIZE = 27

    private const val HEAD_SLOT = 4
    private const val KEEP_SLOT = 11
    private const val FIRE_SLOT = 15

    private val RED_SLOTS = (0..8) + (18..26)
    private val GRAY_SLOTS = listOf(9, 10, 12, 13, 14, 16, 17)

    private const val ARM_WINDOW_MILLIS = 5_000L
    private const val TICKS_PER_SECOND = 20L

    fun <S : Any> build(host: EditorHost<S>, player: NpcPlayer, spec: ConfirmSpec<S>): EditorMenu {
        val session = host.session(player)
        val key = spec.screen.toString()
        val pane = Pane(SIZE)

        pane.on(HEAD_SLOT, spec.head) { disarm(host, player, spec) }

        pane.on(
            KEEP_SLOT,
            Ui.item(
                "GREEN_CONCRETE",
                "<on><b>Keep It",
                listOf("<bd>Returns to the editor.", "", "<key>Left <hnt>Go back"),
            ),
        ) { click ->
            if (click == MenuClick.LEFT) host.back(player) else disarm(host, player, spec)
        }

        val fireItem = if (session.isArmed(key)) {
            val remaining = ((session.armedDeadlineMillis - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
            Ui.item(
                "TNT",
                "<err><b>CLICK AGAIN TO ${spec.verb.uppercase()}",
                listOf(
                    "<warn>Armed · ${remaining}s remaining",
                    "",
                    "<key>Shift+Right <err>${spec.verb} now",
                    "<hnt>Any other click, or waiting, cancels.",
                ),
                glowing = true,
            )
        } else {
            Ui.item(
                "RED_CONCRETE",
                "<err><b>${spec.verb} Forever",
                listOf(
                    "<warn>Step 1 of 2",
                    "",
                    "<key>Shift+Right <hnt>Arm ${spec.noun}",
                    "<hnt>Then click again within ${ARM_WINDOW_MILLIS / 1000} seconds.",
                ),
            )
        }
        pane.on(FIRE_SLOT, fireItem) { click ->
            when {
                click != MenuClick.SHIFT_RIGHT -> disarm(host, player, spec)
                session.isArmed(key) -> {
                    session.disarm()
                    spec.fire()
                }

                else -> arm(host, player, spec, key)
            }
        }

        val cancel: (MenuClick) -> Unit = { disarm(host, player, spec) }
        pane.fill(RED_SLOTS, Pane.RED_PANE, cancel)
        pane.fill(GRAY_SLOTS, onClick = cancel)
        pane.otherwise { _, _ -> disarm(host, player, spec) }

        return pane.menu(spec.title)
    }

    private fun <S : Any> arm(host: EditorHost<S>, player: NpcPlayer, spec: ConfirmSpec<S>, key: String) {
        val serial = host.session(player).arm(key, ARM_WINDOW_MILLIS)
        host.render(player, spec.screen)
        scheduleCountdown(host, player, spec.screen, key, serial)
    }

    private fun <S : Any> scheduleCountdown(
        host: EditorHost<S>,
        player: NpcPlayer,
        screen: S,
        key: String,
        serial: Int,
    ) {
        host.runLater(TICKS_PER_SECOND) {
            val session = host.sessions.peek(player.uniqueId) ?: return@runLater
            if (session.armedSerial != serial || session.armedKey != key) return@runLater
            if (!host.isShowing(player) { it == screen }) return@runLater

            val expired = System.currentTimeMillis() >= session.armedDeadlineMillis
            if (expired) session.disarm()
            host.render(player, screen)
            if (!expired) scheduleCountdown(host, player, screen, key, serial)
        }
    }

    private fun <S : Any> disarm(host: EditorHost<S>, player: NpcPlayer, spec: ConfirmSpec<S>) {
        val session = host.session(player)
        if (session.armedKey == null) return
        session.disarm()
        host.render(player, spec.screen)
    }
}
