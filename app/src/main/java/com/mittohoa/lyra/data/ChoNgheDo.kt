package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Nghe dở tới đâu thì lần sau mở lại từ đó.
 *
 * VÌ SAO CẦN. AURA quét cả thư mục phim và phát video thật — một tệp bốn mươi
 * phút mà mở lại quay về giây số không thì gần như không dùng được. Cũng đúng
 * với podcast, sách nói, và mấy bản thu buổi diễn dài.
 *
 * KHÔNG NHỚ CHO MỌI THỨ, và đây là chỗ dễ làm sai nhất. Một bài hát ba phút mà
 * mở lại từ giữa là khó chịu: nhạc thì người ta nghe lại từ đầu, còn phim thì
 * người ta xem tiếp. Luật ở [dangNho] chia theo đúng ranh giới đó.
 *
 * CHỈ CHO NHẠC AURA TỰ PHÁT. Nhạc ở Zing hay YouTube thì bên đó giữ chỗ của họ,
 * AURA không tua được cho họ và cũng không nên giả vờ là mình nhớ hộ.
 *
 * Nằm ở `filesDir` chứ không phải `cacheDir`: đây là việc người dùng đã làm —
 * họ đã nghe tới chỗ đó — chứ không phải thứ dựng lại được từ máy. Mất nó là
 * mất thật.
 */
class ChoNgheDo(context: Context) {

    private val file = File(context.filesDir, "cho-nghe-do.json")
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Địa chỉ bài → mili-giây đã nghe tới.
     *
     * Giữ trong bộ nhớ và chỉ đọc đĩa một lần: hàm ghi bị gọi vài giây một lần
     * suốt cả bài, mà đọc lại cả tệp mỗi lần thì đúng là thứ cần tránh.
     */
    private val bo: LinkedHashMap<String, Long> = doc()

    private fun doc(): LinkedHashMap<String, Long> = try {
        if (!file.exists()) LinkedHashMap()
        else LinkedHashMap(json.decodeFromString(BO, file.readText()))
    } catch (e: Exception) {
        // Tệp hỏng thì bắt đầu lại từ rỗng. Cái giá là quên vài chỗ đang nghe
        // dở, không phải là app không mở được.
        Log.w(TAG, "Khong doc duoc cho nghe do", e)
        LinkedHashMap()
    }

    /** Chỗ đã nghe tới của bài này, 0 khi chưa nhớ gì. */
    fun viTri(diaChi: String): Long = bo[diaChi] ?: 0L

    /**
     * Nhớ chỗ đang nghe, hoặc quên đi nếu bài này không đáng nhớ nữa.
     *
     * Gộp cả hai việc vào một hàm là cố ý: bên gọi chỉ biết "đang ở đây", còn
     * việc chỗ đó có đáng giữ hay không là luật của kho này. Tách ra thì bên
     * gọi phải nhớ gọi `xoa` đúng lúc, và sớm muộn sẽ có một đường quên gọi.
     */
    fun ghi(diaChi: String, viTri: Long, doDai: Long, laVideo: Boolean) {
        if (diaChi.isBlank()) return
        if (dangNho(doDai, viTri, laVideo)) {
            // Xoá rồi đặt lại để bài vừa nghe đứng CUỐI. `LinkedHashMap` giữ thứ
            // tự thêm vào, nên cuối danh sách là mới nhất — và khi phải cắt bớt
            // thì cắt từ đầu là cắt đúng cái cũ nhất.
            bo.remove(diaChi)
            bo[diaChi] = viTri
        } else if (bo.remove(diaChi) == null) {
            // Không đáng nhớ và trước đó cũng không nhớ gì: không có gì đổi,
            // đừng ghi đĩa.
            return
        }

        while (bo.size > TRAN) bo.remove(bo.keys.first())
        ghiDia()
    }

    private fun ghiDia() {
        try {
            file.writeText(json.encodeToString(BO, bo))
        } catch (e: Exception) {
            Log.w(TAG, "Khong ghi duoc cho nghe do", e)
        }
    }

    companion object {
        private const val TAG = "AuraChoNgheDo"
        private val BO = MapSerializer(String.serializer(), Long.serializer())

        /** Nhớ nhiều nhất chừng này bài. Cũ nhất bị đẩy ra trước. */
        private const val TRAN = 300

        /** Dưới ngưỡng này coi như chưa nghe gì, trên nữa coi như đã nghe xong. */
        internal const val MEP_MS = 30_000L

        /** Nhạc dài hơn chừng này thì nhớ như phim: podcast, sách nói, bản thu buổi diễn. */
        internal const val DAI_MS = 10 * 60_000L

        /**
         * Bài này có đáng nhớ chỗ đang nghe không.
         *
         * VIDEO thì LUÔN nhớ; NHẠC chỉ nhớ khi dài hơn [DAI_MS]. Đây là ranh
         * giới thật giữa hai cách người ta dùng: phim thì xem tiếp, nhạc thì
         * nghe lại từ đầu. Một bài ba phút mở lại từ giữa là khó chịu, còn một
         * tệp bốn mươi phút mở lại từ số không thì gần như không dùng được.
         *
         * Hai mép cắt ở [MEP_MS]:
         *
         *   - mới nghe chưa tới nửa phút thì không có gì để mà nhớ
         *   - còn chưa tới nửa phút là hết thì coi như đã nghe xong; nhớ chỗ đó
         *     nghĩa là lần sau mở ra nghe được đúng mấy giây rồi hết
         *
         * Không biết bài dài bao nhiêu thì KHÔNG nhớ. Đường phát trực tuyến có
         * lúc chưa kịp báo độ dài, mà không biết độ dài thì không phân biệt nổi
         * "đang giữa bài" với "sắp hết".
         *
         * `internal` để kiểm được bằng bài kiểm: đây là luật quyết định app có
         * mở lại từ giữa hay không, và sai ở đây thì người dùng thấy ngay.
         */
        internal fun dangNho(doDai: Long, viTri: Long, laVideo: Boolean): Boolean {
            if (doDai <= 0L) return false
            if (viTri < MEP_MS) return false
            if (doDai - viTri < MEP_MS) return false
            return laVideo || doDai >= DAI_MS
        }

        /**
         * Chỗ nên bắt đầu phát, tính từ thứ đã nhớ.
         *
         * Trả 0 khi không dùng được. Đối chiếu lại với độ dài THẬT lúc phát chứ
         * không tin thẳng số đã lưu: tệp có thể đã bị thay bằng bản khác ngắn
         * hơn mà vẫn nằm ở đúng địa chỉ đó, và nhảy tới một chỗ quá cuối bài
         * thì bộ máy phát nhảy luôn sang bài sau.
         */
        internal fun choBatDau(daNho: Long, doDai: Long): Long {
            if (daNho <= 0L) return 0L
            if (doDai > 0L && daNho >= doDai - MEP_MS) return 0L
            return daNho
        }
    }
}
