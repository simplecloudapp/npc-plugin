package app.simplecloud.npc.bukkit.skin

import com.destroystokyo.paper.profile.ProfileProperty
import org.bukkit.Bukkit
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

object HeadTextures {
    fun apply(meta: ItemMeta, texture: String, signature: String? = null) {
        if (meta !is SkullMeta) return
        meta.playerProfile = Bukkit.createProfile(UUID.nameUUIDFromBytes("npc-head:$texture".toByteArray())).apply {
            setProperty(ProfileProperty("textures", texture, signature))
        }
    }
}
