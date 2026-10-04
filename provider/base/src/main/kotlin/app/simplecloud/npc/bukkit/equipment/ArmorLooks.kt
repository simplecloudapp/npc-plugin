package app.simplecloud.npc.bukkit.equipment

import app.simplecloud.npc.bukkit.skin.HeadTextures
import app.simplecloud.npc.core.config.NpcConfig
import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.meta.trim.ArmorTrim

object ArmorLooks {

    fun apply(meta: ItemMeta, color: String?, trim: NpcConfig.ArmorTrim?, customModelData: Int?) {
        color?.let(::parseColor)?.let { (meta as? LeatherArmorMeta)?.setColor(it) }
        trim?.let(::armorTrim)?.let { (meta as? ArmorMeta)?.trim = it }
        @Suppress("DEPRECATION")
        customModelData?.let(meta::setCustomModelData)
    }

    fun read(stack: ItemStack): NpcConfig.EquipmentItem {
        val meta = stack.itemMeta
        val glintOverride = meta != null && meta.hasEnchantmentGlintOverride() && meta.enchantmentGlintOverride
        val texture = (meta as? SkullMeta)?.playerProfile?.properties?.firstOrNull { it.name == "textures" }
        val dyed = (meta as? LeatherArmorMeta)?.color?.takeUnless { it == DEFAULT_LEATHER }
        val trim = (meta as? ArmorMeta)?.takeIf { it.hasTrim() }?.trim
        @Suppress("DEPRECATION")
        val customModelData = meta?.takeIf { it.hasCustomModelData() }?.customModelData

        return NpcConfig.EquipmentItem(
            material = stack.type.name,
            glowing = meta?.hasEnchants() == true || glintOverride,
            headTexture = texture?.value,
            color = dyed?.let { String.format("#%06x", it.asRGB()) },
            trim = trim?.let {
                NpcConfig.ArmorTrim(material = it.material.key.toString(), pattern = it.pattern.key.toString())
            },
            customModelData = customModelData,
        )
    }

    fun toItemStack(item: NpcConfig.EquipmentItem, base: ItemStack): ItemStack = base.apply {
        editMeta { meta ->
            val texture = item.headTexture
            if (!texture.isNullOrBlank()) HeadTextures.apply(meta, texture)
            if (item.glowing) meta.setEnchantmentGlintOverride(true)
            apply(meta, item.color, item.trim, item.customModelData)
        }
    }

    private fun parseColor(hex: String): Color? =
        hex.removePrefix("#").toIntOrNull(16)?.takeIf { it in 0..0xFFFFFF }?.let(Color::fromRGB)

    private fun armorTrim(trim: NpcConfig.ArmorTrim): ArmorTrim? = runCatching {
        val material = NamespacedKey.fromString(trim.material)?.let(Registry.TRIM_MATERIAL::get)
        val pattern = NamespacedKey.fromString(trim.pattern)?.let(Registry.TRIM_PATTERN::get)
        if (material == null || pattern == null) null else ArmorTrim(material, pattern)
    }.getOrNull()

    private val DEFAULT_LEATHER: Color = Color.fromRGB(0xA06540)
}
