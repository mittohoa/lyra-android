package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chạy các bộ đọc trên MÁY THẬT, không phải trên máy tính.
 *
 * Lý do có bài kiểm này: bộ máy biểu thức chính quy của Android là ICU, còn của
 * máy tính là OpenJDK — và ICU CHẶT HƠN. Một dấu `}` đứng một mình, máy tính coi
 * là ký tự thường, Android gọi là lỗi cú pháp và ném ngay lúc nạp lớp.
 *
 * Chuyện đã xảy ra thật với `parseSrt`: mười một bài kiểm trên máy tính xanh
 * hết, cài lên điện thoại thì sập ở đúng lần đọc phụ đề đầu tiên —
 * `ExceptionInInitializerError` vì `<[^>]*>|\{[^}]*}`.
 *
 * Nên bài kiểm này không kiểm logic — logic đã có bài riêng trên máy tính rồi.
 * Nó chỉ hỏi một câu: mấy biểu thức này có DỊCH ĐƯỢC trên Android không, và có
 * ra đúng kết quả không.
 */
class BieuThucTrenMayTest {

    @Test fun boDocSrtChayDuocTrenAndroid() {
        val doc = parseSrt(
            """
            1
            00:00:01,000 --> 00:00:03,000
            {\an8}<i>Câu</i> có thẻ

            2
            00:01:00,000 --> 00:01:02,000
            Câu hai
            """.trimIndent()
        )
        assertEquals(2, doc.lines.size)
        assertEquals("Câu có thẻ", doc.lines[0].text)
        assertEquals(3_000L, doc.lines[0].ketThuc)
    }

    @Test fun boDocLrcChayDuocTrenAndroid() {
        val doc = parseLrc("[ti:Bài]\n[00:01.00]một câu\n[00:05.00]hai câu")
        assertEquals(2, doc.lines.size)
        assertTrue(doc.synced)
    }

    @Test fun boDocChuTuAnhChayDuocTrenAndroid() {
        // `DocChuTuAnh` cũng giữ một biểu thức hằng ở mức lớp. Gọi một lần cho
        // lớp được nạp là đủ để biết nó dịch được.
        assertEquals("com.mittohoa.lyra.lyrics.DocChuTuAnh", DocChuTuAnh.javaClass.name)
    }
}
