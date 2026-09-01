package com.mittohoa.lyra.sources

import com.mittohoa.lyra.lyrics.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * Hai hàm thuần trong đường góp lời.
 *
 * Kiểm chúng vì đây là chỗ một lỗi sẽ **âm thầm làm bẩn một kho dùng chung**.
 * Sai `nhoHon` thì hoặc không bao giờ giải xong (app treo), hoặc gửi lên một
 * token sai (LRCLIB từ chối). Sai `dungLrc` thì gửi lên một bản lời có mốc lệch
 * — và người tải về sau không có cách nào biết là nó sai.
 *
 * Đường đăng thật KHÔNG chạy trong bài kiểm nào: nó ghi vào một kho công cộng
 * không rút lại được, và một bài kiểm không được phép làm thế.
 */
class LrclibPublishTest {

    // ---- nhoHon: so từng byte từ trái sang, đúng cách LRCLIB kiểm ----

    @Test
    fun `byte dau nho hon thi nho hon`() {
        assertTrue(LrclibPublish.nhoHon(bytes("00", "FF"), bytes("01", "00")))
    }

    @Test
    fun `byte dau lon hon thi khong nho hon`() {
        assertFalse(LrclibPublish.nhoHon(bytes("02", "00"), bytes("01", "FF")))
    }

    @Test
    fun `bang nhau o byte dau thi xet byte sau`() {
        assertTrue(LrclibPublish.nhoHon(bytes("00", "10"), bytes("00", "20")))
        assertFalse(LrclibPublish.nhoHon(bytes("00", "30"), bytes("00", "20")))
    }

    @Test
    fun `bang nhau het thi KHONG nho hon`() {
        // Bằng đúng đích là chưa đạt. Coi là đạt thì token gửi lên bị từ chối,
        // mà người dùng chỉ thấy "LRCLIB từ chối" không rõ vì sao.
        assertFalse(LrclibPublish.nhoHon(bytes("00", "20"), bytes("00", "20")))
    }

    @Test
    fun `so sanh khong dinh dau am cua byte`() {
        // Byte trong Kotlin có dấu: 0x80 là -128. So thẳng thì 0x80 hoá ra nhỏ
        // hơn 0x01, và mọi phép kiểm sai từ đó.
        assertFalse(LrclibPublish.nhoHon(bytes("80"), bytes("01")))
        assertTrue(LrclibPublish.nhoHon(bytes("01"), bytes("80")))
    }

    /** Đúng bài toán LRCLIB đặt ra, ở mức dễ để chạy nhanh. */
    @Test
    fun `giai duoc mot thu thach de`() {
        val prefix = "LyraTest"
        val dich = LrclibPublish.hex("0FFF" + "00".repeat(30))
        val bam = MessageDigest.getInstance("SHA-256")
        var n = 0L
        while (n < 100_000) {
            bam.reset()
            bam.update(prefix.toByteArray())
            bam.update(n.toString().toByteArray())
            if (LrclibPublish.nhoHon(bam.digest(), dich)) break
            n++
        }
        assertTrue("khong giai duoc trong 100k vong", n < 100_000)

        // Và kết quả phải kiểm lại được đúng như máy chủ sẽ kiểm.
        bam.reset()
        bam.update(prefix.toByteArray())
        bam.update(n.toString().toByteArray())
        assertTrue(LrclibPublish.nhoHon(bam.digest(), dich))
    }

    // ---- dungLrc: dựng lại chuỗi .lrc ----

    @Test
    fun `dung dinh dang phut giay tram`() {
        val ra = LrclibPublish.dungLrc(
            listOf(
                LyricLine(0L, "Đầu bài"),
                LyricLine(16_640L, "Em, ngày em đánh rơi nụ cười vào anh"),
                LyricLine(63_450L, "Chỉ tiếc rằng")
            )
        )
        assertEquals(
            "[00:00.00]Đầu bài\n" +
                "[00:16.64]Em, ngày em đánh rơi nụ cười vào anh\n" +
                "[01:03.45]Chỉ tiếc rằng",
            ra
        )
    }

    @Test
    fun `qua mot tieng van dung`() {
        // Mốc phút KHÔNG quay vòng ở 60: một bản thu dài 70 phút phải ra
        // "[70:00.00]", không phải "[10:00.00]". Định dạng .lrc cho phép.
        assertEquals("[70:00.00]x", LrclibPublish.dungLrc(listOf(LyricLine(4_200_000L, "x"))))
    }

    @Test
    fun `dong trong van giu mot moc`() {
        // Khoảng lặng giữa hai đoạn là một dòng có mốc mà không có chữ. Bỏ nó
        // đi thì bản lời gửi lên mất chỗ nghỉ.
        assertEquals("[00:05.00]", LrclibPublish.dungLrc(listOf(LyricLine(5_000L, ""))))
    }

    private fun bytes(vararg hex: String): ByteArray =
        LrclibPublish.hex(hex.joinToString(""))
}
