package app.simplecloud.npc.common.editor.core

data class KnownTarget(val name: String, val group: Boolean)

enum class ServerStatus { ONLINE, STARTING, STOPPING, UNKNOWN, OFFLINE }

data class LiveServer(val name: String, val status: ServerStatus, val playerCount: Int) {
    val online: Boolean get() = status == ServerStatus.ONLINE
}
