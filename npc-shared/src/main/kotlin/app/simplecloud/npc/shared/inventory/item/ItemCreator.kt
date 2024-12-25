package app.simplecloud.npc.shared.inventory.item

import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.text
import com.noxcrew.interfaces.drawable.Drawable
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * @author Niklas Nieberler
 */

class ItemCreator(
    private val config: InventoryConfig
) {

    fun buildDrawableItem(id: String): Drawable? {
        val item = this.config.items.firstOrNull { it.id == id } ?: return null
        return buildDrawableItem(item)
    }

    fun buildDrawableItem(item: InventoryConfig.InventoryItem): Drawable {
        val itemStack = ItemStack(Material.valueOf(item.material))
        itemStack.editMeta(ItemMeta::class.java) {
            it.displayName(text(item.displayName))
        }
        return Drawable.drawable(itemStack)
    }

}