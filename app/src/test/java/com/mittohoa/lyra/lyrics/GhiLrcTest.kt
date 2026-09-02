package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiem than tep .lrc mà AURA ghi ra.
 *
 * Cho de sai nhat cua ca tinh nang: mot the dat sai thi tep hong ma lan ghi van
 * bao "thanh cong", va nguoi dung chi phat hien khi mo bang trinh phat khac -
 * hoac khi chinh AURA doc lai ra mot bai loi day rac.
 */
class GhiLrcTest {

    private val loi = "[00:16.64]câu một\n[00:22.81]câu hai"

    @Test fun `mang du the mo ta`() {
        val ra = LrcCanhTep.dungNoiDung(loi, "Nàng Thơ", "Hoàng Dũng")
        assertTrue(ra.startsWith("[ti:Nàng Thơ]\n[ar:Hoàng Dũng]\n"))
        assertTrue("phải ghi rõ thương hiệu", ra.contains("[re:AURA by #mittoHOA]"))
    }

    @Test fun `ghi ra roi doc lai ra dung tung dong`() {
        // Vong tron quan trong nhat: cai AURA ghi ra thi chinh AURA phai doc
        // lai duoc, va khong duoc de the mo ta lot vao thanh cau hat.
        val doc = parseLrc(LrcCanhTep.dungNoiDung(loi, "Nàng Thơ", "Hoàng Dũng"))
        assertEquals(2, doc.lines.size)
        assertEquals("câu một", doc.lines[0].text)
        assertEquals("câu hai", doc.lines[1].text)
        assertEquals(16_640L, doc.lines[0].time)
        assertTrue(doc.synced)
    }

    @Test fun `khong co ten thi bo han the do`() {
        // Mot the rong "[ti:]" khong noi len dieu gi, chi lam ban tep.
        val ra = LrcCanhTep.dungNoiDung(loi, "", "")
        assertFalse(ra.contains("[ti:"))
        assertFalse(ra.contains("[ar:"))
        assertTrue(ra.contains("[re:"))
    }

    @Test fun `dau dong vuong trong ten khong cat cut the`() {
        // "Nàng Thơ [Live]" - dau ] se dong the som, phan con lai tro thanh
        // mot cau hat ma khong ai go ca.
        val ra = LrcCanhTep.dungNoiDung(loi, "Nàng Thơ [Live]", "Hoàng Dũng")
        assertEquals("[ti:Nàng Thơ [Live)]", ra.lines().first())
        val doc = parseLrc(ra)
        assertEquals("thẻ phải bị bỏ qua hết, chỉ còn lời", 2, doc.lines.size)
    }

    @Test fun `loi chu tron van ghi ra duoc`() {
        val ra = LrcCanhTep.dungNoiDung("câu một\ncâu hai", "A", "B")
        val doc = parseLrc(ra)
        assertEquals(2, doc.lines.size)
        assertFalse("chưa căn giờ thì đọc lại vẫn là chữ trơn", doc.synced)
    }

    @Test fun `luon ket thuc bang mot dau xuong dong`() {
        // Tep chu ma thieu dau xuong dong cuoi thi vai cong cu dong lenh doc
        // hut dong cuoi cung.
        assertTrue(LrcCanhTep.dungNoiDung(loi, "A", "B").endsWith("\n"))
        assertFalse(LrcCanhTep.dungNoiDung(loi + "\n\n\n", "A", "B").endsWith("\n\n"))
    }
}
