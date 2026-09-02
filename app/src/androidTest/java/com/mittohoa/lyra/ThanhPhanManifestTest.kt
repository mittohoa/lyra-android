package com.mittohoa.lyra

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mọi lớp mà manifest khai đều phải TỒN TẠI THẬT.
 *
 * Lý do có bài kiểm này: một lần đổi tên đã sửa nhầm ba dòng trong manifest
 * thành tên lớp không hề tồn tại — `.service.AuraNotificationListener` trong khi
 * lớp thật vẫn là `LyraNotificationListener`. Trình biên dịch KHÔNG BẮT ĐƯỢC:
 * tên trong manifest chỉ là chuỗi, hệ thống mới tra lúc chạy tới đúng thành
 * phần đó. Nên app dựng sạch, cài được, mở được — rồi sập ngay lúc người dùng
 * cấp quyền đọc thông báo, phát nhạc, hoặc bấm ô cài đặt nhanh.
 *
 * Ba đường đó đều nằm ngoài tầm với của bài kiểm bình thường: chúng chỉ chạy
 * khi HỆ THỐNG dựng thành phần lên. Bài này đi đường vòng — hỏi hệ thống xem
 * manifest khai những gì, rồi thử nạp từng lớp một.
 */
class ThanhPhanManifestTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun moiLopManifestKhaiDeuNapDuoc() {
        val pm = ctx.packageManager
        val co = PackageManager.GET_SERVICES or PackageManager.GET_ACTIVITIES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or
            PackageManager.MATCH_DISABLED_COMPONENTS
        val goi = pm.getPackageInfo(ctx.packageName, co)

        val ten = buildList {
            goi.services?.forEach { add("dịch vụ: " + it.name) }
            goi.activities?.forEach { add("màn hình: " + it.name) }
            goi.receivers?.forEach { add("bộ nhận: " + it.name) }
            goi.providers?.forEach { add("nguồn: " + it.name) }
        }

        assertTrue("manifest phải khai ít nhất vài thành phần", ten.size >= 3)

        val hong = ten.filter { dong ->
            val lop = dong.substringAfter(": ")
            runCatching { Class.forName(lop, false, ctx.classLoader) }.isFailure
        }

        assertTrue(
            "manifest trỏ tới lớp không tồn tại:\n" + hong.joinToString("\n"),
            hong.isEmpty()
        )
    }
}
