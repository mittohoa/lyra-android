package com.mittohoa.lyra.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.core.content.edit

/**
 * Cân bằng âm cho bộ phát CỦA AURA.
 *
 * CHỈ ÁP ĐƯỢC CHO NHẠC AURA TỰ PHÁT. Hiệu ứng âm thanh của Android gắn vào một
 * *phiên âm thanh* cụ thể, mà phiên của Zing hay YouTube thì thuộc về app đó.
 * Có một đường vòng là gắn vào phiên số 0 — tức toàn bộ tiếng ra loa của cả
 * máy — nhưng đó là sửa âm thanh của mọi app khác mà không ai cho phép, nên
 * không đi đường ấy.
 *
 * CHỈ DÙNG BỘ MẪU CÓ SẴN, không bày thanh trượt từng dải. Bộ mẫu do chính bộ
 * xử lý âm thanh của máy khai ra, nên tên và số lượng khác nhau tuỳ hãng —
 * nhưng cái nào có thì cái đó chắc chắn chạy. Bày mười thanh trượt để rồi phần
 * lớn người dùng kéo bừa rồi thấy tệ hơn thì không đáng, ít nhất là chưa.
 *
 * DỄ HỎNG THEO TỪNG MÁY, và mã ở đây phải chịu được điều đó. `Equalizer` có
 * máy dựng lên là ném ngoại lệ, có máy khai không có bộ mẫu nào. Mọi lối vào
 * đều bọc lại; hỏng thì [coDung] trả `false` và màn hình nói thẳng là máy này
 * không có, thay vì bày một hàng nút bấm không ra gì.
 */
class CanBangAm(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("can-bang-am", Context.MODE_PRIVATE)

    private var eq: Equalizer? = null
    private var phien = 0

    /** Máy này có cân bằng âm dùng được không. Biết được sau lần gắn đầu tiên. */
    var coDung = false
        private set

    /** Tên các bộ mẫu máy khai ra. Rỗng khi máy không có. */
    var mau: List<String> = emptyList()
        private set

    var dangBat: Boolean = prefs.getBoolean("bat", false)
        private set

    /** Chỉ số bộ mẫu đang chọn. */
    var mauDangChon: Int = prefs.getInt("mau", 0)
        private set

    /**
     * Gắn vào phiên âm thanh của bộ phát.
     *
     * Gọi lại được nhiều lần: đổi phiên thì bỏ cái cũ rồi dựng cái mới. Bộ phát
     * đổi phiên âm thanh khi nó dựng lại đường ra, và một `Equalizer` trỏ tới
     * phiên đã chết thì không kêu ca gì cả — nó chỉ thôi có tác dụng.
     */
    fun gan(sessionId: Int) {
        if (sessionId == 0) return
        if (eq != null && phien == sessionId) return
        bo()
        phien = sessionId
        try {
            val e = Equalizer(PRIORITY, sessionId)
            mau = (0 until e.numberOfPresets).map { e.getPresetName(it.toShort()) }
            coDung = mau.isNotEmpty()
            eq = e
            apDung()
        } catch (t: Throwable) {
            // Máy không có, hoặc phiên đã chết. Không có cân bằng âm thì nhạc
            // vẫn phát bình thường — nuốt lỗi ở đây là đúng.
            Log.d(TAG, "Khong dung duoc can bang am", t)
            coDung = false
            eq = null
        }
    }

    fun bo() {
        runCatching { eq?.release() }
        eq = null
    }

    fun datBat(bat: Boolean) {
        dangBat = bat
        prefs.edit { putBoolean("bat", bat) }
        apDung()
    }

    fun datMau(i: Int) {
        if (i !in mau.indices) return
        mauDangChon = i
        prefs.edit { putInt("mau", i) }
        // Chọn một bộ mẫu là ý muốn NGHE nó, nên bật luôn. Bắt bấm thêm một
        // công tắc nữa sau khi đã chọn là hỏi lại một câu vừa được trả lời.
        if (!dangBat) datBat(true) else apDung()
    }

    private fun apDung() {
        val e = eq ?: return
        try {
            e.enabled = dangBat
            if (dangBat && mauDangChon in mau.indices) {
                e.usePreset(mauDangChon.toShort())
            }
        } catch (t: Throwable) {
            Log.d(TAG, "Khong ap duoc can bang am", t)
        }
    }

    private companion object {
        const val TAG = "AuraCanBang"

        /**
         * Muc uu tien khi gianh hieu ung am thanh voi app khac.
         *
         * De 0 la muc thuong. Cao hon thi gianh duoc cua app khac - ma o day
         * ta chi sua tieng cua chinh minh, khong co ly do gi de gianh.
         */
        const val PRIORITY = 0
    }
}
