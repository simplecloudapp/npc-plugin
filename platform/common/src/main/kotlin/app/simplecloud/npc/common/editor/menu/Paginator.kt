package app.simplecloud.npc.common.editor.menu

class Paginator<T>(
    private val pages: MutableMap<String, Int>,
    private val key: String,
    private val all: List<T>,
    val perPage: Int,
) {
    val pageCount: Int = maxOf(1, (all.size + perPage - 1) / perPage)
    val page: Int = (pages[key] ?: 0).coerceIn(0, pageCount - 1)

    init {
        pages[key] = page
    }

    val visible: List<T> = all.drop(page * perPage).take(perPage)

    fun turn(delta: Int) {
        pages[key] = (page + delta).coerceIn(0, pageCount - 1)
    }
}
