package app.simplecloud.npc.common.editor.core

sealed interface PickerResult {
    data class Added(val label: String, val detail: String) : PickerResult
    data class Rejected(val headline: String, val typed: String, val reason: String) : PickerResult
    data class Ambiguous(val typed: String, val matches: List<String>) : PickerResult
    data class Filtered(val typed: String, val count: Int) : PickerResult
}
