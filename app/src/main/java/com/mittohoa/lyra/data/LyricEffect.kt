package com.mittohoa.lyra.data

import android.content.Context

/**
 * Hiệu ứng chữ ở trang Lời.
 *
 * **Mọi hiệu ứng ở đây chỉ dùng thứ đã biết chắc**, và điều đó giới hạn hẳn
 * những gì làm được. LRCLIB — nguồn lời chính của AURA — chỉ cho mốc theo
 * *dòng*: `[00:24.62] Take my hand`. Không có mốc theo từ (`<00:24.62>` chèn
 * giữa các chữ), đã kiểm trên chính API của họ.
 *
 * Nên AURA biết đúng hai thứ: **dòng nào đang hát**, và **câu đó đã chạy được
 * bao nhiêu phần** — suy ra từ mốc dòng này tới mốc dòng sau. Không biết đang
 * hát tới chữ nào.
 *
 * Vì vậy không có hiệu ứng nào ở đây tô sáng theo từng chữ. Cái gần nhất là
 * [SANG_DAN] và [HIEN_CHU]: cả hai chạy theo *tiến độ trong câu*, thứ biết
 * thật, chứ không giả vờ biết chỗ tiếng hát đang tới. Trông rất giống karaoke,
 * và khác nhau ở chỗ nó không nói dối.
 */
enum class LyricEffect(
    val nhan: String,
    val moTa: String,
    /**
     * Khung lời nổi phải vẽ lại liên tục để chạy hiệu ứng này.
     *
     * Khung nổi bình thường chỉ vẽ lại khi ĐỔI DÒNG - vài giây một lần. Hai
     * hiệu ứng quét bắt nó vẽ hàng chục lần mỗi giây suốt bài, mà đây là cửa
     * sổ nằm đè lên app khác nên hệ thống còn phải trộn nó vào từng khung
     * hình. Người dùng có quyền chọn, nhưng phải được nói trước.
     */
    val tonPin: Boolean = false
) {

    /** Chỉ đổi cỡ chữ và độ mờ theo khoảng cách, như trước giờ. */
    KHONG("Không", "Chỉ mờ dần theo khoảng cách"),

    /** Câu đang hát sáng dần từ trái sang phải theo tiến độ. */
    SANG_DAN("Sáng dần", "Câu đang hát sáng dần từ trái sang", tonPin = true),

    /** Chữ hiện ra dần, phần chưa tới thì trong suốt. */
    HIEN_CHU("Hiện chữ", "Chữ hiện dần ra theo câu hát", tonPin = true),

    /** Đổi dòng thì câu mới nảy lên một cái rồi đứng lại. */
    NAY("Nảy", "Đổi dòng thì câu mới nảy lên một cái"),

    /** Câu đang hát có quầng sáng màu theo bìa album. */
    TOA_SANG("Toả sáng", "Câu đang hát có quầng sáng theo màu bìa"),

    /** Câu mới trôi lên từ dưới và hiện dần. */
    TROI_LEN("Trôi lên", "Câu mới trôi lên từ dưới");

    companion object {
        fun tu(ten: String?): LyricEffect =
            entries.firstOrNull { it.name == ten } ?: SANG_DAN
    }
}

class LyricEffectPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("lyric-effect", Context.MODE_PRIVATE)

    fun read(): LyricEffect = LyricEffect.tu(prefs.getString("effect", null))

    fun write(effect: LyricEffect) {
        prefs.edit().putString("effect", effect.name).apply()
    }
}
