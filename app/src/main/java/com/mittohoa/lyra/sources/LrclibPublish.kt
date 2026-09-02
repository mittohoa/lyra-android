package com.mittohoa.lyra.sources

import android.util.Log
import com.mittohoa.lyra.lyrics.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Góp một bản lời ngược lại cho LRCLIB.
 *
 * Lyra **lấy** lời từ LRCLIB, và trên bản Play thì đó là nguồn duy nhất. Lấy mà
 * không trả thì cái kho chung ấy không lớn lên — mà chính nó quyết định bản
 * Play có ích tới đâu. Phần tự nhập lời trong app đã tạo ra đúng định dạng
 * LRCLIB cần, nên đường trả về ngắn hơn nhiều so với vẻ ngoài.
 *
 * LUÔN do người dùng bấm, không bao giờ tự động. Đây là đăng một thứ lên một
 * kho công cộng ai cũng đọc được và không xoá lại được — việc đó phải là quyết
 * định của người ta, không phải mặc định của app.
 *
 * ## Thử thách bằm
 *
 * LRCLIB không có tài khoản. Để một máy không thể đăng hàng loạt, mỗi lần đăng
 * phải giải một bài toán bằm: xin một `prefix`, tìm một `nonce` sao cho
 * `SHA-256(prefix + nonce)` nhỏ hơn `target` khi so từng byte từ trái sang.
 *
 * Đo thật ngày 02/09/2026: `target` là `000000FF…`, và một lần giải mất khoảng
 * **5,6 triệu lần bằm**. Trên máy tính để bàn là gần một phút bằng Node; ở đây
 * dùng lại MỘT `MessageDigest` và ghi nonce thẳng vào mảng byte, không tạo rác
 * mỗi vòng — nhưng vẫn phải tính bằng chục giây trên điện thoại.
 *
 * Vì thế nó chạy trên luồng nền và huỷ được: `ensureActive()` gọi đều đặn nên
 * người dùng bỏ đi là vòng lặp dừng thật, không chạy tiếp đốt pin.
 */
object LrclibPublish {

    private const val BASE = "https://lrclib.net/api"
    private const val UA = DANH_TINH

    sealed interface KetQua {
        data object Xong : KetQua
        /** `vi` là câu để hiện thẳng lên màn hình. */
        data class Hong(val vi: String) : KetQua
    }

    /**
     * @param tienDo báo số lần bằm đã thử, để màn hình còn nói được là đang chạy
     *               chứ không phải treo.
     */
    suspend fun gop(
        tenBai: String,
        caSi: String,
        album: String,
        doDaiMs: Long,
        cacDong: List<LyricLine>,
        /** Lời có mốc thời gian thật hay chỉ là chữ trơn. */
        coMoc: Boolean,
        tienDo: (Long) -> Unit = {},
        dangGui: () -> Unit = {}
    ): KetQua = withContext(Dispatchers.IO) {
        if (tenBai.isBlank() || caSi.isBlank()) {
            return@withContext KetQua.Hong("Thiếu tên bài hoặc tên ca sĩ")
        }
        // LRCLIB dò lời theo BỘ BỐN tên bài / ca sĩ / album / độ dài. Gửi độ
        // dài bằng 0 là gửi một bản ghi không bao giờ khớp lại được với ai —
        // rác cho kho chung, chứ không giúp gì.
        if (doDaiMs <= 0) {
            return@withContext KetQua.Hong("Không biết độ dài bài, chưa góp được")
        }
        if (cacDong.isEmpty()) return@withContext KetQua.Hong("Chưa có lời để góp")

        val thach = xinThachThuc() ?: return@withContext KetQua.Hong("Không xin được thử thách")
        val nonce = giai(thach.first, thach.second, tienDo)
        val token = "${thach.first}:$nonce"

        dangGui()

        val than = JSONObject().apply {
            put("trackName", tenBai)
            put("artistName", caSi)
            put("albumName", album.ifBlank { tenBai })
            put("duration", doDaiMs / 1000)
            put("plainLyrics", cacDong.joinToString("\n") { it.text })
            // Lời không có mốc thì gửi chuỗi RỖNG, đừng dựng ra mốc giả.
            //
            // `dungLrc` sẽ cho ra "[00:00.00]" ở mọi dòng — nhìn thì giống một
            // bản lời khớp giờ, mà thực ra sai hết. Đăng thứ đó lên kho chung
            // là làm bẩn nó, và người tải về sau không có cách nào biết.
            put("syncedLyrics", if (coMoc) dungLrc(cacDong) else "")
        }.toString()

        val yeuCau = Request.Builder()
            .url("$BASE/publish")
            .header("User-Agent", UA)
            .header("X-Publish-Token", token)
            .post(than.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            Http.client.newCall(yeuCau).execute().use { res ->
                if (res.isSuccessful) KetQua.Xong
                else {
                    Log.i(TAG, "publish tra ve ${res.code}")
                    KetQua.Hong("LRCLIB từ chối (mã ${res.code})")
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "publish hong", e)
            KetQua.Hong("Không gửi được, kiểm tra mạng")
        }
    }

    private fun xinThachThuc(): Pair<String, ByteArray>? = try {
        val yeuCau = Request.Builder()
            .url("$BASE/request-challenge")
            .header("User-Agent", UA)
            .post(ByteArray(0).toRequestBody(null))
            .build()
        Http.client.newCall(yeuCau).execute().use { res ->
            if (!res.isSuccessful) null
            else {
                val o = JSONObject(res.body?.string().orEmpty())
                o.getString("prefix") to hex(o.getString("target"))
            }
        }
    } catch (e: Exception) {
        Log.i(TAG, "khong xin duoc thach thuc", e)
        null
    }

    private suspend fun giai(prefix: String, dich: ByteArray, tienDo: (Long) -> Unit): Long {
        val bam = MessageDigest.getInstance("SHA-256")
        val dauByte = prefix.toByteArray(Charsets.UTF_8)
        var n = 0L
        while (true) {
            bam.reset()
            bam.update(dauByte)
            bam.update(n.toString().toByteArray(Charsets.UTF_8))
            if (nhoHon(bam.digest(), dich)) return n
            n++
            // Kiểm huỷ và báo tiến độ mỗi 100k vòng: thưa hơn thì bỏ đi rồi mà
            // vòng lặp còn chạy cả giây, dày hơn thì chính phép kiểm thành gánh.
            if (n % 100_000L == 0L) {
                coroutineContext.ensureActive()
                tienDo(n)
            }
        }
    }

    /** So từng byte từ trái sang, đúng cách LRCLIB kiểm. */
    internal fun nhoHon(a: ByteArray, b: ByteArray): Boolean {
        for (i in b.indices) {
            val x = a[i].toInt() and 0xFF
            val y = b[i].toInt() and 0xFF
            if (x != y) return x < y
        }
        return false
    }

    internal fun hex(s: String): ByteArray =
        ByteArray(s.length / 2) { ((s[it * 2].digitToInt(16) shl 4) or s[it * 2 + 1].digitToInt(16)).toByte() }

    /** Dựng lại chuỗi .lrc từ các dòng đã có mốc. */
    internal fun dungLrc(cacDong: List<LyricLine>): String = cacDong.joinToString("\n") { d ->
        val tong = d.time / 1000
        val phut = tong / 60
        val giay = tong % 60
        val phanTram = (d.time % 1000) / 10
        "[%02d:%02d.%02d]%s".format(phut, giay, phanTram, d.text)
    }

    private const val TAG = "LyraGopLoi"
}
