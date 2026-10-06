package app.simplecloud.npc.paper.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MaterialResolver {

    fun itemStack(materialName: String, amount: Int): ItemStack =
        ItemStack(Material.matchMaterial(materialName) ?: Material.STONE, amount.coerceIn(1, MAX_STACK))

    fun itemMaterialNames(): List<String> = Material.entries.filter { it.isItem }.map { it.name }

    private const val MAX_STACK = 64
}
