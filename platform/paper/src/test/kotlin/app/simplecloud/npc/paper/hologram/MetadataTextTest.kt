package app.simplecloud.npc.paper.hologram

import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataTextTest {

    private data class Entry(val index: Int, val text: Boolean, val value: String)

    private fun rewrite(data: List<Entry>) =
        MetadataText.withText(data, 23, Entry::text, Entry::index) { Entry(it, true, "Hi Steve") }

    @Test
    fun `a spawn packet without a text entry gets one`() {
        val result = rewrite(listOf(Entry(15, false, "billboard")))

        assertEquals(listOf(Entry(15, false, "billboard"), Entry(23, true, "Hi Steve")), result.data)
        assertEquals(23, result.textIndex)
    }

    @Test
    fun `an existing text entry is replaced and keeps its index`() {
        val result = rewrite(listOf(Entry(22, true, "Hi "), Entry(15, false, "billboard")))

        assertEquals(listOf(Entry(22, true, "Hi Steve"), Entry(15, false, "billboard")), result.data)
        assertEquals(22, result.textIndex)
    }
}
