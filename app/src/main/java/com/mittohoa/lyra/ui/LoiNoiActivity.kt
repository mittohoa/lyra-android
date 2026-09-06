package com.mittohoa.lyra.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.mittohoa.lyra.service.Lyra

/**
 * Bật hoặc tắt khung lời nổi, KHÔNG mở app ra.
 *
 * Android không cho một lối tắt gọi thẳng vào hàm nào cả — nó chỉ biết mở một
 * Activity. Nên đây là một Activity trong suốt, làm đúng một việc rồi tự đóng
 * ngay trong `onCreate`: người dùng thấy khung lời hiện lên trên chính app nhạc
 * họ đang mở, không thấy AURA nhấp nháy một cái ở giữa.
 *
 * TRÙNG VIỆC VỚI Ô QUICK SETTINGS, và đó là cố ý. Ô kia nằm trong bảng kéo
 * xuống, lối tắt này nằm ở màn hình chính — cùng một việc, hai chỗ với tới, và
 * người ta quen tay chỗ nào thì dùng chỗ đó.
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

        // Đóng NGAY trong `onCreate`, trước cả `onResume`. Đây là điều kiện để
        // một Activity không có giao diện không bị hệ thống kêu ca, và cũng là
        // lý do màn hình không chớp.
        finish()
    }
}
