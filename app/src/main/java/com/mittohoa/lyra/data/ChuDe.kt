package com.mittohoa.lyra.data

import android.content.Context

/**
 * Mặt giấy của app: sáng, tối, hay theo máy.
 *
 * Để người dùng chọn chứ không ép một mặt, vì hai mặt phục vụ hai lúc khác
 * nhau thật: đọc lời giữa ban ngày thì mặt giấy rõ hơn hẳn, còn nghe nhạc lúc
 * đã tắt đèn thì một màn hình trắng là chói.
 *
 * Mặc định là THEO_MAY — người dùng đã trả lời câu hỏi sáng/tối một lần ở phần
 * Cài đặt của máy rồi, hỏi lại lần nữa là hỏi thừa.
 */
enum class ChuDe(val nhan: String, val moTa: String) {
    THEO_MAY("Theo máy", "Sáng hay tối tuỳ cài đặt của máy"),
    GIAY("Giấy", "Nền ngà, chữ mực — dễ đọc giữa ban ngày"),
    MUC("Mực", "Nền tối, chữ ngà — đỡ chói khi đã tắt đèn");

    companion object {
        fun tu(ten: String?): ChuDe = entries.firstOrNull { it.name == ten } ?: THEO_MAY
    }
}

/** Nhớ lựa chọn mặt giấy. */
class ChuDePrefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("chu-de", Context.MODE_PRIVATE)

    fun doc(): ChuDe = ChuDe.tu(p.getString("chu-de", null))

    fun ghi(chuDe: ChuDe) {
        p.edit().putString("chu-de", chuDe.name).apply()
    }

    fun docKieuChu(): KieuChu = KieuChu.tu(p.getString("kieu-chu", null))

    fun ghiKieuChu(kieuChu: KieuChu) {
        p.edit().putString("kieu-chu", kieuChu.name).apply()
    }
}

/**
 * Bộ chữ dùng trong app và trong khung lời nổi.
 *
 * Có mục này vì bản 0.2.0 đổi hẳn bộ chữ, và ép một bộ chữ lên người dùng mà
 * không chừa đường quay lại là một việc không nên làm: chữ là thứ người ta nhìn
 * suốt, và khẩu vị chữ thì không ai giống ai.
 *
 * Một lựa chọn duy nhất cho CẢ HAI bề mặt, không tách làm hai. Khung nổi và
 * trang Lời hiện cùng một thứ — lời bài hát — nên chúng khác dáng chữ nhau thì
 * là app hỏng chứ không phải app linh hoạt.
 */
enum class KieuChu(val nhan: String, val moTa: String) {
    SACH("Trang sách", "Chữ có chân cho lời, chữ không chân cho giao diện"),
    MOT_BO("Một bộ", "Be Vietnam Pro cho tất cả — gọn và đều"),
    MAY("Phông máy", "Dùng đúng bộ chữ hệ thống, như mọi app khác");

    companion object {
        fun tu(ten: String?): KieuChu = entries.firstOrNull { it.name == ten } ?: SACH
    }
}
