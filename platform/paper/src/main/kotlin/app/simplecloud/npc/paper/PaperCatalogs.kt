package app.simplecloud.npc.paper

import app.simplecloud.npc.common.platform.PlatformCatalogs
import app.simplecloud.npc.paper.item.MaterialResolver
import app.simplecloud.npc.paper.sound.SoundCatalog

object PaperCatalogs : PlatformCatalogs {
    override fun materials(): List<String> = MaterialResolver.itemMaterialNames()
    override fun sounds(): List<String> = SoundCatalog.names()
}
