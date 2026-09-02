package com.mittohoa.lyra.data

import android.util.Log
import com.mittohoa.lyra.sources.Http
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Xem có bản mới trên trang phát hành không.
 *
 * Android **không cho app tự cài đè chính nó** mà không có người bấm, nên đây
 * không phải cập nhật tự động — nó chỉ làm đúng một việc: cho người dùng *biết*
 * là có bản mới, kèm đường tải. Không biết mới là vấn đề lớn hơn: một người cài
 * tay bản 0.1.0 sẽ dùng nó mãi mãi mà không hay có bản sửa lỗi.
 *
 * Đọc **tên file APK** trong bản phát hành chứ không đọc tên thẻ (`v0.1.2`).
 * Lý do: một bản phát hành mang cả file Windows lẫn Android, và hai phía không
 * đổi phiên bản cùng lúc — bản `v0.1.1` mang file Android vẫn tên `0.1.0` vì
 * phía Android thật sự không đổi gì. So với tên thẻ thì sẽ báo có bản mới trong
 * khi chẳng có gì mới.
 */
object UpdateChecker {

    private const val API =
        "https://api.github.com/repos/mittohoa/lyra-player/releases/latest"

    private const val TRANG_PHAT_HANH =
        "https://github.com/mittohoa/lyra-player/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Tên file dạng `AURA-0.1.2-android-arm64.apk`, và nhận cả tên cũ `Lyra-…`.
     *
     * PHẢI NHẬN CẢ HAI, ít nhất tới khi không còn ai chạy bản trước lúc đổi tên.
     * Mọi bản phát hành từ trước đều mang tên `Lyra-…`, và bản app đang nằm trên
     * máy người dùng cũng dò đúng chữ đó. Đổi hẳn sang `AURA-` thì:
     *
     *     bản cũ trên máy    dò "Lyra-"  ->  không thấy bản mới tên AURA-
     *     bản mới trên máy   dò "AURA-"  ->  không thấy các bản đã phát hành
     *
     * Cả hai chiều đều đứt, mà đứt IM LẶNG: màn hình chỉ báo "đang là bản mới
     * nhất". Nhận cả hai thì không ai mắc kẹt.
     */
    private val TEN_FILE = Regex("""(?:AURA|Lyra)-(\d+\.\d+\.\d+)-android-arm64\.apk""")

    @Serializable
    private data class Asset(val name: String = "", val browser_download_url: String = "")

    @Serializable
    private data class Release(val assets: List<Asset> = emptyList())

    data class BanMoi(val phienBan: String, val duongTai: String, val trang: String)

    /**
     * Trả về bản mới nếu có, `null` nếu đang là bản mới nhất hoặc không hỏi được.
     *
     * Không hỏi được thì im lặng: mất mạng hay GitHub chặn tần suất đều không
     * phải chuyện người dùng làm gì được, và một dải báo lỗi ở đây chỉ làm phiền.
     */
    suspend fun kiem(phienBanDangChay: String): BanMoi? = withContext(Dispatchers.IO) {
        try {
            val raw = Http.text(API, mapOf("Accept" to "application/vnd.github+json"))
                ?: return@withContext null
            val release = json.decodeFromString<Release>(raw)

            val apk = release.assets.firstNotNullOfOrNull { asset ->
                TEN_FILE.find(asset.name)?.let { it.groupValues[1] to asset.browser_download_url }
            } ?: return@withContext null

            val (moi, duong) = apk
            if (!moiHon(moi, phienBanDangChay)) return@withContext null

            Log.i(TAG, "Co ban moi: $moi (dang chay $phienBanDangChay)")
            BanMoi(phienBan = moi, duongTai = duong, trang = TRANG_PHAT_HANH)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Khong hoi duoc ban moi", e)
            null
        }
    }

    /**
     * So hai chuỗi phiên bản theo từng số.
     *
     * So chuỗi thẳng thì `0.1.10` đứng trước `0.1.9`, và tới bản thứ mười thì app
     * lặng lẽ ngừng báo cập nhật — một lỗi chỉ lộ ra sau nhiều tháng.
     */
    internal fun moiHon(a: String, b: String): Boolean {
        val x = a.split('.').mapNotNull { it.toIntOrNull() }
        val y = b.split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(x.size, y.size)) {
            val m = x.getOrElse(i) { 0 }
            val n = y.getOrElse(i) { 0 }
            if (m != n) return m > n
        }
        return false
    }

    private const val TAG = "AuraCapNhat"
}
