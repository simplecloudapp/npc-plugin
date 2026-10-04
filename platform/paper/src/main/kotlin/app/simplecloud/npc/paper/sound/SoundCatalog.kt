package app.simplecloud.npc.paper.sound

import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.Registry

object SoundCatalog {

    private val keysByName: Map<String, NamespacedKey> by lazy {
        val registry: Registry<out Keyed> = Registry.SOUNDS
        registry.associate { sound -> nameOf(sound.key) to sound.key }.toSortedMap()
    }

    fun names(): List<String> = keysByName.keys.toList()

    fun resolve(name: String): NamespacedKey? = keysByName[name.uppercase()]
        ?: NamespacedKey.fromString(name.lowercase())?.takeIf { keysByName[nameOf(it)] == it }

    private fun nameOf(key: NamespacedKey): String = key.key.replace('.', '_').uppercase()
}
