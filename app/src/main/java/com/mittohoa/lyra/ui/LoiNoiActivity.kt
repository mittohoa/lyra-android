package com.mittohoa.lyra.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.mittohoa.lyra.service.Lyra

/**
 * Bật hoặc tắt khung lời nổi, KHÔNG mở app.
 *
 * Android không cho một lối tắt gọi thẳng vào hàm nào cả — nó chỉ biết mở một
 * Activity. Nên đây là một Activity không vẽ gì, làm đúng một việc rồi tự đóng
 * ngay trong `onCreate`: người dùng thấy khung lời hiện lên trên chính app nhạc
 * họ đang mở, không thấy AURA nhấp nháy một cái ở giữa.
 *
 * TRÙNG VIỆC VỚI Ô QUICK SETTINGS, và đó là cố ý. Ô kia nằm trong bảng kéo
 * xuống, lối tắt này nằm ở màn hình chính — cùng một việc, hai chỗ với tới, và
 * người ta quen tay chỗ nào thì dùng chỗ đó.
 *
 * CHỖ CÒN GỢN, và ĐÃ ĐO nên đừng thử lại: bấm lối tắt xong, bảng lối tắt của
 * Pixel Launcher KHÔNG tự đóng. Khung lời vẫn bật đúng và hiện ra ngay sau
 * bảng, nên việc bấm không mất, chỉ là bảng nằm lại. Đã thử hai ngõ, cả hai đều
 * không đổi được gì:
 *
 *   - đổi `Theme.NoDisplay` sang `Theme.Translucent.NoTitleBar` để hoạt động
 *     này thật sự "được hiện", mong trình khởi chạy nhận một nhịp tạm dừng
 *   - dời `finish()` xuống `onResume`, rồi lùi thêm một khung hình nữa bằng
 *     `decorView.post`
 *
 * Cả hai vẫn để bảng nằm nguyên. Đây là cách trình khởi chạy quyết định đóng
 * bảng của nó, không phải chỗ AURA với tới được.
 */
class LoiNoiActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chưa cấp quyền vẽ đè thì bật cũng không hiện được gì, mà một lối tắt
        // bấm xong không xảy ra chuyện gì thì người dùng tưởng máy đơ. Mở app
        // ra — trong đó có sẵn thẻ nói cần quyền gì và nút đi cấp.
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            Lyra.toggleOverlay(applicationContext)
        }

        // Đóng NGAY trong `onCreate`, trước cả `onResume`. Đây là điều kiện bắt
        // buộc của `Theme.NoDisplay`, và cũng là lý do màn hình không chớp.
        finish()
    }
}
