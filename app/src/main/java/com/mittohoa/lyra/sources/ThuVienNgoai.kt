package com.mittohoa.lyra.sources

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import android.util.Log
import com.mittohoa.lyra.data.ThuMucNhac
import com.mittohoa.lyra.lyrics.normalizeForCompare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.CollationKey
import java.text.Collator
import java.util.Locale

/**
 * Nhạc trong những thư mục người dùng tự trỏ vào.
 *
 * Song song với `LocalLibrary` chứ không thay nó. `LocalLibrary` đọc danh mục
 * hệ thống — nhanh, luôn cập nhật, không tốn gì; đây là đường dự phòng cho
 * đúng những tệp danh mục ấy bỏ sót (xem `ThuMucNhac`).
 *
 * ĐỌC THẺ BẰNG `MediaMetadataRetriever`, TỪNG TỆP MỘT. Đắt hơn hẳn một câu
 * truy vấn `MediaStore`, nhưng không còn đường nào: `DocumentsContract` chỉ
 * biết tên tệp, cỡ tệp và kiểu MIME — nó là danh mục THƯ MỤC, không phải danh
 * mục NHẠC, nên không có chỗ nào chứa tên bài hay nghệ sĩ. Chạy trên luồng nền
 * và chỉ mỗi lần nạp lại thư viện, nên cái giá ấy trả một lần.
 */
object ThuVienNgoai {

    /**
     * Thẻ đã đọc, nhớ theo VÂN TAY của tệp chứ không theo địa chỉ.
     *
     * Vì sao cần: `loadLibrary` được gọi lại nhiều lần — mở app, vừa cấp
     * quyền, quay lại sau khi chép thêm nhạc — và màn hình Cài đặt cũng đếm
     * bài. Không nhớ gì thì mỗi lần ấy là một vòng `MediaMetadataRetriever`
     * trên toàn bộ thư mục, tức vài giây đứng hình cho một kết quả y hệt lần
     * trước.
     *
     * Vân tay gồm cỡ tệp và giờ sửa, nên tệp bị thay bằng bản khác cùng tên
     * vẫn được đọc lại. Nhớ theo địa chỉ không thôi thì sửa tệp xong app vẫn
     * hiện thẻ cũ mãi.
     *
     * Chỉ nằm trong bộ nhớ: tắt app là mất, và như thế là đủ. Cái cần chữa là
     * việc quét lại nhiều lần TRONG một phiên.
     */
    private val nho = object : LinkedHashMap<String, Track>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Track>?) =
            size > TRAN_SO_BAI * 2
    }

    /**
     * Đọc hết các thư mục đã trỏ.
     *
     * Không bao giờ ném: một thư mục hỏng — thẻ nhớ rút ra, trình cung cấp tệp
     * của bên thứ ba trả về rác — chỉ làm mất đúng thư mục đó, chứ không được
     * phép làm trắng cả thư viện.
     */
    suspend fun tatCa(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val kho = ThuMucNhac(context)
        val ra = ArrayList<Track>(64)

        for (goc in kho.danhSach()) {
            try {
                quet(context, goc, DocumentsContract.getTreeDocumentId(goc), ra, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Khong doc duoc thu muc $goc", e)
            }
            if (ra.size >= TRAN_SO_BAI) break
        }
        ra
    }

    /**
     * Đi hết một nhánh thư mục.
     *
     * Đệ quy có TRẦN ĐỘ SÂU và TRẦN SỐ BÀI. Người dùng hoàn toàn có thể trỏ
     * vào gốc thẻ nhớ, và ở đó có những thư mục lồng nhau rất sâu hoặc rất
     * rộng; không có trần thì màn hình Cài đặt đứng im mà không ai hiểu vì sao.
     */
    private fun quet(
        context: Context,
        goc: Uri,
        docId: String,
        ra: MutableList<Track>,
        doSau: Int
    ) {
        if (doSau > TRAN_DO_SAU || ra.size >= TRAN_SO_BAI) return

        val con = DocumentsContract.buildChildDocumentsUriUsingTree(goc, docId)
        context.contentResolver.query(
            con,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                // Hai cột này KHÔNG để hiển thị - chúng là vân tay của tệp,
                // dùng để biết bản đã nhớ còn dùng được không. Xem `nho`.
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null, null, null
        )?.use { c ->
            val idCot = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val tenCot = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCot = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val coCot = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val doiCot = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            // Gom thư mục con lại rồi đi sau, KHÔNG đệ quy giữa lúc con trỏ
            // đang mở: mỗi nhánh sâu giữ thêm một con trỏ, và trỏ vào gốc thẻ
            // nhớ thì đủ chạm trần con trỏ của hệ thống.
            val nhanhCon = ArrayList<String>(8)

            while (c.moveToNext()) {
                if (ra.size >= TRAN_SO_BAI) break
                val id = c.getString(idCot) ?: continue
                val ten = c.getString(tenCot).orEmpty()
                val mime = c.getString(mimeCot).orEmpty()

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    nhanhCon.add(id)
                    continue
                }
                val loaiBai = loai(ten, mime) ?: continue

                // Đi qua bộ nhớ đệm trước. Việc duyệt thư mục thì rẻ, còn mở
                // từng tệp ra đọc thẻ mới là chỗ tốn - nên chỗ tốn ấy chỉ trả
                // giá một lần cho mỗi tệp, không phải mỗi lần nạp thư viện.
                val khoa = vanTay(id, c.getLongAn(coCot), c.getLongAn(doiCot))
                val daNho = synchronized(nho) { nho[khoa] }
                if (daNho != null) {
                    ra.add(daNho)
                    continue
                }

                val bai = docMotBai(
                    context, DocumentsContract.buildDocumentUriUsingTree(goc, id), ten, loaiBai
                )
                if (bai != null) {
                    synchronized(nho) { nho[khoa] = bai }
                    ra.add(bai)
                }
            }

            for (id in nhanhCon) quet(context, goc, id, ra, doSau + 1)
        }
    }

    /**
     * Tệp này có phải nhạc không.
     *
     * Xét CẢ kiểu MIME lẫn đuôi tệp. Chỉ xét MIME thì hụt mất kha khá: nhiều
     * trình cung cấp tệp trả về `application/octet-stream` cho mọi thứ chúng
     * không chắc, mà đó đúng là loại tệp MediaStore cũng đã bỏ sót — tức đúng
     * thứ người dùng trỏ vào đây để tìm.
     */
    private fun loai(ten: String, mime: String): MediaKind? {
        if (mime.startsWith("audio/")) return MediaKind.AUDIO
        if (mime.startsWith("video/")) return MediaKind.VIDEO
        if (mime.startsWith("image/") || mime.startsWith("text/")) return null
        val duoi = ten.substringAfterLast('.', "").lowercase()
        return when (duoi) {
            in DUOI_NHAC -> MediaKind.AUDIO
            in DUOI_PHIM -> MediaKind.VIDEO
            else -> null
        }
    }

    /**
     * Một bài, đọc từ thẻ trong tệp.
     *
     * Trả null khi không mở được — tệp có thể mang đuôi nhạc mà bên trong
     * không phải nhạc, hoặc là bản tải dở. Bỏ qua còn hơn xếp vào thư viện một
     * dòng bấm phát không kêu.
     */
    private fun docMotBai(
        context: Context,
        uri: Uri,
        tenTep: String,
        loaiDoan: MediaKind
    ): Track? {
        val doc = MediaMetadataRetriever()
        return try {
            doc.setDataSource(context, uri)

            val thoiLuong = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            // Không có độ dài nghĩa là bộ giải mã không thấy luồng nhạc nào.
            // Đuôi tệp nói dối, hoặc tệp hỏng.
            if (thoiLuong <= 0L) return null

            val rong = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val cao = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val xoay = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            // Đuôi tệp mới là ĐOÁN, khung hình mới là BẰNG CHỨNG. Một tệp
            // `.mp4` chỉ chứa nhạc là chuyện rất thường (nhạc mua trên mạng hay
            // ở dạng đó), và xếp nó vào phim thì người dùng bấm phát rồi nhìn
            // một khung đen.
            val loaiThat =
                if (loaiDoan == MediaKind.VIDEO && rong > 0 && cao > 0) MediaKind.VIDEO
                else MediaKind.AUDIO

            val ten = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotEmpty() }
            // Thẻ trống thì lấy tên tệp đã bỏ đuôi — đúng thứ người dùng nhìn
            // thấy trong trình quản lý tệp, nên tìm theo nó là ra.
                ?: tenTep.substringBeforeLast('.')

            Track(
                id = maHoaDiaChi(uri),
                source = MusicSource.LOCAL,
                title = ten,
                artist = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.trim().orEmpty(),
                album = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.trim().orEmpty(),
                // Ten thu muc cat tu chinh ma tai lieu. Khong hoi lai trinh
                // cung cap: mot cau truy van nua cho MOI tep, chi de lay mot
                // cai nhan, la cai gia khong dang.
                thuMuc = LocalLibrary.tenThuMuc(
                    runCatching { DocumentsContract.getDocumentId(uri).substringAfter(':', "") }
                        .getOrDefault("")
                ),
                // The ghi so thu tu kieu "3" hoac "3/12" - lay phan truoc dau
                // gach. Khong tach thi `toIntOrNull` tra null cho MOI tep cua
                // nhung dia co ghi tong so bai, tuc dung nhung dia duoc ghi the
                // can than nhat lai la nhung dia mat thu tu.
                soThuTu = doc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.substringBefore('/')?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                // Ảnh đại diện nằm trong CHÍNH tệp, không phải một tệp ảnh
                // riêng: nhạc thì là bìa trong thẻ, phim thì là một khung hình.
                // Đánh dấu bằng tiền tố để `Artwork` đi thẳng đường đúng, khỏi
                // nạp cả chục MB vào bộ nhớ rồi mới biết đó không phải ảnh.
                artworkUrl =
                    (if (loaiThat == MediaKind.VIDEO) KHUNG_TRONG_TEP else BIA_TRONG_THE) + uri,
                durationMs = thoiLuong,
                streamUrl = uri.toString(),
                kind = loaiThat,
                // Dùng lại đúng phép tính của thư viện hệ thống, kể cả chỗ bù
                // góc xoay - video quay dọc vẫn ghi khung ngang kèm cờ xoay.
                tiLe = if (loaiThat == MediaKind.VIDEO)
                    LocalLibrary.tiLeKhungHinh(rong, cao, xoay) else null
            )
        } catch (e: Exception) {
            Log.d(TAG, "Bo qua $tenTep", e)
            null
        } finally {
            try {
                doc.release()
            } catch (e: Exception) {
                // release() có ném trên vài máy. Không còn gì để làm tiếp.
                Log.d(TAG, "release() nem", e)
            }
        }
    }

    /**
     * Gộp thư viện hệ thống với thư viện tự trỏ.
     *
     * PHẢI CHỐNG TRÙNG. Người dùng rất dễ trỏ vào đúng một thư mục MediaStore
     * đã quét — `Music/` chẳng hạn — vì họ đâu biết trước thư mục nào bị bỏ
     * sót; cách duy nhất để biết là trỏ vào rồi xem. Không chống trùng thì cái
     * giá của việc thử là cả thư viện nhân đôi.
     *
     * Khoá trùng gồm CẢ độ dài: một bài và bản phối lại của nó thường trùng cả
     * tên lẫn nghệ sĩ, chỉ khác độ dài. Làm tròn xuống giây vì `MediaStore` và
     * `MediaMetadataRetriever` chênh nhau vài mili-giây trên cùng một tệp.
     *
     * Nhạc trước rồi mới tới phim, đúng thứ tự `LocalLibrary` vẫn trả — nối vào
     * cuối là đẩy nhạc xuống sau phim.
     *
     * XẾP LẠI CẢ DANH SÁCH, kể cả khi không có thư mục tự trỏ nào. Hai nguồn
     * vốn xếp theo hai luật khác nhau: `LocalLibrary` để `COLLATE NOCASE` của
     * SQLite lo, mà luật ấy chỉ biết gập hoa-thường của bảng chữ ASCII nên mọi
     * tên bắt đầu bằng chữ có dấu đều bị dồn xuống sau chữ `z`. Trộn hai luật
     * vào một danh sách thì thứ tự nhìn như ngẫu nhiên. Một luật duy nhất, áp
     * cho tất cả, là cách duy nhất để chữ `Á` nằm cạnh chữ `A`.
     */
    fun gop(trongMay: List<Track>, ngoai: List<Track>): List<Track> {
        val daCo = trongMay.mapTo(HashSet(trongMay.size)) { khoaTrung(it) }
        val them = ngoai.filterNot { khoaTrung(it) in daCo }
        val tatCa = if (them.isEmpty()) trongMay else trongMay + them
        return xepTheoTen(tatCa.filter { it.kind == MediaKind.AUDIO }) +
            xepTheoTen(tatCa.filter { it.kind == MediaKind.VIDEO })
    }

    /**
     * Xếp theo tên bài, theo luật so chữ tiếng Việt.
     *
     * Dựng `CollationKey` một lần cho mỗi bài rồi mới xếp, chứ không gọi
     * `Collator.compare` trong lúc so: một lần xếp chạm mỗi phần tử chừng
     * `log n` lần, mà mỗi lần so bằng `Collator` là một lượt phân tích chuỗi
     * lại từ đầu. Dựng khoá trước là đổi việc đó lấy đúng một lượt cho mỗi bài.
     */
    private fun xepTheoTen(bai: List<Track>): List<Track> {
        if (bai.size < 2) return bai
        val luat = Collator.getInstance(Locale.forLanguageTag("vi-VN"))
        // Nhom truoc, roi moi toi ten bai TRONG nhom. Man hinh chen mot dong
        // tieu de moi lan doi nhom, nen thu tu o day phai la thu tu hien ra -
        // xep theo ten bai roi gom nhom sau thi cung mot album se hien thanh
        // may cum roi rac, moi cum mot tieu de.
        // Khoa nhom KHONG PHAN BIET HOA THUONG. Do tren may that: cung mot
        // album ma cac tep ghi the khac nhau mot chu - "Touch Of Light" ba bai,
        // "Touch of Light" mot bai - va xep theo chu nguyen ban thi album ay bi
        // cat lam doi, moi nua mot tieu de. Nguoi dung khong the biet vi sao.
        //
        // Trong mot nhom thi SO THU TU di truoc ten bai. Mot dia nhac co thu tu
        // cua no, va thu tu ay la mot phan cua tac pham - xep theo bang chu cai
        // thi bai mo dau nam giua, bai ket nam dau.
        //
        // Tep khong ghi so thu tu bi day xuong CUOI nhom chu khong len dau: mot
        // album danh so day du kem vai tep le (bonus, ban thu) thi phan danh so
        // phai lien mach, con may tep le doi o duoi.
        return bai.map {
            Bo(
                luat.getCollationKey(it.nhom.lowercase()),
                if (it.soThuTu > 0) it.soThuTu else Int.MAX_VALUE,
                luat.getCollationKey(it.title),
                it
            )
        }
            .sortedWith(compareBy({ it.nhomKhoa }, { it.thuTu }, { it.tenKhoa }))
            .map { it.bai }
    }

    /** Vân tay của một tệp: đổi tệp thì đổi khoá, và thẻ được đọc lại. */
    private fun vanTay(docId: String, co: Long, doi: Long): String = "$docId|$co|$doi"

    /**
     * Đọc một cột số có thể KHÔNG TỒN TẠI.
     *
     * `COLUMN_SIZE` và `COLUMN_LAST_MODIFIED` là tuỳ chọn trong hợp đồng của
     * `DocumentsContract` — trình cung cấp tệp có quyền không trả. Dùng
     * `getColumnIndexOrThrow` ở đó là làm sập cả lần quét vì một cột trang trí.
     * Thiếu thì vân tay còn mỗi mã tài liệu: đệm vẫn chạy, chỉ kém nhạy khi tệp
     * bị sửa.
     */
    private fun android.database.Cursor.getLongAn(cot: Int): Long =
        if (cot < 0 || isNull(cot)) 0L else getLong(cot)

    /**
     * Ba khoá xếp của một bài, dựng sẵn một lần.
     *
     * Gói lại thành lớp riêng thay vì lồng `Triple` trong `Triple`: ba khoá thì
     * `Triple` còn đọc được, thêm cái thứ tư là thành `first.second` với
     * `second.first`, và chỗ đó sai thì thư viện xếp lộn mà không ai thấy sai ở
     * đâu.
     */
    private class Bo(
        val nhomKhoa: CollationKey,
        val thuTu: Int,
        val tenKhoa: CollationKey,
        val bai: Track
    )

    /** So không dấu, không phân biệt hoa thường — như mọi chỗ so tên bài khác. */
    private fun khoaTrung(t: Track): String = buildString {
        append(normalizeForCompare(t.title)).append('|')
        append(normalizeForCompare(t.artist)).append('|')
        append(t.durationMs / 1000)
    }

    /**
     * Gói địa chỉ tài liệu thành một MÃ nằm lọt trong một đoạn đường dẫn.
     *
     * Bắt buộc, và đây là lý do: mọi bài vào hàng đợi dưới dạng
     * `lyra://may/<mã>` (xem `Track.playbackUri`), rồi được giải ra bằng
     * `uri.lastPathSegment`. Một địa chỉ `content://` có sẵn dấu gạch chéo bên
     * trong, nên nhét thẳng vào đó là vỡ — bài phát được ngay lúc bấm nhưng
     * chết khi mở lại từ danh sách phát đã lưu.
     *
     * Base64 kiểu URL, không đệm, không xuống dòng: chỉ còn chữ cái, chữ số,
     * `-` và `_` — an toàn trong một đoạn đường dẫn.
     */
    fun maHoaDiaChi(uri: Uri): String =
        TIEN_TO + Base64.encodeToString(
            uri.toString().toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

    /**
     * Địa chỉ tài liệu của một bài, hoặc null khi bài không đến từ thư mục tự trỏ.
     *
     * Nhận cả hai dạng đang lưu hành trong app: địa chỉ hàng đợi
     * `lyra://may/saf-…` (thứ mọi nơi thấy được), và địa chỉ tài liệu trần
     * (thứ nằm trong `Track.streamUrl`).
     */
    fun diaChiTuMa(chuoi: String): Uri? {
        if (chuoi.startsWith(TIEN_TO)) return giaiMaDiaChi(chuoi)?.let(Uri::parse)
        val u = try {
            Uri.parse(chuoi)
        } catch (e: Exception) {
            return null
        }
        if (u.scheme == "lyra") {
            val ma = u.lastPathSegment ?: return null
            return giaiMaDiaChi(ma)?.let(Uri::parse)
        }
        // Địa chỉ tài liệu do bộ chọn thư mục sinh ra luôn có đoạn `/tree/`.
        // Thiếu nó thì đây là `content://` của MediaStore, đường khác lo.
        if (u.scheme == "content" && u.path?.contains("/tree/") == true) return u
        return null
    }

    /**
     * Một tài liệu KHÁC nằm cùng thư mục, đặt tên theo `doiTen`.
     *
     * Dùng để tìm tệp `.lrc` nằm cạnh bài hát. Với tệp trong máy thì việc này
     * chỉ là đổi đuôi trên một đường dẫn; ở đây không có đường dẫn nào, chỉ có
     * mã tài liệu — nhưng mã ấy có cấu trúc `primary:Music/Album/bài.mp3`, nên
     * cắt ở dấu phân cách cuối là tách được thư mục khỏi tên tệp.
     *
     * KHÔNG kiểm tệp có tồn tại không: `DocumentsContract` không có phép thử
     * nào rẻ hơn việc mở thẳng ra đọc. Bên gọi mở, hụt thì đi tiếp.
     */
    fun anhEm(tep: Uri, doiTen: (String) -> String): Uri? = try {
        val id = DocumentsContract.getDocumentId(tep)
        // Ranh giới thư mục / tên tệp là dấu `/` cuối, hoặc dấu `:` của tên ổ
        // khi tệp nằm ngay gốc (`primary:bài.mp3`).
        val cat = maxOf(id.lastIndexOf('/'), id.lastIndexOf(':'))
        val ten = if (cat < 0) id else id.substring(cat + 1)
        if (ten.isEmpty()) null
        else DocumentsContract.buildDocumentUriUsingTree(
            tep,
            (if (cat < 0) "" else id.substring(0, cat + 1)) + doiTen(ten)
        )
    } catch (e: Exception) {
        Log.d(TAG, "Khong dung duoc dia chi anh em cua $tep", e)
        null
    }

    /**
     * Đường dẫn thật của một thư mục đã được trao quyền.
     *
     * Cần để LỌC danh mục hệ thống: `MediaStore` không biết gì về địa chỉ tài
     * liệu, nó chỉ biết đường dẫn. Muốn nói "chỉ lấy nhạc trong mấy thư mục
     * này" thì phải dịch được từ cái người dùng chọn sang cái MediaStore hiểu.
     *
     * Mã cây có dạng `<ổ>:<đường>`, và hai ổ quy về hai gốc:
     *
     *     primary:Music/Viet   ->  /storage/emulated/0/Music/Viet
     *     1A2B-3C4D:Nhac       ->  /storage/1A2B-3C4D/Nhac
     *
     * KHÔNG có API chính thức nào cho phép dịch này, nhưng đây là hình dạng mà
     * trình cung cấp tệp của hệ thống dùng suốt từ Android 5 tới nay. Trình
     * cung cấp của bên thứ ba có thể khác — lúc đó trả về đường dẫn không khớp
     * gì cả, danh mục hệ thống lọc ra rỗng, và đường quét thẳng thư mục vẫn
     * gánh được. Sai ở đây làm thư viện thiếu, không làm nó sai.
     */
    fun duongTuyetDoi(goc: Uri): String? = try {
        val id = DocumentsContract.getTreeDocumentId(goc)
        val o = id.substringBefore(':')
        val duong = id.substringAfter(':', "")
        val re = if (o.equals("primary", true)) "/storage/emulated/0" else "/storage/$o"
        if (duong.isEmpty()) re else "$re/${duong.trimEnd('/')}"
    } catch (e: Exception) {
        Log.d(TAG, "Khong doi duoc $goc sang duong dan", e)
        null
    }

    /** Tên tệp của một tài liệu, lấy từ mã chứ không hỏi lại trình cung cấp. */
    fun tenTaiLieu(tep: Uri): String? = try {
        val id = DocumentsContract.getDocumentId(tep)
        id.substringAfterLast('/').substringAfterLast(':').takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    /**
     * Thư mục chứa một tài liệu — chỗ để TẠO tệp mới nằm cạnh nó.
     *
     * `DocumentsContract.createDocument` đòi địa chỉ của thư mục cha, mà từ một
     * tài liệu thì không có phép hỏi ngược nào. Cắt mã ra là đường duy nhất.
     *
     * Trả null khi tài liệu nằm ngay gốc cây đã cấp quyền: lúc đó chính địa chỉ
     * cây là cha, và bên gọi có sẵn nó.
     */
    fun thuMucCha(tep: Uri): Uri? = try {
        val id = DocumentsContract.getDocumentId(tep)
        val cat = id.lastIndexOf('/')
        if (cat <= 0) DocumentsContract.buildDocumentUriUsingTree(
            tep, DocumentsContract.getTreeDocumentId(tep)
        )
        else DocumentsContract.buildDocumentUriUsingTree(tep, id.substring(0, cat))
    } catch (e: Exception) {
        Log.d(TAG, "Khong lay duoc thu muc cha cua $tep", e)
        null
    }

    /** Tài liệu này có thật không. Dùng trước khi tạo, để khỏi đè nhầm. */
    fun coThat(context: Context, tep: Uri): Boolean = try {
        context.contentResolver.query(
            tep,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null, null, null
        )?.use { it.count > 0 } ?: false
    } catch (e: Exception) {
        false
    }

    /** Giải mã ngược. Trả null khi mã không phải của thư mục ngoài. */
    fun giaiMaDiaChi(ma: String): String? {
        if (!ma.startsWith(TIEN_TO)) return null
        return try {
            String(
                Base64.decode(
                    ma.removePrefix(TIEN_TO),
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )
            )
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Ma thu muc ngoai hong: $ma", e)
            null
        }
    }

    private val DUOI_NHAC = setOf(
        "mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "mka", "aiff", "alac"
    )

    /**
     * Đuôi phim.
     *
     * `mp4` và `m4v` nằm đây dù chúng chứa được cả tệp chỉ có nhạc — đây mới là
     * bước ĐOÁN, còn `docMotBai` xem khung hình rồi mới chốt. Đoán nhầm sang
     * phim thì được sửa lại; bỏ sót ngay từ đây thì tệp biến mất luôn.
     */
    private val DUOI_PHIM = setOf(
        "mp4", "m4v", "mkv", "webm", "avi", "mov", "3gp", "flv", "ts", "mpg", "mpeg", "wmv"
    )

    /**
     * Trần số bài. Trỏ vào gốc một thẻ nhớ lớn là chuyện có thật, và quét hết
     * nó bằng `MediaMetadataRetriever` mất hàng phút.
     */
    private const val TRAN_SO_BAI = 2000
    private const val TRAN_DO_SAU = 8
    private const val TIEN_TO = "saf-"

    /**
     * Tiền tố báo cho `Artwork` biết bìa nằm trong thẻ của tệp nhạc.
     *
     * Nhạc trong máy đọc qua MediaStore có bìa riêng ở
     * `content://media/external/audio/albumart/<mã>`; tệp đọc thẳng qua thư mục
     * thì không có địa chỉ nào như thế, bìa chỉ nằm trong thẻ.
     */
    const val BIA_TRONG_THE = "the-trong-tep:"

    /**
     * Tiền tố báo cho `Artwork` biết phải rút MỘT KHUNG HÌNH ra làm ảnh đại diện.
     *
     * Tách khỏi `BIA_TRONG_THE` chứ không để `Artwork` thử rồi bắt trượt: thử
     * kiểu đó thì mọi bìa album hỏng đều bị coi là video, và mọi video đều tốn
     * một lần giải mã thất bại trước khi đi đúng đường — đúng cái bẫy mà
     * `laVideo` trong `Artwork` đã ghi lại.
     */
    const val KHUNG_TRONG_TEP = "khung-trong-tep:"
    private const val TAG = "AuraThuVienNgoai"
}
