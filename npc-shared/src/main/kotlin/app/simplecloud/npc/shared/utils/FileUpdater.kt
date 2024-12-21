package app.simplecloud.npc.shared.utils

import java.io.File

/**
 * @author Niklas Nieberler
 */

object FileUpdater {

    private val functions = mutableListOf<(File) -> Unit>()

    fun updateFileRequest(function: (File) -> Unit) {
        this.functions.add(function)
    }

    fun invokeFile(file: File) {
        this.functions.forEach { it(file) }
    }

}