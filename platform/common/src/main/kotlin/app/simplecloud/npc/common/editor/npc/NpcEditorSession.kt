package app.simplecloud.npc.common.editor.npc

import app.simplecloud.npc.common.editor.core.EditorSession
import app.simplecloud.npc.common.editor.core.EditorSessions
import app.simplecloud.npc.core.config.NpcConfig

class NpcEditorSession : EditorSession<NpcEditorScreen>() {
    var hologramState: String? = null
    var actionState: String? = null
    var carriedEquipment: NpcConfig.EquipmentItem? = null
}

class NpcEditorSessions : EditorSessions<NpcEditorSession>(::NpcEditorSession)
