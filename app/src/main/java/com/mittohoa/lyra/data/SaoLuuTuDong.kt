package com.mittohoa.lyra.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tự ghi một bản sao lưu vào thư mục người dùng chỉ, sau mỗi chừng ấy ngày.
 *
 * VÌ SAO CẦN. Màn hình Sao lưu nói thẳng "gỡ app hoặc đổi máy là mất hết", và
 * từ bản này câu ấy đúng hoàn toàn — sao lưu ngầm của Android đã tắt, xem
 * `AndroidManifest.xml`. Nhưng một lời cảnh báo không cứu được ai: nó chỉ dời
 * trách nhiệm sang người dùng, và trách nhiệm ấy là nhớ bấm một cái nút mỗi
 * tháng. Không ai nhớ. Người ta chỉ nhớ ra vào đúng lúc đã mất.
 *
 * NÓ KHÁC SAO LƯU NGẦM Ở ĐÂU. Chỗ ghi là thư mục NGƯỜI DÙNG TỰ CHỌN, thấy được
 * bằng trình quản lý tệp, chép đi đâu cũng được, xoá lúc nào cũng được. Không
 * có tài khoản nào, không có máy chủ nào. Đó là toàn bộ khác biệt giữa thứ này
 * và thứ vừa tắt đi.
 *
 * KHÔNG DÙNG `WorkManager`. Đó là cách sách vở, và nó kéo theo một thư viện
 * nữa cho một việc chạy mỗi tuần một lần. Thay vào đó: mỗi lần mở app thì ngó
 * đồng hồ, quá hạn thì ghi. Cái giá là bản sao lưu chỉ mới tới lần mở app gần
 * nhất — mà người dùng có mở app thì mới có gì mới để mà lưu, nên cái giá ấy
 * gần như bằng không.
 *
 * GIỮ NHIỀU BẢN, KHÔNG GHI ĐÈ. Ghi đè lên một tệp duy nhất nghĩa là dữ liệu
 * hỏng âm thầm — một lần trộn sai, một lần xoá nhầm — sẽ được chính việc sao
 * lưu chép đè lên bản lành cuối cùng. Giữ [SO_BAN_GIU] bản, cũ nhất rơi ra
 * trước, thì luôn còn đường lùi lại vài tuần.
 */
class SaoLuuTuDong(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("sao-luu-tu-dong", Context.MODE_PRIVATE)

    /** Thư mục đang chọn, hoặc `null` khi người dùng chưa bật. */
    fun thuMuc(): Uri? {
        val luu = prefs.getString(KEY_THU_MUC, null) ?: return null
        // Tin quyền hệ thống ĐANG giữ chứ không tin thứ đã lưu: người dùng gỡ
        // quyền trong Cài đặt, hoặc rút thẻ nhớ, là địa chỉ này chết. Cùng lẽ
        // với `ThuMucNhac.danhSach`.
        val conQuyen = app.contentResolver.persistedUriPermissions
            .any { it.uri.toString() == luu && it.isWritePermission }
        if (!conQuyen) {
            prefs.edit().remove(KEY_THU_MUC).apply()
            return null
        }
        return Uri.parse(luu)
    }

    /** Ghi nhớ thư mục vừa chọn, và giữ quyền ghi vĩnh viễn. */
    fun datThuMuc(uri: Uri) {
        app.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit().putString(KEY_THU_MUC, uri.toString()).apply()
    }

    fun tat() {
        prefs.edit().remove(KEY_THU_MUC).remove(KEY_LAN_CUOI).apply()
    }

    /** Mấy ngày ghi một lần. */
    fun soNgay(): Int = prefs.getInt(KEY_SO_NGAY, SO_NGAY_MAC_DINH)

    fun datSoNgay(n: Int) {
        prefs.edit().putInt(KEY_SO_NGAY, n.coerceIn(1, 30)).apply()
    }

    /** Mốc thời gian lần ghi gần nhất, 0 khi chưa ghi lần nào. */
    fun lanCuoi(): Long = prefs.getLong(KEY_LAN_CUOI, 0L)

    /** Kết quả một lượt ngó đồng hồ. */
    sealed interface KetQua {
        /** Chưa bật, hoặc chưa tới hạn. Không làm gì cả. */
        data object ChuaToiHan : KetQua

        data class DaGhi(val ten: String, val soByte: Int) : KetQua

        data class Hong(val vi: String) : KetQua
    }

    /**
     * Ghi nếu đã quá hạn.
     *
     * [noiDung] nhận vào một hàm chứ không phải một chuỗi: dựng nội dung sao
     * lưu là đọc cả bốn kho dữ liệu, và chưa tới hạn thì không việc gì phải
     * đọc. Gần như mọi lần gọi đều rơi vào nhánh không làm gì.
     *
     * CHẠY Ở LUỒNG NỀN — nó đụng đĩa và đụng SAF. Bên gọi lo việc đó.
     */
    fun soat(bayGio: Long = System.currentTimeMillis(), noiDung: () -> String): KetQua {
        val goc = thuMuc() ?: return KetQua.ChuaToiHan
        val han = lanCuoi() + soNgay() * MOT_NGAY_MS
        if (bayGio < han) return KetQua.ChuaToiHan
        return ghi(goc, bayGio, noiDung)
    }

    /**
     * Ghi ngay lập tức, không ngó đồng hồ.
     *
     * Có riêng một đường này để màn hình Chỉnh có nút "Ghi ngay một bản" — bật
     * một thứ tự chạy mà không thấy nó chạy lần nào thì không ai tin là nó có
     * chạy thật.
     */
    fun ghiNgay(bayGio: Long = System.currentTimeMillis(), noiDung: () -> String): KetQua {
        val goc = thuMuc() ?: return KetQua.Hong("chưa chọn thư mục")
        return ghi(goc, bayGio, noiDung)
    }

    private fun ghi(goc: Uri, bayGio: Long, noiDung: () -> String): KetQua {
        val ten = "aura-sao-luu-" + NGAY_GIO.format(Date(bayGio)) + ".txt"
        return try {
            val thuMucId = DocumentsContract.getTreeDocumentId(goc)
            val thuMucUri = DocumentsContract.buildDocumentUriUsingTree(goc, thuMucId)

            val tep = DocumentsContract.createDocument(
                app.contentResolver, thuMucUri, "text/plain", ten
            ) ?: return KetQua.Hong("không tạo được tệp trong thư mục đã chọn")

            val chu = noiDung().toByteArray()
            app.contentResolver.openOutputStream(tep, "wt")?.use { it.write(chu) }
                ?: return KetQua.Hong("không mở được tệp vừa tạo")

            prefs.edit().putLong(KEY_LAN_CUOI, bayGio).apply()
            donBanCu(thuMucUri)
            KetQua.DaGhi(ten, chu.size)
        } catch (e: Exception) {
            // KHÔNG ném lên trên. Sao lưu hỏng là chuyện đáng báo, không phải
            // chuyện đáng làm app tắt ngay lúc mở.
            Log.w(TAG, "Khong ghi duoc ban sao luu tu dong", e)
            KetQua.Hong(e.message ?: "lỗi không rõ")
        }
    }

    /**
     * Xoá bớt cho còn [SO_BAN_GIU] bản mới nhất.
     *
     * CHỈ ĐỘNG VÀO TỆP DO CHÍNH NÓ ĐẶT TÊN. Thư mục này là của người dùng, và
     * trong đó có thể có bất cứ thứ gì — kể cả bản sao lưu họ tự bấm nút lưu
     * ra, trùng tiền tố nhưng khác đuôi ngày giờ. Một vòng lặp xoá quét cả thư
     * mục là cách chắc chắn nhất để biến việc sao lưu thành việc mất dữ liệu.
     */
    private fun donBanCu(thuMucUri: Uri) {
        val con = DocumentsContract.buildChildDocumentsUriUsingTree(
            thuMucUri, DocumentsContract.getDocumentId(thuMucUri)
        )
        val cac = mutableListOf<Pair<String, String>>()   // ten -> documentId
        try {
            app.contentResolver.query(
                con,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val ten = c.getString(1) ?: continue
                    if (DUNG_TEN.matches(ten)) cac += ten to id
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Khong liet ke duoc thu muc sao luu", e)
            return
        }

        // Tên mang ngày giờ dạng yyyyMMdd-HHmm nên xếp theo chữ cái cũng là xếp
        // theo thời gian. Không phải mẹo — đó là lý do chọn dạng ngày ấy.
        cac.sortBy { it.first }
        val duXoa = cac.size - SO_BAN_GIU
        if (duXoa <= 0) return
        for ((_, id) in cac.take(duXoa)) {
            try {
                DocumentsContract.deleteDocument(
                    app.contentResolver,
                    DocumentsContract.buildDocumentUriUsingTree(thuMucUri, id)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Khong xoa duoc ban sao luu cu", e)
            }
        }
    }

    companion object {
        private const val TAG = "AuraSaoLuuTuDong"

        private const val KEY_THU_MUC = "thu-muc"
        private const val KEY_SO_NGAY = "so-ngay"
        private const val KEY_LAN_CUOI = "lan-cuoi"

        private const val MOT_NGAY_MS = 24L * 60 * 60 * 1000

        /** Mặc định mỗi tuần một bản. */
        const val SO_NGAY_MAC_DINH = 7

        /**
         * Giữ lại mấy bản.
         *
         * Năm bản với nhịp một tuần là lùi được hơn một tháng — đủ xa để bắt
         * được một lần hỏng mà cả tháng sau mới nhận ra, và mỗi tệp chỉ vài
         * chục KB nên không có lý do gì phải tiếc chỗ.
         */
        const val SO_BAN_GIU = 5

        /**
         * Có cả GIÂY, và đó không phải để cho đẹp.
         *
         * Tên chỉ tới phút thì bấm "Ghi ngay một bản" hai lần trong cùng một
         * phút là SAF tự đặt tên thành `... (1).txt` — đo được trên máy thật.
         * Cái tên ấy không khớp [DUNG_TEN] nên vòng dọn không bao giờ nhận ra
         * nó, và nó nằm lại đó mãi.
         */
        private val NGAY_GIO = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

        /**
         * Đúng dạng tên do chính lớp này đặt, không nhận thứ gì khác.
         *
         * ĐÂY LÀ LUẬT QUYẾT ĐỊNH XOÁ TỆP NÀO, nên nó phải hẹp. Thư mục này là
         * của người dùng và trong đó có thể có bất cứ thứ gì — kể cả bản sao
         * lưu họ tự bấm nút lưu ra, tên `aura-sao-luu-20260911.txt`, chỉ có
         * ngày chứ không có giờ. Nới luật ra một chút là việc sao lưu biến
         * thành việc xoá mất bản họ tự cất.
         *
         * Nhận cả ba dạng: giờ-phút (những bản ghi trước khi thêm giây),
         * giờ-phút-giây, và đuôi `(n)` do SAF tự thêm khi trùng tên. Cả ba đều
         * chắc chắn do lớp này sinh ra; bản tự lưu tay thì không dạng nào khớp
         * vì nó không có đoạn giờ.
         */
        internal val DUNG_TEN = Regex("""aura-sao-luu-\d{8}-\d{4}(\d{2})?( \(\d+\))?\.txt""")
    }
}
