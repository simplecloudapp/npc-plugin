package app.simplecloud.npc.common.editor.ui

object Palette {

    private val TOKENS = listOf(
        "<ttl>" to "<#f1f5f9>",
        "<bd>" to "<#9fb0c4>",
        "<hnt>" to "<#5b6b83>",
        "<val>" to "<#ffd479>",
        "<key>" to "<#b79cff>",
        "<on>" to "<#8ee06a>",
        "<off>" to "<#7d8794>",
        "<warn>" to "<#ffb038>",
        "<err>" to "<#ff6b5e>",
        "<info>" to "<#52c8f5>",
    )

    fun expand(text: String): String {
        if (text.isEmpty() || '<' !in text) return text
        return TOKENS.fold(text) { expanded, (token, hex) -> expanded.replace(token, hex) }
    }
}
