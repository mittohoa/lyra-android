package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hai mệnh đề canh chỗ "app đưa nhầm lời của bài khác".
 *
 * Cả hai đều quyết định thứ hiện ra trước mắt người dùng, và cả hai đều sai
 * được mà không có gì báo: một cái làm mất câu cảnh báo đáng ra phải có, cái
 * kia làm dòng tiêu đề in tên ca sĩ hai lần.
 */
class NgoKhopNhamTest {

    // ---- ngoKhacCaSi: có nên ngờ đây là bài của người khác không ----

    @Test fun `cung mot ca si thi khong ngo`() {
        assertFalse(LyricsRepository.ngoKhacCaSi("Luis Fonsi", "Luis Fonsi"))
    }

    @Test fun `ca si khac han thi NGO`() {
        // Đúng ca đo được trên máy: tệp tag `Sacré Nom! - Hours` nhận lại lời
        // của `Bhaskar - Hours and Hours`, và không có câu cảnh báo nào.
        assertTrue(LyricsRepository.ngoKhacCaSi("Sacré Nom!", "Bhaskar"))
    }

    @Test fun `khong biet ca si la ai thi NGO`() {
        // Khớp bằng mỗi tên bài. Với một cái tên chung chung thì đó gần như là
        // bốc thăm, nên phải nói ra.
        assertTrue(LyricsRepository.ngoKhacCaSi(null, "Bhaskar"))
        assertTrue(LyricsRepository.ngoKhacCaSi("", "Bhaskar"))
    }

    @Test fun `nguon khong noi ca si thi KHONG ngo`() {
        // Im lặng không phải là bằng chứng chống lại chính nó. Nhiều nguồn
        // không trả tên ca sĩ kèm kết quả, và ngờ tất cả thì cảnh báo kêu suốt.
        assertFalse(LyricsRepository.ngoKhacCaSi("Sacré Nom!", ""))
        assertFalse(LyricsRepository.ngoKhacCaSi(null, ""))
    }

    @Test fun `ten viet dai them mot doan thi coi la cung nguoi`() {
        // "Đen" / "Đen Vâu" là một người. Bắt bẻ từng ký tự thì cả báo động
        // kêu suốt ngày, mà báo động kêu suốt ngày thì không ai đọc nữa.
        assertFalse(LyricsRepository.ngoKhacCaSi("Đen", "Đen Vâu"))
        assertFalse(LyricsRepository.ngoKhacCaSi("Đen Vâu", "Đen"))
    }

    @Test fun `khac dau nhung cung ten thi coi la cung nguoi`() {
        assertFalse(LyricsRepository.ngoKhacCaSi("son tung m-tp", "Sơn Tùng M-TP"))
    }

    @Test fun `hop tac nhieu nguoi van coi la cung nguoi neu co ten chung`() {
        assertFalse(
            LyricsRepository.ngoKhacCaSi("Sơn Tùng M-TP", "Sơn Tùng M-TP, Snoop Dogg")
        )
    }

    // ---- tenBaiDeHien: bỏ đoạn lặp tên ca sĩ ở đầu tên bài ----

    @Test fun `ten video YouTube bo duoc doan lap`() {
        // Đúng ca đo được trên khung lời nổi: nó hiện
        // "Luis Fonsi — Luis Fonsi - Despacito ft. Daddy Yankee".
        assertEquals(
            "Despacito ft. Daddy Yankee",
            tenBaiDeHien("Luis Fonsi", "Luis Fonsi - Despacito ft. Daddy Yankee")
        )
    }

    @Test fun `ten bai binh thuong thi khong dong toi`() {
        assertEquals("Nàng Thơ", tenBaiDeHien("Hoàng Dũng", "Nàng Thơ"))
    }

    @Test fun `khong cat khi ten ca si chi tinh co nam giua ten bai`() {
        // Chỉ cắt ở ĐẦU. "Anh" nằm giữa tên bài không phải là đoạn lặp.
        assertEquals("Có Chàng Trai Anh Viết Lên Cây", tenBaiDeHien("Anh", "Có Chàng Trai Anh Viết Lên Cây"))
    }

    @Test fun `ten bai TRUNG ten ca si thi giu nguyen, khong cat thanh rong`() {
        // Cắt hết thì còn một dòng trống, tệ hơn hẳn một dòng lặp.
        assertEquals("Đen", tenBaiDeHien("Đen", "Đen"))
    }

    @Test fun `bo duoc ca dau hai cham va gach dung`() {
        assertEquals("Bài Này Chill Phết", tenBaiDeHien("Đen", "Đen: Bài Này Chill Phết"))
        assertEquals("Bài Này Chill Phết", tenBaiDeHien("Đen", "Đen | Bài Này Chill Phết"))
    }

    @Test fun `bo qua khac biet dau va hoa thuong khi cat`() {
        assertEquals(
            "Nơi Này Có Anh",
            tenBaiDeHien("son tung m-tp", "Sơn Tùng M-TP - Nơi Này Có Anh")
        )
    }

    @Test fun `truong rong thi tra ve nguyen ten bai`() {
        assertEquals("Nàng Thơ", tenBaiDeHien("", "Nàng Thơ"))
        assertEquals("", tenBaiDeHien("Hoàng Dũng", ""))
    }
}
