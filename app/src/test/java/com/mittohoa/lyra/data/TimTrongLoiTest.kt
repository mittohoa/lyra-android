package com.mittohoa.lyra.data

import com.mittohoa.lyra.lyrics.normalizeForCompare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Luật khớp chuỗi khi tìm trong LỜI bài hát.
 *
 * `LyricCache.timTrongLoi` đọc đĩa nên không kiểm thẳng được ở đây; thứ kiểm
 * được — và cũng là thứ dễ sai nhất — là phép so chuỗi nó dựa vào. Người Việt
 * gõ tìm gần như luôn bỏ dấu và không phân biệt hoa thường, nên nếu phép so này
 * chặt thì tính năng coi như không dùng được với chính người dùng của nó.
 */
class TimTrongLoiTest {

    /** Đúng phép so mà `timTrongLoi` dùng cho mỗi câu lời. */
    private fun khop(cauLoi: String, goTim: String): Boolean =
        normalizeForCompare(cauLoi).contains(normalizeForCompare(goTim))

    // ---- Gõ không dấu ----

    @Test fun `go khong dau van tim ra cau co dau`() {
        assertTrue(khop("Em ơi có bao nhiêu", "em oi"))
        assertTrue(khop("Ngày mai em đi", "ngay mai"))
    }

    @Test fun `go co dau van tim ra`() {
        assertTrue(khop("Em ơi có bao nhiêu", "em ơi"))
    }

    @Test fun `hoa thuong khong quan trong`() {
        assertTrue(khop("EM ƠI CÓ BAO NHIÊU", "em oi"))
        assertTrue(khop("em ơi có bao nhiêu", "EM OI"))
    }

    // ---- Khớp giữa câu, không chỉ đầu câu ----

    @Test fun `khop o giua cau`() {
        assertTrue(khop("Rồi mai em đi xa rồi", "em di"))
    }

    @Test fun `chuoi khong co trong cau thi khong khop`() {
        assertFalse(khop("Em ơi có bao nhiêu", "anh oi"))
    }

    // ---- Tiếng nước ngoài ----

    @Test fun `loi tieng Anh van tim duoc`() {
        assertTrue(khop("Despacito, quiero respirar tu cuello", "quiero"))
    }

    @Test fun `dau phu tieng Tay Ban Nha cung duoc bo`() {
        // Cùng lẽ với tiếng Việt: người gõ trên bàn phím tiếng Việt không có
        // cách nào gõ "á" của tiếng Tây Ban Nha cho đúng chỗ.
        assertTrue(khop("Sí, sabes que ya llevo un rato", "si sabes"))
    }

    // ---- Chuỗi rỗng ----

    @Test fun `chuoi rong thi khop moi cau nen ben goi phai chan truoc`() {
        // `contains("")` luôn đúng — đó là lý do `timTrongLoi` chặn chuỗi rỗng
        // ngay ở đầu thay vì để phép so này quyết định.
        assertTrue(khop("bất cứ câu nào", ""))
        assertEquals("", normalizeForCompare("   "))
    }
}
