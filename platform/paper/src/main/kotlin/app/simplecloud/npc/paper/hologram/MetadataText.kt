package app.simplecloud.npc.paper.hologram

object MetadataText {

    class Rewrite<T>(val data: List<T>, val textIndex: Int)

    fun <T> withText(
        data: List<T>,
        textIndex: Int,
        isText: (T) -> Boolean,
        indexOf: (T) -> Int,
        entry: (Int) -> T,
    ): Rewrite<T> {
        val position = data.indexOfFirst(isText)
        if (position < 0) return Rewrite(data + entry(textIndex), textIndex)

        val index = indexOf(data[position])
        val rewritten = data.toMutableList().apply { set(position, entry(index)) }

        return Rewrite(rewritten, index)
    }
}
