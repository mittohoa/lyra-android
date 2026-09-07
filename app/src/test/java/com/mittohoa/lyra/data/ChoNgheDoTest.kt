package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Luật quyết định app có mở lại từ giữa bài hay không.
 *
 * Sai ở đây thì người dùng thấy ngay, và thấy theo kiểu khó chịu: hoặc một bài
 * hát ba phút tự nhiên bắt đầu từ giữa, hoặc một tệp bốn mươi phút vẫn quay về
 * số không như chưa có gì thay đổi.
 */
class ChoNgheDoTest {

    private val phut = 60_000L

    // ---- Ranh giới giữa nhạc và phim ----

    @Test fun `bai hat ngan thi KHONG nho`() {
        // Nhạc thì người ta nghe lại từ đầu. Một bài ba phút mở lại từ giữa là
        // khó chịu chứ không phải tiện.
        assertFalse(ChoNgheDo.dangNho(doDai = 3 * phut, viTri = 90_000L, laVideo = false))
    }

    @Test fun `video ngan thi VAN nho`() {
        // Phim thì người ta xem tiếp, dài ngắn không đổi điều đó.
        assertTrue(ChoNgheDo.dangNho(doDai = 3 * phut, viTri = 90_000L, laVideo = true))
    }

    @Test fun `nhac dai hon muoi phut thi nho nhu phim`() {
        // Podcast, sách nói, bản thu buổi diễn — nghe dở thì nghe tiếp.
        assertTrue(ChoNgheDo.dangNho(doDai = 40 * phut, viTri = 10 * phut, laVideo = false))
    }

    @Test fun `dung muoi phut la da du dai`() {
        assertTrue(ChoNgheDo.dangNho(doDai = ChoNgheDo.DAI_MS, viTri = 5 * phut, laVideo = false))
    }

    // ---- Hai mép ----

    @Test fun `moi nghe chua toi nua phut thi khong co gi de nho`() {
        assertFalse(ChoNgheDo.dangNho(doDai = 40 * phut, viTri = 20_000L, laVideo = true))
    }

    @Test fun `sap het bai thi coi nhu da nghe xong`() {
        // Nhớ chỗ này nghĩa là lần sau mở ra nghe được mấy giây rồi hết.
        assertFalse(ChoNgheDo.dangNho(doDai = 40 * phut, viTri = 40 * phut - 10_000L, laVideo = true))
    }

    @Test fun `giua bai thi nho`() {
        assertTrue(ChoNgheDo.dangNho(doDai = 40 * phut, viTri = 20 * phut, laVideo = true))
    }

    // ---- Không biết bài dài bao nhiêu ----

    @Test fun `khong biet do dai thi KHONG nho`() {
        // Đường phát trực tuyến có lúc chưa kịp báo độ dài. Không biết độ dài
        // thì không phân biệt nổi "đang giữa bài" với "sắp hết".
        assertFalse(ChoNgheDo.dangNho(doDai = 0L, viTri = 20 * phut, laVideo = true))
        assertFalse(ChoNgheDo.dangNho(doDai = -1L, viTri = 20 * phut, laVideo = true))
    }

    // ---- Chỗ bắt đầu phát ----

    @Test fun `chua nho gi thi bat dau tu dau bai`() {
        assertEquals(0L, ChoNgheDo.choBatDau(daNho = 0L, doDai = 40 * phut))
    }

    @Test fun `da nho thi bat dau tu do`() {
        assertEquals(20 * phut, ChoNgheDo.choBatDau(daNho = 20 * phut, doDai = 40 * phut))
    }

    @Test fun `cho da nho vuot qua do dai that thi bo`() {
        // Tệp có thể đã bị thay bằng bản khác NGẮN HƠN mà vẫn nằm ở đúng địa
        // chỉ đó. Nhảy tới một chỗ quá cuối bài thì bộ máy phát nhảy luôn sang
        // bài sau, và người dùng bấm phát một bài rồi nghe một bài khác.
        assertEquals(0L, ChoNgheDo.choBatDau(daNho = 30 * phut, doDai = 5 * phut))
    }

    @Test fun `cho da nho sat cuoi bai thi bo`() {
        assertEquals(0L, ChoNgheDo.choBatDau(daNho = 40 * phut - 10_000L, doDai = 40 * phut))
    }

    @Test fun `khong biet do dai luc phat thi cu tin cho da nho`() {
        // Ngược với `dangNho`: lúc GHI mà không biết độ dài thì không ghi, nên
        // đã có số ở đây tức là lúc ghi nó hợp lệ. Lúc PHÁT chưa biết độ dài là
        // chuyện thường — bộ máy phát báo độ dài sau khi nạp xong.
        assertEquals(20 * phut, ChoNgheDo.choBatDau(daNho = 20 * phut, doDai = 0L))
    }
}
