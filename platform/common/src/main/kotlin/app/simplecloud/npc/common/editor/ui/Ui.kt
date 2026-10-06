package app.simplecloud.npc.common.editor.ui

import app.simplecloud.npc.common.item.NpcItem
import java.util.UUID

object Ui {
    private const val ADD_HEAD_TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjBiNTVmNzQ2ODFjNjgyODNhMWMxY2U1MWYxYzgzYjUyZTI5NzFjOTFlZTM0ZWZjYjU5OGRmMzk5MGE3ZTcifX19"
    private const val BACK_HEAD_TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ3M2NmNjZkMzFiODNjZDhiODY0NGMxNTk1OGMxYjczYzhkOTczMjNiODAxMTcwYzFkODg2NGJiNmE4NDZkIn19fQ=="
    private const val PREVIOUS_HEAD_TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2RjOWU0ZGNmYTQyMjFhMWZhZGMxYjViMmIxMWQ4YmVlYjU3ODc5YWYxYzQyMzYyMTQyYmFlMWVkZDUifX19"
    private const val NEXT_HEAD_TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTU2YTM2MTg0NTllNDNiMjg3YjIyYjdlMjM1ZWM2OTk1OTQ1NDZjNmZjZDZkYzg0YmZjYTRjZjMwYWI5MzExIn19fQ=="

    private const val TITLE_NAME_LENGTH = 18
    private const val DISABLED_MATERIAL = "LIGHT_GRAY_DYE"

    fun item(
        material: String,
        name: String,
        lore: List<String> = emptyList(),
        glowing: Boolean = false,
        amount: Int = 1,
    ): NpcItem = NpcItem(
        material,
        Palette.expand(name),
        lore.map(Palette::expand),
        amount = amount,
        glowing = glowing,
    )

    fun head(
        name: String,
        lore: List<String> = emptyList(),
        texture: String? = null,
        signature: String? = null,
        owner: UUID? = null,
        glowing: Boolean = false,
    ): NpcItem = NpcItem.playerHead(
        name = Palette.expand(name),
        lore = lore.map(Palette::expand),
        owner = owner,
        texture = texture,
        signature = signature,
        glowing = glowing,
    )

    fun option(material: String, name: String, lore: List<String>, selected: Boolean): NpcItem = item(
        material,
        if (selected) "<on>$name" else "<ttl>$name",
        lore + if (selected) listOf("<on>Currently selected") else listOf("<key>Left <hnt>Select"),
        glowing = selected,
    )

    fun disabled(name: String, lore: List<String>): NpcItem = item(DISABLED_MATERIAL, "<off>$name", lore)

    fun unavailable(name: String, lore: List<String>, reason: String): NpcItem =
        disabled(name, lore + listOf("<err>$reason"))

    fun field(material: String, label: String, valueLine: String?, setVerb: String): NpcItem = item(
        material,
        "<ttl>$label",
        listOfNotNull(
            valueLine ?: "<off>Not set",
            "",
            setVerb,
            "<key>Q <hnt>Clear".takeIf { valueLine != null },
        ),
        glowing = valueLine != null,
    )

    fun toggle(
        label: String,
        enabled: Boolean,
        extraLore: List<String> = emptyList(),
        material: String? = null,
    ): NpcItem = item(
        material ?: if (enabled) "LIME_DYE" else "GRAY_DYE",
        "<ttl>$label",
        listOf(if (enabled) "<on>On" else "<off>Off") + extraLore +
            listOf(if (enabled) "<key>Left <hnt>Turn off" else "<key>Left <hnt>Turn on"),
        glowing = enabled,
    )

    fun addHead(label: String, lore: List<String> = emptyList()): NpcItem =
        head("<ttl>$label", lore, texture = ADD_HEAD_TEXTURE)

    fun back(): NpcItem = head("<ttl>Back", texture = BACK_HEAD_TEXTURE)

    fun close(): NpcItem = item("OAK_DOOR", "<ttl>Exit")

    fun previousPage(page: Int): NpcItem =
        if (page <= 0) {
            head("<off>Previous Page", listOf("<hnt>You are on the first page."), texture = PREVIOUS_HEAD_TEXTURE)
        } else {
            head("<ttl>Previous Page", listOf("<key>Left <hnt>Page $page"), texture = PREVIOUS_HEAD_TEXTURE)
        }

    fun nextPage(page: Int, pageCount: Int): NpcItem =
        if (page >= pageCount - 1) {
            head("<off>Next Page", listOf("<hnt>No further pages."), texture = NEXT_HEAD_TEXTURE)
        } else {
            head("<ttl>Next Page", listOf("<key>Left <hnt>Page ${page + 2}"), texture = NEXT_HEAD_TEXTURE)
        }

    fun pageIndicator(page: Int, pageCount: Int, lore: List<String>): NpcItem =
        item("PAPER", "<ttl>Page ${page + 1} <hnt>of <ttl>$pageCount", lore)

    fun title(section: String, npcName: String): String = "$section · ${clipName(npcName)}"

    private fun clipName(name: String): String =
        if (name.length <= TITLE_NAME_LENGTH) name else name.take(TITLE_NAME_LENGTH) + "..."

    fun pretty(material: String): String =
        material.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    fun quote(text: String): String = "\"$text\""
}

internal fun <T> List<T>.after(current: T): T = this[(indexOf(current) + 1) % size]
