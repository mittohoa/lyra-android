package com.mittohoa.lyra.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.test.platform.app.InstrumentationRegistry
import com.mittohoa.lyra.data.KieuChu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Vẽ đủ sáu mẫu thẻ ra tệp để xem bằng mắt.
 *
 * Kiểm bằng máy chỉ nói được tấm ảnh đúng kích thước và không phải một mảng
 * trống trơn — nó không nói được tấm ảnh có ĐẸP không, mà đó mới là điểm của
 * tính năng này. Nên phần kiểm tự động giữ ở mức bắt lỗi thô (mẫu vẽ hụt, mẫu
 * ra một màu đặc, mẫu quên dòng thương hiệu), còn tệp PNG để lại trong thư mục
 * của app thì kéo về xem tay.
 *
 * Chạy xong lấy ảnh:
 *   adb pull /sdcard/Android/data/<id>/files/the-loi/
 */
class TheLoiTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun moiMauTrenCaHaiNenDeuVeRaAnhCoNoiDung() {
        val thuMuc = File(ctx.getExternalFilesDir(null), "the-loi").apply {
            deleteRecursively(); mkdirs()
        }
        val bia = biaGia()

        for (giay in listOf(false, true)) for (mau in MauThe.entries) {
            val anh = TheLoi.ve(
                context = ctx,
                cauHat = "Từng tia sáng mong manh từ nơi xa",
                tenBai = "Những Kẻ Mộng Mơ",
                caSi = "Noo Phước Thịnh",
                mauNhan = Color.rgb(0x7C, 0x3A, 0xED),
                laGiay = giay,
                kieuChu = KieuChu.SACH,
                mau = mau,
                bia = bia
            )

            assertEquals("${mau.name}/${giay}: sai chiều rộng", TheLoi.RONG, anh.width)
            assertEquals("${mau.name}/${giay}: sai chiều cao", TheLoi.CAO, anh.height)

            // Một mẫu vẽ hụt cho ra tấm ảnh một màu đặc. Đếm số màu khác nhau
            // trên một lưới thưa: chữ và nền phải khác nhau ít nhất vài màu.
            assertTrue("${mau.name}/${giay}: ảnh gần như một màu", soMau(anh) >= 4)

            // Dòng thương hiệu nằm ở góc dưới phải và phải hiện trên cả sáu mẫu.
            assertTrue("${mau.name}/${giay}: góc dưới phải trống trơn", coVet(anh))

            File(thuMuc, "${if (giay) "giay" else "muc"}-${mau.name.lowercase()}.png").outputStream().use {
                anh.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    /** Bìa giả: một dải chuyển màu, đủ để các mẫu dùng bìa có cái mà vẽ. */
    private fun biaGia(): Bitmap {
        val b = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        Canvas(b).drawRect(0f, 0f, 600f, 600f, Paint().apply {
            shader = LinearGradient(
                0f, 0f, 600f, 600f,
                Color.rgb(0x2E, 0x1A, 0x47), Color.rgb(0xE8, 0x9F, 0x3C),
                Shader.TileMode.CLAMP
            )
        })
        return b
    }

    private fun soMau(anh: Bitmap): Int {
        val mau = HashSet<Int>()
        for (y in 0 until anh.height step 17)
            for (x in 0 until anh.width step 17) mau += anh.getPixel(x, y)
        return mau.size
    }

    /**
     * Có nét vẽ nào ở góc dưới phải không — nơi đặt dòng thương hiệu.
     *
     * Không đọc được chữ, nên chỉ hỏi: dải đó có nhiều hơn một màu không. Nền
     * trơn thì chỉ một màu; có chữ đè lên thì thành nhiều.
     */
    private fun coVet(anh: Bitmap): Boolean {
        val mau = HashSet<Int>()
        for (y in TheLoi.CAO - 110 until TheLoi.CAO - 40 step 3)
            for (x in TheLoi.RONG - 420 until TheLoi.RONG - 60 step 3)
                mau += anh.getPixel(x, y)
        return mau.size > 1
    }
}
