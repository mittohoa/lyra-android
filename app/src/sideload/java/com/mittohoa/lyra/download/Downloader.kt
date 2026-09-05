package com.mittohoa.lyra.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.mittohoa.lyra.data.ThuMucNhac
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
        val cho = chonChoGhi(context, track)
            ?: return@withContext DownloadResult.Failed("Không tạo được file để lưu bài này")
        val item = cho.dich

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

            // Chi ban ghi cua MediaStore moi co co `is_pending`. Tai lieu SAF
            // khong co thu tuong duong - bu lai bang cach xoa tep do o nhanh
            // bat loi ben duoi.
            if (cho.quaMediaStore && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            runCatching { xoaTepDo(context, cho) }
            throw e
        } catch (e: Exception) {
            runCatching { xoaTepDo(context, cho) }
            Log.w(TAG, "Tai that bai", e)
            DownloadResult.Failed(describe(e))
        }
    }

    /** Cho de ghi tep, kem cach no duoc tao ra - hai duong don dep khac nhau. */
    private class ChoGhi(val dich: Uri, val quaMediaStore: Boolean)

    /**
     * Chon cho ghi tep tai ve.
     *
     * ƯU TIÊN THƯ MỤC NGƯỜI DÙNG ĐÃ TRỎ VÀO. Từ bản 0.3.13, AURA chỉ đọc trong
     * mấy thư mục ấy — nên ghi ra `Music/Lyra` như trước là tự tải về một tệp
     * rồi tự không nhìn thấy nó. Hiện chưa lộ vì phần lớn người dùng trỏ vào
     * `Music`, mà `Music/Lyra` nằm trong đó; ai trỏ vào một thư mục con thì tải
     * xong không thấy bài đâu, và không có gì trên màn hình gợi ý tại sao.
     *
     * Đặt vào thư mục con `Lyra` chứ không đổ thẳng vào thư mục nhạc của người
     * ta: nhạc họ tự xếp và nhạc app tải về nên tách ra. Thư mục con vẫn nằm
     * trong phạm vi quét vì bộ quét đi đệ quy.
     *
     * Chưa trỏ thư mục nào thì lùi về `MediaStore` như cũ. Lúc đó thư viện rỗng
     * nên app cũng không hiện bài vừa tải — nhưng tệp vẫn nằm trong máy và mọi
     * trình phát khác đọc được, tức người dùng không mất gì.
     */
    private fun chonChoGhi(context: Context, track: Track): ChoGhi? {
        val ten = fileName(track)
        taoTrongThuMucDaTro(context, ten)?.let { return ChoGhi(it, quaMediaStore = false) }
        return taoTrongMediaStore(context, track, ten)?.let { ChoGhi(it, quaMediaStore = true) }
    }

    private fun taoTrongThuMucDaTro(context: Context, ten: String): Uri? {
        val goc = ThuMucNhac(context).danhSach().firstOrNull() ?: return null
        return try {
            val gocDoc = DocumentsContract.buildDocumentUriUsingTree(
                goc, DocumentsContract.getTreeDocumentId(goc)
            )
            val thuMuc = timHoacTaoThuMuc(context, gocDoc) ?: return null
            DocumentsContract.createDocument(context.contentResolver, thuMuc, "audio/mpeg", ten)
        } catch (e: Exception) {
            Log.w(TAG, "Khong tao duoc tep trong thu muc da tro", e)
            null
        }
    }

    /**
     * Thư mục `Lyra` trong thư mục đã trỏ — tìm trước, không có mới tạo.
     *
     * Gọi thẳng `createDocument` mà không tìm trước thì lần tải thứ hai đẻ ra
     * `Lyra (1)`, lần ba ra `Lyra (2)`: bộ cung cấp tệp gặp tên trùng là tự đổi
     * tên chứ không báo lỗi, và người dùng có một dãy thư mục gần giống nhau mà
     * không hiểu từ đâu ra.
     */
    private fun timHoacTaoThuMuc(context: Context, cha: Uri): Uri? {
        val chaId = DocumentsContract.getDocumentId(cha)
        val con = DocumentsContract.buildChildDocumentsUriUsingTree(cha, chaId)
        context.contentResolver.query(
            con,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR &&
                    c.getString(1) == THU_MUC_TAI
                ) {
                    return DocumentsContract.buildDocumentUriUsingTree(cha, c.getString(0))
                }
            }
        }
        return DocumentsContract.createDocument(
            context.contentResolver, cha, DocumentsContract.Document.MIME_TYPE_DIR, THU_MUC_TAI
        )
    }

    private fun taoTrongMediaStore(context: Context, track: Track, ten: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, ten)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.TITLE, track.title)
            put(MediaStore.Audio.Media.ARTIST, track.artist)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/$THU_MUC_TAI")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        return try {
            context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(TAG, "Khong tao duoc file trong Music/$THU_MUC_TAI", e)
            null
        }
    }

    /** Dọn tệp dở. Hai kiểu địa chỉ, hai phép xoá khác nhau. */
    private fun xoaTepDo(context: Context, cho: ChoGhi) {
        if (cho.quaMediaStore) context.contentResolver.delete(cho.dich, null, null)
        else DocumentsContract.deleteDocument(context.contentResolver, cho.dich)
    }

    private const val THU_MUC_TAI = "Lyra"

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
