package com.mittohoa.lyra.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Hai lựa chọn về cách tiếng nhạc đi ra, giữ chung một chỗ.
 *
 * NẰM RIÊNG KHỎI `Lyra` vì bên đọc là BỘ PHÁT, chạy trong tiến trình dịch vụ,
 * còn bên ghi là màn hình Chỉnh. Hai bên không gọi thẳng nhau được — dịch vụ
 * có thể đang chạy khi màn hình chưa mở, và ngược lại. `SharedPreferences` là
 * chỗ gặp nhau: cả hai bên đều đọc được, và bên phát nghe được lúc nó đổi nhờ
 * [nghe].
 */
class CaiAmThanh(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("cai-am-thanh", Context.MODE_PRIVATE)

    /**
     * Bỏ khoảng lặng trong bài.
     *
     * ExoPlayer tự dò và cắt những đoạn im lặng kéo dài. Có ích thật với hai
     * thứ: album rip liền mạch để chừa vài giây trống cuối mỗi bài, và mấy bản
     * thu có đoạn intro im đằng trước.
     *
     * MẶC ĐỊNH TẮT. Nó sửa chính tiếng nhạc, và có những bài mà khoảng lặng
     * nằm trong ý đồ của người làm ra nó — cắt đi là hỏng bài. Thứ gì đổi cái
     * người dùng nghe thấy thì phải do họ bật.
     */
    fun boKhoangLang(): Boolean = prefs.getBoolean(KEY_BO_LANG, false)

    fun datBoKhoangLang(bat: Boolean) = prefs.edit().putBoolean(KEY_BO_LANG, bat).apply()

    /**
     * Mờ dần khi đổi bài, tính bằng mili-giây. 0 là tắt.
     *
     * ĐÂY LÀ MỜ DẦN, KHÔNG PHẢI CHỒNG TIẾNG. Hai bài chồng lên nhau cần hai bộ
     * phát chạy song song, mà bộ phát ở đây còn gánh hàng đợi, thẻ màn hình
     * khoá, Android Auto và lịch sử nghe — tách đôi nó ra là đánh đổi cả đống
     * thứ đang chạy đúng để lấy một hiệu ứng. Nên: nhỏ tiếng dần ở cuối bài,
     * to dần lên ở đầu bài sau. Không có đoạn nào nghe thấy cả hai.
     */
    fun moDanMs(): Int = prefs.getInt(KEY_MO_DAN, 0)

    fun datMoDanMs(ms: Int) = prefs.edit().putInt(KEY_MO_DAN, ms.coerceIn(0, TOI_DA_MS)).apply()

    /**
     * Gọi lại mỗi khi một trong hai thứ trên đổi.
     *
     * Trả về thứ để bỏ nghe. Bộ phát phải bỏ nghe lúc nó tắt, không thì cái
     * nghe ấy giữ luôn cả bộ phát đã chết.
     */
    fun nghe(goi: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> goi() }
        prefs.registerOnSharedPreferenceChangeListener(l)
        return l
    }

    fun thoiNghe(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(l)
    }

    companion object {
        private const val KEY_BO_LANG = "bo-khoang-lang"
        private const val KEY_MO_DAN = "mo-dan-ms"

        /**
         * Trần của đoạn mờ dần.
         *
         * Bốn giây đã là dài với một cú chuyển không chồng tiếng: quá mức đó
         * thì cái người ta nghe thấy không còn là "chuyển mượt" nữa mà là một
         * khoảng lặng ở giữa hai bài.
         */
        const val TOI_DA_MS = 4_000

        /** Các mức bày ra trong Chỉnh. */
        val CAC_MUC = listOf(0, 500, 1_000, 2_000, 4_000)
    }
}
