package com.mittohoa.lyra.sources

import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.parseLrc
import com.mittohoa.lyra.lyrics.titleSimilarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.math.abs

/**
 * Zing MP3.
 *
 * Zing khong co API cong khai; doan nay goi API noi bo cua chinh trang web ho.
 * Nghia la no CO THE HONG bat cu luc nao khi ho doi phia may chu - moi that bai
 * deu thanh "khong tim thay loi", khong bao gio lam sap app.
 *
 * Chuyen tu `src/main/sources/zing.ts` cua ban Windows: duong LOI, va tu khi
 * Lyra tu phat nhac thi ca duong TIM BAI va LAY LINK PHAT.
 */
object ZingClient {

    private const val HOST = "https://zingmp3.vn"
    private const val VERSION = "1.13.16"
    private const val API_KEY = "88265e23d4284f25963e6eedac8fbfa3"
    private const val SECRET_KEY = "2aa2d1c561e809b267f3638c4a307aab"

    private const val COOKIE_TTL_MS = 30 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true }

    private var cookie = ""
    private var cookieAt = 0L

    /**
     * Chu ky Zing: hmac512(path + sha256(cac tham so ky, sap theo alphabet)).
     *
     * CHI mot so tham so tham gia ky. Vi du `/search/multi` chi ky `ctime` va
     * `version`, con `q` thi KHONG. Nem ca `q` vao la Zing tra ve
     * "Incorect signature" - da mat kha nhieu thoi gian cho cho nay o ban
     * Windows, nen ghi lai day cho ban sau khoi vap.
     */
    private fun sign(path: String, signedParams: Map<String, String>): Pair<String, String> {
        val ctime = (System.currentTimeMillis() / 1000).toString()
        val payload = signedParams + mapOf("ctime" to ctime, "version" to VERSION)
        val canonical = payload.keys.sorted().joinToString("") { "$it=${payload[it]}" }
        return ctime to hmacSha512Hex(path + sha256Hex(canonical), SECRET_KEY)
    }

    private fun ensureCookie(): String {
        val now = System.currentTimeMillis()
        if (cookie.isNotEmpty() && now - cookieAt < COOKIE_TTL_MS) return cookie
        cookie = Http.collectCookies(HOST)
        cookieAt = now
        return cookie
    }

    @Serializable
    private data class Envelope<T>(val err: Int = -1, val msg: String = "", val data: T? = null)

    @Serializable
    private data class Song(
        val encodeId: String = "",
        val title: String = "",
        @SerialName("artistsNames") val artistsNames: String = "",
        val duration: Int = 0,
        val thumbnailM: String = "",
        val thumbnail: String = ""
    )

    @Serializable
    private data class SearchData(val songs: List<Song> = emptyList())

    @Serializable
    private data class LyricData(val file: String = "")

    /**
     * @param signedKeys ten cac tham so tham gia tinh chu ky (ngoai ctime/version)
     */
    private inline fun <reified T> call(
        path: String,
        params: Map<String, String>,
        signedKeys: List<String> = emptyList()
    ): T? {
        val signed = params.filterKeys { it in signedKeys }
        val (ctime, sig) = sign(path, signed)

        val url = (HOST + path).toHttpUrl().newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
            addQueryParameter("ctime", ctime)
            addQueryParameter("version", VERSION)
            addQueryParameter("sig", sig)
            addQueryParameter("apiKey", API_KEY)
        }.build()

        val body = Http.text(
            url.toString(),
            headers = mapOf("Cookie" to ensureCookie(), "Referer" to "$HOST/")
        ) ?: return null

        return try {
            json.decodeFromString<Envelope<T>>(body).takeIf { it.err == 0 }?.data
        } catch (e: Exception) {
            // Ho doi dinh dang tra ve - coi nhu khong co loi, khong lam sap gi
            null
        }
    }

    /**
     * Tim bai theo tu khoa nguoi dung go.
     *
     * Khac han `fetch`: o do ta DA biet ten bai va chi di doi chieu, con o day
     * nguoi dung go gi thi tim nay - khong cham diem, khong loc. Zing xep hang
     * ket qua theo do lien quan cua ho, va ho lam viec do tot hon ta.
     */
    suspend fun search(query: String, limit: Int): List<Track> =
        withContext(Dispatchers.IO) {
            call<SearchData>("/api/v2/search/multi", mapOf("q" to query))
                ?.songs.orEmpty()
                .take(limit)
                .map { song ->
                    Track(
                        id = song.encodeId,
                        source = MusicSource.ZING,
                        title = song.title,
                        artist = song.artistsNames,
                        artworkUrl = song.thumbnailM.ifBlank { song.thumbnail }.ifBlank { null },
                        durationMs = song.duration * 1000L
                    )
                }
        }

    /**
     * Duong phat cho mot bai.
     *
     * Zing tra ve mot bang chat luong; ban 320 thuong la chuoi "VIP" thay vi
     * dia chi, nen phai kiem tra la http chu khong chi kiem tra khac rong. Bai
     * chi danh cho tai khoan tra phi thi tra null - va do la dung, ta khong tim
     * cach lach.
     */
    suspend fun streamUrl(id: String): String? = withContext(Dispatchers.IO) {
        val data = call<Map<String, String>>(
            "/api/v2/song/get/streaming",
            mapOf("id" to id),
            listOf("id")
        ) ?: return@withContext null
        listOf("320", "128").firstNotNullOfOrNull { key ->
            data[key]?.takeIf { it.startsWith("http") }
        }
    }

    /** Tim loi cho mot cap (nghe si, ten bai). */
    suspend fun fetch(artist: String, title: String, durationMs: Long): Lyrics? =
        withContext(Dispatchers.IO) {
            val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
            if (query.isBlank()) return@withContext null

            val songs = call<SearchData>("/api/v2/search/multi", mapOf("q" to query))
                ?.songs.orEmpty()
            val best = pickBest(songs, artist, title, durationMs) ?: return@withContext null

            val file = call<LyricData>(
                "/api/v2/lyric/get/lyric",
                mapOf("id" to best.encodeId),
                listOf("id")
            )?.file
            if (file.isNullOrBlank()) return@withContext null

            val content = Http.text(file, mapOf("Referer" to "$HOST/"))
            if (content.isNullOrBlank()) return@withContext null

            parseLrc(content, from = "zing").copy(
                matchedTitle = best.title,
                matchedArtist = best.artistsNames,
                sourceDuration = best.duration * 1000L
            )
        }

    /**
     * Cham diem cac ket qua roi lay ban cao nhat.
     *
     * Ten bai la chinh, nghe si phu. Do dai bai la manh moi manh nhat: lech
     * duoi 3 giay thi gan nhu chac chan cung mot ban thu am, lech qua 25 giay
     * thi thuong la ban remix hay ban live - phat diem nang.
     */
    private fun pickBest(
        songs: List<Song>,
        artist: String,
        title: String,
        durationMs: Long
    ): Song? {
        if (songs.isEmpty()) return null

        val scored = songs.map { song ->
            var score = titleSimilarity(song.title, title)
            if (artist.isNotBlank()) {
                score = score * 0.75 + titleSimilarity(song.artistsNames, artist) * 0.25
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

    /** Duoi nguong nay coi nhu tra nham bai. */
    private const val MIN_SIMILARITY = 0.6
}
