package app.simplecloud.npc.shared.inventory

import app.simplecloud.npc.shared.text
import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.GridPositionGenerator
import com.noxcrew.interfaces.interfaces.buildCombinedInterface
import com.noxcrew.interfaces.transform.builtin.PaginationButton
import com.noxcrew.interfaces.transform.builtin.PaginationTransformation
import com.noxcrew.interfaces.utilities.forEachInGrid
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * @author Niklas Nieberler
 */

class ActionInventory {

    suspend fun test(player: Player) {
        val buildCombinedInterface = buildCombinedInterface {
            rows = 6
            initialTitle = text("YEEPEEE")

            /*
            withTransform { pane, view ->
                val itemStack = ItemStack(Material.STONECUTTER)
                pane[1,1] = StaticElement(Drawable.drawable(itemStack)) {

                }
            }

             */

            addTransform(
                PaginationTransformation(
                    GridPositionGenerator { listOf(GridPoint(1, 1)) },
                    listOf(
                        StaticElement(Drawable.drawable(Material.STONECUTTER)),
                        StaticElement(Drawable.drawable(Material.STONE))
                    ),
                    PaginationButton(GridPoint(5, 1), Drawable.drawable(Material.RED_STAINED_GLASS), hashMapOf(ClickType.LEFT to -1)),
                    PaginationButton(GridPoint(5, 2), Drawable.drawable(Material.GREEN_STAINED_GLASS), hashMapOf(ClickType.LEFT to 1)),
                )
            )

        }
        buildCombinedInterface.open(player)
    }
}