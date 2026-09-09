package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Định dạng tệp sao lưu lịch sử nghe.
 *
 * Sai ở đây thì hỏng đúng lúc không được phép hỏng: người dùng đổi máy, mở tệp
 * ra khôi phục, và thứ duy nhất không dựng lại được bằng cách nào khác thì
 * không đọc được.
 */
class SaoLuuLichSuTest {

    private val mau = listOf(
        LanNghe("lyra://may/1", "Insania Circi", "Sacré Nom!", "", 1_788_879_351_258L),
        LanNghe("", "Despacito", "Luis Fonsi", "com.google.android.youtube", 1_788_879_000_000L)
    )

    @Test fun `xuat roi nhap lai ra dung nhu cu`() {
        val lai = SaoLuuLichSu.nhap(SaoLuuLichSu.xuat(mau))
        assertEquals(mau, lai)
    }

    @Test fun `tep co nhan va so phien ban o dong dau`() {
        val dong = SaoLuuLichSu.xuat(mau).split('\n')
        assertEquals("LYRA-NGHE\t1", dong[0])
    }

    @Test fun `moi lan nghe dung mot dong`() {
        // Một dòng mỗi lần nghe là điều kiện để một dòng hỏng chỉ mất đúng dòng
        // đó — khác bên lời, nơi một bài trải nhiều dòng nên phải đếm dòng.
        val dong = SaoLuuLichSu.xuat(mau).trimEnd('\n').split('\n')
        assertEquals(3, dong.size)
    }

    @Test fun `cot gio doc duoc khong lam hong viec doc lai`() {
        // Cột cuối là giờ cho người đọc, bộ đọc bỏ qua. Sửa tay cột đó thì thời
        // điểm thật vẫn phải giữ nguyên.
        val chu = SaoLuuLichSu.xuat(mau).replace("2026-", "1999-")
        assertEquals(mau.map { it.luc }, SaoLuuLichSu.nhap(chu).map { it.luc })
    }

    // ---- Tệp lạ, tệp hỏng ----

    @Test fun `tep khong phai cua AURA thi tra ve rong`() {
        assertTrue(SaoLuuLichSu.nhap("day la mot tep bat ky\nkhong phai cua app").isEmpty())
        assertTrue(SaoLuuLichSu.nhap("").isEmpty())
    }

    @Test fun `dong hong o giua khong lam mat cac dong con lai`() {
        val chu = "LYRA-NGHE\t1\n" +
            "khong-phai-so\tlyra://may/1\t\tA\tB\t\n" +
            "1000\tlyra://may/2\t\tCa Sĩ\tTên Bài\t2026-01-01 00:00\n" +
            "\n" +
            "thieu cot\n"
        val ra = SaoLuuLichSu.nhap(chu)
        assertEquals(1, ra.size)
        assertEquals("Tên Bài", ra[0].ten)
    }

    @Test fun `dong khong ten khong ca si thi bo qua`() {
        val chu = "LYRA-NGHE\t1\n1000\tlyra://may/1\t\t\t\t\n"
        assertTrue(SaoLuuLichSu.nhap(chu).isEmpty())
    }

    @Test fun `tab trong ten bai khong lam vo cot`() {
        // Tab là dấu tách cột, nên tên bài mang tab phải bị dọn lúc ghi ra —
        // không thì một bài đủ làm lệch mọi cột của chính dòng nó.
        val co = listOf(LanNghe("lyra://may/9", "Tên\tcó\ttab", "Ca\tSĩ", "", 500L))
        val lai = SaoLuuLichSu.nhap(SaoLuuLichSu.xuat(co))
        assertEquals(1, lai.size)
        assertEquals("Tên có tab", lai[0].ten)
        assertEquals("Ca Sĩ", lai[0].caSi)
        assertEquals(500L, lai[0].luc)
    }

    @Test fun `tep xuong dong kieu Windows van doc duoc`() {
        val chu = SaoLuuLichSu.xuat(mau).replace("\n", "\r\n")
        assertEquals(mau, SaoLuuLichSu.nhap(chu))
    }
}
