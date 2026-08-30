package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chuyen tu `scripts/test-identify.ts` cua ban Windows, giu nguyen tung truong
 * hop. Cac chuoi duoi day lay tu ten video that tren YouTube.
 */
class IdentifyTest {

    /** Co phuong an nao khop cap (nghe si, ten bai) mong doi khong. */
    private fun has(list: List<Candidate>, artist: String, title: String) =
        list.any {
            titleSimilarity(it.artist, artist) >= 0.99 && titleSimilarity(it.title, title) >= 0.99
        }

    @Test
    fun `boc cum OFFICIAL MUSIC VIDEO giua ten`() {
        val c = candidatesFrom(RawNowPlaying("NƠI NÀY CÓ ANH | OFFICIAL MUSIC VIDEO | SƠN TÙNG M-TP"))
        assertTrue(c.take(3).toString(), has(c, "SƠN TÙNG M-TP", "NƠI NÀY CÓ ANH"))
    }

    @Test
    fun `boc ngoac quang cao va duoi YouTube`() {
        val c = candidatesFrom(
            RawNowPlaying("Hà Anh Tuấn - Nhà Tôi Có Treo Một Lá Cờ (Official Lyric Video) - YouTube")
        )
        assertTrue(c.take(3).toString(), has(c, "Hà Anh Tuấn", "Nhà Tôi Có Treo Một Lá Cờ"))
    }

    @Test
    fun `boc ngoac kieu Nhat va duoi 4K`() {
        val c = candidatesFrom(RawNowPlaying("【MV】Bó Hoa - Vũ Cát Tường「Lyrics Video」4K"))
        assertTrue(c.take(3).toString(), has(c, "Vũ Cát Tường", "Bó Hoa"))
    }

    @Test
    fun `bo duoi Official trong ten kenh`() {
        val c = candidatesFrom(RawNowPlaying("NƠI NÀY CÓ ANH", artist = "Sơn Tùng M-TP Official"))
        assertEquals("Sơn Tùng M-TP", c[0].artist)
        assertEquals("NƠI NÀY CÓ ANH", c[0].title)
    }

    @Test
    fun `bo duoi Topic ma YouTube Music them vao`() {
        val c = candidatesFrom(RawNowPlaying("Chúng Ta Của Hiện Tại", artist = "Sơn Tùng M-TP - Topic"))
        assertEquals("Sơn Tùng M-TP", c[0].artist)
    }

    @Test
    fun `nghe si tu app duoc uu tien cao nhat`() {
        val c = candidatesFrom(RawNowPlaying("A - B", artist = "Nghệ Sĩ Thật"))
        assertEquals("phuong an dau phai dung nghe si app khai bao", "Nghệ Sĩ Thật", c[0].artist)
        assertTrue("van phai co phuong an du phong tu viec tach ten", c.size > 1)
    }

    @Test
    fun `sinh ca hai chieu nghe si va ten bai`() {
        val c = candidatesFrom(RawNowPlaying("Sơn Tùng M-TP - Nơi Này Có Anh"))
        assertTrue("chieu thuan", has(c, "Sơn Tùng M-TP", "Nơi Này Có Anh"))
        assertTrue("chieu nguoc", has(c, "Nơi Này Có Anh", "Sơn Tùng M-TP"))

        // Chieu thuan phai duoc thu truoc
        val iThuan = c.indexOfFirst { titleSimilarity(it.artist, "Sơn Tùng M-TP") >= 0.99 }
        val iNguoc = c.indexOfFirst { titleSimilarity(it.title, "Sơn Tùng M-TP") >= 0.99 }
        assertTrue("chieu nghe si truoc ten bai phai duoc uu tien", iThuan < iNguoc)
    }

    @Test
    fun `giu lai ngoac mang thong tin that`() {
        val c = candidatesFrom(RawNowPlaying("Nơi Này Có Anh (Remix) - Sơn Tùng M-TP"))
        assertTrue(
            "khong duoc boc mat chu Remix: ${c.take(3)}",
            c.any { it.title.contains("Remix") }
        )
    }

    @Test
    fun `ten khong tach duoc thi van tra ve mot phuong an`() {
        val c = candidatesFrom(RawNowPlaying("Bohemian Rhapsody"))
        assertTrue(c.isNotEmpty())
        assertEquals("", c.last().artist)
        assertEquals("Bohemian Rhapsody", c.last().title)
    }

    @Test
    fun `chuoi rong thi khong no`() {
        assertTrue(candidatesFrom(RawNowPlaying("")).isEmpty())
        assertTrue(candidatesFrom(RawNowPlaying("   ")).isEmpty())
    }

    @Test
    fun `chuoi toan tu rac thi khong sinh phuong an vo nghia`() {
        val c = candidatesFrom(RawNowPlaying("(Official Music Video) [4K]"))
        assertEquals("khong con gi that thi khong nen tra ve gi: $c", 0, c.size)
    }

    @Test
    fun `bo ten nghe si bi lap trong chinh ten bai`() {
        // Ten video YouTube rat hay lap lai ten nghe si o cuoi; app khai bao
        // nghe si rieng nen ta biet doan nao la thua
        val c = candidatesFrom(
            RawNowPlaying(
                "NƠI NÀY CÓ ANH | OFFICIAL MUSIC VIDEO | SƠN TÙNG M-TP",
                artist = "Sơn Tùng M-TP Official"
            )
        )
        assertEquals("Sơn Tùng M-TP", c[0].artist)
        assertEquals(c.take(3).toString(), "NƠI NÀY CÓ ANH", c[0].title)
    }

    @Test
    fun `khong bo nham khi ten bai chinh la ten nghe si`() {
        // Bai trung ten nghe si - bo het thi con chuoi rong, phai giu phuong an goc
        val c = candidatesFrom(RawNowPlaying("Hà Anh Tuấn", artist = "Hà Anh Tuấn"))
        assertTrue("khong duoc xoa sach ten bai: $c", c.any { it.title == "Hà Anh Tuấn" })
    }

    @Test
    fun `do giong nhau bo qua dau va phan thua`() {
        assertEquals(1.0, titleSimilarity("Nơi Này Có Anh", "noi nay co anh"), 0.001)
        assertEquals(1.0, titleSimilarity("Nơi Này Có Anh", "Nơi Này Có Anh (Remix)"), 0.001)
        assertTrue(titleSimilarity("Nơi Này Có Anh", "Chúng Ta Của Hiện Tại") < 0.3)
        assertEquals(0.0, titleSimilarity("", "abc"), 0.001)
    }
}
