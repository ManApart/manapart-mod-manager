package nexus

import gameMode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import verbose
import java.io.File

private val client = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 1000 * 60 * 60 * 2
    }
}

fun parseDownloadRequest(url: String): DownloadRequest {
    val modId = url.between("mods/", "/").toInt()
    val fileId = url.between("files/", "?key").toInt()
    val key = url.between("?key=", "&")
    val expires = url.between("&expires=", "&")
    return DownloadRequest(modId, fileId, key, expires)
}

private fun String.between(start: String, end: String): String {
    val first = indexOf(start) + start.length
    val last = indexOf(end, first)
    return this.substring(first, last)
}

private fun HttpRequestBuilder.commonHeaders(apiKey: String) {
    header("accept", "application/json")
    header("apikey", apiKey)
    header("User-Agent", "StarfieldModManager")
}

suspend fun getModDetails(apiKey: String, id: Int): ModInfo? {
    return try {
        client.get("https://api.nexusmods.com/v1/games/${gameMode.urlName}/mods/$id.json") {
            commonHeaders(apiKey)
        }.body()
    } catch (e: Exception) {
        verbose(e.message ?: "")
        verbose(e.stackTraceToString())
        null
    }
}

suspend fun getModFiles(apiKey: String, id: Int): ModFileInfo? {
    return try {
        client.get("https://api.nexusmods.com/v1/games/${gameMode.urlName}/mods/$id/files.json") {
            commonHeaders(apiKey)
        }.body()
    } catch (e: Exception) {
        println(e.message ?: "")
        verbose(e.stackTraceToString())
        null
    }
}

suspend fun getDownloadUrl(apiKey: String, downloadRequest: DownloadRequest): String? {
    return try {
        val links: List<DownloadLink> = with(downloadRequest) {
            client.get("https://api.nexusmods.com/v1/games/${gameMode.urlName}/mods/$modId/files/$fileId/download_link.json?key=$key&expires=$expires") {
                commonHeaders(apiKey)
            }.body()
        }
        links.first().URI
    } catch (e: Exception) {
        println(e.message ?: "")
        verbose(e.stackTraceToString())
        null
    }
}

suspend fun getDownloadUrl(apiKey: String, modId: Int, fileId: Int): String? {
    return try {
        val links: List<DownloadLink> =
            client.get("https://api.nexusmods.com/v1/games/${gameMode.urlName}/mods/$modId/files/$fileId/download_link.json") {
                commonHeaders(apiKey)
            }.body()
        links.first().URI
    } catch (e: Exception) {
        println(e.message ?: "")
        verbose(e.stackTraceToString())
        null
    }
}

fun parseFileExtension(url: String): String {
    val namePart = url.split("/").last()
    val start = namePart.indexOf(".")
    val end = namePart.indexOf("?", start)
    return namePart.substring(start, end)
}

fun downloadMod(initialUrl: String, destination: String, forceRedownload: Boolean = false): File? {
    val url = initialUrl.replace(" ", "%20")
    val result = File(destination).also { it.parentFile.mkdirs() }

    if (result.exists()) {
        if (forceRedownload) {
            result.delete()
        } else {
            println("Skipping download since it already exists")
            return result
        }
    }

    return try {
        result.createNewFile()
        runBlocking {
            client.prepareGet(url) {
            }.execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                var iterations = 0
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.exhausted()) {
                        val bytes = packet.readByteArray()
                        result.appendBytes(bytes)
                        iterations++
                        if (iterations > 1000) {
                            iterations = 0
                            println("Progress: ${result.length()} / ${httpResponse.contentLength()}")
                        }
                    }
                }
            }
        }
        result
    } catch (e: Exception) {
        println(e.message ?: "")
        verbose(e.stackTraceToString())
        result.delete()
        null
    }
}

fun getGameInfo(apiKey: String): GameInfo? {
    return try {
        runBlocking {
            client.get("https://api.nexusmods.com/v1/games/${gameMode.urlName}.json") {
                commonHeaders(apiKey)
            }.body()
        }
    } catch (e: Exception) {
        println(e.message ?: "")
        verbose(e.stackTraceToString())
        null
    }
}

suspend fun endorseMod(apiKey: String, modId: Int) = endorseMod(apiKey, modId, true)

suspend fun abstainMod(apiKey: String, modId: Int) = endorseMod(apiKey, modId, false)

private suspend fun endorseMod(apiKey: String, modId: Int, endorse: Boolean) {
    val urlPart = if (endorse) "endorse" else "abstain"
    try {
        client.post("https://api.nexusmods.com/v1/games/${gameMode.urlName}/mods/$modId/$urlPart.json") {
            commonHeaders(apiKey)
        }.body()
    } catch (e: Exception) {
        println(e.message ?: "")
        verbose(e.stackTraceToString())
    }
}
