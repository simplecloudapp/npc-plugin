package app.simplecloud.npc.core.cloud

interface CloudSnapshots {
    fun peek(): CloudSnapshot
    fun requestRefresh()
}
