package app.simplecloud.npc.paper.canvas

import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.inventory.InventoryConfiguration.HeadConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

object ItemLooks {

    fun of(stack: ItemStack): InventoryItemConfiguration {
        val meta = stack.itemMeta
        @Suppress("DEPRECATION")
        val customModelData = meta?.takeIf { it.hasCustomModelData() }?.customModelData
        val head = (meta as? SkullMeta)?.playerProfile?.properties
            ?.firstOrNull { it.name == "textures" }
            ?.let { HeadConfiguration(texture = it.value, signature = it.signature) }
        val glintOverride = meta != null && meta.hasEnchantmentGlintOverride() && meta.enchantmentGlintOverride

        return InventoryItemConfiguration(
            material = stack.type.name,
            name = meta?.takeIf { it.hasDisplayName() }?.displayName()?.let(Msg.miniMessage::serialize),
            lore = meta?.lore()?.map(Msg.miniMessage::serialize).orEmpty().toMutableList(),
            amount = stack.amount,
            glowing = meta?.hasEnchants() == true || glintOverride,
            customModelData = customModelData,
            head = head,
        )
    }
}
