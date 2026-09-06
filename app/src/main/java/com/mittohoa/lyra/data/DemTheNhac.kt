package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import com.mittohoa.lyra.sources.Track
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Thẻ đã đọc từ các tệp nhạc, GIỮ LẠI QUA CÁC LẦN MỞ APP.
 *
 * VÌ SAO CẦN. Đọc thẻ là mở từng tệp ra bằng `MediaMetadataRetriever` — vài
 * mili-giây một tệp, mà trần là 2000 tệp. Bộ nhớ đệm trong RAM đã chữa được
 * việc quét lại nhiều lần TRONG một phiên, nhưng đóng app rồi mở lại là mất
 * sạch: người có thư viện lớn phải ngồi nhìn màn hình trống mỗi lần mở app,
 * cho đúng một kết quả y hệt hôm qua.
 *
 * KHOÁ LÀ VÂN TAY chứ không phải địa chỉ tệp: mã tài liệu + cỡ tệp + giờ sửa.
 * Sửa thẻ một bài xong thì vân tay đổi, bản nhớ cũ không khớp nữa, và app đọc
 * lại. Nhớ theo địa chỉ không thôi thì sửa thẻ xong app hiện tên cũ mãi mãi.
 *
 * NẰM Ở `cacheDir`, không phải `filesDir`. Đây là thứ LÚC NÀO cũng dựng lại
 * được từ chính mấy tệp nhạc trên máy — mất nó thì chỉ tốn một lần quét, không
 * mất gì của người dùng. Hệ thống được phép dọn `cacheDir` khi máy đầy, và với
 * dữ liệu loại này thì đó là quyết định đúng.
 *
 * GHI CẢ TỆP MỘT LẦN chứ không mỗi bài một tệp: hai nghìn bài chừng vài trăm
 * KB, luôn đọc hết một lượt, không bao giờ phải tìm kiếm. Chia nhỏ ra chỉ được
 * mỗi một thứ là nhiều tệp hơn.
 */
class DemTheNhac(context: Context) {

    private val file = File(context.cacheDir, "the-nhac.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun doc(): Map<String, Track> = try {
        if (!file.exists()) emptyMap()
        else json.decodeFromString(BO, file.readText())
    } catch (e: Exception) {
        // Tệp hỏng thì bắt đầu lại từ rỗng — tốn một lần quét, không hơn. KHÔNG
        // xoá tệp ở đây: lần ghi sau sẽ đè lên, mà xoá ngay thì mất luôn thứ có
        // thể mở ra xem khi cần tìm hiểu vì sao nó hỏng.
        Log.w(TAG, "Khong doc duoc dem the nhac", e)
        emptyMap()
    }

    fun ghi(bo: Map<String, Track>) {
        try {
            file.writeText(json.encodeToString(BO, bo))
        } catch (e: Exception) {
            // Không ghi được thì app vẫn chạy đúng, chỉ là lần mở sau quét lại
            // từ đầu. Không đáng để làm hỏng một lần nạp thư viện.
            Log.w(TAG, "Khong ghi duoc dem the nhac", e)
        }
    }

    private companion object {
        const val TAG = "DemTheNhac"
        val BO = MapSerializer(String.serializer(), Track.serializer())
    }
}
