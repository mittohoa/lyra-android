package com.mittohoa.lyra.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ghi lại lần app tự tắt, để người dùng có cái mà kể lại.
 *
 * VÌ SAO CẦN. Chín bản ra trong mấy ngày, tất cả đo trên đúng một chiếc máy
 * của chính người viết. Bản đầu tiên tới tay người lạ mà tắt ngang thì họ
 * không có cách nào kể cho ai — và người viết không có cách nào biết. Một app
 * mà lỗi chỉ tồn tại trong đầu người gặp nó thì không bao giờ sửa được.
 *
 * KHÔNG GỬI ĐI ĐÂU CẢ. Không máy chủ, không dịch vụ thu thập sự cố, không một
 * gói thư viện nào của bên thứ ba. Nhật ký nằm trong bộ nhớ riêng của app, và
 * nó chỉ rời khỏi máy khi người dùng TỰ BẤM gửi, tới nơi họ tự chọn.
 *
 * Điều đó không phải để cho sạch tiếng. Bản này vừa tắt sao lưu ngầm của
 * Android vì không muốn dữ liệu người dùng tự đi khỏi máy — rồi quay sang gắn
 * một bộ tự gửi sự cố thì đó là nói một đằng làm một nẻo.
 *
 * KHÔNG NUỐT LỖI. Bộ bắt này ghi xong thì trao lại cho bộ bắt cũ của hệ thống.
 * Nuốt đi thì app không tắt nữa mà đứng chết dở: màn hình còn đó, bấm không ăn,
 * và người dùng không hiểu chuyện gì. Tắt hẳn còn dễ hiểu hơn.
 */
object NhatKySuCo {

    private const val TEN_TEP = "su-co.txt"

    /**
     * Trần của tệp nhật ký.
     *
     * Một vòng lặp lỗi có thể ghi hàng nghìn dòng trong vài giây. Không có trần
     * thì thứ sinh ra để giúp gỡ lỗi lại là thứ làm đầy bộ nhớ máy.
     */
    private const val TRAN_BYTE = 128 * 1024

    private val GIO = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun tep(context: Context) = File(context.applicationContext.filesDir, TEN_TEP)

    /**
     * Gắn bộ bắt lỗi. Gọi càng sớm càng tốt, ở mọi cửa vào của app.
     *
     * Gọi nhiều lần không sao: lần thứ hai trở đi nhìn thấy bộ bắt đã là của
     * mình thì thôi. Thiếu chỗ chặn ấy thì mỗi lần gọi lại lồng thêm một tầng,
     * và một lỗi ghi ra nhiều bản.
     */
    fun gan(context: Context) {
        val cu = Thread.getDefaultUncaughtExceptionHandler()
        if (cu is BoBat) return
        val app = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler(BoBat(app, cu))
    }

    private class BoBat(
        private val app: Context,
        private val cu: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            // Bọc trong try: một lỗi lúc đang ghi lỗi mà ném tiếp thì hệ thống
            // mất luôn cả bộ bắt cũ, và app tắt không một dấu vết nào.
            try {
                ghi(app, t, e)
            } catch (_: Throwable) {
            }
            cu?.uncaughtException(t, e)
        }
    }

    private fun ghi(context: Context, t: Thread, e: Throwable) {
        val f = tep(context)
        if (f.exists() && f.length() > TRAN_BYTE) f.delete()

        val vet = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
        val ban = buildString {
            append("=== ").append(GIO.format(Date())).append('\n')
            append("ban   ").append(banApp(context)).append('\n')
            append("may   ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("  Android ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("luong ").append(t.name).append('\n')
            append(vet).append('\n')
        }
        f.appendText(ban)
    }

    private fun banApp(context: Context): String = try {
        val p = context.packageManager.getPackageInfo(context.packageName, 0)
        p.versionName + " (" + p.longVersionCode + ")"
    } catch (_: Exception) {
        "khong ro"
    }

    /** Nội dung nhật ký, rỗng khi chưa có lần nào. */
    fun doc(context: Context): String =
        tep(context).takeIf { it.exists() }?.runCatching { readText() }?.getOrNull().orEmpty()

    /** Đã ghi được mấy lần tắt ngang. */
    fun dem(context: Context): Int {
        val chu = doc(context)
        if (chu.isBlank()) return 0
        return chu.lineSequence().count { it.startsWith("=== ") }
    }

    /** Lúc gần nhất app tự tắt, dạng người đọc được. `null` khi chưa lần nào. */
    fun lanGanNhat(context: Context): String? =
        doc(context).lineSequence()
            .lastOrNull { it.startsWith("=== ") }
            ?.removePrefix("=== ")
            ?.trim()

    fun xoa(context: Context) {
        runCatching { tep(context).delete() }
    }
}
