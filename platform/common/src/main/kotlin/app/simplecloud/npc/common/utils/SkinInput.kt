package app.simplecloud.npc.common.utils

import java.util.UUID

sealed interface SkinInput {
    data class Uuid(val uuid: UUID) : SkinInput
    data class Username(val name: String) : SkinInput
    data class Texture(val value: String, val signature: String?) : SkinInput

    companion object {
        private const val MIN_TEXTURE_LENGTH = 40
        private val USERNAME = Regex("[A-Za-z0-9_]{3,16}")
        private val WHITESPACE = Regex("\\s+")

        fun parse(input: String): SkinInput? {
            val parts = input.trim().split(WHITESPACE).filter { it.isNotEmpty() }
            val first = parts.singleOrNull() ?: return texture(parts)

            return InputChecks.parseUuid(first)?.let(::Uuid)
                ?: first.takeIf(USERNAME::matches)?.let(::Username)
                ?: texture(parts)
        }

        private fun texture(parts: List<String>): Texture? {
            if (parts.size !in 1..2 || !parts.all(InputChecks::isValidBase64)) return null
            if (parts.first().length < MIN_TEXTURE_LENGTH) return null

            return Texture(parts.first(), parts.getOrNull(1))
        }
    }
}
