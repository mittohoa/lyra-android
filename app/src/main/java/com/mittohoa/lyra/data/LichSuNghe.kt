package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Một lần nghe đã ghi lại.
 *
 * `diaChi` là địa chỉ phát của bài, và nó RỖNG khi nhạc đến từ app khác. Đó là
 * dấu phân biệt duy nhất giữa "bấm vào là nghe lại được" và "chỉ nhớ là đã
 * nghe" — AURA không phát hộ được Zing hay YouTube, nên một dòng lịch sử của họ
 * mà bấm vào thì phải đi tìm chứ không thể phát.
 */
@Serializable
data class LanNghe(
    val diaChi: String,
    val ten: String,
    val caSi: String,
    /** Tên gói của app đã phát. Rỗng khi chính AURA phát. */
    val app: String = "",
    /** Mili-giây đồng hồ thật. Dùng để xếp và để nói "hôm qua", "tuần trước". */
    val luc: Long = 0L
) {
    /**
     * Khoá gộp: cùng một bài nghe lại nhiều lần chỉ chiếm MỘT dòng.
     *
     * Không gộp thì nghe đi nghe lại một bài yêu thích là đủ đẩy hết mọi bài
     * khác ra khỏi danh sách, và lịch sử biến thành một cột lặp cùng một tên.
     */
    val khoa: String get() = diaChi.ifBlank { "$app|$caSi|$ten" }

    /** Nghe lại được ngay trong AURA hay chỉ là một dòng ghi nhớ. */
    val ngheLaiDuoc: Boolean get() = diaChi.isNotBlank()
}

/**
 * Đã nghe những bài nào, gần nhất lên trước.
 *
 * VÌ SAO CẦN. Thư viện xếp theo album hay theo tên là cách tìm một bài mình
 * ĐANG NGHĨ TỚI. Nhưng cách hay gặp hơn là "bài hôm qua nghe trên đường về, tên
 * gì ấy nhỉ" — và với câu hỏi đó thì cả album lẫn bảng chữ cái đều vô dụng.
 * Thứ trả lời được nó là thứ tự THỜI GIAN, mà trước đây AURA không giữ.
 *
 * GHI CẢ NHẠC CỦA APP KHÁC. Đây là chỗ AURA làm được thứ mà chính Zing hay
 * YouTube Music không làm: nó đứng ngoài và nghe cả hai, nên lịch sử ở đây là
 * lịch sử của NGƯỜI DÙNG chứ không phải của một app. Bài từ app khác không phát
 * lại được, và [LanNghe.ngheLaiDuoc] nói thẳng điều đó ra chứ không để người
 * dùng bấm rồi mới biết.
 *
 * XOÁ ĐƯỢC, và nút xoá phải nằm ngay cạnh danh sách. Đây là bản ghi những gì
 * một người đã nghe — một thứ riêng tư hơn hẳn mọi dữ liệu khác trong app. Ai
 * đưa máy cho người khác mượn cũng phải xoá được trong một cú chạm, không phải
 * đi lục trong Cài đặt.
 *
 * Nằm ở `filesDir` chứ không phải `cacheDir`: đây là việc người dùng đã làm,
 * dựng lại từ máy không được. Cùng lẽ với [ChoNgheDo].
 */
class LichSuNghe(context: Context) {

    private val file = File(context.filesDir, "lich-su-nghe.json")
    private val json = Json { ignoreUnknownKeys = true }

    private val _lichSu = MutableStateFlow(doc())
    val lichSu: StateFlow<List<LanNghe>> = _lichSu.asStateFlow()

    private fun doc(): List<LanNghe> = try {
        if (!file.exists()) emptyList()
        else json.decodeFromString(BO, file.readText())
    } catch (e: Exception) {
        // Tệp hỏng thì bắt đầu lại từ rỗng. Cái giá là quên mình đã nghe gì,
        // không phải là app không mở được.
        Log.w(TAG, "Khong doc duoc lich su nghe", e)
        emptyList()
    }

    /**
     * Ghi một lần nghe, đẩy bài lên đầu.
     *
     * Bên gọi quyết định KHI NÀO một lần phát đủ để gọi là đã nghe — xem
     * [dangGhi]. Kho này chỉ lo gộp, xếp và cắt.
     */
    fun ghi(lan: LanNghe) {
        if (lan.ten.isBlank() && lan.caSi.isBlank()) return
        val cu = _lichSu.value
        // Bỏ bản cũ CÙNG KHOÁ rồi chèn lên đầu: nghe lại một bài là làm mới chỗ
        // của nó trong lịch sử, không phải thêm một dòng nữa.
        val moi = ArrayList<LanNghe>(minOf(cu.size + 1, TRAN))
        moi.add(lan)
        for (c in cu) {
            if (c.khoa == lan.khoa) continue
            if (moi.size >= TRAN) break
            moi.add(c)
        }
        _lichSu.value = moi
        ghiDia(moi)
    }

    /**
     * Trộn một mớ lần nghe từ tệp sao lưu vào kho đang có.
     *
     * TRỘN CHỨ KHÔNG GHI ĐÈ. Khôi phục trên một máy đã nghe được vài tuần mà
     * xoá sạch rồi chép tệp vào thì bản sao lưu — thứ sinh ra để CỨU dữ liệu —
     * lại là thứ làm mất dữ liệu. Nên bên nào cũng giữ, và bài trùng thì lấy
     * lần nghe MỚI HƠN: hai máy cùng nghe một bài thì lần gần nhất mới là câu
     * trả lời đúng cho "tôi nghe nó lúc nào".
     *
     * Đếm `daCo` là số dòng trong tệp đã có sẵn trên máy với thời điểm mới hơn
     * hoặc bằng — tức là những dòng không đổi được gì. Màn hình cần con số ấy
     * để nói thật thay vì báo "xong" cho một việc chẳng thay đổi gì.
     */
    fun gop(cac: List<LanNghe>): SaoLuuLichSu.KetQua {
        if (cac.isEmpty()) return SaoLuuLichSu.KetQua(0, 0, 0)

        val theoKhoa = LinkedHashMap<String, LanNghe>()
        for (l in _lichSu.value) theoKhoa[l.khoa] = l

        var them = 0
        var daCo = 0
        for (l in cac) {
            if (l.ten.isBlank() && l.caSi.isBlank()) continue
            val cu = theoKhoa[l.khoa]
            if (cu != null && cu.luc >= l.luc) {
                daCo++
                continue
            }
            theoKhoa[l.khoa] = l
            them++
        }
        if (them == 0) return SaoLuuLichSu.KetQua(0, daCo, 0)

        val moi = theoKhoa.values.sortedByDescending { it.luc }.take(TRAN)
        _lichSu.value = moi
        ghiDia(moi)
        return SaoLuuLichSu.KetQua(them, daCo, 0)
    }

    /** Xoá sạch. Không có bước hoàn tác — bên gọi phải hỏi trước. */
    fun xoaHet() {
        _lichSu.value = emptyList()
        ghiDia(emptyList())
    }

    /** Xoá một dòng, cho lúc người dùng chỉ muốn giấu đúng một bài. */
    fun xoa(khoa: String) {
        val moi = _lichSu.value.filterNot { it.khoa == khoa }
        if (moi.size == _lichSu.value.size) return
        _lichSu.value = moi
        ghiDia(moi)
    }

    private fun ghiDia(ds: List<LanNghe>) {
        try {
            file.writeText(json.encodeToString(BO, ds))
        } catch (e: Exception) {
            Log.w(TAG, "Khong ghi duoc lich su nghe", e)
        }
    }

    companion object {
        private const val TAG = "AuraLichSu"
        private val BO = ListSerializer(LanNghe.serializer())

        /** Giữ nhiều nhất chừng này dòng. Cũ nhất rơi ra trước. */
        internal const val TRAN = 200

        /** Nghe đủ chừng này thì tính là đã nghe, dù bài dài tới đâu. */
        internal const val NGUONG_MS = 30_000L

        /**
         * Đã nghe đủ lâu để tính là một lần nghe chưa.
         *
         * KHÔNG GHI MỌI THỨ CHẠM VÀO. Lướt qua mười bài trong ba mươi giây để
         * tìm một bài là chuyện thường ngày; ghi cả mười thì lần sau mở lịch sử
         * ra, cái bài thật sự đã nghe nằm lẫn giữa chín cái vừa bấm nhầm. Lịch
         * sử chỉ đáng tin khi nó KHÔNG ghi những lần đó.
         *
         * Ngưỡng là NỬA PHÚT, hoặc một phần ba bài nếu bài ngắn hơn một phút
         * rưỡi. Chỉ để nửa phút cứng thì mọi đoạn intro, mọi bản nhạc chuông,
         * mọi bài dưới ba mươi giây đều không bao giờ vào được lịch sử dù người
         * dùng đã nghe trọn vẹn từ đầu tới cuối.
         *
         * Tính trên thời gian ĐÃ PHÁT chứ không trên vị trí trong bài: kéo
         * thanh tua tới phút thứ hai rồi bấm bài khác ngay thì vị trí lớn mà
         * người dùng chưa nghe gì cả.
         *
         * `internal` để kiểm được bằng bài kiểm: đây là luật quyết định cái gì
         * vào lịch sử, và một lịch sử đầy rác thì không ai mở lần thứ hai.
         */
        internal fun dangGhi(doDai: Long, daPhat: Long): Boolean {
            if (daPhat <= 0L) return false
            val nguong = if (doDai > 0L) minOf(NGUONG_MS, doDai / 3) else NGUONG_MS
            return daPhat >= nguong
        }
    }
}
