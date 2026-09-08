package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/** Mấy trường người dùng sửa lại cho một bài. Rỗng nghĩa là giữ nguyên thẻ gốc. */
@Serializable
data class TheSua(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    /** -1 nghĩa là không sửa. 0 là một giá trị hợp lệ: "bài này không đánh số". */
    val soThuTu: Int = -1
) {
    val rong: Boolean
        get() = title.isBlank() && artist.isBlank() && album.isBlank() && soThuTu < 0
}

/**
 * Thẻ nhạc người dùng sửa lại trong AURA.
 *
 * VÌ SAO CẦN. Rất nhiều tệp nhạc gắn thẻ sai hoặc bỏ trống, và AURA thì DỰA VÀO
 * THẺ cho gần như mọi việc: gom album, xếp thứ tự đĩa, và quan trọng nhất là đi
 * tìm lời. Một tệp ghi ca sĩ là "Unknown Artist" thì không có nguồn lời nào tìm
 * ra được, mà người dùng thì nhìn cái tên sai ấy mỗi ngày và không sửa được.
 *
 * KHÔNG GHI VÀO TỆP, và đây là quyết định lớn nhất ở đây.
 *
 * Ghi thẻ ID3 vào tệp thì các trình phát khác cũng thấy — nghe hấp dẫn hơn hẳn.
 * Nhưng thẻ ID3v2 nằm ở ĐẦU tệp, nên nới nó ra một byte là phải chép lại toàn
 * bộ phần nhạc; một lần chép đứt giữa chừng là mất bài hát của người dùng.
 * Thêm nữa mỗi định dạng một cách ghi (MP3, FLAC, M4A, OGG), và tệp nằm ngoài
 * thư mục đã trỏ còn phải xin quyền ghi từng tệp một.
 *
 * Đổi lại một bảng sửa nằm trong app: chạy cho MỌI định dạng, MỌI nguồn, không
 * xin thêm quyền nào, và không có đường nào làm hỏng tệp gốc. Cái mất là các
 * trình phát khác không thấy — nói thẳng ra ở màn hình sửa chứ không giấu.
 *
 * KHOÁ THEO `playbackUri`, thứ đã dùng làm khoá ở khắp nơi khác trong app.
 * Không khoá theo tên bài: sửa tên xong thì khoá đổi, và bản sửa tự mất tác
 * dụng ngay lần nạp sau.
 */
class SuaThe(context: Context) {

    private val file = File(context.filesDir, "sua-the.json")
    private val json = Json { ignoreUnknownKeys = true }

    private val bo: MutableMap<String, TheSua> = doc()

    private fun doc(): MutableMap<String, TheSua> = try {
        if (!file.exists()) mutableMapOf()
        else json.decodeFromString(BO, file.readText()).toMutableMap()
    } catch (e: Exception) {
        // Tệp hỏng thì thư viện quay về đúng thẻ gốc trong tệp — vẫn dùng được,
        // chỉ là mất mấy chỗ đã sửa. KHÔNG xoá tệp: để đó còn cứu bằng tay.
        Log.w(TAG, "Khong doc duoc bang sua the", e)
        mutableMapOf()
    }

    fun cua(khoa: String): TheSua? = bo[khoa]

    /** Toàn bộ bảng, để áp một lượt lên cả thư viện mà không mở lại tệp. */
    fun tatCa(): Map<String, TheSua> = bo

    fun dat(khoa: String, sua: TheSua) {
        if (sua.rong) bo.remove(khoa) else bo[khoa] = sua
        ghi()
    }

    private fun ghi() {
        try {
            file.writeText(json.encodeToString(BO, bo))
        } catch (e: Exception) {
            Log.w(TAG, "Khong ghi duoc bang sua the", e)
        }
    }

    private companion object {
        const val TAG = "AuraSuaThe"
        val BO = MapSerializer(String.serializer(), TheSua.serializer())
    }
}
