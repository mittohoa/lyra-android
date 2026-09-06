package com.mittohoa.lyra.sources

import com.mittohoa.lyra.lyrics.normalizeForCompare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiem viec tim mot bai trong may.
 *
 * Bon truong duoc so - ten bai, ca si, album, thu muc - va sot mot cai thi
 * KHONG CO GI BAO. O tim van chay, van tra ve ket qua, chi la it hon dang le,
 * va nguoi dung ket luan "app khong tim thay bai cua minh".
 */
class TimTrongMayTest {

    private fun bai(
        title: String = "",
        artist: String = "",
        album: String = "",
        thuMuc: String = ""
    ) = Track(
        id = "1",
        source = MusicSource.LOCAL,
        title = title,
        artist = artist,
        album = album,
        thuMuc = thuMuc
    )

    private fun khop(bai: Track, tuKhoa: String) =
        LocalLibrary.khop(bai, normalizeForCompare(tuKhoa))

    // ---- Bon truong deu duoc so ----

    @Test fun `khop ten bai`() {
        assertTrue(khop(bai(title = "Nàng Thơ"), "nang tho"))
    }

    @Test fun `khop ten ca si`() {
        assertTrue(khop(bai(artist = "Hoàng Dũng"), "hoang dung"))
    }

    @Test fun `khop ten album`() {
        // Day la cho bi sot truoc 0.3.23. Man hinh BAY RA ten album lam dau
        // hieu nhan biet, roi go dung cai ten do vao o tim thi khong ra gi.
        assertTrue(khop(bai(album = "Touch Of Light"), "touch of light"))
    }

    @Test fun `khop ten thu muc`() {
        // Rat nhieu nguoi xep nhac theo thu muc chu khong theo the, va voi ho
        // thi ten thu muc CHINH LA ten album.
        assertTrue(khop(bai(thuMuc = "Trịnh Công Sơn"), "trinh cong son"))
    }

    // ---- Cach nguoi Viet go tren dien thoai ----

    @Test fun `khong dau van tim ra bai co dau`() {
        assertTrue(khop(bai(album = "Ru Ta Ngậm Ngùi"), "ngam ngui"))
    }

    @Test fun `khong phan biet hoa thuong`() {
        assertTrue(khop(bai(thuMuc = "CD1 - Nhac Vang"), "NHAC VANG"))
    }

    @Test fun `khop mot phan giua chuoi, khong phai chi dau chuoi`() {
        assertTrue(khop(bai(album = "Best Of Trịnh"), "of trinh"))
    }

    // ---- Khong khop thi phai KHONG khop ----

    @Test fun `khong dinh dang toi thi khong khop`() {
        assertFalse(khop(bai(title = "Nàng Thơ", album = "Ru Ta"), "cat bui"))
    }

    @Test fun `truong rong khong keo ca thu vien vao ket qua`() {
        // `"".contains(x)` la false voi moi `x` khong rong - nhung day la loai
        // menh de de dao nguoc luc sua vu vo, nen ghim lai.
        assertFalse(khop(bai(title = "Nàng Thơ"), "khong co gi"))
    }

    // ---- Bo loc tren ca danh sach ----

    @Test fun `filter tra ve dung nhung bai khop, khong lan bai khac`() {
        val thuVien = listOf(
            bai(title = "Nàng Thơ", album = "Ru Ta"),
            bai(title = "Cát Bụi", album = "Trịnh Ca"),
            bai(title = "Diễm Xưa", thuMuc = "Trinh Cong Son")
        )
        val ra = LocalLibrary.filter(thuVien, "trinh", 50)
        assertEquals(listOf("Cát Bụi", "Diễm Xưa"), ra.map { it.title })
    }

    @Test fun `filter ton trong tran so ket qua`() {
        val thuVien = List(10) { bai(title = "Bài $it", album = "Chung") }
        assertEquals(3, LocalLibrary.filter(thuVien, "chung", 3).size)
    }

    @Test fun `tu khoa rong tra ve rong, khong tra ve ca thu vien`() {
        // O tim con trong ma da do ca thu vien ra thi man hinh nhay mot cai
        // ngay khi vua cham vao o.
        val thuVien = listOf(bai(title = "Nàng Thơ"))
        assertTrue(LocalLibrary.filter(thuVien, "   ", 50).isEmpty())
    }
}
