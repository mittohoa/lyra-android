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
 * Bản SIDELOAD: đủ cả — tìm, phát, và lấy lời.
 *
 * Ranh giới giữa hai biến thể kẻ ở **bộ mã nguồn**, nhưng nó đã dời chỗ. Trước
 * 0.3.2 bản Play không mang một dòng nào của hai nguồn này; giờ nó mang phần
 * TÌM, và chỉ phần tìm. Lý do đổi: thứ khiến một app bị gỡ khỏi Play là phục
 * vụ nội dung có bản quyền, không phải liệt kê tên bài — nên cắt ở chỗ nội
 * dung mới đúng chỗ, còn cắt cả phần tìm là cắt thừa.
 *
 * Bản này khác bản Play ở ba dòng dưới đây, và chỉ ba dòng đó:
 *   - `PHAT_DUOC` bật, nên `duongPhat()` trả đường thật.
 *   - `NGUON_LOI` có Zing và NCT xếp sau LRCLIB.
 * Người dùng bản tải thẳng tự quyết định chuyện đó cho máy mình.
 */
object NguonNgoai {

    /** Bản dựng này có TÌM được nhạc online không. */
    const val CO_ONLINE = true

    /** Bản dựng này có PHÁT được nhạc từ hai nguồn đó không. */
    const val PHAT_DUOC = true

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

    private const val TAG = "AuraNguon"
}
