package app.simplecloud.npc.common.command

object ChatPixels {
    private const val SPACE = 4

    fun width(text: String): Int = text.sumOf { char ->
        when (char) {
            'i', '!', '.', ',', ':', ';', '|', '\'' -> 2
            'l', '`' -> 3
            't', 'I', ' ', '[', ']' -> 4
            'f', 'k', '<', '>', '(', ')', '"', '*' -> 5
            '@', '~' -> 7
            else -> 6
        }
    }

    fun pad(text: String, target: Int): String {
        val missing = target - width(text)
        return if (missing <= 0) text else text + " ".repeat((missing + SPACE - 1) / SPACE)
    }
}
