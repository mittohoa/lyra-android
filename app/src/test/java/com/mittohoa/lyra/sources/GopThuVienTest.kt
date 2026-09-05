package com.mittohoa.lyra.sources

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kiem cach tron thu vien he thong voi thu muc nguoi dung tu tro vao.
 *
 * Hai viec de sai o day, va ca hai deu khong lo ra ngay:
 *
 *   - CHONG TRUNG. Nguoi dung rat de tro vao dung mot thu muc MediaStore da
 *     quet, vi ho dau biet truoc thu muc nao bi bo sot - cach duy nhat de biet
 *     la tro vao roi xem. Sai cho nay thi cai gia cua viec thu la ca thu vien
 *     nhan doi.
 *   - THU TU. Hai nguon von xep theo hai luat khac nhau; tron vao mot danh
 *     sach ma khong ap lai mot luat thi thu tu nhin nhu ngau nhien.
 */
class GopThuVienTest {

    private fun bai(
        ten: String,
        caSi: String = "Ai Do",
        dai: Long = 200_000,
        loai: MediaKind = MediaKind.AUDIO,
        album: String = ""
    ) = Track(
        id = "ma-$ten",
        source = MusicSource.LOCAL,
        title = ten,
        artist = caSi,
        album = album,
        durationMs = dai,
        kind = loai
    )

    private fun ten(bai: List<Track>) = bai.map { it.title }

    // ---- Chong trung ----

    @Test fun `bai da co trong danh muc he thong thi khong them lan hai`() {
        val trongMay = listOf(bai("Nang Tho"))
        val ngoai = listOf(bai("Nang Tho"))
        assertEquals(listOf("Nang Tho"), ten(ThuVienNgoai.gop(trongMay, ngoai)))
    }

    @Test fun `cung ten cung ca si nhung khac do dai thi giu ca hai`() {
        // Ban goc va ban phoi lai thuong trung ca ten lan nghe si, chi khac do
        // dai. Gop lam mot la nguoi dung mat mot ban.
        val trongMay = listOf(bai("Nang Tho", dai = 200_000))
        val ngoai = listOf(bai("Nang Tho", dai = 245_000))
        assertEquals(2, ThuVienNgoai.gop(trongMay, ngoai).size)
    }

    @Test fun `lech vai mili giay van tinh la mot bai`() {
        // MediaStore va MediaMetadataRetriever doc cung mot tep ra hai con so
        // chenh nhau vai mili giay. Khong lam tron thi moi bai deu nhan doi.
        val trongMay = listOf(bai("Nang Tho", dai = 200_120))
        val ngoai = listOf(bai("Nang Tho", dai = 200_880))
        assertEquals(1, ThuVienNgoai.gop(trongMay, ngoai).size)
    }

    @Test fun `so ten khong dau va khong phan biet hoa thuong`() {
        val trongMay = listOf(bai("Nàng Thơ", caSi = "Hoàng Dũng"))
        val ngoai = listOf(bai("nang tho", caSi = "hoang dung"))
        assertEquals(1, ThuVienNgoai.gop(trongMay, ngoai).size)
    }

    @Test fun `bai that su moi thi duoc them vao`() {
        val trongMay = listOf(bai("Nang Tho"))
        val ngoai = listOf(bai("Bai Khac"))
        assertEquals(listOf("Bai Khac", "Nang Tho"), ten(ThuVienNgoai.gop(trongMay, ngoai)))
    }

    // ---- Thu tu ----

    @Test fun `chu co dau nam canh chu khong dau, khong bi don xuong cuoi`() {
        // Day la ly do phai xep lai. `COLLATE NOCASE` cua SQLite chi biet gap
        // hoa-thuong cua bang chu ASCII, nen moi ten bat dau bang chu co dau
        // deu bi day xuong sau chu `z` - "Anh" nam sau "Zoo".
        val trongMay = listOf(bai("Zoo"), bai("Ánh"), bai("An"))
        assertEquals(listOf("An", "Ánh", "Zoo"), ten(ThuVienNgoai.gop(trongMay, emptyList())))
    }

    @Test fun `xep lai ca khi khong co thu muc tu tro nao`() {
        // Mot luat duy nhat, ap cho tat ca. Bo qua nhanh nay thi thu vien cua
        // nguoi khong bat tinh nang nay van xep theo luat cu.
        val trongMay = listOf(bai("Zoo"), bai("Ánh"))
        assertEquals(listOf("Ánh", "Zoo"), ten(ThuVienNgoai.gop(trongMay, emptyList())))
    }

    @Test fun `cung album ma the ghi khac chu hoa van nam lien nhau`() {
        // Do duoc tren may that: mot album tai ve co ba tep ghi "Touch Of
        // Light" va mot tep ghi "Touch of Light". Xep theo chu nguyen ban thi
        // album ay bi cat lam doi, moi nua mot tieu de, va nguoi dung khong co
        // cach nao doan ra vi sao.
        val trongMay = listOf(
            bai("Illusion Original", album = "Touch Of Light"),
            bai("Zzz Bai Khac", album = "Zzz Album Khac"),
            bai("Illusion Remix", album = "Touch of Light")
        )
        val ra = ThuVienNgoai.gop(trongMay, emptyList())
        // Hai bai cua cung album phai dung canh nhau, khong bi bai cua album
        // khac chen vao giua.
        assertEquals(
            listOf("Illusion Original", "Illusion Remix", "Zzz Bai Khac"),
            ten(ra)
        )
    }

    @Test fun `bai khong co album xep theo nhom rong, khong lan vao album khac`() {
        val trongMay = listOf(
            bai("Bbb Co Album", album = "Mmm Album"),
            bai("Aaa Khong Album")
        )
        // Nhóm rỗng đứng trước mọi nhóm có tên.
        assertEquals(listOf("Aaa Khong Album", "Bbb Co Album"), ten(ThuVienNgoai.gop(trongMay, emptyList())))
    }

    @Test fun `nhac di truoc phim`() {
        val trongMay = listOf(
            bai("Zzz Phim", loai = MediaKind.VIDEO),
            bai("Aaa Nhac")
        )
        val ngoai = listOf(
            bai("Bbb Phim Moi", loai = MediaKind.VIDEO),
            bai("Ccc Nhac Moi")
        )
        assertEquals(
            listOf("Aaa Nhac", "Ccc Nhac Moi", "Bbb Phim Moi", "Zzz Phim"),
            ten(ThuVienNgoai.gop(trongMay, ngoai))
        )
    }

    @Test fun `khong co gi ben ngoai thi khong mat bai nao`() {
        val trongMay = listOf(bai("Mot"), bai("Hai"), bai("Ba", loai = MediaKind.VIDEO))
        assertEquals(3, ThuVienNgoai.gop(trongMay, emptyList()).size)
    }
}
