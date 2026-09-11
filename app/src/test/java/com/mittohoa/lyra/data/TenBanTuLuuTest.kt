package com.mittohoa.lyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Luật nhận dạng tên bản tự lưu.
 *
 * ĐÂY LÀ LUẬT QUYẾT ĐỊNH XOÁ TỆP NÀO. Thư mục tự lưu là thư mục của người
 * dùng, và trong đó có thể có bất cứ thứ gì — kể cả bản sao lưu họ tự bấm nút
 * lưu ra. Nới luật ra một chút là chính việc sao lưu trở thành việc mất dữ
 * liệu, và mất theo kiểu không ai ngờ tới nhất.
 *
 * Nên mỗi dạng tên PHẢI khớp có một bài kiểm, và mỗi dạng KHÔNG ĐƯỢC khớp
 * cũng có một bài kiểm.
 */
class TenBanTuLuuTest {

    private fun khop(ten: String) = SaoLuuTuDong.DUNG_TEN.matches(ten)

    @Test fun `nhan dang co giay`() {
        assertTrue(khop("aura-sao-luu-20260911-170512.txt"))
    }

    @Test fun `nhan dang cu chi co gio phut`() {
        // Những bản ghi ra trước khi thêm giây vẫn phải dọn được, nếu không
        // chúng nằm lại mãi và số bản giữ cứ thế phình ra.
        assertTrue(khop("aura-sao-luu-20260911-1705.txt"))
    }

    @Test fun `nhan duoi (n) do SAF tu them khi trung ten`() {
        // Đo được trên máy thật: bấm "Ghi ngay" hai lần trong cùng một phút
        // thì SAF đặt tên thành "... (1).txt".
        assertTrue(khop("aura-sao-luu-20260911-1705 (1).txt"))
        assertTrue(khop("aura-sao-luu-20260911-170512 (2).txt"))
    }

    @Test fun `KHONG nhan ban nguoi dung tu bam nut luu ra`() {
        // `SaoLuuTatCaMuc` đặt tên bản lưu tay là "aura-sao-luu-<ngày>.txt" —
        // chỉ có ngày, không có đoạn giờ. Xoá nhầm nó là xoá đúng cái bản mà
        // người dùng chủ động cất đi.
        assertFalse(khop("aura-sao-luu-20260911.txt"))
    }

    @Test fun `KHONG nhan tep nguoi dung tu dat ten`() {
        assertFalse(khop("aura-sao-luu-quan-trong.txt"))
        assertFalse(khop("ban sao aura-sao-luu-20260911-1705.txt"))
        assertFalse(khop("aura-sao-luu-20260911-1705.txt.bak"))
        assertFalse(khop("nhac.mp3"))
        assertFalse(khop("aura-nghe-20260908.txt"))
    }

    @Test fun `KHONG nhan doan gio thieu hoac thua chu so`() {
        assertFalse(khop("aura-sao-luu-20260911-17.txt"))
        assertFalse(khop("aura-sao-luu-20260911-1705123.txt"))
        assertFalse(khop("aura-sao-luu-2026091-1705.txt"))
    }
}
