package app.simplecloud.npc.common.editor.npc.behavior

import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.core.toggle
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.SoundField
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

object BehaviorMenuBuilder {
    private const val SIZE = 27

    private const val LOOK_AT_TOGGLE_SLOT = 2
    private const val LOOK_AT_RANGE_SLOT = 4
    private const val RENDER_RANGE_SLOT = 6
    private const val PUSH_TOGGLE_SLOT = 9
    private const val PUSH_STRENGTH_SLOT = 11
    private const val PUSH_RADIUS_SLOT = 13
    private const val UPWARD_BOOST_SLOT = 15
    private const val PUSH_SOUND_SLOT = 17

    private val ENTITY_DEFAULTS = NpcConfig.NpcEntityConfiguration()
    private val PUSH_DEFAULTS = NpcConfig.PushbackConfiguration()

    private val LOOK_AT_RANGE = Stepper(
        label = "Look at player Range",
        range = NpcConfig.NpcEntityConfiguration.LOOK_AT_PLAYER_DISTANCE_RANGE,
        step = 0.5,
        default = ENTITY_DEFAULTS.lookAtPlayerDistance,
        decimals = 1,
        unit = "blocks",
    )
    private val RENDER_RANGE = Stepper(
        label = "Render Range",
        material = "SPYGLASS",
        range = NpcConfig.NpcEntityConfiguration.VIEW_DISTANCE_RANGE,
        step = 4.0,
        default = ENTITY_DEFAULTS.viewDistance,
        decimals = 0,
        unit = "blocks",
    )
    private val PUSH_STRENGTH = Stepper(
        label = "Push Strength",
        range = NpcConfig.PushbackConfiguration.STRENGTH_RANGE,
        step = 0.05,
        default = PUSH_DEFAULTS.strength,
        decimals = 2,
    )
    private val PUSH_RADIUS = Stepper(
        label = "Push Radius",
        range = NpcConfig.PushbackConfiguration.RADIUS_RANGE,
        step = 0.5,
        default = PUSH_DEFAULTS.radius,
        decimals = 1,
        unit = "blocks",
    )
    private val UPWARD_BOOST = Stepper(
        label = "Upward Boost",
        range = NpcConfig.PushbackConfiguration.VERTICAL_RANGE,
        step = 0.05,
        default = PUSH_DEFAULTS.vertical,
        decimals = 2,
    )

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val entity = config.entity
        val pushback = config.pushback
        val screen = NpcEditorScreen.Behavior(config.id)
        val prompts = context.textPrompts
        val pane = Pane(SIZE)

        fun editEntity(mutate: (NpcConfig.NpcEntityConfiguration) -> NpcConfig.NpcEntityConfiguration) =
            context.editEntity(player, config.id, screen, mutate)

        fun editPushback(mutate: (NpcConfig.PushbackConfiguration) -> Unit) =
            context.commit(player, config.id, screen) { fresh -> fresh.also { mutate(it.pushback) } }

        pane.toggle(LOOK_AT_TOGGLE_SLOT, "Look at player", entity.lookAtPlayer) {
            editEntity { it.copy(lookAtPlayer = !it.lookAtPlayer) }
        }

        pane[LOOK_AT_RANGE_SLOT] = LOOK_AT_RANGE.element(prompts, player, entity.lookAtPlayerDistance) { value ->
            editEntity { it.copy(lookAtPlayerDistance = value) }
        }
        pane[RENDER_RANGE_SLOT] = RENDER_RANGE.element(prompts, player, entity.viewDistance) { value ->
            editEntity { it.copy(viewDistance = value) }
        }

        pane.on(
            PUSH_TOGGLE_SLOT,
            Ui.toggle("Push", pushback.enabled, listOf("<key>Shift+Right <hnt>Push me")),
        ) { click ->
            when (click) {
                MenuClick.LEFT -> editPushback { it.enabled = !it.enabled }

                MenuClick.SHIFT_RIGHT -> {
                    player.push(entity.location, pushback.strength, pushback.vertical)
                    pushback.sound?.let { player.playSound(it, pushback.soundOptions) }
                }

                else -> Unit
            }
        }

        pane[PUSH_STRENGTH_SLOT] = PUSH_STRENGTH.element(prompts, player, pushback.strength) { value ->
            editPushback { it.strength = value }
        }
        pane[PUSH_RADIUS_SLOT] = PUSH_RADIUS.element(prompts, player, pushback.radius) { value ->
            editPushback { it.radius = value }
        }
        pane[UPWARD_BOOST_SLOT] = UPWARD_BOOST.element(prompts, player, pushback.vertical) { value ->
            editPushback { it.vertical = value }
        }

        val soundLine = pushback.sound?.let { "<bd>Current <val>$it" } ?: "<bd>Current <off>not set"
        pane.on(
            PUSH_SOUND_SLOT,
            Ui.item(
                "NOTE_BLOCK",
                "<ttl>Push Sound",
                listOfNotNull(
                    soundLine,
                    NpcFormat.soundSettings(pushback.soundOptions).takeIf { pushback.sound != null },
                    "",
                    "<key>Left <info>Choose a sound",
                    "<key>Q <hnt>Clear",
                ),
            ),
        ) { click ->
            when (click) {
                MenuClick.DROP -> editPushback { it.sound = null }
                MenuClick.LEFT -> context.navigate(
                    player,
                    NpcEditorScreen.Picker(config.id, PickerPurpose.PickSound(SoundField.Push)),
                )

                else -> Unit
            }
        }

        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Behavior", NpcFormat.displayName(config)))
    }
}
