package com.mittohoa.lyra.sources

import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.parseLrc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * LRCLIB - kho loi bai hat mo, khong can khoa API.
 *
 * Hai duong:
 *   /api/get     doi ten CHINH XAC, nhanh, dung khi ta tin vao ten da nhan dien
 *   /api/search  do hon, dung khi /get truot
 *
 * Uu tien ban co moc thoi gian (`syncedLyrics`); khong co thi lay ban chu tron.
 */
object LrclibClient {

    private const val BASE = "https://lrclib.net/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Hit(
        val id: Long = 0,
        @SerialName("trackName") val trackName: String = "",
        @SerialName("artistName") val artistName: String = "",
        val duration: Double = 0.0,
        @SerialName("plainLyrics") val plainLyrics: String? = null,
        @SerialName("syncedLyrics") val syncedLyrics: String? = null
    )

    /**
     * Tim loi cho mot cap (nghe si, ten bai).
     * @param durationMs do dai bai; giup LRCLIB chon dung ban thu am
     */
    suspend fun fetch(artist: String, title: String, durationMs: Long): Lyrics? =
        withContext(Dispatchers.IO) {
            exact(artist, title, durationMs) ?: search(artist, title)
        }

    private fun exact(artist: String, title: String, durationMs: Long): Lyrics? {
        if (artist.isBlank() || title.isBlank()) return null

        val url = "$BASE/get".toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("track_name", title)
            .apply {
                // LRCLIB doi don vi GIAY. Gui mili-giay vao day thi khong bao
                // gio khop, ma no cung khong bao loi - chi lang le tra ve rong.
                if (durationMs > 0) addQueryParameter("duration", (durationMs / 1000).toString())
            }
            .build()

        val hit = get<Hit>(url.toString()) ?: return null
        return hit.toLyrics()
    }

    private fun search(artist: String, title: String): Lyrics? {
        val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isBlank()) return null

        val url = "$BASE/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build()

        val hits = get<List<Hit>>(url.toString()).orEmpty()
        // Uu tien ban co moc thoi gian; khong co thi lay ban dau
        return (hits.firstOrNull { !it.syncedLyrics.isNullOrBlank() } ?: hits.firstOrNull())
            ?.toLyrics()
    }

    private inline fun <reified T> get(url: String): T? = try {
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build())
            .execute()
            .use { res ->
                if (!res.isSuccessful) null
                else res.body?.string()?.let { json.decodeFromString<T>(it) }
            }
    } catch (e: Exception) {
        // Mat mang, may chu doi dinh dang, bai khong co trong kho - deu cung
        // mot ket qua voi ben goi: khong co loi. Ben goi se thu nguon khac.
        null
    }

    private fun Hit.toLyrics(): Lyrics? {
        val synced = syncedLyrics?.takeIf { it.isNotBlank() }
        val plain = plainLyrics?.takeIf { it.isNotBlank() }
        val content = synced ?: plain ?: return null

        return parseLrc(content, from = "lrclib").copy(
            matchedTitle = trackName,
            matchedArtist = artistName
        )
    }

    /** LRCLIB de nghi tu gioi thieu; khong dat thi co the bi tu choi. */
    private const val UA = "Lyra/0.1.0 (https://github.com/mittohoa/lyra-android)"
}
