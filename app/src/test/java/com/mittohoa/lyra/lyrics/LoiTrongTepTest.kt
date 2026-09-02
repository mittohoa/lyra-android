package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream

/**
 * Kiem bo doc loi nam trong the cua tep nhac.
 *
 * Dung THE THAT dung bang byte chu khong dung tep mau: moi nhanh o day la mot
 * cach ghi the co that ngoai doi, va chi co dung byte moi kiem duoc. Doc hut mot
 * nhanh thi voi nguoi dung no chi la "bai nay khong co loi" - khong co thong bao
 * loi nao de ma lan ra.
 */
class LoiTrongTepTest {

    @get:Rule val thuMuc = TemporaryFolder()

    // ---- dung the ----

    private fun songAn(v: Int) = byteArrayOf(
        ((v shr 21) and 0x7F).toByte(), ((v shr 14) and 0x7F).toByte(),
        ((v shr 7) and 0x7F).toByte(), (v and 0x7F).toByte()
    )

    private fun lonTruoc(v: Int) = byteArrayOf(
        ((v shr 24) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte()
    )

    /** Mot tep mp3 gia: chi co the ID3 roi vai byte rac lam phan am thanh. */
    private fun tep(vararg khung: Pair<String, ByteArray>, ban: Int = 3, co: Int = 0): String {
        val than = ByteArrayOutputStream()
        for ((ten, noi) in khung) {
            than.write(ten.toByteArray(Charsets.ISO_8859_1))
            than.write(if (ban >= 4) songAn(noi.size) else lonTruoc(noi.size))
            than.write(byteArrayOf(0, 0))
            than.write(noi)
        }
        val t = than.toByteArray()

        val ra = ByteArrayOutputStream()
        ra.write("ID3".toByteArray(Charsets.ISO_8859_1))
        ra.write(byteArrayOf(ban.toByte(), 0, co.toByte()))
        ra.write(songAn(t.size))
        ra.write(t)
        ra.write(ByteArray(64) { 0x55 })   // "am thanh"

        val f = thuMuc.newFile("bài hát-${khung.hashCode()}-$ban-$co.mp3")
        f.writeBytes(ra.toByteArray())
        return f.absolutePath
    }

    /** USLT: ma chu · ngon ngu · mo ta · loi */
    private fun uslt(loi: String, ma: Int = 3, moTa: String = ""): ByteArray {
        val o = ByteArrayOutputStream()
        o.write(ma)
        o.write("vie".toByteArray(Charsets.ISO_8859_1))
        o.write(moTa.toByteArray(charset(ma)))
        o.write(ByteArray(if (ma == 1 || ma == 2) 2 else 1))
        o.write(loi.toByteArray(charset(ma)))
        return o.toByteArray()
    }

    /** SYLT: ma chu · ngon ngu · dang moc · loai · mo ta · [chu · moc] */
    private fun sylt(cac: List<Pair<Long, String>>, dangMoc: Int = 2, ma: Int = 3): ByteArray {
        val o = ByteArrayOutputStream()
        o.write(ma)
        o.write("vie".toByteArray(Charsets.ISO_8859_1))
        o.write(dangMoc)
        o.write(1)                       // loai noi dung: loi bai hat
        o.write(ByteArray(if (ma == 1 || ma == 2) 2 else 1))   // mo ta rong
        for ((moc, chu) in cac) {
            o.write(chu.toByteArray(charset(ma)))
            o.write(ByteArray(if (ma == 1 || ma == 2) 2 else 1))
            o.write(lonTruoc(moc.toInt()))
        }
        return o.toByteArray()
    }

    private fun charset(ma: Int) = when (ma) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        else -> Charsets.UTF_8
    }

    // ---- bai kiem ----

    @Test fun `doc duoc loi chu tron trong USLT`() {
        val d = LoiTrongTep.doc(tep("USLT" to uslt("câu một\ncâu hai")))!!
        assertEquals(2, d.lines.size)
        assertEquals("câu một", d.lines[0].text)
        assertEquals(LoiTrongTep.NGUON, d.from)
    }

    @Test fun `USLT chua san moc gio thi doc duoc luon ban co moc`() {
        // Rat nhieu tep nhet loi dang [00:12.30] vao chinh khung chu tron.
        val d = LoiTrongTep.doc(tep("USLT" to uslt("[00:12.30]câu một\n[00:15.00]câu hai")))!!
        assertTrue(d.synced)
        assertEquals(12_300L, d.lines[0].time)
    }

    @Test fun `doc duoc loi co moc trong SYLT`() {
        val d = LoiTrongTep.doc(
            tep("SYLT" to sylt(listOf(1_000L to "câu một", 5_500L to "câu hai")))
        )!!
        assertTrue(d.synced)
        assertEquals(2, d.lines.size)
        assertEquals(1_000L, d.lines[0].time)
        assertEquals("câu hai", d.lines[1].text)
    }

    @Test fun `co ca hai thi lay SYLT vi no mang moc gio`() {
        val d = LoiTrongTep.doc(
            tep(
                "USLT" to uslt("chữ trơn"),
                "SYLT" to sylt(listOf(2_000L to "có mốc"))
            )
        )!!
        assertEquals("có mốc", d.lines.single().text)
        assertTrue(d.synced)
    }

    @Test fun `SYLT dem theo khung MPEG thi bo qua, roi xuong USLT`() {
        // Dang moc 1 dem theo khung MPEG; doi ra mili-giay phai biet nhip bit,
        // doan sai mot chut la ca bai lech dan. Tha roi xuong USLT.
        val d = LoiTrongTep.doc(
            tep(
                "SYLT" to sylt(listOf(30L to "theo khung"), dangMoc = 1),
                "USLT" to uslt("chữ trơn")
            )
        )!!
        assertEquals("chữ trơn", d.lines.single().text)
    }

    @Test fun `doc duoc chu UTF-16 co dau BOM`() {
        val d = LoiTrongTep.doc(tep("USLT" to uslt("nàng thơ", ma = 1)))!!
        assertEquals("nàng thơ", d.lines.single().text)
    }

    @Test fun `doc duoc the doi ID3v2 phay 4`() {
        val d = LoiTrongTep.doc(tep("USLT" to uslt("câu một"), ban = 4))!!
        assertEquals("câu một", d.lines.single().text)
    }

    @Test fun `bo qua duoc khung khac nam truoc khung loi`() {
        val d = LoiTrongTep.doc(
            tep(
                "TIT2" to byteArrayOf(3) + "Nàng Thơ".toByteArray(),
                "APIC" to ByteArray(400) { 0x7F },
                "USLT" to uslt("câu một")
            )
        )!!
        assertEquals("câu một", d.lines.single().text)
    }

    @Test fun `tep khong co the thi tra null chu khong nem`() {
        val f = thuMuc.newFile("trơ trọi.mp3")
        f.writeBytes(ByteArray(200) { 0x11 })
        assertNull(LoiTrongTep.doc(f.absolutePath))
    }

    @Test fun `tep khong ton tai thi tra null`() {
        assertNull(LoiTrongTep.doc(thuMuc.root.absolutePath + "/không-có.mp3"))
    }

    @Test fun `the co ma khong co loi thi tra null`() {
        assertNull(LoiTrongTep.doc(tep("TIT2" to (byteArrayOf(3) + "Nàng Thơ".toByteArray()))))
    }

    @Test fun `khung loi rong coi nhu khong co`() {
        assertNull(LoiTrongTep.doc(tep("USLT" to uslt("   \n\n"))))
    }

    @Test fun `doc duoc tep FLAC`() {
        // fLaC · khoi cuoi cung loai 4 (Vorbis comment)
        val than = ByteArrayOutputStream()
        val nsx = "Lavf".toByteArray()
        fun nho(v: Int) = byteArrayOf(
            (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
            ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte()
        )
        than.write(nho(nsx.size)); than.write(nsx)
        val truong = "LYRICS=[00:03.00]câu một\n[00:07.00]câu hai".toByteArray(Charsets.UTF_8)
        than.write(nho(1)); than.write(nho(truong.size)); than.write(truong)
        val t = than.toByteArray()

        val ra = ByteArrayOutputStream()
        ra.write("fLaC".toByteArray(Charsets.ISO_8859_1))
        ra.write(byteArrayOf((0x80 or 4).toByte(),
            ((t.size shr 16) and 0xFF).toByte(), ((t.size shr 8) and 0xFF).toByte(),
            (t.size and 0xFF).toByte()))
        ra.write(t)

        val f = thuMuc.newFile("bài.flac")
        f.writeBytes(ra.toByteArray())

        val d = LoiTrongTep.doc(f.absolutePath)!!
        assertTrue(d.synced)
        assertEquals(2, d.lines.size)
        assertEquals(3_000L, d.lines[0].time)
    }
}
