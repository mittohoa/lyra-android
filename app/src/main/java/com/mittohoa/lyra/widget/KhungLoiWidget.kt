package com.mittohoa.lyra.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mittohoa.lyra.R
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.ui.MainActivity

/**
 * Widget khung lời: câu đang hát, trên màn hình chính.
 *
 * VÌ SAO CÓ. Widget nhạc của Zing và của NCT đều là bìa album cộng mấy nút bấm
 * — đúng với họ, vì sản phẩm của họ là bài nhạc. Sản phẩm của AURA là con chữ,
 * nên widget của nó bày ra con chữ. Không có nút phát, không có nút chuyển bài:
 * thẻ media trên màn hình khoá đã làm việc đó và làm tốt hơn, còn bày lại một
 * bộ điều khiển thứ hai chỉ tổ chia đôi chỗ mà không thêm được gì.
 *
 * NÓ CHẠY BẰNG GÌ. Phần khó — bám theo câu đang hát của nhạc phát ở app khác —
 * đã dựng sẵn cho khung lời nổi. Widget dùng lại đúng bộ máy đó: `Lyra` gọi
 * [dat] mỗi lần đổi câu, giống hệt cách nó đẩy câu lên thẻ media.
 *
 * ĐẨY CHỨ KHÔNG HỎI. `updatePeriodMillis` của Android thưa nhất là 30 phút, mà
 * một câu lời sống vài giây. Nên hệ thống không bao giờ tự gọi lại; AURA tự đẩy
 * bản mới, và chỉ đẩy KHI CHỮ ĐỔI THẬT — xem `Lyra.pushLineToWidget`.
 */
class KhungLoiWidget : AppWidgetProvider() {

    /**
     * Hệ thống gọi khi widget vừa được thả xuống màn hình, và sau mỗi lần khởi
     * động lại máy.
     *
     * Vẽ ngay bằng câu đang nhớ chứ không để trống chờ nhịp sau: người vừa thả
     * widget xuống mà thấy một ô rỗng thì tưởng nó hỏng. Rồi báo cho `Lyra`
     * bật lại nhịp — nếu lúc này nhạc đang phát ở app khác mà khung lời nổi
     * đang tắt thì nhịp đã dừng, và không ai đánh thức nó dậy nữa.
     */
    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ve(context, manager, ids, tenBai, cau, false)
        Lyra.widgetDoi(context)
    }

    /** Bớt một widget, nhưng có thể vẫn còn cái khác. Đếm lại. */
    override fun onDeleted(context: Context, ids: IntArray) {
        demLai()
    }

    /**
     * Widget cuối cùng vừa bị gỡ khỏi màn hình.
     *
     * Quên câu đang nhớ đi. Không quên thì lần sau thả widget xuống, nó hiện
     * lại một câu của buổi nghe hôm kia trong khi máy đang im.
     */
    override fun onDisabled(context: Context) {
        demLai()
        tenBai = null
        cau = null
    }

    companion object {
        /**
         * Câu đang bày và bài của nó.
         *
         * Giữ trong bộ nhớ chứ không ghi xuống đĩa: nó chỉ có nghĩa trong lúc
         * đang có nhạc, mà lúc đó tiến trình còn sống. Tiến trình chết thì nhạc
         * cũng đã tắt, và một câu lời cũ bày lại là nói dối.
         */
        private var tenBai: String? = null
        private var cau: String? = null

        /**
         * Có widget nào đang nằm trên màn hình không — NHỚ LẠI CÂU TRẢ LỜI.
         *
         * `getAppWidgetIds` là một cú gọi sang tiến trình hệ thống, mà câu hỏi
         * này bị hỏi mỗi nhịp, tức mỗi giây suốt cả bài. Số widget thì chỉ đổi
         * lúc người dùng thêm hoặc gỡ, và cả hai lúc đó hệ thống đều gọi vào
         * lớp này — nên chỉ cần quên đi ở đúng hai chỗ đó.
         *
         * `null` nghĩa là chưa hỏi lần nào.
         */
        @Volatile
        private var coWidget: Boolean? = null

        fun dangDung(context: Context): Boolean =
            coWidget ?: maSo(context).isNotEmpty().also { coWidget = it }

        fun demLai() {
            coWidget = null
        }

        /**
         * Đặt câu mới lên mọi widget đang có.
         *
         * Trả về ngay nếu chữ không đổi: đây là hàm bị gọi mỗi nhịp, mà mỗi lần
         * đẩy `RemoteViews` là một lần vượt qua ranh giới tiến trình.
         *
         * @param dangTim còn đang đi tìm lời hay đã tìm xong mà không có. Hai
         *   việc khác nhau và phải nói khác nhau — báo "chưa có lời" trong lúc
         *   còn đang tìm là kết luận sớm.
         */
        fun dat(context: Context, tenBaiMoi: String?, cauMoi: String?, dangTim: Boolean) {
            if (tenBaiMoi == tenBai && cauMoi == cau) return
            tenBai = tenBaiMoi
            cau = cauMoi

            val manager = AppWidgetManager.getInstance(context)
            val ids = maSo(context, manager)
            if (ids.isEmpty()) return
            ve(context, manager, ids, tenBaiMoi, cauMoi, dangTim)
        }

        private fun maSo(
            context: Context,
            manager: AppWidgetManager = AppWidgetManager.getInstance(context)
        ): IntArray = try {
            manager.getAppWidgetIds(ComponentName(context, KhungLoiWidget::class.java))
        } catch (e: Exception) {
            // Máy không có màn hình chính nào nhận widget (Android TV, một số
            // máy chuyên dụng). Không có widget thì cũng không có gì để vẽ.
            IntArray(0)
        }

        private fun ve(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            tenBai: String?,
            cau: String?,
            dangTim: Boolean
        ) {
            if (ids.isEmpty()) return
            val view = RemoteViews(context.packageName, R.layout.widget_khung_loi)

            view.setTextViewText(
                R.id.khung_loi_ten_bai,
                tenBai?.uppercase() ?: context.getString(R.string.widget_chua_co_nhac)
            )
            view.setTextViewText(
                R.id.khung_loi_cau,
                when {
                    cau != null -> cau
                    // Có bài mà chưa có câu. Để trống thì người ta tưởng widget
                    // hỏng, nên nói ra — và nói đúng cái đang xảy ra.
                    tenBai != null && dangTim -> context.getString(R.string.widget_dang_tim_loi)
                    tenBai != null -> context.getString(R.string.widget_chua_co_loi)
                    else -> context.getString(R.string.widget_moi_mo_nhac)
                }
            )

            // Chạm vào đâu cũng mở AURA. Không cần gửi kèm gì: app mở ra là vào
            // thẳng trang Bài (`START_PANE`), tức đúng chỗ có cả bài lời này.
            val mo = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            view.setOnClickPendingIntent(
                R.id.khung_loi_goc,
                PendingIntent.getActivity(
                    context,
                    0,
                    mo,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            ids.forEach { manager.updateAppWidget(it, view) }
        }
    }
}
