package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Bộ cắt tệp sao lưu gộp thành từng phần.
 *
 * Chỗ này từng nhận BỪA mọi dòng mở đầu bằng `"=== "` là đầu một phần mới. Lời
 * bài hát là chữ người dùng tự gõ, nên một câu như thế cắt đôi tệp ngay giữa
 * phần lời: nửa sau rơi vào một phần mang tên là chính câu hát đó, và mọi thứ
 * nằm sau nó — lịch sử nghe, yêu thích, cân bằng âm — biến mất khỏi bản khôi
 * phục mà không có một dòng báo nào.
 *
 * Lộ ra khi đem một tệp do chính bên này sinh ra cho bộ đọc bên Windows đọc,
 * xem [XuatMauSaoLuuTest]. Mỗi bên tự kiểm lấy bên mình thì cả hai đều xanh
 * trong khi tệp thì hỏng.
 */
class TachTepGopTest {

    private fun tep(vararg dong: String) =
        (listOf("${SaoLuuTatCa.NHAN}\t1") + dong).joinToString("\n") + "\n"

    @Test fun `cat duoc bon phan`() {
        val phan = SaoLuuTatCa.tach(
            tep(
                "=== ${SaoLuuTatCa.PHAN_LOI}", "${SaoLuuLoi.NHAN}\t1",
                "=== ${SaoLuuTatCa.PHAN_NGHE}", "${SaoLuuLichSu.NHAN}\t1",
                "=== ${SaoLuuTatCa.PHAN_THICH}", "lyra://may/5",
                "=== ${SaoLuuTatCa.PHAN_CANBANG}", "bat=1", "mau=2"
            )
        )
        assertEquals(4, phan.size)
        assertEquals("lyra://may/5", phan[SaoLuuTatCa.PHAN_THICH]?.trim())
    }

    @Test fun `cau hat bat dau bang dau bang khong cat doi tep`() {
        val phan = SaoLuuTatCa.tach(
            tep(
                "=== ${SaoLuuTatCa.PHAN_LOI}",
                "${SaoLuuLoi.NHAN}\t1",
                "===\t2\tk\tSơn\tBài A",
                "câu một",
                "=== đây là một câu hát",
                "=== ${SaoLuuTatCa.PHAN_THICH}",
                "lyra://may/5"
            )
        )

        // Phần nằm SAU câu hát ấy vẫn tới nơi. Đây là vế quan trọng: mất nó là
        // mất im lặng, vì bản khôi phục vẫn chạy và vẫn báo thành công.
        assertEquals("lyra://may/5", phan[SaoLuuTatCa.PHAN_THICH]?.trim())
        assertFalse("đây là một câu hát" in phan)

        // Và câu hát ấy vẫn nằm trong lời, đúng chỗ của nó.
        val loi = SaoLuuLoi.nhap(phan[SaoLuuTatCa.PHAN_LOI].orEmpty())
        assertEquals(1, loi.size)
        assertEquals("câu một\n=== đây là một câu hát", loi[0].loi)
    }

    @Test fun `tep khong phai cua AURA thi khong cat gi`() {
        assertEquals(emptyMap<String, String>(), SaoLuuTatCa.tach("một tệp bất kỳ\nnội dung"))
    }
}
