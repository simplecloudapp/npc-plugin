package app.simplecloud.npc.paper.item

import app.simplecloud.npc.bukkit.equipment.ArmorLooks
import app.simplecloud.npc.bukkit.skin.HeadTextures
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.text.Msg
import com.google.common.collect.ImmutableMultimap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

object PaperNpcItem {

    fun toItemStack(item: NpcItem): ItemStack = MaterialResolver.itemStack(item.material, item.amount).apply {
        editMeta { meta ->
            val texture = item.skinTexture

            if (texture != null) {
                HeadTextures.apply(meta, texture, item.skinSignature)
            } else {
                item.playerHeadOwner?.let { owner ->
                    val profile = Bukkit.getPlayer(owner)?.playerProfile ?: Bukkit.createProfile(owner)
                    (meta as? SkullMeta)?.playerProfile = profile
                }
            }

            meta.isHideTooltip = item.hideTooltip
            if (!item.hideTooltip) {
                meta.displayName(withoutDefaultItalic(item.name))
                if (item.lore.isNotEmpty()) meta.lore(item.lore.map(::withoutDefaultItalic))
            }

            item.customModelData?.let(meta::setCustomModelData)
            item.maxStackSize?.let(meta::setMaxStackSize)
            ArmorLooks.apply(meta, item.armorColor, item.armorTrim, null)
            if (item.glowing) meta.setEnchantmentGlintOverride(true)
            meta.attributeModifiers = ImmutableMultimap.of()
            meta.addItemFlags(*ItemFlag.entries.toTypedArray())
        }
    }

    private fun withoutDefaultItalic(miniMessageText: String): Component = Component.text()
        .decoration(TextDecoration.ITALIC, false)
        .append(if (miniMessageText.isEmpty()) Component.empty() else Msg.miniMessage.deserialize(miniMessageText))
        .build()

    fun menuClickOf(click: ClickType): MenuClick = when (click) {
        ClickType.LEFT -> MenuClick.LEFT
        ClickType.SHIFT_LEFT -> MenuClick.SHIFT_LEFT
        ClickType.RIGHT -> MenuClick.RIGHT
        ClickType.SHIFT_RIGHT -> MenuClick.SHIFT_RIGHT
        ClickType.DROP, ClickType.CONTROL_DROP -> MenuClick.DROP
        ClickType.MIDDLE -> MenuClick.MIDDLE
        ClickType.SWAP_OFFHAND -> MenuClick.OFFHAND
        ClickType.DOUBLE_CLICK -> MenuClick.DOUBLE_CLICK
        else -> MenuClick.OTHER
    }
}
