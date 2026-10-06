package app.simplecloud.npc.core.cloud

object CloudNames {
    fun servers(snapshot: CloudSnapshot = CloudListCache.peek()): List<String> = buildList {
        val gameServers = snapshot.servers.filterNot { (it.serverBase ?: it.group)?.isProxy() == true }
        addAll(gameServers.mapNotNull { it.connectName() })
        addAll(snapshot.persistentServers.filterNot { it.isProxy() }.map { it.name })
    }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun targets(snapshot: CloudSnapshot = CloudListCache.peek()): List<String> = buildList {
        addAll(snapshot.groups.filterNot { it.isProxy() }.map { it.name })
        addAll(snapshot.persistentServers.filterNot { it.isProxy() }.map { it.name })
    }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
}
