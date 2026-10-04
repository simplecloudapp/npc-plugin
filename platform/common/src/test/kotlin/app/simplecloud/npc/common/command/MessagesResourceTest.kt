package app.simplecloud.npc.common.command

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessagesResourceTest {

    private fun load(): ConfigurationNode {
        val resource = assertNotNull(javaClass.getResourceAsStream("/messages.yml"), "resource missing")
        val file = createTempDirectory("npc-messages").resolve("messages.yml")
        file.writeBytes(resource.readBytes())

        return YamlConfigurationLoader.builder().path(file).build().load()
    }

    private fun leaves(node: ConfigurationNode, prefix: String = ""): Set<String> =
        if (node.isMap) {
            node.childrenMap().flatMap { (key, child) ->
                leaves(child, if (prefix.isEmpty()) "$key" else "$prefix.$key")
            }.toSet()
        } else {
            setOf(prefix)
        }

    @Test
    fun `code paths and resource leaves match`() {
        val root = load()
        val fileLeaves = leaves(root)
            .filterNot { it.startsWith("variables.") || it.startsWith("command.help.pages.") }
            .toSet()
        assertEquals(CommandMessages.resourcePaths(), fileLeaves)
    }
}
