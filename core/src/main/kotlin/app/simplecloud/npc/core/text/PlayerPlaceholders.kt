package app.simplecloud.npc.core.text

import app.simplecloud.npc.core.platform.NpcPlayer
import java.util.UUID

object PlayerPlaceholders {

    const val PLAYER_NAME = "<playername>"
    const val PLAYER_UUID = "<playeruuid>"

    private val EXTERNAL = Regex("%[^%\\s]+%")

    @Volatile
    var external: ((UUID, String) -> String)? = null

    fun resolve(text: String, player: NpcPlayer): String = resolve(text, player.uniqueId, player.name)

    fun resolve(text: String, uniqueId: UUID, name: String): String {
        val own = resolveOwn(text, uniqueId, name)
        if (!usesExternal(own)) return own
        val hook = external ?: return own

        return runCatching { hook(uniqueId, own) }.getOrDefault(own)
    }

    fun resolveOwn(text: String, uniqueId: UUID, name: String): String {
        if ('<' !in text) return text

        return text.replace(PLAYER_NAME, name).replace(PLAYER_UUID, uniqueId.toString())
    }

    fun usesExternal(text: String): Boolean = external != null && '%' in text && EXTERNAL.containsMatchIn(text)

    fun isPersonal(text: String): Boolean = PLAYER_NAME in text || PLAYER_UUID in text || usesExternal(text)

    fun strip(text: String): String {
        val own = text.replace(PLAYER_NAME, "").replace(PLAYER_UUID, "")

        return if (external != null) EXTERNAL.replace(own, "") else own
    }
}
