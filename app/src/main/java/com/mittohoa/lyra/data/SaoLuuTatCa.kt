package com.mittohoa.lyra.data

/**
 * Một tệp sao lưu duy nhất, gói mọi thứ người dùng đã tự tạo.
 *
 * VÌ SAO GỘP. App có ba bốn kho dữ liệu do người dùng làm ra — lời tự nhập,
 * lịch sử nghe, danh sách yêu thích, lựa chọn cân bằng âm — và mỗi kho một nút
 * sao lưu riêng nghĩa là bốn nút nằm ở bốn mục. Ai cũng sẽ bấm hai cái rồi
 * quên hai cái, và tới lúc đổi máy mới biết mình quên cái nào.
 *
 * NÓ SỬA MỘT CHỖ HỞ CÓ THẬT: yêu thích và cân bằng âm ra ở 0.3.29 mà không
 * kèm đường sao lưu nào, trong khi lời và lịch sử thì có. Gỡ app là mất, và
 * không có gì báo trước.
 *
 * ĐỊNH DẠNG — vẫn chữ trơn, và vẫn là mấy định dạng cũ nguyên vẹn, chỉ thêm
 * một dòng tiêu đề trước mỗi phần:
 *
 *     LYRA-TATCA  1
 *     === LYRA-LOI
 *     <nguyên văn tệp sao lưu lời>
 *     === LYRA-NGHE
 *     <nguyên văn tệp sao lưu lịch sử>
 *     === LYRA-THICH
 *     <mỗi dòng một địa chỉ bài>
 *     === LYRA-CANBANG
 *     bat=1
 *     mau=2
 *
 * KHÔNG DỰNG MỘT ĐỊNH DẠNG MỚI cho từng phần: mỗi phần bên trong y hệt tệp
 * riêng của nó. Nhờ vậy bộ đọc của từng kho không phải sửa một dòng nào, và
 * người mở tệp ra vẫn đọc được từng phần như trước.
 *
 * ĐỌC ĐƯỢC CẢ TỆP CŨ. Tệp sao lưu chỉ có lời, hoặc chỉ có lịch sử, đã nằm
 * trong máy người dùng từ mấy bản trước — [loai] nhìn dòng đầu để biết đang
 * cầm loại nào. Ra một định dạng mới rồi bỏ rơi tệp cũ thì đúng vào lúc người
 * ta cần khôi phục nhất lại là lúc app nói không đọc được.
 */
object SaoLuuTatCa {

    const val NHAN = "LYRA-TATCA"
    const val PHIEN_BAN = 1

    const val PHAN_LOI = "LYRA-LOI"
    const val PHAN_NGHE = "LYRA-NGHE"
    const val PHAN_THICH = "LYRA-THICH"
    const val PHAN_CANBANG = "LYRA-CANBANG"

    /** Tệp đang cầm là loại nào. */
    enum class Loai { TAT_CA, CHI_LOI, CHI_NGHE, KHONG_BIET }

    fun loai(raw: String): Loai {
        val dong = raw.trimStart().lineSequence().firstOrNull().orEmpty()
        return when {
            dong.startsWith(NHAN) -> Loai.TAT_CA
            dong.startsWith(SaoLuuLoi.NHAN) -> Loai.CHI_LOI
            dong.startsWith(SaoLuuLichSu.NHAN) -> Loai.CHI_NGHE
            else -> Loai.KHONG_BIET
        }
    }

    /** Số việc đã khôi phục, để màn hình nói thật thay vì báo "xong". */
    data class KetQua(
        val loi: SaoLuuLoi.KetQua = SaoLuuLoi.KetQua(0, 0, 0),
        val nghe: SaoLuuLichSu.KetQua = SaoLuuLichSu.KetQua(0, 0, 0),
        val thich: Int = 0,
        val coCanBang: Boolean = false,
        val hong: Boolean = false
    )

    /** Lựa chọn cân bằng âm, gói lại để đi qua tệp. */
    data class CanBang(val bat: Boolean, val mau: Int)

    fun xuat(
        loi: String,
        nghe: String,
        thich: Collection<String>,
        canBang: CanBang?
    ): String = buildString {
        append(NHAN).append('\t').append(PHIEN_BAN).append('\n')

        append("=== ").append(PHAN_LOI).append('\n')
        append(loi.trimEnd('\n')).append('\n')

        append("=== ").append(PHAN_NGHE).append('\n')
        append(nghe.trimEnd('\n')).append('\n')

        append("=== ").append(PHAN_THICH).append('\n')
        for (t in thich) append(don(t)).append('\n')

        if (canBang != null) {
            append("=== ").append(PHAN_CANBANG).append('\n')
            append("bat=").append(if (canBang.bat) 1 else 0).append('\n')
            append("mau=").append(canBang.mau).append('\n')
        }
    }

    /**
     * Cắt tệp gộp thành từng phần.
     *
     * Phần nào không có thì trả chuỗi rỗng — tệp của một máy chưa nhập lời nào
     * vẫn hợp lệ, chỉ là phần lời trống.
     */
    fun tach(raw: String): Map<String, String> {
        val dong = raw.replace("\r\n", "\n").split('\n')
        if (dong.isEmpty() || !dong[0].startsWith(NHAN)) return emptyMap()

        val ra = LinkedHashMap<String, StringBuilder>()
        var hien: StringBuilder? = null
        for (i in 1 until dong.size) {
            val d = dong[i]
            if (d.startsWith("=== ")) {
                val ten = d.removePrefix("=== ").trim()
                hien = ra.getOrPut(ten) { StringBuilder() }
                continue
            }
            hien?.append(d)?.append('\n')
        }
        return ra.mapValues { it.value.toString() }
    }

    /** Đọc phần yêu thích: mỗi dòng một địa chỉ. */
    fun docThich(phan: String): List<String> =
        phan.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    /**
     * Đọc phần cân bằng âm.
     *
     * `null` khi phần này không có hoặc hỏng — lúc đó giữ nguyên lựa chọn đang
     * có trên máy, chứ không đặt về một giá trị mặc định nào. Người dùng khôi
     * phục một tệp cũ không có phần này thì đó không phải lý do để đổi tiếng
     * loa của họ.
     */
    fun docCanBang(phan: String): CanBang? {
        var bat: Boolean? = null
        var mau: Int? = null
        for (d in phan.lineSequence()) {
            val (k, v) = d.split('=', limit = 2).let {
                if (it.size < 2) return@let null to null
                it[0].trim() to it[1].trim()
            }
            when (k) {
                "bat" -> bat = v == "1"
                "mau" -> mau = v?.toIntOrNull()
            }
        }
        val b = bat ?: return null
        val m = mau ?: return null
        return CanBang(b, m.coerceAtLeast(0))
    }

    private fun don(s: String) = s.replace('\t', ' ').replace('\n', ' ').trim()
}
