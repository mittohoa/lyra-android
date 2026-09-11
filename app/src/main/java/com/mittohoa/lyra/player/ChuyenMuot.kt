package com.mittohoa.lyra.player

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mittohoa.lyra.data.CaiAmThanh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Nhỏ tiếng dần ở cuối bài, to dần lại ở đầu bài sau.
 *
 * VÌ SAO KHÔNG PHẢI CHỒNG TIẾNG. Cho hai bài đè lên nhau cần hai bộ phát chạy
 * song song, mà bộ phát ở đây còn gánh hàng đợi, thẻ màn hình khoá, Android
 * Auto và việc ghi lịch sử nghe. Tách đôi nó ra là đem một đống thứ đang chạy
 * đúng ra đổi lấy một hiệu ứng. Cái làm ở đây rẻ hơn nhiều và vẫn bỏ được cú
 * cắt đột ngột giữa hai bài — nhưng nó là MỜ DẦN, và chỗ nào nói ra nó cũng
 * phải gọi đúng tên như vậy.
 *
 * CÁCH LÀM. Một vòng lặp ngó vị trí phát; còn [CaiAmThanh.moDanMs] mili-giây
 * nữa là hết bài thì hạ [ExoPlayer.volume] tuyến tính về 0, và bài mới bắt đầu
 * thì kéo ngược lên 1.
 *
 * `volume` của ExoPlayer là mức khuếch đại riêng của app, không phải nút âm
 * lượng của máy — kéo nó không đụng gì tới con số người dùng đã chỉnh.
 *
 * PHẢI TRẢ VỀ 1 Ở MỌI ĐƯỜNG RA. Bỏ sót một nhánh — người dùng tua ngược, tắt
 * tính năng, hàng đợi hết bài — là bộ phát kẹt ở mức gần 0 và mọi bài sau đó
 * phát ra tiếng rất nhỏ mà không có gì trên màn hình giải thích tại sao. Đó là
 * kiểu hỏng tệ nhất: im lặng, dai dẳng, và trông y như máy hỏng.
 */
class ChuyenMuot(context: Context, private val player: ExoPlayer) {

    private val cai = CaiAmThanh(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var vong: Job? = null

    private val nghe = cai.nghe { apDung() }

    private val theoDoi = object : Player.Listener {
        override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) {
            // Bài mới: kéo tiếng về đủ ngay, không chờ vòng lặp. Vòng lặp chạy
            // mỗi NHIP_MS, và bắt đầu một bài ở mức gần 0 rồi mới to lên là
            // nghe thấy rõ.
            player.volume = 1f
        }
    }

    init {
        player.addListener(theoDoi)
        apDung()
    }

    /** Đọc lại lựa chọn và bật/tắt cho khớp. */
    private fun apDung() {
        player.skipSilenceEnabled = cai.boKhoangLang()

        val ms = cai.moDanMs()
        vong?.cancel()
        if (ms <= 0) {
            // TẮT THÌ TRẢ TIẾNG VỀ ĐỦ. Người dùng tắt giữa lúc đang mờ dần là
            // chuyện bình thường, và không trả về là kẹt ở mức nhỏ vĩnh viễn.
            player.volume = 1f
            return
        }
        vong = scope.launch { chay(ms) }
    }

    private suspend fun chay(moDanMs: Int) {
        while (scope.isActive) {
            delay(NHIP_MS)

            if (!player.isPlaying) {
                // Dừng giữa đoạn mờ dần thì để nguyên mức đang có: bấm phát
                // tiếp sẽ đi tiếp đúng chỗ ấy. Kéo về 1 ở đây là mỗi lần tạm
                // dừng lại giật tiếng to lên một nhịp.
                continue
            }

            player.volume = mucAm(
                doDai = player.duration,
                viTri = player.currentPosition,
                moDanMs = moDanMs,
                coBaiSau = player.hasNextMediaItem()
            )
        }
    }

    /** Bỏ mọi thứ đã đăng ký. Bộ phát tắt thì phải gọi. */
    fun thoi() {
        cai.thoiNghe(nghe)
        player.removeListener(theoDoi)
        scope.cancel()
        // Không đặt `volume = 1f` ở đây: bộ phát sắp bị huỷ, và chạm vào nó
        // sau khi `release()` là ném lỗi.
    }

    companion object {
        /**
         * Ngó vị trí phát mỗi chừng này.
         *
         * 100ms cho ra chừng 10 bậc cho một đoạn mờ dần 1 giây — đủ mượt để
         * tai không nghe ra từng bậc, và rẻ: một phép trừ, chỉ chạy khi tính
         * năng đang bật.
         */
        private const val NHIP_MS = 100L

        /**
         * Mức âm đáng đặt ngay lúc này.
         *
         * TÁCH RA KHỎI VÒNG LẶP ĐỂ KIỂM ĐƯỢC. Đây là chỗ duy nhất trong tính
         * năng này có thể sai một cách dai dẳng: sót một nhánh là bộ phát kẹt
         * ở mức gần 0, mọi bài sau đó phát ra tiếng rất nhỏ, và trên màn hình
         * không có gì giải thích. Hỏng kiểu đó trông y như máy hỏng, nên nó
         * phải được kiểm bằng bài kiểm chứ không bằng tai.
         *
         * `internal` chứ không `private`: bài kiểm cần với tới, mà chỉ bài
         * kiểm thôi.
         */
        internal fun mucAm(doDai: Long, viTri: Long, moDanMs: Int, coBaiSau: Boolean): Float {
            // Chưa biết độ dài — luồng phát trực tiếp, hoặc bài chưa nạp xong.
            // Không biết còn bao lâu thì không có cách nào mờ dần cho đúng, và
            // đoán bừa thì tiếng nhỏ đi giữa bài.
            if (doDai <= 0L || doDai == androidx.media3.common.C.TIME_UNSET) return 1f

            // BÀI CUỐI HÀNG ĐỢI KHÔNG MỜ DẦN. Mờ dần là để nối sang bài sau;
            // không có bài sau thì nó chỉ là tiếng nhỏ đi ở cuối bài cuối
            // cùng, và nghe như máy sắp hỏng.
            if (!coBaiSau) return 1f

            val con = doDai - viTri
            if (con >= moDanMs) return 1f
            if (con <= 0L) return 0f
            return (con.toFloat() / moDanMs).coerceIn(0f, 1f)
        }
    }
}
