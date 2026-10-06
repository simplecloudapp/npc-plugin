package app.simplecloud.npc.common.inventory.source

import java.util.concurrent.ConcurrentHashMap

class InventoryDataSources {

    private val sources = ConcurrentHashMap<String, InventoryDataSource>()

    fun register(source: InventoryDataSource) {
        sources[source.type.lowercase()] = source
    }

    fun find(type: String): InventoryDataSource? = sources[type.lowercase()]

    fun types(): List<String> = sources.keys.sorted()

    companion object {
        fun builtIns(): InventoryDataSources = InventoryDataSources().apply {
            register(GroupServersSource)
            register(PersistentServersSource)
            register(GroupsSource)
            register(StaticEntriesSource)
        }
    }
}
