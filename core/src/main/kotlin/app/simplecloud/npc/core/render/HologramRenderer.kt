package app.simplecloud.npc.core.render

import app.simplecloud.npc.core.config.NpcConfig

interface HologramRenderer {
    fun createOrUpdate(config: NpcConfig)
    fun destroy(config: NpcConfig)
    fun destroyAll()
}
