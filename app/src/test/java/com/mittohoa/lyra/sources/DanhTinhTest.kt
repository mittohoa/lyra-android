package com.mittohoa.lyra.sources

import com.mittohoa.lyra.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dong tu xung gui ra ngoai la thu duy nhat mang ten Lyra roi khoi may nguoi
 * dung, nen no phai dung so hieu va tuyet doi khong kem gi cua nguoi dung.
 */
class DanhTinhTest {

    @Test fun `mang dung so hieu ban dang chay`() {
        // Truoc day go tay "Lyra/0.1.0" va "Lyra/1.0" trong khi app da 0.3.4.
        // Buoc no phai lay tu ban dung thi khong the lech duoc nua.
        assertTrue(
            "phải mang số hiệu ${BuildConfig.VERSION_NAME}, đang là: $DANH_TINH",
            DANH_TINH.contains(BuildConfig.VERSION_NAME)
        )
        assertTrue(DANH_TINH.startsWith("Lyra/"))
    }

    @Test fun `chi co mot dong tu xung duy nhat`() {
        // Hai noi go tay hai kieu la loi cu. Ca hai gio deu tro toi hang nay.
        assertEquals(DANH_TINH, "Lyra/${BuildConfig.VERSION_NAME} " +
            "(https://mittohoa.github.io/lyra-player/)")
    }

    @Test fun `khong kem gi cua nguoi dung`() {
        // Khong the kiem "khong co du lieu ca nhan" mot cach tong quat, nhung
        // kiem duoc rang hang nay la HANG CO DINH: no khong ghep them mot bien
        // nao ngoai so hieu ban dung, nen khong co cho nao de lot du lieu vao.
        val chuaSoHieu = DANH_TINH.replace(BuildConfig.VERSION_NAME, "")
        assertEquals("Lyra/ (https://mittohoa.github.io/lyra-player/)", chuaSoHieu)
    }
}
