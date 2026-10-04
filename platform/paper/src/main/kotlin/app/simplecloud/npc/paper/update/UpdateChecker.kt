package app.simplecloud.npc.paper.update

import com.google.gson.JsonParser
import org.bukkit.plugin.Plugin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object UpdateChecker {

    private const val PROJECT = "eyNPY9oJ"
    private const val VERSIONS_URL = "https://api.modrinth.com/v2/project/$PROJECT/version"
    private const val DOWNLOAD_URL = "https://modrinth.com/project/$PROJECT"
    private val TIMEOUT = Duration.ofSeconds(10)

    fun check(plugin: Plugin) {
        val current = plugin.pluginMeta.version
        val request = HttpRequest.newBuilder(URI.create(VERSIONS_URL))
            .timeout(TIMEOUT)
            .header("User-Agent", "simplecloud-npc/$current")
            .build()

        HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() != 200) return@thenAccept
                val newest = newestVersion(response.body()) ?: return@thenAccept
                if (isNewer(newest, current)) {
                    plugin.logger.info("SimpleCloud NPCs $newest is available (running $current): $DOWNLOAD_URL")
                }
            }.exceptionally { null }
    }

    fun newestVersion(json: String): String? = runCatching {
        JsonParser.parseString(json).asJsonArray
            .map { it.asJsonObject }
            .maxByOrNull { it.get("date_published").asString }
            ?.get("version_number")?.asString
    }.getOrNull()

    fun isNewer(candidate: String, current: String): Boolean {
        val a = parts(candidate)
        val b = parts(current)
        for (index in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left > right
        }

        return '-' in current && '-' !in candidate
    }

    private fun parts(version: String): List<Int> =
        version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
}
