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
 * Bản SIDELOAD: có thật.
 *
 * Đây là đường ranh giữa hai biến thể, và nó được kẻ ở **bộ mã nguồn** chứ
 * không phải ở một cái cờ bật/tắt lúc chạy. Một cái cờ vẫn để lại toàn bộ mã
 * gọi API của Zing và NhacCuaTui nằm trong file cài đặt; người duyệt Play mở
 * file ra xem thì thấy, và họ có lý khi cho rằng thứ nằm trong app là thứ app
 * làm được. Tách bằng source set thì bản Play **không mang** dòng nào.
 *
 * Vì sao bản Play không được mang: hai API này là API nội bộ, không ai cấp
 * phép cho Lyra dùng, và thứ chúng trả về là cả một kho nhạc thương mại. Phát
 * kho đó miễn phí qua một app trên Play là chuyện bị gỡ, và gỡ kèm cả tài
 * khoản nhà phát triển. Bản tải thẳng thì người dùng tự quyết định.
 */
object NguonNgoai {

    /** Bản dựng này có tìm và phát nhạc online không. */
    const val CO_ONLINE = true

    /**
     * Tìm ở CẢ HAI nguồn cùng lúc. Mỗi nguồn một danh sách, để bên gọi trộn
     * kiểu cài răng lược — nối đuôi thì một nguồn trả nhiều kết quả rác sẽ
     * đẩy hết kết quả tốt của nguồn kia xuống dưới màn hình.
     *
     * Hỏi lần lượt thì thời gian chờ là tổng hai lần gọi; hỏi song song thì
     * bằng lần chậm hơn — với một ô tìm kiếm thì mỗi phần trăm giây đều thấy.
     */
    suspend fun tim(query: String, limit: Int): List<List<Track>> = coroutineScope {
        listOf(
            async { thu("zing") { ZingClient.search(query, limit) } },
            async { thu("nct") { NctClient.search(query, limit) } }
        ).awaitAll()
    }

    /** Đường phát thật, hoặc null khi nguồn từ chối. */
    suspend fun duongPhat(track: Track): String? = when (track.source) {
        MusicSource.NCT -> track.streamUrl ?: thuLay("nct") { NctClient.streamUrl(track.id) }
        MusicSource.ZING -> thuLay("zing") { ZingClient.streamUrl(track.id) }
        else -> null
    }

    /** Nguồn lời phụ, xếp sau LRCLIB. */
    val NGUON_LOI: List<Pair<String, suspend (String, String, Long) -> Lyrics?>> = listOf(
        "zing" to ZingClient::fetch,
        "nct" to NctClient::fetch
    )

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

    private inline fun <T> thuLay(ten: String, block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.i(TAG, "$ten khong tra duong phat", e)
        null
    }

    private const val TAG = "LyraNguon"
}
