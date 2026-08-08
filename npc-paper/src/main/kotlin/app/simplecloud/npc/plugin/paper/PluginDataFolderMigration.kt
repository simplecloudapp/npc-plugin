package app.simplecloud.npc.plugin.paper

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID

internal object PluginDataFolderMigration {
    const val LEGACY_FOLDER_NAME = "SimpleCloud-NPC"

    fun migrate(dataFolder: Path): Result {
        val parent = dataFolder.parent ?: return Result.None
        val legacyFolder = findExactEntry(parent, LEGACY_FOLDER_NAME) ?: return Result.None

        if (Files.exists(dataFolder) && Files.isSameFile(legacyFolder, dataFolder)) {
            renameCaseOnly(legacyFolder, dataFolder)
            return Result.Moved
        }

        if (!Files.exists(dataFolder)) {
            Files.move(legacyFolder, dataFolder)
            return Result.Moved
        }

        val conflicts = mergeDirectories(legacyFolder, dataFolder)
        return if (conflicts.isEmpty()) Result.Merged else Result.MergedWithConflicts(conflicts)
    }

    internal fun mergeDirectories(source: Path, target: Path): List<Path> {
        require(Files.isDirectory(source)) { "Legacy plugin data path is not a directory: $source" }
        require(Files.isDirectory(target)) { "Plugin data path is not a directory: $target" }

        val conflicts = mutableListOf<Path>()
        Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(directory: Path, attributes: BasicFileAttributes): FileVisitResult {
                val relative = source.relativize(directory)
                val destination = target.resolve(relative)
                if (Files.exists(destination) && !Files.isDirectory(destination)) {
                    conflicts.add(relative)
                    return FileVisitResult.SKIP_SUBTREE
                }
                Files.createDirectories(destination)
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                val relative = source.relativize(file)
                val destination = target.resolve(relative)
                when {
                    !Files.exists(destination) -> Files.move(file, destination)
                    filesMatch(file, destination) -> Files.delete(file)
                    else -> conflicts.add(relative)
                }
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(directory: Path, exception: java.io.IOException?): FileVisitResult {
                exception?.let { throw it }
                if (isEmpty(directory)) Files.delete(directory)
                return FileVisitResult.CONTINUE
            }
        })
        return conflicts.sortedBy(Path::toString)
    }

    private fun findExactEntry(parent: Path, name: String): Path? {
        if (!Files.isDirectory(parent)) return null
        return Files.newDirectoryStream(parent).use { entries ->
            entries.firstOrNull { it.fileName.toString() == name }
        }
    }

    private fun renameCaseOnly(source: Path, target: Path) {
        if (source.fileName.toString() == target.fileName.toString()) return
        val temporary = source.parent.resolve(".simplecloud-npc-migration-${UUID.randomUUID()}")
        Files.move(source, temporary)
        try {
            Files.move(temporary, target)
        } catch (exception: Exception) {
            try {
                Files.move(temporary, source)
            } catch (rollbackException: Exception) {
                exception.addSuppressed(rollbackException)
            }
            throw exception
        }
    }

    private fun filesMatch(first: Path, second: Path): Boolean {
        return Files.isRegularFile(first) && Files.isRegularFile(second) && Files.mismatch(first, second) == -1L
    }

    private fun isEmpty(directory: Path): Boolean {
        return Files.newDirectoryStream(directory).use { entries -> !entries.iterator().hasNext() }
    }

    sealed interface Result {
        data object None : Result
        data object Moved : Result
        data object Merged : Result
        data class MergedWithConflicts(val paths: List<Path>) : Result
    }
}
