package com.mittohoa.lyra.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.mittohoa.lyra.sources.Catalog
import com.mittohoa.lyra.sources.Http
import com.mittohoa.lyra.sources.MusicSource
import com.mittohoa.lyra.sources.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Tai mot bai ve may.
 *
 * File di vao `Music/Lyra/` cua bo nho chung chu khong vao thu muc rieng cua
 * app. Hai le: nguoi dung tai ve la de SO HUU file - go AURA ra thi nhac phai
 * con, va moi trinh phat khac phai thay duoc no. Ngoai ra chinh thu vien trong
 * may cua AURA doc qua MediaStore, nen bai vua tai tu no xuat hien o do.
 *
 * Khong can quyen ghi: tu Android 10, app duoc phep CHEN file media cua chinh
 * no vao bo suu tap chung ma khong xin gi ca. Quyen ghi chi can khi muon sua
 * file cua app khac - va ta khong lam viec do.
 *
 * `is_pending` bat trong suot lan ghi. Ghi nua chung ma may sap nguon thi ban
 * ghi treo do se bi he thong don, va khong trinh phat nao nhin thay mot file
 * nhac cut duoi.
 */
object Downloader {

    /**
     * Tai `track` va nhung `lyrics` (dang .lrc) vao trong file.
     *
     * `onProgress` nhan phan tram 0..100, hoac -1 khi nguon khong noi truoc do
     * dai - luc do giao dien chi hien "dang tai" chu khong ve duoc vach chay.
     */
    suspend fun download(
        context: Context,
        track: Track,
        lyrics: String?,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        if (track.source == MusicSource.LOCAL) {
            return@withContext DownloadResult.Failed("Bài này đã có sẵn trong máy")
        }

        val url = Catalog.streamUrl(track)
            ?: return@withContext DownloadResult.Failed(
                "Nguồn không trả đường tải. Bài có thể đã bị gỡ, hoặc chỉ dành cho tài khoản trả phí."
            )

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName(track))
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.TITLE, track.title)
            put(MediaStore.Audio.Media.ARTIST, track.artist)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Lyra")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val item = try {
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(TAG, "Khong tao duoc file trong Music/Lyra", e)
            null
        } ?: return@withContext DownloadResult.Failed("Không tạo được file trong thư mục Nhạc")

        try {
            Http.client.newCall(Request.Builder().url(url).build()).execute().use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("Nguồn trả về ${res.code}")
                }
                val body = res.body ?: throw IllegalStateException("Nguồn trả về rỗng")
                val total = body.contentLength()

                resolver.openOutputStream(item)?.use { out ->
                    // The cua ta di TRUOC, roi moi toi phan nhac. Phai bo the cu
                    // cua nguon di - hai the chong len nhau thi trinh phat doc
                    // cai dau tien va bo cai sau, ma cai sau moi la cua ta.
                    out.write(Id3.tag(track.title, track.artist, null, lyrics))

                    val input = body.byteStream()
                    // Phai doc 10 byte de biet nguon co the hay khong. Neu khong
                    // co thi 10 byte ay LA NHAC - ghi lai ngay, dung nuot.
                    val leading = skipSourceTag(input)
                    out.write(leading)

                    val buffer = ByteArray(64 * 1024)
                    var written = leading.size.toLong()
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        written += read

                        if (total > 0) {
                            val percent = ((written * 100) / total).toInt().coerceIn(0, 100)
                            // Chi bao khi doi han mot phan tram: bao theo tung
                            // goi la hang nghin lan dung lai giao dien cho mot
                            // vach chay khong nhuc nhich
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        } else {
                            onProgress(-1)
                        }
                    }
                    out.flush()
                } ?: throw IllegalStateException("Không mở được file để ghi")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    item,
                    ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
            Log.i(TAG, "Da tai: '${track.artist}' - '${track.title}'")
            DownloadResult.Done(item.toString())
        } catch (e: CancellationException) {
            // Nguoi dung bo giua chung: don ban ghi treo roi nem tiep
            runCatching { resolver.delete(item, null, null) }
            throw e
        } catch (e: Exception) {
            runCatching { resolver.delete(item, null, null) }
            Log.w(TAG, "Tai that bai", e)
            DownloadResult.Failed(describe(e))
        }
    }

    /**
     * Nhay qua the ID3 cua nguon, neu co.
     *
     * Doc dung 10 byte dau de biet the dai bao nhieu, roi bo qua phan con lai.
     * Tra ve phan phai ghi lai: rong khi that su co the, va chinh 10 byte da doc
     * khi khong co - luc do chung la nhac that.
     */
    private fun skipSourceTag(input: java.io.InputStream): ByteArray {
        val head = ByteArray(10)
        var filled = 0
        while (filled < head.size) {
            val read = input.read(head, filled, head.size - filled)
            if (read < 0) break
            filled += read
        }

        val length = Id3.tagLength(head.copyOf(filled))
        // Khong phai the: tra lai phan da doc de ben goi ghi xuong. Giu no trong
        // mot bien dung chung thi vua khong an toan khi tai hai bai mot luc, vua
        // de quen ghi - va quen ghi thi file mat 10 byte dau, hong mot cach rat
        // kho lan ra.
        if (length == 0) return head.copyOf(filled)

        var toSkip = (length - filled).toLong()
        while (toSkip > 0) {
            val skipped = input.skip(toSkip)
            if (skipped <= 0) break
            toSkip -= skipped
        }
        return ByteArray(0)
    }

    private fun describe(e: Exception): String = when {
        e.message?.contains("ENOSPC") == true -> "Máy hết dung lượng"
        e is java.net.UnknownHostException -> "Không có mạng"
        e is java.net.SocketTimeoutException -> "Mạng chậm quá, thử lại sau"
        else -> "Không tải được bài này"
    }

    /**
     * Ten file, bo cac ky tu he thong tap tin khong nhan.
     *
     * Giu dau tieng Viet: he thong tap tin cua Android nhan Unicode, va mot thu
     * muc nhac toan ten khong dau thi kho doc hon han.
     */
    private fun fileName(track: Track): String {
        val raw = listOf(track.artist, track.title)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "Bài không tên" }
        return raw.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(120) + ".mp3"
    }

    private const val TAG = "AuraTai"
}
