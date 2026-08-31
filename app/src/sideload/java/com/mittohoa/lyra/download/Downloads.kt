package com.mittohoa.lyra.download

import android.content.Context
import com.mittohoa.lyra.sources.Track

/**
 * Cua duy nhat phan con lai cua app di qua de tai nhac.
 *
 * Ban SIDELOAD: co that.
 *
 * Toan bo phan tai nam trong `src/sideload/`, nen ban dung cho Play khong chi
 * TAT tinh nang nay - no khong duoc bien dich vao trong do. Mot cai co bat/tat
 * luc chay van de lai ma trong file cai dat, va nguoi duyet Play mo file ra
 * xem thi thay. Day la khac biet giua "khong dung" va "khong co".
 */
object Downloads {

    const val SUPPORTED = true

    suspend fun download(
        context: Context,
        track: Track,
        lyrics: String?,
        onProgress: (Int) -> Unit
    ): DownloadResult = Downloader.download(context, track, lyrics, onProgress)
}
