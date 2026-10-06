package app.simplecloud.npc.common.editor.npc.tabs

import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.NpcTab
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.editor.ui.after
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.NpcPose
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.Providers

object LooksTabBuilder {
    private const val SKIN_SLOT = 19
    private const val NAME_SLOT = 21
    private const val GLOW_SLOT = 23
    private const val EQUIPMENT_SLOT = 25
    private const val POSE_SLOT = 28
    private const val SCALE_SLOT = 30

    private val SCALE = Stepper(
        label = "Size",
        material = "SLIME_BALL",
        range = NpcConfig.NpcEntityConfiguration.SCALE_RANGE,
        step = 0.05,
        default = 1.0,
        decimals = 2,
        unit = "x",
    )

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val entity = config.entity
        val name = NpcFormat.displayName(config)
        val screen = NpcEditorScreen.Tab(config.id, NpcTab.LOOKS)
        val pane = TabFrame.pane(context, player, config, NpcTab.LOOKS)

        fun edit(mutate: (NpcConfig.NpcEntityConfiguration) -> NpcConfig.NpcEntityConfiguration) =
            context.editEntity(player, config.id, screen, mutate)

        if (entity.provider in context.providers.fixedSkinProviders) {
            pane.left(
                SKIN_SLOT,
                Ui.head(
                    "<ttl>Skin",
                    listOf("<val>${NpcFormat.skinLabel(entity.skin)}", "<key>Left <hnt>Choose"),
                    texture = entity.skin.texture,
                    signature = entity.skin.signature,
                ),
            ) { context.navigate(player, NpcEditorScreen.SkinPicker(config.id)) }
        } else {
            pane[SKIN_SLOT] = Ui.disabled("Skin", listOf("<hnt>${entity.provider} shows each viewer their own skin."))
        }

        pane.left(NAME_SLOT, Ui.item("NAME_TAG", "<ttl>Name", listOf("<val>$name", "<key>Left <hnt>Change"))) {
            context.textPrompts.open(
                player,
                title = "Display Name",
                instruction = "Type the name shown above the NPC in chat.",
                current = entity.customName,
                format = PromptFormat.MINI_MESSAGE,
                onClear = { edit { it.copy(customName = null) } }.takeIf { entity.customName != null },
            ) { text ->
                edit { it.copy(customName = text.ifBlank { null }) }
            }
        }

        val color = NpcFormat.glowColor(entity)
        val glowItem = if (entity.glowing) {
            Ui.item(
                NpcFormat.glowIcon(color),
                "<ttl>Glow",
                listOf(
                    "<on>On <hnt>· <val>${NpcFormat.glowLabel(color)}",
                    "<key>Left <hnt>Turn off · <key>Right <hnt>Color",
                ),
                glowing = true,
            )
        } else {
            Ui.item("GRAY_DYE", "<ttl>Glow", listOf("<off>Off", "<key>Left <hnt>Turn on · <key>Right <hnt>Color"))
        }
        pane.on(GLOW_SLOT, glowItem) { click ->
            when (click) {
                MenuClick.LEFT -> edit { it.copy(glowing = !it.glowing) }
                MenuClick.RIGHT -> context.navigate(player, NpcEditorScreen.Glow(config.id))
                else -> Unit
            }
        }

        if (entity.provider in Providers.OWN && !entity.providerLinked) {
            val worn = entity.equipment.filled().size
            pane.left(
                EQUIPMENT_SLOT,
                Ui.item(
                    "ARMOR_STAND",
                    "<ttl>Equipment",
                    listOf(
                        if (worn == 0) "<off>Nothing worn" else "<val>$worn <hnt>items worn",
                        "<key>Left <hnt>Dress it",
                    ),
                    glowing = worn > 0,
                ),
            ) { context.navigate(player, NpcEditorScreen.Equipment(config.id)) }

            pane.left(
                POSE_SLOT,
                Ui.item(
                    "LEATHER_BOOTS",
                    "<ttl>Pose",
                    listOf("<val>${poseLabel(entity.pose)}", "<key>Left <hnt>Next pose"),
                ),
            ) {
                context.commit(player, config.id, screen, Refresh.ALL) { fresh ->
                    fresh.copy(entity = fresh.entity.copy(pose = NpcPose.entries.after(fresh.entity.pose)))
                }
            }

            pane[SCALE_SLOT] = SCALE.element(entity.scale) { value ->
                context.commit(player, config.id, screen, Refresh.ALL) { fresh ->
                    fresh.copy(entity = fresh.entity.copy(scale = value))
                }
            }
        } else {
            val reason = listOf("<hnt>Only NPCs this plugin draws itself.")
            pane[EQUIPMENT_SLOT] = Ui.disabled("Equipment", reason)
            pane[POSE_SLOT] = Ui.disabled("Pose", reason)
            pane[SCALE_SLOT] = Ui.disabled("Size", reason)
        }

        return TabFrame.menu(pane, config)
    }

    private fun poseLabel(pose: NpcPose): String = when (pose) {
        NpcPose.STANDING -> "Standing"
        NpcPose.SNEAKING -> "Sneaking"
        NpcPose.SWIMMING -> "Swimming"
        NpcPose.SLEEPING -> "Lying down"
    }
}
