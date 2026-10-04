package app.simplecloud.npc.core.hologram

import app.simplecloud.npc.core.text.PlayerPlaceholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonalTextTest {

    private val viewer = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val target = TagResolver.resolver(
        Placeholder.component("target_name", Component.text("Lobby")),
        Placeholder.component("target_motd", Component.text("50% off for <playername>")),
    )

    @AfterTest
    fun reset() {
        PlayerPlaceholders.external = null
    }

    @Test
    fun `target values reach the viewer untouched by player and PlaceholderAPI substitution`() {
        PlayerPlaceholders.external = { _, text -> text.replace(Regex("%[^%\\s]+%"), "7") }
        val text = PersonalText.of("<target_motd> | <playername> has %level%", target)

        assertEquals("50% off for <playername> | Steve has 7", plain(text.render(text.resolve(viewer, "Steve"))))
    }

    @Test
    fun `the fallback leaves out everything personal`() {
        PlayerPlaceholders.external = { _, text -> text }
        val text = PersonalText.of("Hi <playername>, %level% on <target_name>", target)

        assertEquals("Hi ,  on Lobby", plain(text.fallback()))
    }

    private fun plain(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)
}
