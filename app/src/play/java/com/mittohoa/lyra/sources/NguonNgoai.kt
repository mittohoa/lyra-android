package com.mittohoa.lyra.sources

import com.mittohoa.lyra.lyrics.Lyrics

/**
 * Hai nguồn nhạc Việt: Zing MP3 và NhacCuaTui.
 *
 * Bản PLAY: **không có**.
 *
 * Hai API đó là API nội bộ, không ai cấp phép cho Lyra dùng, và thứ chúng trả
 * về là cả một kho nhạc thương mại. Phát kho đó miễn phí qua một app trên Play
 * là chuyện bị gỡ — và gỡ kèm cả tài khoản nhà phát triển, chứ không chỉ rớt
 * một lần nộp.
 *
 * Bản Play còn lại một app trọn vẹn chứ không phải một app cụt: nhạc trong
 * máy, lời khớp giờ từ LRCLIB, dịch chạy tại chỗ, khung lời nổi cho nhạc phát
 * ở app khác, và thẻ điều khiển trên màn hình khoá. Thứ mất đi là tìm và phát
 * nhạc trực tuyến.
 *
 * Bỏ luôn cả phần **tra lời** từ hai nguồn đó, không chỉ phần phát. Tra lời
 * rủi ro thấp hơn hẳn nhưng vẫn là gọi API không được phép, và một bản dựng
 * *không mang dòng nào* là điều nói được bằng một câu kiểm chứng được — khác
 * hẳn với "có mang nhưng không gọi tới". Giá phải trả là độ phủ lời bài Việt
 * giảm, vì LRCLIB yếu hơn Zing ở mảng đó.
 */
object NguonNgoai {

    /** Bản dựng này có tìm và phát nhạc online không. */
    const val CO_ONLINE = false

    suspend fun tim(query: String, limit: Int): List<List<Track>> = emptyList()

    suspend fun duongPhat(track: Track): String? = null

    val NGUON_LOI: List<Pair<String, suspend (String, String, Long) -> Lyrics?>> = emptyList()
}
