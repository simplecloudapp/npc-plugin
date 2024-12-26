package app.simplecloud.npc.shared.inventory.item

import app.simplecloud.npc.shared.decodeComponent
import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.text
import com.noxcrew.interfaces.drawable.Drawable
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * @author Niklas Nieberler
 */

class ItemCreator(
    private val config: InventoryConfig
) {

    fun buildDrawableItem(id: String, placeholder: (String) -> Component = { decodeComponent(text(it)) }): Drawable? {
        val item = this.config.items.firstOrNull { it.id == id } ?: return null
        return buildDrawableItem(item, placeholder)
    }

    fun buildDrawableItem(
        item: InventoryConfig.InventoryItem,
        placeholder: (String) -> Component = { decodeComponent(text(it)) }
    ): Drawable {
        val itemStack = ItemStack(Material.valueOf(item.material))
        itemStack.editMeta(ItemMeta::class.java) { itemMeta ->
            itemMeta.lore(item.lores.map(placeholder))
            itemMeta.displayName(placeholder(item.displayName))
            itemMeta.setCustomModelData(item.customModelData)
        }
        return Drawable.drawable(itemStack)
    }

}