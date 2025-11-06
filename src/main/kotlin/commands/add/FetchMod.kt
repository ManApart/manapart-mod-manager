package commands.add

import cyan
import fetchModInfo
import io.ktor.server.util.url
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import logFetch
import red
import toolConfig
import urlToId

val fetchDescription = """
   Add mod metadata without downloading files
   Useful for adding NEW mods. To check for updates on existing mods, see update
""".trimIndent()

val fetchUsage = """
   fetch <mod id>
   fetch 111 222 333
""".trimIndent()

fun fetchMod(command: String, args: List<String>) {
    val firstArg = args.firstOrNull() ?: ""
    when {
        args.isEmpty() -> println(fetchDescription)
        firstArg.startsWith("http") -> addModByUrls(args)
        else -> fetchModsById(args.mapNotNull { it.toIntOrNull() })
    }
}

fun fetchModsById(ids: List<Int>) {
    ids.chunked(toolConfig.chunkSize).forEach { chunk ->
        runBlocking {
            chunk.map { id ->
                async {
                    fetchModInfo(id)?.let { mod -> println("Fetched info for ${mod.id} ${mod.name}") }
                }
            }.awaitAll()
        }
    }
    logFetch(ids)
    println(cyan("Done Fetching"))
}

private fun addModByUrls(urls: List<String>) {
    val ids = urls.mapNotNull { url -> url.urlToId().also { if (it == null) println(red("Could not find id for $url")) } }

    ids.chunked(toolConfig.chunkSize).forEach { chunk ->
        runBlocking {
            chunk.map { id ->
                async {
                    fetchModInfo(id)?.let { println("Fetched info for ${it.id} ${it.name}") }
                }
            }.awaitAll()
        }
    }
    logFetch(ids)
    println(cyan("Done fetching"))
}
