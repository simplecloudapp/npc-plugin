package app.simplecloud.npc.bukkit.equipment

import app.simplecloud.npc.core.config.NpcConfig
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object EquipmentItems {

    fun toItemStack(item: NpcConfig.EquipmentItem): ItemStack? {
        val material = Material.matchMaterial(item.material)?.takeIf { it.isItem && !it.isAir } ?: return null
        return ArmorLooks.toItemStack(item, ItemStack(material))
    }
}
