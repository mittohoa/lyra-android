package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Định dạng tệp sao lưu gộp.
 *
 * Chỗ dễ hỏng nhất không phải phần ghi ra mà là phần ĐỌC TỆP CŨ: người dùng
 * giữ trong máy những tệp sao lưu chỉ có lời hoặc chỉ có lịch sử từ mấy bản
 * trước, và họ mở chúng ra đúng vào lúc vừa đổi máy — tức lúc không còn đường
 * lui nào.
 */
class SaoLuuTatCaTest {

    private val loi = "LYRA-LOI\t1\n===\t1\tkhoa1\tCa Sĩ\tTên Bài\n[00:01.00]một câu\n"
    private val nghe = "LYRA-NGHE\t1\n1000\tlyra://may/1\t\tCa Sĩ\tTên Bài\t3\t\n"

    // ---- Nhận đúng loại tệp ----

    @Test fun `nhan ra tep gop`() {
        val chu = SaoLuuTatCa.xuat(loi, nghe, listOf("lyra://may/1"), null)
        assertEquals(SaoLuuTatCa.Loai.TAT_CA, SaoLuuTatCa.loai(chu))
    }

    @Test fun `nhan ra tep cu chi co loi`() {
        assertEquals(SaoLuuTatCa.Loai.CHI_LOI, SaoLuuTatCa.loai(loi))
    }

    @Test fun `nhan ra tep cu chi co lich su`() {
        assertEquals(SaoLuuTatCa.Loai.CHI_NGHE, SaoLuuTatCa.loai(nghe))
    }

    @Test fun `tep la thi noi khong biet`() {
        assertEquals(SaoLuuTatCa.Loai.KHONG_BIET, SaoLuuTatCa.loai("day la mot tep bat ky\n"))
        assertEquals(SaoLuuTatCa.Loai.KHONG_BIET, SaoLuuTatCa.loai(""))
    }

    // ---- Cắt ra rồi ghép lại phải nguyên vẹn ----

    @Test fun `cat ra tung phan dung nguyen van`() {
        val chu = SaoLuuTatCa.xuat(loi, nghe, listOf("lyra://may/1", "lyra://may/2"), null)
        val phan = SaoLuuTatCa.tach(chu)

        // Từng phần phải đọc lại được bằng CHÍNH bộ đọc cũ của nó — đó là lý do
        // không dựng định dạng mới cho từng phần.
        assertEquals(1, SaoLuuLoi.nhap(phan[SaoLuuTatCa.PHAN_LOI]!!).size)
        assertEquals(1, SaoLuuLichSu.nhap(phan[SaoLuuTatCa.PHAN_NGHE]!!).size)
        assertEquals(3, SaoLuuLichSu.nhap(phan[SaoLuuTatCa.PHAN_NGHE]!!)[0].soLan)
    }

    @Test fun `phan yeu thich giu dung thu tu`() {
        val chu = SaoLuuTatCa.xuat(loi, nghe, listOf("lyra://may/9", "lyra://may/3"), null)
        val ra = SaoLuuTatCa.docThich(SaoLuuTatCa.tach(chu)[SaoLuuTatCa.PHAN_THICH]!!)
        assertEquals(listOf("lyra://may/9", "lyra://may/3"), ra)
    }

    @Test fun `khong co bai yeu thich nao van hop le`() {
        val chu = SaoLuuTatCa.xuat(loi, nghe, emptyList(), null)
        val phan = SaoLuuTatCa.tach(chu)
        assertTrue(SaoLuuTatCa.docThich(phan[SaoLuuTatCa.PHAN_THICH].orEmpty()).isEmpty())
        // Phần lời vẫn phải còn nguyên — một phần rỗng không được nuốt phần khác.
        assertEquals(1, SaoLuuLoi.nhap(phan[SaoLuuTatCa.PHAN_LOI]!!).size)
    }

    // ---- Cân bằng âm ----

    @Test fun `can bang am di qua tep con nguyen`() {
        val chu = SaoLuuTatCa.xuat(loi, nghe, emptyList(), SaoLuuTatCa.CanBang(true, 4))
        val cb = SaoLuuTatCa.docCanBang(SaoLuuTatCa.tach(chu)[SaoLuuTatCa.PHAN_CANBANG]!!)
        assertEquals(SaoLuuTatCa.CanBang(true, 4), cb)
    }

    @Test fun `may khong co can bang am thi khong ghi phan do`() {
        val chu = SaoLuuTatCa.xuat(loi, nghe, emptyList(), null)
        assertNull(SaoLuuTatCa.tach(chu)[SaoLuuTatCa.PHAN_CANBANG])
    }

    @Test fun `phan can bang hong thi tra null chu khong doan bua`() {
        // Thiếu một nửa thì không đủ để đặt gì cả. Trả `null` nghĩa là giữ
        // nguyên lựa chọn đang có trên máy — khôi phục một tệp cũ không phải
        // lý do để đổi tiếng loa của người ta.
        assertNull(SaoLuuTatCa.docCanBang("bat=1\n"))
        assertNull(SaoLuuTatCa.docCanBang("mau=2\n"))
        assertNull(SaoLuuTatCa.docCanBang(""))
    }

    // ---- Tệp không phải của app ----

    @Test fun `tach mot tep khong phai tep gop thi ra rong`() {
        assertTrue(SaoLuuTatCa.tach(loi).isEmpty())
        assertTrue(SaoLuuTatCa.tach("linh tinh").isEmpty())
    }

    @Test fun `xuong dong kieu Windows van tach duoc`() {
        val chu = SaoLuuTatCa.xuat(loi, nghe, listOf("lyra://may/1"), null)
            .replace("\n", "\r\n")
        val phan = SaoLuuTatCa.tach(chu)
        assertEquals(listOf("lyra://may/1"), SaoLuuTatCa.docThich(phan[SaoLuuTatCa.PHAN_THICH]!!))
    }
}
