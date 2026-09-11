package com.mittohoa.lyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phép quyết định duy nhất của việc tự sao lưu: đã tới lúc ghi chưa.
 *
 * Sai ở đây không có gì báo. Người dùng bật tính năng lên, đọc thấy dòng chữ
 * "Đang tự lưu", rồi hàng tháng không có bản nào được ghi — và họ chỉ biết vào
 * đúng lúc cần khôi phục. Đó là kiểu hỏng mà cả tính năng này sinh ra để
 * tránh, nên chính nó phải được kiểm kỹ nhất.
 */
class ToiHanSaoLuuTest {

    private val NGAY = 24L * 60 * 60 * 1000
    private val BAY_GIO = 1_800_000_000_000L

    private fun han(lanCuoi: Long, soNgay: Int = 7, bayGio: Long = BAY_GIO) =
        SaoLuuTuDong.toiHan(lanCuoi, soNgay, bayGio)

    @Test fun `chua ghi lan nao thi ghi ngay`() {
        // Người vừa bật lên phải thấy nó chạy thật, chứ không phải chờ hết một
        // tuần rồi mới biết mình có bật đúng không.
        assertTrue(han(0L))
    }

    @Test fun `chua du so ngay thi chua toi han`() {
        assertFalse(han(BAY_GIO - 6 * NGAY))
    }

    @Test fun `du dung so ngay la toi han`() {
        assertTrue(han(BAY_GIO - 7 * NGAY))
    }

    @Test fun `qua han lau roi thi van la toi han`() {
        assertTrue(han(BAY_GIO - 90 * NGAY))
    }

    @Test fun `nhip mot ngay va muoi bon ngay deu dung`() {
        assertTrue(han(BAY_GIO - 1 * NGAY, soNgay = 1))
        assertFalse(han(BAY_GIO - 13 * NGAY, soNgay = 14))
        assertTrue(han(BAY_GIO - 14 * NGAY, soNgay = 14))
    }

    @Test fun `dong ho chay lui thi tinh la toi han`() {
        // Người dùng chỉnh tay ngày giờ, hoặc máy đồng bộ lại sau khi hết pin.
        // So thẳng "bây giờ < hạn" thì lúc ấy hạn nằm ở tương lai xa và không
        // bao giờ tới — tính năng chết lặng vĩnh viễn.
        assertTrue(han(BAY_GIO + 30 * NGAY))
        assertTrue(han(BAY_GIO + 1))
    }

    @Test fun `dung mot mili giay truoc han thi chua toi`() {
        assertFalse(han(BAY_GIO - 7 * NGAY + 1))
    }
}
