package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.utils.FileUpdater
import java.io.File

/**
 * @author Niklas Nieberler
 */

class HologramManager(
    private val namespace: NpcNamespace
) {

    private val holograms = hashMapOf<String, HologramEditor>()

    init {
        FileUpdater.updateFileRequest {
            println("update file $it")

            println("oha " + it.nameWithoutExtension)

            val npcConfig = this.namespace.npcRepository.get(it.nameWithoutExtension)
            if (npcConfig == null)
                return@updateFileRequest

            println(npcConfig)
            println(this.namespace.findLocationByNpc(npcConfig.id))

            /*
            val get = namespace.npcRepository.get(it.nameWithoutExtension) ?: return@updateFileRequest
            update(get)
        */
        }
    }

    fun add(id: String, editor: HologramEditor) {
        this.holograms[id] = editor
    }

    fun update(npcConfig: NpcConfig) {
        destroy(npcConfig.id)
        val hologram = npcConfig.hologram
        val location = this.namespace.findLocationByNpc(npcConfig.id)
            ?: throw NullPointerException("failed to find location")
        println("update=")
        var hologramEditor = HologramEditor(location)
        hologram.holograms.forEach {
            hologramEditor = hologramEditor.withCustomName(it.text)
                .withNextLine()
        }
    }

    fun destroy(id: String) {
        this.holograms[id]?.destroy()
        this.holograms.remove(id)
    }
}