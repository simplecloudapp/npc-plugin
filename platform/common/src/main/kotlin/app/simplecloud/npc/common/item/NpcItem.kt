package app.simplecloud.npc.common.item

import app.simplecloud.npc.core.config.NpcConfig
import java.util.UUID

data class NpcItem(
    val material: String,
    val name: String,
    val lore: List<String> = emptyList(),
    val playerHeadOwner: UUID? = null,
    val skinTexture: String? = null,
    val skinSignature: String? = null,
    val amount: Int = 1,
    val glowing: Boolean = false,
    val customModelData: Int? = null,
    val hideTooltip: Boolean = false,
    val maxStackSize: Int? = null,
    val armorColor: String? = null,
    val armorTrim: NpcConfig.ArmorTrim? = null,
) {
    companion object {
        fun playerHead(
            name: String,
            lore: List<String> = emptyList(),
            owner: UUID? = null,
            texture: String? = null,
            signature: String? = null,
            glowing: Boolean = false,
        ): NpcItem = NpcItem(
            material = "PLAYER_HEAD",
            name = name,
            lore = lore,
            playerHeadOwner = owner,
            skinTexture = texture,
            skinSignature = signature,
            glowing = glowing,
        )
    }
}
