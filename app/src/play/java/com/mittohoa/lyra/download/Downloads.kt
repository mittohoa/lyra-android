package com.mittohoa.lyra.download

import android.content.Context
import com.mittohoa.lyra.sources.Track

/**
 * Cua duy nhat phan con lai cua app di qua de tai nhac.
 *
 * Ban PLAY: khong co.
 *
 * Chinh sach cua Google Play cam app cho tai noi dung tu dich vu phat truc
 * tuyen. Ban nay khong mang theo ma tai nao ca - `Downloader` va `Id3` nam
 * trong `src/sideload/` va khong duoc bien dich vao day.
 *
 * Ham van con de phan chung khong phai biet minh dang o ban nao. Giao dien doc
 * `SUPPORTED` va giau han nut tai di, nen tren thuc te ham nay khong bao gio
 * duoc goi - no chi ton tai de ma nguon chung con bien dich duoc.
 */
object Downloads {

    const val SUPPORTED = false

    @Suppress("UNUSED_PARAMETER")
    suspend fun download(
        context: Context,
        track: Track,
        lyrics: String?,
        onProgress: (Int) -> Unit
    ): DownloadResult = DownloadResult.Failed("Bản này không có tính năng tải xuống")
}
