package app.simplecloud.npc.paper

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.fail

class PluginJarTest {

    private val jar = File(requireNotNull(System.getProperty("npc.pluginJar")) { "run through gradle" })
    private val entries = JarFile(jar).use { archive -> archive.entries().asSequence().map { it.name }.toSet() }
    private val floorAdventure = requireNotNull(System.getProperty("npc.floorAdventure"))
    private val shipped = entries.filter { it.endsWith(".class") }.map { it.removeSuffix(".class") }.toSet()

    @Test
    fun `ships only the relocated adventure`() {
        val forbidden = listOf(
            "net/kyori/adventure/",
            "net/kyori/examination/",
            "net/kyori/option/",
            "META-INF/services/net.kyori.",
        )
        val required = listOf(
            "app/simplecloud/npc/relocate/packets/kyori/adventure/text/Component.class",
            "app/simplecloud/npc/relocate/packets/kyori/adventure/nbt/CompoundBinaryTag.class",
            "app/simplecloud/npc/relocate/packets/kyori/examination/Examinable.class",
        )

        val problems = forbidden.mapNotNull { prefix ->
            entries.filter { it.startsWith(prefix) }.takeIf { it.isNotEmpty() }
                ?.let { "'$prefix' must not be in the jar, but ${it.size} entries are, e.g. ${it.take(3)}" }
        } + required.filterNot { it in entries }.map { "'$it' has to be in the jar and is not" }

        if (problems.isNotEmpty()) fail("${jar.name} has the wrong shape:\n" + problems.joinToString("\n") { "  $it" })
    }

    @Test
    fun `every relocated class it names is shipped`() = assertResolves(
        prefix = "app/simplecloud/npc/relocate/",
        provided = emptySet(),
        allowed = setOf(
            "app/simplecloud/npc/relocate/configurate/kotlin/ObjectMappingKt",
            "app/simplecloud/npc/relocate/packets/kyori/adventure/text/logger/slf4j/ComponentLogger",
        ),
    )

    @Test
    fun `every adventure class it names is on the floor server`() = assertResolves(
        prefix = "net/kyori/",
        provided = classNamesIn(serverClasspath(floorAdventure)),
        skipped = gatedClasses(floorAdventure),
    )

    @Test
    fun `packet stack never names the server's adventure`() = assertResolves(
        prefix = "net/kyori/",
        provided = emptySet(),
        scanned = listOf(
            "app/simplecloud/npc/relocate/packetevents/",
            "app/simplecloud/npc/relocate/entitylib/",
            "app/simplecloud/npc/relocate/packets/",
        ),
    )

    @Test
    fun `links against floor adventure`() = assertLinks(floorAdventure)

    @Test
    fun `links against adventure 4_24 of paper 1_21_8`() = assertLinks("4.24.0")

    @Test
    fun `links against adventure 5 of 26_x`() = assertLinks("5.2.0")

    private fun assertResolves(
        prefix: String,
        provided: Set<String>,
        allowed: Set<String> = emptySet(),
        scanned: List<String> = emptyList(),
        skipped: List<String> = emptyList(),
    ) {
        val referenced = sortedSetOf<String>()
        JarFile(jar).use { archive ->
            archive.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .filter { entry -> scanned.isEmpty() || scanned.any { entry.name.startsWith(it) } }
                .filterNot { entry -> skipped.any { entry.name.startsWith(it) } }
                .forEach { entry ->
                    val bytes = archive.getInputStream(entry).use { it.readBytes() }
                    referenced += referencesIn(bytes, prefix)
                }
        }

        val missing = referenced - shipped - provided - allowed
        if (missing.isNotEmpty()) {
            fail(
                "${missing.size} class(es) under '$prefix' are named by ${jar.name} but neither shipped " +
                    "in it nor provided by the server. Either ship them, or allow them here with the " +
                    "reason they cannot be hit at runtime:\n" + missing.joinToString("\n") { "  $it" },
            )
        }
    }

    private fun assertLinks(adventureVersion: String) {
        val skipped = gatedClasses(adventureVersion)
        val failures = sortedMapOf<String, String>()
        val classpath = (listOf(jar) + serverClasspath(adventureVersion)).map { it.toURI().toURL() }
        URLClassLoader(classpath.toTypedArray(), ClassLoader.getPlatformClassLoader()).use { loader ->
            shipped.asSequence()
                .filterNot { it.endsWith("module-info") || '-' in it }
                .filterNot { name -> skipped.any { name.startsWith(it) } }
                .forEach { name ->
                    adventureLinkageProblem(name.replace('/', '.'), loader)?.let { failures[name] = it }
                }
        }

        if (failures.isNotEmpty()) {
            fail(
                "${failures.size} class(es) do not link against the server's Adventure $adventureVersion:\n" +
                    failures.entries.joinToString("\n") { (name, problem) -> "  $name -> $problem" },
            )
        }
    }

    private fun adventureLinkageProblem(name: String, loader: ClassLoader): String? {
        val error = runCatching { Class.forName(name, true, loader) }.exceptionOrNull() ?: return null

        val root = generateSequence(error) { it.cause }.last()
        if (root !is LinkageError && root !is ClassNotFoundException) return null
        val message = root.message.orEmpty()
        if (message.startsWith("Could not initialize class")) return null

        return if ("net.kyori" in message || "net/kyori" in message) "${root.javaClass.simpleName}: $message" else null
    }

    private fun serverClasspath(adventureVersion: String): List<File> =
        System.getProperty("npc.serverClasspath.$adventureVersion").split(File.pathSeparator).map(::File)

    private fun gatedClasses(adventureVersion: String): List<String> =
        System.getProperty("npc.gatedClasses.$adventureVersion").orEmpty().split(',').filter { it.isNotBlank() }

    private fun classNamesIn(files: List<File>): Set<String> = files.filter { it.isFile }.flatMapTo(HashSet()) { file ->
        JarFile(file).use { archive ->
            archive.entries().asSequence()
                .map { it.name }
                .filter { it.endsWith(".class") }
                .map { it.removeSuffix(".class") }
                .toList()
        }
    }

    private fun referencesIn(classBytes: ByteArray, prefix: String): Set<String> =
        constantPoolClasses(classBytes)
            .map { it.trimStart('[').removePrefix("L").removeSuffix(";") }
            .filter { it.startsWith(prefix) }
            .toSet()

    private fun constantPoolClasses(classBytes: ByteArray): List<String> {
        val input = DataInputStream(ByteArrayInputStream(classBytes))
        if (input.readInt() != CLASS_FILE_MAGIC) return emptyList()
        input.skipBytes(4)

        val entryCount = input.readUnsignedShort()
        val utf8 = arrayOfNulls<String>(entryCount)
        val classNameIndexes = mutableListOf<Int>()

        var index = 1
        while (index < entryCount) {
            when (input.readUnsignedByte()) {
                1 -> utf8[index] = input.readUTF()
                7 -> classNameIndexes += input.readUnsignedShort()
                8, 16, 19, 20 -> input.skipBytes(2)
                15 -> input.skipBytes(3)
                3, 4, 9, 10, 11, 12, 17, 18 -> input.skipBytes(4)
                5, 6 -> {
                    input.skipBytes(8)
                    index++
                }
                else -> fail("unknown constant pool tag in a class of ${jar.name}")
            }
            index++
        }

        return classNameIndexes.mapNotNull { utf8.getOrNull(it) }
    }

    private companion object {
        const val CLASS_FILE_MAGIC = -0x35014542
    }
}
