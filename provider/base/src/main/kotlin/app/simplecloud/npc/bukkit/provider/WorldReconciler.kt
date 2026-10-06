package app.simplecloud.npc.bukkit.provider

interface WorldReconciler {
    var knownIdsProvider: (() -> Set<String>)?
    fun reconcileLoadedWorlds()
}
