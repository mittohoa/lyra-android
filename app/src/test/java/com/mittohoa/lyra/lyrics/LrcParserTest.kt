package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chuyen tu `scripts/test-lrc.ts` cua ban Windows. */
class LrcParserTest {

    @Test
    fun `doc moc thoi gian co bản`() {
        val out = parseLrc("[00:12.34]Dong mot\n[01:05.00]Dong hai")
        assertTrue(out.synced)
        assertEquals(2, out.lines.size)
        assertEquals(12_340, out.lines[0].time)
        assertEquals("Dong mot", out.lines[0].text)
        assertEquals(65_000, out.lines[1].time)
    }

    @Test
    fun `mili-giay 3 chu so doc dung`() {
        val out = parseLrc("[00:01.500]Nua giay ruoi")
        assertEquals(1_500, out.lines[0].time)
    }

    @Test
    fun `mili-giay 2 chu so la phan tram giay`() {
        // .05 la 50ms, khong phai 5ms
        val out = parseLrc("[00:01.05]Nam phan tram")
        assertEquals(1_050, out.lines[0].time)
    }

    @Test
    fun `khong co phan le van doc duoc`() {
        val out = parseLrc("[02:30]Hai phut ruoi")
        assertEquals(150_000, out.lines[0].time)
    }

    @Test
    fun `mot dong nhieu moc thi nhan ban ra`() {
        // Doan diep khuc lap lai - mot dong chu, nhieu lan hat
        val out = parseLrc("[00:10.00][01:10.00][02:10.00]Diep khuc")
        assertEquals(3, out.lines.size)
        assertTrue(out.lines.all { it.text == "Diep khuc" })
        assertEquals(listOf(10_000L, 70_000L, 130_000L), out.lines.map { it.time })
    }

    @Test
    fun `bo tag metadata khoi danh sach dong`() {
        val out = parseLrc("[ar:Nghe Si]\n[ti:Ten Bai]\n[00:05.00]Loi that")
        assertEquals(1, out.lines.size)
        assertEquals("Loi that", out.lines[0].text)
    }

    @Test
    fun `doc duoc tag offset`() {
        val out = parseLrc("[offset:+250]\n[00:05.00]Dong")
        assertEquals(250, out.offset)
    }

    @Test
    fun `sap xep lai khi moc de lon xon`() {
        val out = parseLrc("[00:30.00]Sau\n[00:10.00]Truoc")
        assertEquals("Truoc", out.lines[0].text)
        assertEquals("Sau", out.lines[1].text)
    }

    @Test
    fun `khong co moc nao thi la loi chu tron`() {
        val out = parseLrc("Dong mot\nDong hai")
        assertFalse(out.synced)
        assertEquals(2, out.lines.size)
        assertTrue(out.lines.all { it.time == 0L })
    }

    @Test
    fun `chuoi rong tra ve khong co gi`() {
        assertTrue(parseLrc("").isEmpty)
        assertTrue(parseLrc("   \n  \n").isEmpty)
    }

    @Test
    fun `dong chi co moc ma khong co chu van duoc giu`() {
        // Doan nhac dao - can moc de khung noi biet luc do khong hat gi
        val out = parseLrc("[00:05.00]\n[00:10.00]Bat dau hat")
        assertEquals(2, out.lines.size)
        assertEquals("", out.lines[0].text)
    }

    @Test
    fun `tim dung dong dang hat`() {
        val lines = listOf(
            LyricLine(0, "Mot"),
            LyricLine(10_000, "Hai"),
            LyricLine(20_000, "Ba")
        )
        assertEquals(0, activeLineIndex(lines, 0))
        assertEquals(0, activeLineIndex(lines, 9_999))
        assertEquals(1, activeLineIndex(lines, 10_000))
        assertEquals(2, activeLineIndex(lines, 999_999))
    }

    @Test
    fun `chua toi dong dau thi khong co dong nao dang hat`() {
        val lines = listOf(LyricLine(5_000, "Bat dau"))
        assertEquals(-1, activeLineIndex(lines, 1_000))
        assertEquals(-1, activeLineIndex(emptyList(), 1_000))
    }

    @Test
    fun `do lech dich dong dang hat`() {
        val lines = listOf(LyricLine(0, "Mot"), LyricLine(10_000, "Hai"))
        // Loi hien som hon 1 giay
        assertEquals(1, activeLineIndex(lines, 9_000, offset = 1_000))
    }

    @Test
    fun `file tron thi bo cac dong khong moc`() {
        // Vai nguon de mot dong tieu de hoac ten nguoi dich khong mang moc lan
        // giua cac dong co moc. Chung mang thoi gian 0 nen sap xep theo thoi
        // gian se day het len dau bai - loi dao lon con te hon la thieu dong.
        val loi = parseLrc(
            """
            Ten nguoi dich: ai do
            [00:10.00]Cau mot
            Mot dong lac
            [00:20.00]Cau hai
            """.trimIndent()
        )
        assertTrue(loi.synced)
        assertEquals(listOf("Cau mot", "Cau hai"), loi.lines.map { it.text })
    }

    @Test
    fun `dong dau o giay khong van duoc giu`() {
        // Moc [00:00.00] cung cho time = 0. Loc theo "time > 0" se vut mat
        // dung dong dau cua nhung ban loi bat dau ngay giay thu khong.
        val loi = parseLrc(
            """
            [00:00.00]Ngay tu dau
            [00:05.00]Sau do
            """.trimIndent()
        )
        assertEquals(listOf("Ngay tu dau", "Sau do"), loi.lines.map { it.text })
    }
}