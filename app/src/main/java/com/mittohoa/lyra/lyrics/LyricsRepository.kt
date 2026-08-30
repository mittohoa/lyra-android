package com.mittohoa.lyra.lyrics

import android.util.Log
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.sources.LrclibClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tim loi cho bai dang phat o app khac.
 *
 * Khac han voi tim loi cho bai trong may: o day ta khong biet chac ten bai va
 * nghe si. Quy trinh:
 *
 *   1. Sinh nhieu phuong an tu thu app khai bao (`candidatesFrom`)
 *   2. Voi tung phuong an, hoi LRCLIB
 *   3. Chi nhan khi TEN TRA VE DU GIONG - hien nham loi bai khac con te hon la
 *      khong hien gi
 *   4. Uu tien ban co moc thoi gian
 */
class LyricsRepository(private val scope: CoroutineScope) {

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
        _lyrics.value = Lyrics.NONE

        if (now == null || now.title.isBlank()) {
            _loading.value = false
            return
        }

        _loading.value = true
        job = scope.launch {
            _lyrics.value = resolve(now) ?: Lyrics.NONE
            _loading.value = false
        }
    }

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
            val hit = LrclibClient.fetch(c.artist, c.title, now.duration) ?: continue

            // Ten tra ve phai du giong, khong thi bo. LRCLIB tra ve gan dung
            // con hon khong tra gi, nhung "gan dung" o day nghia la bai khac.
            val score = titleSimilarity(hit.matchedTitle, c.title)
            if (score < MIN_SIMILARITY) {
                Log.d(TAG, "Bo qua '${hit.matchedTitle}' - chi giong ${(score * 100).toInt()}%")
                continue
            }

            if (hit.synced) return hit
            if (plainFallback == null) plainFallback = hit
        }

        return plainFallback
    }

    private companion object {
        const val TAG = "LyraLyrics"

        /** Duoi nguong nay coi nhu tra nham bai. */
        const val MIN_SIMILARITY = 0.6

        /** Chi thu vai phuong an dau - moi lan thu la mot lan goi mang. */
        const val MAX_CANDIDATES = 3
    }
}
