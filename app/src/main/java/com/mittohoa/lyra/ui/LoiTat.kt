package com.mittohoa.lyra.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.mittohoa.lyra.R

/**
 * Hai lối tắt hiện ra khi nhấn giữ biểu tượng AURA.
 *
 * DỰNG BẰNG MÃ, KHÔNG PHẢI `res/xml/shortcuts.xml`. Bản đầu khai tĩnh trong XML
 * và nó HỎNG: `android:targetPackage` ở đó KHÔNG phân giải tham chiếu tài
 * nguyên. Viết `@string/id_app` thì hệ thống lưu lại nguyên chuỗi "@2131624022"
 * làm tên gói, trình khởi chạy đi tìm gói tên đó, không thấy, và báo "ứng dụng
 * chưa được cài đặt". Đo được trong `dumpsys shortcut`:
 *
 *     intents=[Intent { act=... cmp=@2131624022/com.mittohoa.lyra.ui.LoiNoiActivity }]
 *
 * Mà viết cứng "com.mittohoa.lyra_player" vào XML cũng không xong: bản gỡ lỗi
 * mang đuôi `.debug`, nên lối tắt của nó sẽ trỏ sang bản phát hành.
 *
 * Tên gói là một sự thật LÚC CHẠY. `Intent(context, Lop::class.java)` lấy đúng
 * tên gói của chính bản đang chạy, không có gì để mà sai.
 *
 * ĐỔI LẠI: lối tắt chỉ xuất hiện sau khi mở app lần đầu. Chấp nhận được — muốn
 * dùng lối tắt thì cũng phải cài rồi mở app ra cấp quyền đã.
 *
 * KHÔNG dùng cách "bỏ hẳn tên gói, để hệ thống tự phân giải theo action". Máy
 * đang phát triển thường cài CẢ HAI bản cạnh nhau, và lúc đó hai app cùng khai
 * một action — bấm lối tắt ra một hộp chọn giữa "AURA" và "AURA".
 */
object LoiTat {

    fun dat(context: Context) {
        val loiNoi = ShortcutInfoCompat.Builder(context, "loi_noi")
            .setShortLabel(context.getString(R.string.tile_label))
            .setLongLabel(context.getString(R.string.loi_tat_loi_noi_dai))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_loi_tat_loi_noi))
            .setIntent(
                Intent(context, LoiNoiActivity::class.java).setAction(Intent.ACTION_VIEW)
            )
            .build()

        val tim = ShortcutInfoCompat.Builder(context, "tim_bai")
            .setShortLabel(context.getString(R.string.loi_tat_tim))
            .setLongLabel(context.getString(R.string.loi_tat_tim_dai))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_loi_tat_tim))
            .setIntent(
                // Nói "mở trang Tìm" bằng ACTION RIÊNG chứ không bằng một
                // `extra`: `MainActivity` chỉ cần đọc `intent.action`, không
                // phải lo `getIntExtra` đọc trúng kiểu hay không.
                Intent(context, MainActivity::class.java).setAction(ACTION_TIM)
            )
            .build()

        // `set` chứ không `add`: đặt lại đúng danh sách này mỗi lần mở app, nên
        // đổi nhãn hay bớt một lối tắt là lần mở sau đã đúng. `add` thì lối tắt
        // của bản cũ nằm lại mãi.
        //
        // Bọc `try`: hệ thống có hạn mức số lối tắt và có thể từ chối. Không có
        // lối tắt thì app vẫn chạy bình thường, còn để nó ném ra thì app không
        // mở lên được — đổi một tiện nghi lấy cả cái app là món hời ngược.
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(loiNoi, tim))
        } catch (e: Exception) {
            // Không có lối tắt cũng không sao.
        }
    }
}
