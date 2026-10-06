package app.simplecloud.npc.common.inventory.view

import app.simplecloud.npc.common.inventory.source.InventoryEntry
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.common.text.substitute
import app.simplecloud.npc.core.cloud.BridgePlaceholders
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import java.util.UUID

object ItemIconResolver {

    fun icon(item: InventoryConfiguration.InventoryItemConfiguration, viewer: UUID? = null): NpcItem {
        val head = item.head
        val showsViewer = head?.showsViewer == true && viewer != null
        val owned = head?.owner?.takeUnless { showsViewer }?.let(HeadOwners::skin)
        val texture = owned?.texture ?: head?.texture?.takeIf { it.isNotBlank() && !showsViewer }

        return NpcItem(
            material = item.material,
            name = item.name.orEmpty(),
            lore = item.lore.toList(),
            amount = item.amount,
            glowing = item.glowing,
            customModelData = item.customModelData,
            hideTooltip = item.hideTooltip,
            skinTexture = texture,
            skinSignature = if (owned != null) owned.signature else head?.signature?.takeIf { texture != null },
            playerHeadOwner = viewer?.takeIf { showsViewer },
        )
    }

    suspend fun liveIcon(
        item: InventoryConfiguration.InventoryItemConfiguration,
        entry: InventoryEntry,
        look: InventoryConfiguration.StateLook? = null,
        viewer: UUID? = null,
    ): NpcItem {
        entry.overrideIcon?.let { return icon(it, viewer) }
        val styled = look?.let { styled(item, it) } ?: item

        return icon(styled, viewer).copy(
            name = resolveText(styled.name.orEmpty(), entry),
            lore = styled.lore.map { resolveText(it, entry) },
        )
    }

    fun styled(
        item: InventoryConfiguration.InventoryItemConfiguration,
        look: InventoryConfiguration.StateLook,
    ): InventoryConfiguration.InventoryItemConfiguration = item.copy(
        material = look.material ?: item.material,
        name = look.name ?: item.name,
        lore = look.lore?.toMutableList() ?: item.lore,
        glowing = look.glowing ?: item.glowing,
        head = if (look.material != null) null else item.head,
    )

    private suspend fun resolveText(text: String, entry: InventoryEntry): String {
        if (text.isEmpty()) return text
        val resolved = entry.placeholders
            ?.let { Msg.miniMessage.serialize(BridgePlaceholders.append(it, text, "entry")) }
            ?: text

        return resolved.substitute(entry.substitutions)
    }
}
