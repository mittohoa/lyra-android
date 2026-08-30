package com.mittohoa.lyra.sources

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Do thu ba nguon loi bang API THAT.
 *
 * Khong chay trong lan kiem tra thuong: no can mang, va ket qua phu thuoc vao
 * viec ben kia hom nay co doi gi khong - de chung voi cac phep kiem tra thuan
 * logic thi mot ngay dep troi ca bo se do vi ly do khong lien quan gi toi ma
 * nguon cua ta.
 *
 * Chay khi can:
 *
 *   LYRA_LIVE=1 ./gradlew :app:testDebugUnitTest --tests '*LiveSourcesProbe*' -i
 *
 * Day la cach duy nhat biet chac chu ky Zing con dung va khoa RC4 cua NCT con
 * giai duoc - hai thu do sai mot chi tiet la hong, ma khong bao loi ro rang.
 */
class LiveSourcesProbe {

    private val artist = "Sơn Tùng M-TP"
    private val title = "Nơi Này Có Anh"
    private val durationMs = 260_000L

    @Test
    fun `LRCLIB tra ve loi`() = live {
        val lyrics = runBlocking { LrclibClient.fetch(artist, title, durationMs) }
        report("LRCLIB", lyrics?.lines?.size, lyrics?.synced, lyrics?.matchedTitle)
        assertTrue("LRCLIB khong tra ve gi", (lyrics?.lines?.size ?: 0) > 0)
    }

    @Test
    fun `Zing tra ve loi - chung to chu ky con dung`() = live {
        val lyrics = runBlocking { ZingClient.fetch(artist, title, durationMs) }
        report("Zing", lyrics?.lines?.size, lyrics?.synced, lyrics?.matchedTitle)
        assertTrue(
            "Zing khong tra ve gi - co the chu ky da sai hoac ho doi API",
            (lyrics?.lines?.size ?: 0) > 0
        )
    }

    @Test
    fun `NCT tra ve loi - chung to giai ma RC4 con dung`() = live {
        val lyrics = runBlocking { NctClient.fetch(artist, title, durationMs) }
        report("NCT", lyrics?.lines?.size, lyrics?.synced, lyrics?.matchedTitle)
        assertTrue(
            "NCT khong tra ve gi - co the khoa RC4 hoac API da doi",
            (lyrics?.lines?.size ?: 0) > 0
        )
        // Ban co moc thoi gian cua NCT chinh la ban RC4; ra duoc no nghia la
        // giai ma dung, khong phai vo tinh doc duoc ban chu tron
        if (lyrics?.synced == true) {
            println("    → giải mã RC4 thành công, có mốc thời gian")
        }
    }

    private fun live(block: () -> Unit) {
        assumeTrue(
            "Bo qua: dat LYRA_LIVE=1 de do bang API that",
            System.getenv("LYRA_LIVE") != null
        )
        block()
    }

    private fun report(source: String, lines: Int?, synced: Boolean?, matched: String?) {
        println(
            "  $source: ${lines ?: 0} dòng, " +
                (if (synced == true) "có mốc thời gian" else "không có mốc") +
                ", khớp với \"${matched.orEmpty()}\""
        )
    }
}
