package com.mittohoa.lyra.data

/**
 * Bảy ngày qua bạn nghe gì.
 *
 * VÌ SAO TÁCH RA KHỎI MÀN HÌNH. Đây là phép tính, và phép tính thì kiểm được
 * bằng bài kiểm; để nó nằm trong một `@Composable` là để nó không bao giờ được
 * kiểm. Mấy con số này còn dễ sai một cách im lặng nữa — lệch một ngày ở ranh
 * giới tuần thì không có gì báo, chỉ là con số hơi khác.
 *
 * NÓ KHÔNG PHẢI "TUẦN QUA BẠN NGHE BAO NHIÊU LẦN", và chỗ này phải nói rõ.
 * [LichSuNghe] gộp mỗi bài một dòng và chỉ giữ LẦN NGHE GẦN NHẤT, nên:
 *
 *  - Đếm được: bao nhiêu BÀI có lần nghe gần nhất rơi vào bảy ngày qua.
 *  - Không đếm được: một bài nghe mười lần trong tuần thì vẫn là một dòng.
 *  - Một bài nghe cả tuần này lẫn tuần trước chỉ tính vào TUẦN NÀY, vì dấu vết
 *    của lần nghe tuần trước đã bị lần sau đè lên.
 *
 * [LanNghe.soLan] có giữ tổng số lần, nhưng đó là tổng TỪ TRƯỚC TỚI GIỜ chứ
 * không phải trong tuần — đem nó ra làm con số của tuần là bịa. Nên ở đây
 * không dùng tới nó, và màn hình phải nói "bài" chứ đừng nói "lần".
 */
object TuanNghe {

    const val MOT_NGAY_MS = 24L * 60 * 60 * 1000
    const val MOT_TUAN_MS = 7 * MOT_NGAY_MS

    /** Một ca sĩ và số bài của họ trong kỳ. */
    data class CaSi(val ten: String, val soBai: Int)

    data class TomTat(
        /** Số bài có lần nghe gần nhất nằm trong bảy ngày qua. */
        val soBai: Int,
        /** Cùng phép đếm ấy cho bảy ngày liền trước. Để nói "hơn tuần trước". */
        val tuanTruoc: Int,
        /** Bài đến từ app khác — thứ mà chính các app đó không kể cho bạn. */
        val tuAppKhac: Int,
        val caSi: List<CaSi>
    ) {
        val trong: Boolean get() = soBai == 0

        /** Bài do chính AURA phát. */
        val tuAura: Int get() = soBai - tuAppKhac

        /**
         * Hơn tuần trước bao nhiêu bài. Âm là ít hơn.
         *
         * `null` khi tuần trước không có dòng nào: lúc ấy so sánh là vô nghĩa —
         * "nhiều hơn tuần trước 12 bài" trong khi tuần trước máy còn chưa cài
         * app thì đó là một câu nói suông.
         */
        val lech: Int? get() = if (tuanTruoc == 0) null else soBai - tuanTruoc
    }

    /**
     * Tính tóm tắt cho bảy ngày tính ngược từ [bayGio].
     *
     * Cắt theo mốc bảy ngày chẵn chứ không theo "từ thứ Hai": người ta mở cái
     * này ra vào một ngày bất kỳ, và "bảy ngày qua" đúng nghĩa với mọi ngày
     * trong tuần. Cắt theo thứ Hai thì sáng thứ Hai mở ra là một trang gần như
     * trống, mỗi tuần một lần.
     */
    fun tinh(
        cac: List<LanNghe>,
        bayGio: Long = System.currentTimeMillis(),
        soCaSi: Int = SO_CA_SI
    ): TomTat {
        val dauTuanNay = bayGio - MOT_TUAN_MS
        val dauTuanTruoc = bayGio - 2 * MOT_TUAN_MS

        // Bỏ dòng không có mốc giờ. Dữ liệu từ mấy bản đầu có thể `luc = 0`, và
        // một dòng như thế rơi vào "trước cả hai tuần" chứ không phải tuần này.
        val tuanNay = cac.filter { it.luc > 0L && it.luc >= dauTuanNay && it.luc <= bayGio }
        val tuanTruoc = cac.count { it.luc in dauTuanTruoc until dauTuanNay }

        val theoCaSi = LinkedHashMap<String, Int>()
        for (l in tuanNay) {
            val ten = l.caSi.trim()
            if (ten.isEmpty()) continue
            theoCaSi[ten] = (theoCaSi[ten] ?: 0) + 1
        }

        return TomTat(
            soBai = tuanNay.size,
            tuanTruoc = tuanTruoc,
            tuAppKhac = tuanNay.count { !it.ngheLaiDuoc },
            caSi = theoCaSi.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key })
                .take(soCaSi)
                .map { CaSi(it.key, it.value) }
        )
    }

    /** Bày mấy ca sĩ. Ba là vừa một dòng chữ, và quá ba thì không ai đọc nữa. */
    const val SO_CA_SI = 3
}
