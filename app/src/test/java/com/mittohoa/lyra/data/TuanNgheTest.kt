package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phép tính "bảy ngày qua".
 *
 * Chỗ dễ sai nhất là RANH GIỚI: lệch một mili-giây ở mốc bảy ngày thì không có
 * gì báo, con số chỉ hơi khác, và không ai đối chiếu được vì chẳng ai nhớ tuần
 * trước mình nghe đúng bao nhiêu bài.
 */
class TuanNgheTest {

    private val BAY_GIO = 1_800_000_000_000L
    private val NGAY = TuanNghe.MOT_NGAY_MS

    private fun bai(ten: String, caSi: String, truocMayNgay: Double, app: String = "") =
        LanNghe(
            diaChi = if (app.isEmpty()) "lyra://may/$ten" else "",
            ten = ten,
            caSi = caSi,
            app = app,
            luc = BAY_GIO - (truocMayNgay * NGAY).toLong()
        )

    @Test fun `chi dem bai trong bay ngay qua`() {
        val t = TuanNghe.tinh(
            listOf(
                bai("A", "X", 1.0),
                bai("B", "X", 6.9),
                bai("C", "Y", 7.5),      // ngoài tuần
                bai("D", "Y", 20.0)      // ngoài hẳn
            ),
            BAY_GIO
        )
        assertEquals(2, t.soBai)
    }

    @Test fun `dong dung mep bay ngay van tinh vao tuan nay`() {
        // Đúng mốc bảy ngày là còn trong tuần, không phải rơi ra ngoài. Cắt
        // bằng `>=` chứ không `>` — sai chỗ này thì mỗi tuần mất đúng một
        // khoảnh khắc, và nó chỉ lộ ra khi có dòng rơi trúng.
        val t = TuanNghe.tinh(listOf(bai("A", "X", 7.0)), BAY_GIO)
        assertEquals(1, t.soBai)
    }

    @Test fun `dong khong co moc gio khong tinh vao dau ca`() {
        val t = TuanNghe.tinh(
            listOf(LanNghe(diaChi = "lyra://may/A", ten = "A", caSi = "X", luc = 0L)),
            BAY_GIO
        )
        assertEquals(0, t.soBai)
        assertEquals(0, t.tuanTruoc)
        assertTrue(t.trong)
    }

    @Test fun `tuan truoc dem rieng va khong trung voi tuan nay`() {
        val t = TuanNghe.tinh(
            listOf(
                bai("A", "X", 2.0),
                bai("B", "X", 9.0),
                bai("C", "X", 13.9),
                bai("D", "X", 15.0)      // quá hai tuần
            ),
            BAY_GIO
        )
        assertEquals(1, t.soBai)
        assertEquals(2, t.tuanTruoc)
        assertEquals(-1, t.lech)
    }

    @Test fun `tuan truoc trong thi khong so sanh`() {
        val t = TuanNghe.tinh(listOf(bai("A", "X", 1.0)), BAY_GIO)
        assertEquals(0, t.tuanTruoc)
        assertNull(t.lech)
    }

    @Test fun `tach duoc nhac app khac voi nhac AURA tu phat`() {
        val t = TuanNghe.tinh(
            listOf(
                bai("A", "X", 1.0),
                bai("B", "X", 2.0, app = "com.zing.mp3"),
                bai("C", "X", 3.0, app = "com.google.android.youtube")
            ),
            BAY_GIO
        )
        assertEquals(3, t.soBai)
        assertEquals(2, t.tuAppKhac)
        assertEquals(1, t.tuAura)
    }

    @Test fun `ca si xep theo so bai, bang nhau thi theo ten`() {
        val t = TuanNghe.tinh(
            listOf(
                bai("A", "Sơn", 1.0),
                bai("B", "Sơn", 2.0),
                bai("C", "An", 3.0),
                bai("D", "Bình", 4.0)
            ),
            BAY_GIO
        )
        assertEquals(
            listOf(TuanNghe.CaSi("Sơn", 2), TuanNghe.CaSi("An", 1), TuanNghe.CaSi("Bình", 1)),
            t.caSi
        )
    }

    @Test fun `bai khong ro ca si khong dung ra mot muc rong`() {
        val t = TuanNghe.tinh(
            listOf(bai("A", "  ", 1.0), bai("B", "Sơn", 2.0)),
            BAY_GIO
        )
        assertEquals(2, t.soBai)
        assertEquals(listOf(TuanNghe.CaSi("Sơn", 1)), t.caSi)
    }

    @Test fun `dong o tuong lai khong tinh vao`() {
        // Đồng hồ máy chạy sai, hoặc khôi phục một tệp từ máy lệch múi giờ.
        // Một dòng "sẽ nghe vào tuần sau" không được phép làm phồng con số.
        val t = TuanNghe.tinh(listOf(bai("A", "X", -2.0)), BAY_GIO)
        assertEquals(0, t.soBai)
    }
}
