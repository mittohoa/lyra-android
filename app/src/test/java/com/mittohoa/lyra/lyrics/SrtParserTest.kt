package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiem bo doc phu de .srt.
 *
 * Tep .srt ngoai doi ban hon dinh dang trong sach nhieu: xuong dong kieu
 * Windows, dau BOM o dau tep, the in nghieng giua cau, moc gio thieu so 0, va
 * ca nhung dong trong lac cho. Doc hut mot cau thi nguoi dung khong biet, chi
 * thay phu de "nhay coc" - nen kiem ky tung kieu ban mot.
 */
class SrtParserTest {

    @Test fun `doc duoc mot tep binh thuong`() {
        val srt = """
            1
            00:00:20,000 --> 00:00:24,400
            Câu thoại thứ nhất

            2
            00:00:24,600 --> 00:00:27,800
            Câu thoại thứ hai
        """.trimIndent()

        val doc = parseSrt(srt)
        assertEquals(2, doc.lines.size)
        assertTrue(doc.synced)
        assertEquals(20_000L, doc.lines[0].time)
        assertEquals(24_400L, doc.lines[0].ketThuc)
        assertEquals("Câu thoại thứ nhất", doc.lines[0].text)
        assertEquals(24_600L, doc.lines[1].time)
    }

    @Test fun `cau hai dong duoc noi lai`() {
        // Xuong dong trong .srt la de vua be ngang man hinh chu khong mang
        // nghia; cho hien cua AURA tu xuong dong theo be ngang cua no.
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            Dòng trên
            dòng dưới
        """.trimIndent()
        assertEquals("Dòng trên dòng dưới", parseSrt(srt).lines.single().text)
    }

    @Test fun `bo the in nghieng va the vi tri`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            {\an8}<i>Nghiêng</i> và <b>đậm</b>
        """.trimIndent()
        assertEquals("Nghiêng và đậm", parseSrt(srt).lines.single().text)
    }

    @Test fun `chiu duoc xuong dong kieu Windows va dau BOM`() {
        val srt = "﻿1\r\n00:00:01,000 --> 00:00:04,000\r\nMột câu\r\n"
        assertEquals("Một câu", parseSrt(srt).lines.single().text)
    }

    @Test fun `chiu duoc dau cham thay cho dau phay`() {
        // Vai cong cu xuat ra "00:00:01.500" thay vi "00:00:01,500".
        val srt = """
            1
            00:00:01.500 --> 00:00:04.000
            Một câu
        """.trimIndent()
        assertEquals(1_500L, parseSrt(srt).lines.single().time)
    }

    @Test fun `phan le thieu chu so van doc dung`() {
        // "00:00:01,5" la MOT PHAN MUOI giay, khong phai nam mili-giay.
        val srt = """
            1
            00:00:01,5 --> 00:00:04,00
            Một câu
        """.trimIndent()
        val d = parseSrt(srt).lines.single()
        assertEquals(1_500L, d.time)
        assertEquals(4_000L, d.ketThuc)
    }

    @Test fun `gio lon hon mot tieng van dung`() {
        val srt = """
            1
            01:02:03,004 --> 01:02:05,000
            Một câu
        """.trimIndent()
        assertEquals(3_723_004L, parseSrt(srt).lines.single().time)
    }

    @Test fun `khong phai srt thi tra ve rong chu khong nem`() {
        assertTrue(parseSrt("").lines.isEmpty())
        assertTrue(parseSrt("[00:01.00]đây là lrc").lines.isEmpty())
        assertTrue(parseSrt("một tệp văn bản bất kỳ").lines.isEmpty())
    }

    @Test fun `mot khoi hong khong lam mat cac khoi con lai`() {
        val srt = """
            1
            00:00:01,000 --> hỏng
            Bỏ qua

            2
            00:00:05,000 --> 00:00:07,000
            Giữ lại
        """.trimIndent()
        val doc = parseSrt(srt)
        assertEquals(1, doc.lines.size)
        assertEquals("Giữ lại", doc.lines.single().text)
    }

    @Test fun `cau het gio thi khong con la cau dang hien`() {
        // Day la ly do phai co moc ket thuc: giua hai cau thoai co the la ca
        // mot phut im lang, ma khong biet luc nao cau cu HET thi no nam li
        // tren man hinh suot phut do.
        val doc = parseSrt(
            """
            1
            00:00:01,000 --> 00:00:03,000
            Câu một

            2
            00:01:00,000 --> 00:01:02,000
            Câu hai
            """.trimIndent()
        )
        assertEquals(0, activeLineIndex(doc.lines, 2_000))
        assertEquals("hết giờ rồi thì không tô sáng nữa",
            -1, activeLineIndex(doc.lines, 30_000))
        assertEquals(1, activeLineIndex(doc.lines, 61_000))
    }

    @Test fun `loi bai hat khong co moc ket thuc thi giu nguyen nhu cu`() {
        // Ban .lrc khong mang moc ket thuc, va cach to sang cua no khong duoc
        // phep doi vi mot tinh nang danh cho phu de.
        val loi = parseLrc("[00:01.00]câu một\n[00:10.00]câu hai")
        assertEquals(0, activeLineIndex(loi.lines, 5_000))
        assertEquals(0, activeLineIndex(loi.lines, 9_999))
        assertEquals(1, activeLineIndex(loi.lines, 10_000))
    }
}
