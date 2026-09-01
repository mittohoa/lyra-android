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
}
