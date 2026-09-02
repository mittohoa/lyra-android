package com.mittohoa.lyra.sources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kiem cach tinh ti le khung hinh video.
 *
 * Cho de sai nhat cua ca phan video, va sai thi khong co dau hieu nao ngoai
 * "sao video quay doc lai nam ngang". Dien thoai GHI khung nam ngang roi kem
 * mot co xoay 90 do - bo qua co do thi moi video quay bang dien thoai deu bi
 * hieu nham.
 */
class TiLeKhungHinhTest {

    @Test fun `video nam ngang giu nguyen`() {
        assertEquals(16f / 9f, LocalLibrary.tiLeKhungHinh(1920, 1080, 0)!!, 0.001f)
    }

    @Test fun `video quay doc da luu dung chieu`() {
        // Vai may ghi thang 720x1280, khong kem goc xoay.
        assertEquals(9f / 16f, LocalLibrary.tiLeKhungHinh(720, 1280, 0)!!, 0.001f)
    }

    @Test fun `goc xoay 90 do thi dao lai`() {
        // Khung ghi 1920x1080 + xoay 90 nghia la nguoi ta cam doc dien thoai.
        assertEquals(9f / 16f, LocalLibrary.tiLeKhungHinh(1920, 1080, 90)!!, 0.001f)
    }

    @Test fun `goc xoay 270 do cung dao lai`() {
        assertEquals(9f / 16f, LocalLibrary.tiLeKhungHinh(1920, 1080, 270)!!, 0.001f)
    }

    @Test fun `goc xoay 180 do khong dao`() {
        // Lat nguoc chu khong dung len: khung van nam ngang.
        assertEquals(16f / 9f, LocalLibrary.tiLeKhungHinh(1920, 1080, 180)!!, 0.001f)
    }

    @Test fun `khong biet kich thuoc thi tra null`() {
        // MediaStore de trong cot nay voi mot so tep. Tra 0 hay tra bua mot so
        // deu dan toi mot khung hinh vo ly tren man hinh; tra null thi ben goi
        // dung ti le mac dinh.
        assertNull(LocalLibrary.tiLeKhungHinh(0, 0, 0))
        assertNull(LocalLibrary.tiLeKhungHinh(1920, 0, 0))
        assertNull(LocalLibrary.tiLeKhungHinh(-1, 1080, 0))
    }
}
