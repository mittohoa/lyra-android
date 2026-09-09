package com.mittohoa.lyra.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Định dạng tệp sao lưu lịch sử nghe.
 *
 * VÌ SAO CẦN. Lịch sử nghe nằm trong bộ nhớ riêng của app: gỡ app là mất, đổi
 * máy là mất, và người dùng không có đường nào lấy ra. Khác lời tự nhập ở một
 * chỗ — lời gõ lại được, còn "tháng trước tôi nghe những gì" thì không dựng lại
 * được bằng bất cứ cách nào. Mất là mất hẳn.
 *
 * ĐỊNH DẠNG — chữ trơn, mỗi lần nghe một dòng, các cột cách nhau bằng tab:
 *
 *     LYRA-NGHE  1
 *     <lúc>  <địa chỉ>  <app>  <ca sĩ>  <tên bài>  <giờ đọc được>
 *
 * Không dùng JSON, cùng lẽ với [SaoLuuLoi]: tệp này người dùng sẽ mở ra xem, và
 * một dòng JSON dài ngoằng thì không ai đọc nổi.
 *
 * MỖI LẦN NGHE ĐÚNG MỘT DÒNG nên không cần đếm dòng như bên lời — chỗ ấy phải
 * đếm vì lời bài hát trải nhiều dòng. Ở đây một dòng hỏng chỉ mất đúng dòng đó.
 *
 * CỘT `lúc` LÀ MILI-GIÂY, không phải giờ đọc được. Giờ đọc được phụ thuộc múi
 * giờ: sao lưu ở Việt Nam rồi khôi phục ở Nhật là mọi dòng lệch hai tiếng, và
 * bài nghe lúc nửa đêm nhảy sang ngày hôm sau. Mili-giây thì không có múi giờ
 * nào để mà lệch.
 *
 * Cột cuối là GIỜ ĐỌC ĐƯỢC, và bộ đọc BỎ QUA nó. Nó ở đó chỉ để người mở tệp ra
 * hiểu mình đang nhìn cái gì — một cột toàn số mười ba chữ số thì tệp này thành
 * ra không đọc được, mà đọc được chính là lý do không dùng JSON.
 */
object SaoLuuLichSu {

    const val NHAN = "LYRA-NGHE"
    const val PHIEN_BAN = 1

    /** Kết quả khôi phục, để màn hình nói thật con số thay vì "xong". */
    data class KetQua(val them: Int, val daCo: Int, val hong: Int)

    fun xuat(cac: List<LanNghe>): String = buildString {
        append(NHAN).append('\t').append(PHIEN_BAN).append('\n')
        val gio = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        for (l in cac) {
            append(l.luc).append('\t')
                .append(don(l.diaChi)).append('\t')
                .append(don(l.app)).append('\t')
                .append(don(l.caSi)).append('\t')
                .append(don(l.ten)).append('\t')
                .append(if (l.luc > 0L) gio.format(Date(l.luc)) else "")
                .append('\n')
        }
    }

    /**
     * Đọc một tệp sao lưu.
     *
     * Trả về danh sách rỗng khi tệp không phải của AURA. Đọc được bao nhiêu thì
     * lấy bấy nhiêu: một dòng hỏng ở giữa không được phép làm mất những dòng
     * còn lại — người dùng đang ở đúng lúc cần nhất.
     */
    fun nhap(raw: String): List<LanNghe> {
        val dong = raw.replace("\r\n", "\n").split('\n')
        if (dong.isEmpty() || !dong[0].startsWith(NHAN)) return emptyList()

        val ra = mutableListOf<LanNghe>()
        for (i in 1 until dong.size) {
            val d = dong[i]
            if (d.isBlank()) continue
            val cot = d.split('\t')
            if (cot.size < 5) continue

            val luc = cot[0].trim().toLongOrNull() ?: continue
            val ten = cot[4]
            val caSi = cot[3]
            // Không tên lẫn không ca sĩ thì dòng ấy chẳng nói lên điều gì —
            // cùng luật với `LichSuNghe.ghi`.
            if (ten.isBlank() && caSi.isBlank()) continue

            ra += LanNghe(
                diaChi = cot[1],
                ten = ten,
                caSi = caSi,
                app = cot[2],
                luc = luc
            )
        }
        return ra
    }

    private fun don(s: String) = s.replace('\t', ' ').replace('\n', ' ').trim()
}
