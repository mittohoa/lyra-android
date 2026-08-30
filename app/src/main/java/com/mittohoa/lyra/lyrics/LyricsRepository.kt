package com.mittohoa.lyra.lyrics

import android.util.Log
import com.mittohoa.lyra.data.LyricCache
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.sources.LrclibClient
import com.mittohoa.lyra.sources.NctClient
import com.mittohoa.lyra.sources.ZingClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tim loi cho bai dang phat o app khac.
 *
 * Khac han voi tim loi cho bai trong may: o day ta khong biet chac ten bai va
 * nghe si, chi co thu app khai bao. Quy trinh:
 *
 *   1. Xem da nho san chua - nghe lai mot bai la chuyen rat thuong
 *   2. Sinh nhieu phuong an tu thu app khai bao (`candidatesFrom`)
 *   3. Voi tung phuong an, hoi CA BA NGUON CUNG LUC
 *   4. Chi nhan khi TEN TRA VE DU GIONG - hien nham loi bai khac con te hon la
 *      khong hien gi
 *   5. Uu tien ban co moc thoi gian
 */
class LyricsRepository(
    private val scope: CoroutineScope,
    private val cache: LyricCache?
) {

    private val _lyrics = MutableStateFlow(Lyrics.NONE)
    val lyrics: StateFlow<Lyrics> = _lyrics.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Bai da tra gan nhat - tranh tra lai lien tuc theo tung ban tin. */
    private var resolvedKey: String? = null
    private var job: Job? = null

    fun onNowPlaying(now: NowPlaying?) {
        val key = now?.key
        if (key == resolvedKey) return
        resolvedKey = key

        job?.cancel()

        if (now == null || now.title.isBlank()) {
            _lyrics.value = Lyrics.NONE
            _loading.value = false
            return
        }

        // Nho san thi tra ve NGAY, khong qua trang thai "dang tim" - nhap nhay
        // mot khung trong roi moi hien chu la cam giac cham nhat, du that ra
        // chi ton vai mili-giay
        cache?.get(now.artist, now.title)?.let {
            _lyrics.value = it
            _loading.value = false
            return
        }

        _lyrics.value = Lyrics.NONE
        _loading.value = true
        job = scope.launch {
            val found = resolve(now)
            _lyrics.value = found ?: Lyrics.NONE
            _loading.value = false
            if (found != null) cache?.put(now.artist, now.title, found)
        }
    }

    /**
     * Thu tung phuong an, moi phuong an hoi ca ba nguon CUNG LUC.
     *
     * Hoi lan luot thi thoi gian cho la TONG cua ba lan goi, va voi ba phuong
     * an la chin lan - nguoi dung ngoi nhin khung trong hang chuc giay. Hoi
     * song song thi thoi gian cho chi bang lan goi CHAM NHAT trong ba, va
     * thuong dung o phuong an dau.
     *
     * Van duyet theo phuong an o vong ngoai chu khong gop het chin lan goi:
     * phuong an dau (thu app tu khai bao) dung hon han, nen dang no mot ket qua
     * tot con hon mot ket qua tot cua phuong an doan mo.
     */
    private suspend fun resolve(now: NowPlaying): Lyrics? {
        val candidates = candidatesFrom(
            RawNowPlaying(
                title = now.title,
                artist = now.artist.ifBlank { null },
                album = now.album.ifBlank { null }
            )
        ).take(MAX_CANDIDATES)

        var plainFallback: Lyrics? = null

        for (c in candidates) {
            val results = coroutineScope {
                SOURCES.map { (name, fetch) ->
                    async {
                        try {
                            fetch(c.artist, c.title, now.duration)
                        } catch (e: Exception) {
                            Log.d(TAG, "$name khong tra loi duoc", e)
                            null
                        }
                    }
                }.awaitAll()
            }

            for (hit in results.filterNotNull()) {
                // Ten tra ve phai du giong. Nguon nao cung tra ve "gan dung"
                // con hon khong tra gi - ma "gan dung" o day nghia la bai khac.
                val score = titleSimilarity(hit.matchedTitle, c.title)
                if (score < MIN_SIMILARITY) {
                    Log.d(TAG, "Bo qua '${hit.matchedTitle}' - chi giong ${(score * 100).toInt()}%")
                    continue
                }

                if (hit.synced) return hit
                if (plainFallback == null) plainFallback = hit
            }
        }

        return plainFallback
    }

    private companion object {
        const val TAG = "LyraLyrics"

        /**
         * Thu tu chi con y nghia khi nhieu nguon cung tra ve ban co moc: luc do
         * ban dau danh sach thang. LRCLIB dat truoc vi no co ca nhac quoc te.
         */
        val SOURCES: List<Pair<String, suspend (String, String, Long) -> Lyrics?>> = listOf(
            "lrclib" to LrclibClient::fetch,
            "zing" to ZingClient::fetch,
            "nct" to NctClient::fetch
        )

        /** Duoi nguong nay coi nhu tra nham bai. */
        const val MIN_SIMILARITY = 0.6

        /** Chi thu vai phuong an dau - moi lan thu la ba lan goi mang. */
        const val MAX_CANDIDATES = 3
    }
}
