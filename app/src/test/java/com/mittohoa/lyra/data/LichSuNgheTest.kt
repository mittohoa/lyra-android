package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Luật quyết định cái gì được vào lịch sử nghe.
 *
 * Sai theo hướng ghi thừa thì lịch sử đầy những bài vừa bấm nhầm, và bài thật
 * sự đã nghe nằm lẫn giữa chúng — lúc đó không ai mở nó lần thứ hai. Sai theo
 * hướng ghi thiếu thì bài nghe xong không có ở đó, và đấy đúng là câu hỏi duy
 * nhất người ta mở lịch sử ra để hỏi.
 */
class LichSuNgheTest {

    private val giay = 1_000L
    private val phut = 60_000L

    // ---- Ngưỡng nửa phút ----

    @Test fun `luot qua vai giay thi KHONG ghi`() {
        // Bấm vào một bài, nghe ba giây, thấy không phải bài mình tìm rồi bấm
        // sang bài khác. Đó không phải một lần nghe.
        assertFalse(LichSuNghe.dangGhi(doDai = 4 * phut, daPhat = 3 * giay))
    }

    @Test fun `sat duoi nua phut thi van chua ghi`() {
        assertFalse(LichSuNghe.dangGhi(doDai = 4 * phut, daPhat = 29 * giay))
    }

    @Test fun `dung nua phut la ghi`() {
        assertTrue(LichSuNghe.dangGhi(doDai = 4 * phut, daPhat = 30 * giay))
    }

    @Test fun `nghe het ca bai dai thi ghi`() {
        assertTrue(LichSuNghe.dangGhi(doDai = 40 * phut, daPhat = 40 * phut))
    }

    // ---- Bài ngắn hơn ngưỡng ----

    @Test fun `bai ngan nghe tron ven van vao duoc lich su`() {
        // Chỉ để nửa phút cứng thì mọi bản nhạc chuông, mọi đoạn thu ngắn đều
        // không bao giờ vào được lịch sử dù đã nghe trọn từ đầu tới cuối.
        assertTrue(LichSuNghe.dangGhi(doDai = 20 * giay, daPhat = 20 * giay))
    }

    @Test fun `bai ngan moi cham vao thi van khong ghi`() {
        // Một phần ba của hai mươi giây là gần bảy giây; hai giây chưa tới.
        assertFalse(LichSuNghe.dangGhi(doDai = 20 * giay, daPhat = 2 * giay))
    }

    @Test fun `bai mot phut ruoi lay dung mot phan ba`() {
        // 90 giây: một phần ba là 30 giây, đúng bằng ngưỡng cứng. Từ đây trở
        // lên thì ngưỡng cứng thắng, dưới nữa thì một phần ba thắng.
        assertFalse(LichSuNghe.dangGhi(doDai = 90 * giay, daPhat = 29 * giay))
        assertTrue(LichSuNghe.dangGhi(doDai = 90 * giay, daPhat = 30 * giay))
    }

    // ---- Không biết bài dài bao nhiêu ----

    @Test fun `khong biet do dai thi lay nua phut`() {
        // Đường phát trực tuyến có lúc chưa kịp báo độ dài. Không biết thì
        // dùng ngưỡng cứng, chứ không phải là không bao giờ ghi.
        assertFalse(LichSuNghe.dangGhi(doDai = 0L, daPhat = 10 * giay))
        assertTrue(LichSuNghe.dangGhi(doDai = 0L, daPhat = 30 * giay))
    }

    @Test fun `chua phat gi thi khong ghi`() {
        assertFalse(LichSuNghe.dangGhi(doDai = 4 * phut, daPhat = 0L))
        assertFalse(LichSuNghe.dangGhi(doDai = 0L, daPhat = 0L))
    }

    // ---- Khoá gộp ----

    @Test fun `bai trong may gop theo dia chi`() {
        val a = LanNghe(diaChi = "lyra://may/1.mp3", ten = "A", caSi = "X")
        val b = LanNghe(diaChi = "lyra://may/1.mp3", ten = "A đổi tên", caSi = "X")
        // Cùng tệp thì cùng một dòng, dù thẻ đã sửa khác đi.
        assertEquals(a.khoa, b.khoa)
    }

    @Test fun `bai app khac gop theo app va ten`() {
        val a = LanNghe(diaChi = "", ten = "A", caSi = "X", app = "com.google.android.youtube")
        val b = LanNghe(diaChi = "", ten = "A", caSi = "X", app = "com.google.android.youtube")
        val c = LanNghe(diaChi = "", ten = "A", caSi = "X", app = "com.spotify.music")
        assertEquals(a.khoa, b.khoa)
        // Cùng bài mà nghe ở hai app là hai dòng: chỗ nghe được là một phần của
        // câu trả lời cho "hôm qua tôi nghe nó ở đâu".
        assertTrue(a.khoa != c.khoa)
    }

    @Test fun `co dia chi thi nghe lai duoc, khong thi khong`() {
        assertTrue(LanNghe(diaChi = "lyra://may/1.mp3", ten = "A", caSi = "X").ngheLaiDuoc)
        assertFalse(LanNghe(diaChi = "", ten = "A", caSi = "X", app = "ht.nct").ngheLaiDuoc)
    }
}
