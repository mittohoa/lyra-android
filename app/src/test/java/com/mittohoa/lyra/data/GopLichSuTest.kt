package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phép đếm khi khôi phục lịch sử nghe.
 *
 * Sai ở đây không làm mất dữ liệu — nó làm người dùng TƯỞNG mất. Khôi phục
 * mười chín dòng mà đa số là bài máy đã có thì tổng chỉ nhích một, và nếu câu
 * báo chỉ nói một con số thì họ đọc ra "mười tám dòng kia đi đâu".
 *
 * `KetQua` là thứ duy nhất màn hình dựa vào để nói, nên nó phải phân biệt được
 * ba việc khác nhau: thêm một bài chưa từng có, làm mới một bài đã có, và bỏ
 * qua một dòng cũ hơn thứ đang giữ.
 */
class GopLichSuTest {

    private fun ket(themMoi: Int, capNhat: Int, daCo: Int) =
        SaoLuuLichSu.KetQua(themMoi, capNhat, daCo, 0)

    @Test fun `bai chua tung co thi tinh la them moi`() {
        assertEquals(ket(2, 0, 0), ket(2, 0, 0))
        assertTrue(ket(2, 0, 0).coDoi)
    }

    @Test fun `bai da co ma dong trong tep moi hon thi tinh la lam moi`() {
        val kq = ket(0, 18, 0)
        assertEquals(18, kq.capNhat)
        assertEquals(0, kq.themMoi)
        assertTrue(kq.coDoi)
    }

    @Test fun `khong doi gi thi coDoi la false`() {
        // Cả tệp toàn dòng cũ hơn thứ máy đang giữ: không có gì để ghi đĩa, và
        // màn hình phải nói "máy đã có sẵn" chứ không phải "đã khôi phục".
        val kq = ket(0, 0, 19)
        assertFalse(kq.coDoi)
        assertEquals(19, kq.daCo)
    }

    @Test fun `tep hong dem rieng, khong lan vao ba so kia`() {
        val kq = SaoLuuLichSu.KetQua(0, 0, 0, 1)
        assertFalse(kq.coDoi)
        assertEquals(1, kq.hong)
    }

    /**
     * Đúng tình huống đã gặp thật: mười chín dòng chuyển từ một bản cài khác
     * sang, mười tám bài máy đã có, một bài chưa.
     */
    @Test fun `truong hop that muoi chin dong`() {
        val kq = ket(1, 18, 0)
        assertEquals(19, kq.themMoi + kq.capNhat)
        // Danh sách chỉ dài thêm ĐÚNG số bài mới, không phải mười chín.
        assertEquals(1, kq.themMoi)
    }
}
