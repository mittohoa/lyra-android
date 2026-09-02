package com.mittohoa.lyra.sources

import android.util.Log
import com.mittohoa.lyra.lyrics.Lyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Hai nguồn nhạc Việt: Zing MP3 và NhacCuaTui.
 *
 * Bản PLAY: **tìm được, nhưng không phát và không lấy lời**.
 *
 * Ranh giới đi qua đúng chỗ rủi ro thật nằm. Thứ khiến một app bị gỡ khỏi Play
 * là **phục vụ nội dung có bản quyền** — cả một kho nhạc thương mại phát miễn
 * phí. Liệt kê tên bài và tên ca sĩ thì không phải chuyện đó.
 *
 * Nên bản Play mượn đúng cái Zing và NhacCuaTui giỏi — **tìm nhạc Việt**, thứ
 * LRCLIB làm rất kém — mà không lấy một chữ nội dung nào của họ:
 *
 *   - `tim()` có thật: trả về tên bài, tên ca sĩ, ảnh bìa, độ dài.
 *   - `duongPhat()` trả `null`. Bản này KHÔNG phát nhạc từ hai nguồn đó.
 *   - `NGUON_LOI` rỗng. Lời vẫn chỉ lấy từ LRCLIB — kho mở, ai cũng đóng góp
 *     được, không phải API nội bộ của ai.
 *
 * Người dùng bản Play chạm vào một kết quả thì AURA **tra lời** cho bài đó,
 * không phát. Xem `AURA.xemLoi`.
 *
 * Điều này đổi một điều so với các bản 0.1.8–0.3.1: file cài đặt của bản Play
 * **có mang** mã gọi API của Zing và NhacCuaTui. Trước đây nó không mang dòng
 * nào — kiểm được bằng cách soi dex. Giờ không nói câu đó được nữa, và đó là
 * cái giá của việc bản Play tìm được nhạc Việt.
 */
object NguonNgoai {

    /** Bản dựng này có TÌM được nhạc online không. */
    const val CO_ONLINE = true

    /** Bản dựng này có PHÁT được nhạc từ hai nguồn đó không. */
    const val PHAT_DUOC = false

    /**
     * Tìm ở CẢ HAI nguồn cùng lúc. Mỗi nguồn một danh sách, để bên gọi trộn
     * kiểu cài răng lược — nối đuôi thì một nguồn trả nhiều kết quả rác sẽ
     * đẩy hết kết quả tốt của nguồn kia xuống dưới màn hình.
     */
    suspend fun tim(query: String, limit: Int): List<List<Track>> = coroutineScope {
        listOf(
            async { thu("zing") { ZingClient.search(query, limit) } },
            async { thu("nct") { NctClient.search(query, limit) } }
        ).awaitAll()
    }

    /**
     * Luôn `null` ở bản này.
     *
     * Không phải "chưa làm" mà là ranh giới của bản dựng. Giao diện đọc
     * `PHAT_DUOC` để không bao giờ bày ra một nút phát chẳng bao giờ ăn gì.
     */
    suspend fun duongPhat(track: Track): String? = null

    /** Rỗng ở bản này: lời chỉ lấy từ LRCLIB. */
    val NGUON_LOI: List<Pair<String, suspend (String, String, Long) -> Lyrics?>> = emptyList()

    /**
     * Một nguồn hỏng không được kéo theo nguồn kia: mất mạng giữa chừng hay
     * API đổi hình dạng đều là chuyện thường, và trả về danh sách rỗng của
     * riêng nguồn đó vẫn còn hơn là cả ô tìm kiếm không ra gì.
     */
    private inline fun thu(ten: String, block: () -> List<Track>): List<Track> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(TAG, "$ten khong tim duoc", e)
        emptyList()
    }

    private const val TAG = "AuraNguon"
}
