package app.simplecloud.npc.common.editor

import app.simplecloud.npc.common.text.Msg
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.time.Duration
import java.util.Locale

object PromptCard {

    class Controls(
        val cancel: () -> Unit,
        val clear: (() -> Unit)?,
        val apply: (String) -> Unit,
        val extra: ((String) -> Unit)?,
    )

    private const val INDENT = "   "
    private const val RULE_WIDTH = 64
    private const val FORMAT_DOCS = "https://docs.advntr.dev/minimessage/format.html"

    private val BRAND = color("#0ea5e9")
    private val ACCENT = color("#f8fafc")
    private val MUTED = color("#94a3b8")
    private val SUBTLE = color("#475569")
    private val RULE = color("#334155")
    private val SUCCESS = color("#a3e635")
    private val WARNING = color("#f59e0b")
    private val ERROR = color("#dc2626")

    private val CALLBACK_OPTIONS = ClickCallback.Options.builder()
        .uses(1)
        .lifetime(Duration.ofMinutes(15))
        .build()

    private val FORMAT_EXAMPLES = listOf(
        "<red>Red text",
        "<#0ea5e9>Any hex color",
        "<gradient:#0ea5e9:#a3e635>A gradient</gradient>",
        "<b>Bold</b> <i>italic</i> <u>underlined</u>",
        "<rainbow>Rainbow</rainbow>",
    )

    fun open(prompt: Prompt, controls: Controls): Component = card(
        listOfNotNull(
            header(prompt, null),
            instruction(prompt),
            current(prompt),
            placeholders(prompt),
            Component.empty(),
            buttons(
                listOfNotNull(
                    prompt.current?.let { suggestButton("✎ Edit current", it) },
                    controls.clear?.let { clear ->
                        button("⌫ Clear", WARNING, text("Removes the value.", MUTED), callback(clear))
                    },
                    formatButton().takeIf { prompt.format == PromptFormat.MINI_MESSAGE },
                    cancelButton(prompt, controls),
                ),
            ),
        ),
    )

    fun preview(prompt: Prompt, typed: String, controls: Controls): Component = card(
        listOf(
            header(prompt, "Preview"),
            Msg.miniMessage.deserialize(typed),
            Component.empty(),
            buttons(
                listOfNotNull(
                    button(
                        "✔ Apply",
                        SUCCESS,
                        text("Saves it exactly like this.", MUTED),
                        callback { controls.apply(typed) },
                    ),
                    prompt.extra?.let { extra ->
                        controls.extra?.let { run ->
                            button("⟳ ${extra.label}", BRAND, text(extra.hint, MUTED), callback { run(typed) })
                        }
                    },
                    suggestButton("✎ Edit", typed),
                    cancelButton(prompt, controls),
                ),
            ),
        ),
    )

    fun rejected(prompt: Prompt, reason: Component, controls: Controls): Component = Component.text()
        .append(text(" ✖ ", ERROR))
        .append(reason.colorIfAbsent(ERROR))
        .append(text("  Try again, or ", MUTED))
        .append(cancelButton(prompt, controls))
        .build()

    fun failed(): Component = Component.text()
        .append(text(" ✖ ", ERROR))
        .append(text("Something went wrong, the input was cancelled.", ERROR))
        .build()

    fun hud(prompt: Prompt, staged: Boolean): Component = if (staged) {
        Component.text()
            .append(text("✔ ", SUCCESS))
            .append(text("Preview ready", ACCENT))
            .append(text("  ·  click ", SUBTLE))
            .append(text("Apply", SUCCESS))
            .append(text(" or type again", SUBTLE))
            .build()
    } else {
        Component.text()
            .append(text("✎ ", BRAND))
            .append(text(prompt.title, ACCENT))
            .append(text("  ·  type in chat  ·  ", SUBTLE))
            .append(text(prompt.cancelKeyword, MUTED))
            .append(text(" to stop", SUBTLE))
            .build()
    }

    private fun card(lines: List<Component>): Component {
        val body = lines.mapIndexed { index, line -> Component.text(if (index == 0) " " else INDENT).append(line) }

        return Component.join(JoinConfiguration.newlines(), listOf(Component.empty(), rule()) + body + rule())
    }

    private fun rule(): Component = Component.text(" ".repeat(RULE_WIDTH), RULE, TextDecoration.STRIKETHROUGH)

    private fun header(prompt: Prompt, suffix: String?): Component {
        val label = prompt.title.uppercase(Locale.ROOT)
        val builder = Component.text()
            .append(text("✎ ", BRAND))
            .append(Msg.miniMessage.deserialize("<b><gradient:#0ea5e9:#a3e635>${Msg.miniMessage.escapeTags(label)}"))
        suffix?.let { builder.append(text("  ·  ", SUBTLE)).append(text(it, ACCENT)) }
        prompt.context?.let { builder.append(text("  ·  ", SUBTLE)).append(text(it, SUBTLE)) }

        return builder.build()
    }

    private fun instruction(prompt: Prompt): Component {
        val highlighted = Msg.highlight(
            Msg.miniMessage.escapeTags(prompt.instruction),
            prompt.instructionArgs.toTypedArray(),
            Msg.MUTED,
        )

        return Msg.miniMessage.deserialize(Msg.MUTED + highlighted)
    }

    private fun current(prompt: Prompt): Component? {
        val value = prompt.current?.takeIf { it.isNotBlank() } ?: return null
        val shown = if (prompt.format == PromptFormat.MINI_MESSAGE) {
            runCatching { Msg.miniMessage.deserialize(value) }.getOrElse { text(value, ACCENT) }
        } else {
            text(value, ACCENT)
        }
        val hover = Component.join(
            JoinConfiguration.newlines(),
            text(value, MUTED),
            Component.empty(),
            text("Click to put it into your chat bar", SUBTLE),
        )

        return Component.text()
            .append(text("Current  ", SUBTLE))
            .append(shown.hoverEvent(HoverEvent.showText(hover)).clickEvent(ClickEvent.suggestCommand(value)))
            .build()
    }

    private fun placeholders(prompt: Prompt): Component? {
        if (prompt.placeholders.isEmpty()) return null
        val chips = prompt.placeholders.map { placeholder ->
            val hover = Component.join(
                JoinConfiguration.newlines(),
                text(placeholder.token, ACCENT),
                text(placeholder.description, MUTED),
                Component.empty(),
                text("Click to copy, then paste it into your text", SUBTLE),
            )
            Component.text()
                .append(text(placeholder.token, BRAND))
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(ClickEvent.copyToClipboard(placeholder.token))
                .build()
        }

        return Component.text()
            .append(text("Insert   ", SUBTLE))
            .append(Component.join(JoinConfiguration.separator(text("  ", SUBTLE)), chips))
            .build()
    }

    private fun buttons(buttons: List<Component>): Component =
        Component.join(JoinConfiguration.separator(Component.text("  ")), buttons)

    private fun cancelButton(prompt: Prompt, controls: Controls): Component = button(
        "✖ Cancel",
        ERROR,
        text("Back to the menu. Typing '${prompt.cancelKeyword}' does the same.", MUTED),
        callback(controls.cancel),
    )

    private fun suggestButton(label: String, value: String): Component = button(
        label,
        BRAND,
        text("Puts the text into your chat bar to change it.", MUTED),
        ClickEvent.suggestCommand(value),
    )

    private fun formatButton(): Component {
        val examples = FORMAT_EXAMPLES.map { example ->
            Component.text()
                .append(text(example, SUBTLE))
                .append(text("  →  ", SUBTLE))
                .append(Msg.miniMessage.deserialize(example))
                .build()
        }
        val hover = Component.join(
            JoinConfiguration.newlines(),
            listOf(text("MiniMessage formatting", ACCENT), Component.empty()) + examples +
                listOf(Component.empty(), text("Click to open the full reference", SUBTLE)),
        )

        return button("? Format", MUTED, hover, ClickEvent.openUrl(FORMAT_DOCS))
    }

    private fun button(label: String, color: TextColor, hover: Component, click: ClickEvent): Component =
        Component.text()
            .append(text("[", SUBTLE))
            .append(text(label, color))
            .append(text("]", SUBTLE))
            .hoverEvent(HoverEvent.showText(hover))
            .clickEvent(click)
            .build()

    private fun callback(action: () -> Unit): ClickEvent = ClickEvent.callback({ action() }, CALLBACK_OPTIONS)

    private fun text(content: String, color: TextColor): Component = Component.text(content, color)

    private fun color(hex: String): TextColor = TextColor.fromHexString(hex)!!
}
