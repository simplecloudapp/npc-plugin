package app.simplecloud.npc.core.cloud

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class NoBlockingCloudReadsTest {

    @Test
    fun `no production source under core or platform-common blocks on a coroutine`() {
        val root = repositoryRoot()
        val offenders = listOf("core/src/main", "platform/common/src/main")
            .map(root::resolve)
            .flatMap { directory ->
                Files.walk(directory).use { paths ->
                    paths.filter { it.extension == "kt" }
                        .filter { it.readText().contains("runBlocking") }
                        .map(root::relativize)
                        .toList()
                }
            }

        assertTrue(
            offenders.isEmpty(),
            "these run a coroutine to completion on the calling thread, which may be the server " +
                "thread: ${offenders.joinToString()}",
        )
    }

    private fun repositoryRoot(): Path = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .first { it.resolve("settings.gradle.kts").exists() }
}
