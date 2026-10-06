package app.simplecloud.npc.common.command

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
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

    @Test
    fun `old default prefixes are replaced, custom ones kept`() {
        val dir = createTempDirectory("npc-prefix")
        val old = dir.resolve("old.yml")
        old.writeText("variables:\n  prefix: '<#0EA5E9><bold>NPCs</bold> <#475569>|'\n")
        val custom = dir.resolve("custom.yml")
        custom.writeText("variables:\n  prefix: '<red>Mine |'\n")

        CommandMessages.updateDefaultPrefix(old)
        CommandMessages.updateDefaultPrefix(custom)

        assertEquals("variables:\n  prefix: '<#0EA5E9>⚡ NPC <#475569>|'\n", old.readText())
        assertEquals("variables:\n  prefix: '<red>Mine |'\n", custom.readText())
    }
}
