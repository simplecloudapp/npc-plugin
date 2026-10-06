package app.simplecloud.npc.common.platform

import app.simplecloud.npc.common.editor.inventory.CommonMaterials

interface PlatformCatalogs {
    fun materials(): List<String>
    fun sounds(): List<String>

    companion object {
        val DEFAULT: PlatformCatalogs = object : PlatformCatalogs {
            override fun materials(): List<String> = CommonMaterials.COMMON
            override fun sounds(): List<String> = emptyList()
        }
    }
}
