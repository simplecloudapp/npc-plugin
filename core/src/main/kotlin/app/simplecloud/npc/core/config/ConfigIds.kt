package app.simplecloud.npc.core.config

object ConfigIds {
    private val PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")

    const val DESCRIPTION = "Letters, digits, - and _ only, up to 64 characters"

    fun isValid(id: String): Boolean = PATTERN.matches(id)
}
