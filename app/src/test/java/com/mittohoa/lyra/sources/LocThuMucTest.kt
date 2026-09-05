package com.mittohoa.lyra.sources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kiem menh de loc thu vien theo thu muc duoc phep quet.
 *
 * Day la cho bien mot lua chon cua nguoi dung thanh mot cau SQL, va sai o day
 * khong bao gi ca - thu vien chi lang le thua ra hoac thieu di vai bai.
 */
class LocThuMucTest {

    // ---- Thoat ky tu ----

    @Test fun `gach duoi duoc thoat`() {
        // `_` trong LIKE la "mot ky tu bat ky". Khong thoat thi thu muc
        // `My_Nhac` cung khop `MyXNhac` - pham vi rong hon nguoi dung dat ra.
        assertEquals("/storage/My\\_Nhac", LocalLibrary.thoatLike("/storage/My_Nhac"))
    }

    @Test fun `phan tram duoc thoat`() {
        assertEquals("/storage/100\\%", LocalLibrary.thoatLike("/storage/100%"))
    }

    @Test fun `dau cheo nguoc duoc thoat TRUOC, khong thi hong ca hai cai kia`() {
        // Thu tu la tat ca. Thoat `_` truoc roi moi thoat `\` thi chinh dau
        // cheo vua them vao lai bi nhan doi, va menh de het nghia.
        assertEquals("a\\\\b\\_c", LocalLibrary.thoatLike("a\\b_c"))
    }

    @Test fun `duong dan binh thuong thi khong doi gi`() {
        assertEquals(
            "/storage/emulated/0/Music",
            LocalLibrary.thoatLike("/storage/emulated/0/Music")
        )
    }

    // ---- Menh de ----

    @Test fun `khong co thu muc nao thi khong gioi han`() {
        // `null` chu khong phai mot menh de luon dung: bat khong gioi han
        // thanh mot cau SQL thua la bat co so du lieu lam viec vo ich.
        assertNull(LocalLibrary.locThuMuc("_data", emptyList()))
    }

    @Test fun `mot thu muc ra mot ve LIKE`() {
        val (ve, thamSo) = LocalLibrary.locThuMuc("_data", listOf("/storage/emulated/0/Music"))!!
        assertEquals("(_data LIKE ? ESCAPE '\\')", ve)
        assertEquals(listOf("/storage/emulated/0/Music/%"), thamSo.toList())
    }

    @Test fun `nhieu thu muc noi bang OR`() {
        val (ve, thamSo) = LocalLibrary.locThuMuc(
            "_data",
            listOf("/storage/emulated/0/Music", "/storage/1A2B-3C4D/Nhac")
        )!!
        assertEquals("(_data LIKE ? ESCAPE '\\' OR _data LIKE ? ESCAPE '\\')", ve)
        assertEquals(
            listOf("/storage/emulated/0/Music/%", "/storage/1A2B-3C4D/Nhac/%"),
            thamSo.toList()
        )
    }

    @Test fun `tham so mang duong da thoat`() {
        val (_, thamSo) = LocalLibrary.locThuMuc("_data", listOf("/storage/My_Nhac"))!!
        assertEquals(listOf("/storage/My\\_Nhac/%"), thamSo.toList())
    }
}
