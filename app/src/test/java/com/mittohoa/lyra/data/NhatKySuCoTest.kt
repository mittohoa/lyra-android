package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bộ bắt lỗi: nó ghi lại, rồi TRAO LẠI cho bộ bắt cũ.
 *
 * VẾ THỨ HAI MỚI LÀ VẾ NGUY HIỂM. Nuốt mất lỗi thì app không tắt nữa mà đứng
 * chết dở — màn hình còn đó, bấm không ăn, và người dùng không hiểu chuyện gì.
 * Tắt hẳn còn dễ hiểu hơn nhiều. Một bộ ghi sự cố mà làm app treo thay vì tắt
 * là đổi một lỗi rõ ràng lấy một lỗi không ai gỡ nổi.
 *
 * Ở đây kiểm phần thuần Kotlin — chuỗi bắt và đếm. Phần chạm đĩa và chạm
 * Android cần máy thật, đo riêng.
 */
class NhatKySuCoTest {

    /** Bộ bắt giả, chỉ nhớ là mình có được gọi hay không. */
    private class BoBatGia : Thread.UncaughtExceptionHandler {
        var daGoi = false
        var nhanDuoc: Throwable? = null
        override fun uncaughtException(t: Thread, e: Throwable) {
            daGoi = true
            nhanDuoc = e
        }
    }

    @Test fun `dem dung so lan tu nhat ky`() {
        val chu = """
            === 2026-09-12 08:00:00
            ban   0.3.34 (45)
            java.lang.IllegalStateException: mot
            === 2026-09-12 09:30:00
            ban   0.3.34 (45)
            java.lang.NullPointerException: hai
        """.trimIndent()
        assertEquals(2, chu.lineSequence().count { it.startsWith("=== ") })
    }

    @Test fun `lay dung lan gan nhat, khong phai lan dau`() {
        val chu = "=== 2026-09-12 08:00:00\nvet\n=== 2026-09-12 09:30:00\nvet\n"
        val ganNhat = chu.lineSequence()
            .lastOrNull { it.startsWith("=== ") }
            ?.removePrefix("=== ")
            ?.trim()
        assertEquals("2026-09-12 09:30:00", ganNhat)
    }

    @Test fun `nhat ky rong thi dem bang khong`() {
        assertEquals(0, "".lineSequence().count { it.startsWith("=== ") })
    }

    /**
     * Đúng luật "không nuốt lỗi", viết lại bằng một chuỗi bắt nhỏ.
     *
     * [NhatKySuCo.BoBat] là `private`, nên ở đây kiểm CÁI LUẬT chứ không kiểm
     * chính lớp ấy: bọc một bộ bắt thì bộ bên trong vẫn phải được gọi, và phải
     * nhận đúng lỗi ban đầu.
     */
    @Test fun `boc mot bo bat thi bo cu VAN duoc goi voi dung loi`() {
        val cu = BoBatGia()
        var daGhi = false
        val moi = Thread.UncaughtExceptionHandler { t, e ->
            daGhi = true
            cu.uncaughtException(t, e)
        }

        val loi = IllegalStateException("thu")
        moi.uncaughtException(Thread.currentThread(), loi)

        assertTrue("phai ghi lai", daGhi)
        assertTrue("bo cu phai duoc goi, khong duoc nuot", cu.daGoi)
        assertSame("bo cu phai nhan dung loi ban dau", loi, cu.nhanDuoc)
    }

    @Test fun `loi xay ra trong luc ghi khong duoc nuot mat bo cu`() {
        // Nếu phần ghi ném lỗi mà không bọc, hệ thống mất luôn bộ bắt cũ và app
        // tắt không một dấu vết nào — mất đúng thứ ta đang cố giữ.
        val cu = BoBatGia()
        val moi = Thread.UncaughtExceptionHandler { t, e ->
            try {
                error("ghi that bai")
            } catch (_: Throwable) {
            }
            cu.uncaughtException(t, e)
        }

        val loi = RuntimeException("thu")
        moi.uncaughtException(Thread.currentThread(), loi)
        assertTrue(cu.daGoi)
        assertNotNull(cu.nhanDuoc)
    }
}
