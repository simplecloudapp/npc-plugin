package app.simplecloud.npc.common.utils

import java.util.Base64
import java.util.UUID

object InputChecks {
    private val UNDASHED_UUID_PATTERN = Regex("^[0-9a-fA-F]{32}$")
    private val HOST_PORT_PATTERN = Regex("^[A-Za-z0-9.-]+(?::[0-9]{1,5})?$")
    private val SOUND_PATTERN = Regex("^[A-Za-z0-9_./-]+(?::[a-z0-9_./-]+)?$")

    fun isValidBase64(value: String): Boolean = runCatching { Base64.getDecoder().decode(value) }.isSuccess
    fun isValidSound(value: String): Boolean = value.isNotBlank() && SOUND_PATTERN.matches(value)
    fun isValidHostPort(value: String): Boolean = parseHostPort(value) != null

    fun parseHostPort(value: String): Pair<String, Int>? {
        if (!HOST_PORT_PATTERN.matches(value)) return null
        val port = value.substringAfter(':', DEFAULT_PORT.toString()).toIntOrNull()?.takeIf { it in 1..65535 }
            ?: return null

        return value.substringBefore(':') to port
    }

    fun parseUuid(input: String): UUID? {
        runCatching { UUID.fromString(input) }.getOrNull()?.let { return it }
        if (!UNDASHED_UUID_PATTERN.matches(input)) return null
        val dashed = "${input.substring(0, 8)}-${input.substring(8, 12)}-${input.substring(12, 16)}-" +
            "${input.substring(16, 20)}-${input.substring(20, 32)}"

        return UUID.fromString(dashed)
    }

    private const val DEFAULT_PORT = 25565
}
