package com.mittohoa.lyra.sources

import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.parseLrc
import com.mittohoa.lyra.lyrics.titleSimilarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import kotlin.math.abs

/**
 * NhacCuaTui.
 *
 * Ho da viet lai web thanh SPA va goi mot API REST noi bo o
 * `graph.nhaccuatui.com` - doan nay goi thang API do. Khong co cam ket nao, nen
 * moi that bai deu thanh "khong tim thay loi".
 *
 * Diem rieng cua NCT: ban loi CO MOC THOI GIAN nam trong mot file .lrc **ma hoa
 * RC4**, va khoa giai ma di kem ngay trong chinh ban tin loi (`keyDecryptLyric`).
 *
 * Chuyen tu `src/main/sources/nct.ts`, chi giu duong LOI.
 */
object NctClient {

    private const val API = "https://graph.nhaccuatui.com"
    private const val WEB = "https://www.nhaccuatui.com"

    private val json = Json { ignoreUnknownKeys = true }

    private val headers = mapOf(
        "Origin" to WEB,
        "Referer" to "$WEB/",
        "Accept" to "application/json",
        "Content-Type" to "application/json"
    )

    @Serializable
    private data class Envelope<T>(
        val code: Int = -1,
        val success: Boolean = false,
        val msg: String = "",
        val data: T? = null
    )

    @Serializable
    private data class Song(
        val key: String = "",
        val name: String = "",
        val artistName: String = "",
        val duration: Int = 0
    )

    @Serializable
    private data class SearchData(val songs: List<Song> = emptyList())

    @Serializable
    private data class LyricData(
        val content: String = "",
        val timedLyric: String = "",
        val keyDecryptLyric: String = ""
    )

    private inline fun <reified T> call(path: String, post: Boolean = false): T? {
        // API nay doi POST cho tim kiem du khong co than tin - gui than rong
        val body = if (post) "".toRequestBody(null) else null
        val raw = Http.text(API + path, headers, body) ?: return null
        return try {
            json.decodeFromString<Envelope<T>>(raw)
                .takeIf { it.success && it.code == 0 }
                ?.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetch(artist: String, title: String, durationMs: Long): Lyrics? =
        withContext(Dispatchers.IO) {
            val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
            if (query.isBlank()) return@withContext null

            val encoded = URLEncoder.encode(query, "UTF-8")
            val songs = call<SearchData>(
                "/api/v1/search/song?keyword=$encoded&pageindex=1&pagesize=10&correct=false",
                post = true
            )?.songs.orEmpty()

            val best = pickBest(songs, artist, title, durationMs) ?: return@withContext null

            val lyric = call<LyricData>(
                "/api/v1/song/lyric/detail?songKey=${URLEncoder.encode(best.key, "UTF-8")}"
            ) ?: return@withContext null

            decode(lyric)?.copy(
                matchedTitle = best.name,
                matchedArtist = best.artistName,
                sourceDuration = best.duration * 1000L
            )
        }

    /**
     * Doc ban co moc thoi gian truoc, khong co thi lay loi chu tron.
     *
     * File .lrc cua NCT tra ve duoi dang chuoi HEX da ma hoa RC4. Kiem ky truoc
     * khi giai: neu ho doi cach ma hoa thi chuoi se khong con la hex, va giai
     * bua se ra rac ma van "thanh cong" - hien mot man hinh ky tu vo nghia con
     * te hon la khong hien gi.
     */
    private fun decode(lyric: LyricData): Lyrics? {
        if (lyric.timedLyric.isNotBlank() && lyric.keyDecryptLyric.isNotBlank()) {
            val hex = Http.text(lyric.timedLyric, mapOf("Referer" to "$WEB/"))?.trim()
            val bytes = hex?.hexToBytesOrNull()
            if (bytes != null) {
                val plain = String(rc4(lyric.keyDecryptLyric.toByteArray(), bytes))
                if (plain.isNotBlank()) return parseLrc(plain, from = "nct")
            }
        }

        return lyric.content.takeIf { it.isNotBlank() }?.let { parseLrc(it, from = "nct") }
    }

    private fun pickBest(
        songs: List<Song>,
        artist: String,
        title: String,
        durationMs: Long
    ): Song? {
        if (songs.isEmpty()) return null

        val scored = songs.map { song ->
            var score = titleSimilarity(song.name, title)
            if (artist.isNotBlank()) {
                score = score * 0.75 + titleSimilarity(song.artistName, artist) * 0.25
            }
            if (durationMs > 0 && song.duration > 0) {
                val diff = abs(song.duration - durationMs / 1000)
                when {
                    diff <= 3 -> score += 0.15
                    diff > 25 -> score -= 0.25
                }
            }
            song to score
        }

        val (song, score) = scored.maxByOrNull { it.second } ?: return null
        return if (score >= MIN_SIMILARITY) song else null
    }

    private const val MIN_SIMILARITY = 0.6
}
