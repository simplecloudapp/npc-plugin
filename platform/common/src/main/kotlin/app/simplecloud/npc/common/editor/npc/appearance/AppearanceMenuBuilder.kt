package app.simplecloud.npc.common.editor.npc.appearance

import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.core.toggle
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.editor.ui.after
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.NpcPose
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.Providers

object AppearanceMenuBuilder {
    private const val SIZE = 36

    private const val PREVIEW_SLOT = 4
    private const val SKIN_SLOT = 10
    private const val NAME_SLOT = 12
    private const val GLOW_SLOT = 14
    private const val COLOR_SLOT = 16
    private const val EQUIPMENT_SLOT = 20
    private const val POSE_SLOT = 22
    private const val SCALE_SLOT = 24

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
        val skinLabel = NpcFormat.skinLabel(entity.skin)
        val screen = NpcEditorScreen.Appearance(config.id)
        val pane = Pane(SIZE)

        fun edit(mutate: (NpcConfig.NpcEntityConfiguration) -> NpcConfig.NpcEntityConfiguration) =
            context.editEntity(player, config.id, screen, mutate)

        pane[PREVIEW_SLOT] = Ui.npcHead(
            entity.skin,
            "<ttl><b>$name",
            listOf(
                "<bd>Skin <val>$skinLabel",
                "<bd>Name <val>$name",
                NpcFormat.glowLine(entity),
            ),
        )

        if (entity.provider in context.providers.fixedSkinProviders) {
            pane.left(
                SKIN_SLOT,
                Ui.head(
                    "<ttl>Skin",
                    listOf("<bd>Current <val>$skinLabel", "", "<key>Left <info>Choose a skin"),
                    texture = entity.skin.texture,
                    signature = entity.skin.signature,
                ),
            ) { context.navigate(player, NpcEditorScreen.SkinPicker(config.id)) }
        } else {
            pane[SKIN_SLOT] = Ui.disabled(
                "Skin",
                listOf("<hnt>Provider <val>${entity.provider} <hnt>shows each", "<hnt>viewer their own skin."),
            )
        }

        pane.on(
            NAME_SLOT,
            Ui.item(
                "NAME_TAG",
                "<ttl>Display Name",
                listOf(
                    "<bd>Current <val>$name",
                    "",
                    "<key>Left <hnt>Type a new name in chat",
                    "<key>Q <hnt>Clear back to default",
                ),
            ),
        ) { click ->
            when (click) {
                MenuClick.DROP -> edit { it.copy(customName = null) }

                MenuClick.LEFT -> context.textPrompts.open(
                    player,
                    title = "Display Name",
                    instruction = "Type the name shown above the NPC in chat.",
                    current = entity.customName,
                    format = PromptFormat.MINI_MESSAGE,
                    onClear = { edit { it.copy(customName = null) } },
                ) { text ->
                    edit { it.copy(customName = text.ifBlank { null }) }
                }

                else -> Unit
            }
        }

        pane.toggle(GLOW_SLOT, "Glow", entity.glowing) { edit { it.copy(glowing = !it.glowing) } }

        if (entity.glowing) {
            val color = NpcFormat.glowColor(entity)
            pane.left(
                COLOR_SLOT,
                Ui.item(
                    NpcFormat.glowIcon(color),
                    "<ttl>Glow Color",
                    listOf("<bd>Current <val>$color", "", "<key>Left <info>Choose color"),
                ),
            ) { context.navigate(player, NpcEditorScreen.Glow(config.id)) }
        } else {
            pane[COLOR_SLOT] = Ui.disabled("Glow Color", listOf("<hnt>Turn glow on first."))
        }

        if (entity.provider in Providers.OWN && !entity.providerLinked) {
            val worn = entity.equipment.filled().size
            val wornLine = if (worn == 0) "<off>Nothing worn" else "<bd><val>$worn <bd>slots worn"
            pane.left(
                EQUIPMENT_SLOT,
                Ui.item(
                    "ARMOR_STAND",
                    "<ttl>Equipment",
                    listOf(wornLine, "", "<key>Left <info>Dress the NPC"),
                    glowing = worn > 0,
                ),
            ) { context.navigate(player, NpcEditorScreen.Equipment(config.id)) }

            val poses = NpcPose.entries
            pane.left(
                POSE_SLOT,
                Ui.item(
                    "LEATHER_BOOTS",
                    "<ttl>Pose",
                    Ui.cycleLines(poses, entity.pose, ::poseLabel) + listOf("", "<key>Left <hnt>Next pose"),
                ),
            ) {
                context.commit(player, config.id, screen, Refresh.ALL) { fresh ->
                    fresh.copy(entity = fresh.entity.copy(pose = poses.after(fresh.entity.pose)))
                }
            }

            pane[SCALE_SLOT] = SCALE.element(context.textPrompts, player, entity.scale) { value ->
                context.commit(player, config.id, screen, Refresh.ALL) { fresh ->
                    fresh.copy(entity = fresh.entity.copy(scale = value))
                }
            }
        } else {
            val reason = listOf("<hnt>Only NPCs this plugin draws itself", "<hnt>can wear items, pose and resize.")
            pane[EQUIPMENT_SLOT] = Ui.disabled("Equipment", reason)
            pane[POSE_SLOT] = Ui.disabled("Pose", reason)
            pane[SCALE_SLOT] = Ui.disabled("Size", reason)
        }

        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Appearance", name))
    }

    private fun poseLabel(pose: NpcPose): String = when (pose) {
        NpcPose.STANDING -> "Standing"
        NpcPose.SNEAKING -> "Sneaking"
        NpcPose.SWIMMING -> "Swimming"
        NpcPose.SLEEPING -> "Lying down"
    }
}
