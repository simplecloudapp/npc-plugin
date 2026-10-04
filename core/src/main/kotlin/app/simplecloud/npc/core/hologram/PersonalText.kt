package app.simplecloud.npc.core.hologram

import app.simplecloud.npc.core.text.PlayerPlaceholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.security.SecureRandom
import java.util.UUID

class PersonalText private constructor(
    val template: String,
    private val values: Map<String, Component>,
) {
    val external: Boolean = PlayerPlaceholders.usesExternal(template)

    private val markers: TagResolver =
        TagResolver.resolver(values.map { (marker, value) -> Placeholder.component(marker, value) })

    fun resolve(uniqueId: UUID, name: String): String = PlayerPlaceholders.resolve(template, uniqueId, name)

    fun render(resolved: String): Component = MINI_MESSAGE.deserialize(resolved, markers)

    fun fallback(): Component = render(PlayerPlaceholders.strip(template))

    override fun equals(other: Any?): Boolean =
        other is PersonalText && other.template == template && other.values == values

    override fun hashCode(): Int = 31 * template.hashCode() + values.hashCode()

    companion object {
        private val MINI_MESSAGE = MiniMessage.miniMessage()
        private val TARGET_TAG = Regex("(?i)<target_[a-z0-9_-]+(?::[^<>]*)?>")
        private val SALT = SecureRandom().let { random ->
            (1..8).map { 'a' + random.nextInt(26) }.joinToString("")
        }

        fun of(text: String, resolver: TagResolver): PersonalText {
            val markerOf = HashMap<String, String>()
            val values = LinkedHashMap<String, Component>()
            val template = TARGET_TAG.replace(text) { match ->
                val marker = markerOf.getOrPut(match.value) {
                    "scnpc_${SALT}_${markerOf.size}".also { values[it] = MINI_MESSAGE.deserialize(match.value, resolver) }
                }
                "<$marker>"
            }

            return PersonalText(template, values)
        }
    }
}
