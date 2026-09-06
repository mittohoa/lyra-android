package com.mittohoa.lyra.download

import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import com.mittohoa.lyra.data.ThuMucNhac
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Đường ghi tệp tải về vào THƯ MỤC NGƯỜI DÙNG ĐÃ TRỎ.
 *
 * VÌ SAO PHẢI CHẠY TRÊN MÁY THẬT. Cả nhánh này đứng trên `DocumentsContract`,
 * và `DocumentsContract` không phải một thư viện — nó là một cuộc trò chuyện với
 * trình cung cấp tài liệu của hệ thống. Không có gì giả lập được trên máy tính,
 * nên trước bản 0.3.23 nhánh này chưa hề chạy một lần nào ngoài lúc viết ra.
 *
 * KHÔNG TẢI GÌ CẢ. Bài kiểm này gọi thẳng hai hàm dựng chỗ ghi, không đi qua
 * `download` — thứ cần đo là chỗ ghi có ra đúng không, còn việc kéo byte về từ
 * mạng thì nhánh MediaStore đã dùng chung và đã chạy lâu nay.
 *
 * CẦN người dùng đã trỏ ít nhất một thư mục. Chưa trỏ thì bỏ qua chứ không
 * trượt: máy chưa cấu hình không phải là mã hỏng.
 */
class TaiVaoThuMucSafTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun gocDaTro(): Uri? {
        val goc = ThuMucNhac(ctx).danhSach().firstOrNull() ?: return null
        return DocumentsContract.buildDocumentUriUsingTree(
            goc, DocumentsContract.getTreeDocumentId(goc)
        )
    }

    /**
     * Gọi hai lần phải ra ĐÚNG MỘT thư mục `Lyra`.
     *
     * Đây là lý do `timHoacTaoThuMuc` tồn tại. `createDocument` gặp tên trùng thì
     * tự đổi tên chứ không báo lỗi, nên gọi thẳng nó hai lần sẽ để lại `Lyra` và
     * `Lyra (1)` — và người dùng có một dãy thư mục gần giống nhau mà không hiểu
     * từ đâu ra.
     */
    @Test fun timHoacTaoThuMuc_goi_hai_lan_van_ra_mot_thu_muc() {
        val goc = gocDaTro()
        assumeTrue("Chua tro thu muc nao - bo qua", goc != null)

        val lan1 = Downloader.timHoacTaoThuMuc(ctx, goc!!)
        assertNotNull("Khong tao duoc thu muc ${Downloader.THU_MUC_TAI}", lan1)

        val lan2 = Downloader.timHoacTaoThuMuc(ctx, goc)
        assertEquals("Lan goi thu hai de ra mot thu muc khac", lan1, lan2)

        assertEquals(
            "Co nhieu hon mot thu muc ten ${Downloader.THU_MUC_TAI}",
            1,
            demThuMucTai(goc)
        )
    }

    /** Tệp tải về phải nằm trong `Lyra`, và mở ra ghi được thật. */
    @Test fun taoTrongThuMucDaTro_ra_tep_ghi_duoc() {
        val goc = gocDaTro()
        assumeTrue("Chua tro thu muc nao - bo qua", goc != null)

        val ten = "aura-kiem-${System.currentTimeMillis()}.mp3"
        val tep = Downloader.taoTrongThuMucDaTro(ctx, ten)
        assertNotNull("Khong tao duoc tep trong thu muc da tro", tep)

        try {
            // Ghi thật rồi đọc lại. Tạo được tài liệu mà không ghi được vào thì
            // lúc tải về mới lộ, và lúc đó là giữa chừng một lần tải.
            val than = ByteArray(1024) { (it % 251).toByte() }
            ctx.contentResolver.openOutputStream(tep!!)?.use { it.write(than) }
                ?: error("khong mo duoc tep de ghi")

            val docLai = ctx.contentResolver.openInputStream(tep)?.use { it.readBytes() }
            assertEquals("Doc lai khong ra dung so byte da ghi", than.size, docLai?.size)

            assertEquals("Tep khong nam trong ${Downloader.THU_MUC_TAI}", ten, tenTaiLieu(tep))
        } finally {
            runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, tep!!) }
        }
    }

    private fun demThuMucTai(goc: Uri): Int {
        val con = DocumentsContract.buildChildDocumentsUriUsingTree(
            goc, DocumentsContract.getDocumentId(goc)
        )
        var so = 0
        ctx.contentResolver.query(
            con,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == DocumentsContract.Document.MIME_TYPE_DIR &&
                    c.getString(0) == Downloader.THU_MUC_TAI
                ) so++
            }
        }
        return so
    }

    private fun tenTaiLieu(tep: Uri): String? = ctx.contentResolver.query(
        tep, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
    )?.use { if (it.moveToFirst()) it.getString(0) else null }
}
