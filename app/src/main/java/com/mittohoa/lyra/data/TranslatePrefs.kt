package com.mittohoa.lyra.data

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage

/**
 * Cai dat phan dich loi.
 *
 * `readingLanguage` la ngon ngu NGUOI DUNG DOC, khong phai ngon ngu can dich
 * sang - noi vay cho dung cach nghi: app tu biet bai dang la tieng gi, viec cua
 * nguoi dung chi la noi minh doc duoc tieng gi. Loi da dung tieng ay thi khong
 * co gi phai dich.
 */
data class TranslateSettings(
    val enabled: Boolean = true,
    val readingLanguage: String = TranslateLanguage.VIETNAMESE,
    /** Chi tai goi ngon ngu khi dang o Wi-Fi. */
    val wifiOnly: Boolean = true
)

class TranslatePrefs(context: Context) {

    private val prefs = context.getSharedPreferences("translate", Context.MODE_PRIVATE)

    fun read(): TranslateSettings = TranslateSettings(
        enabled = prefs.getBoolean("enabled", true),
        readingLanguage = prefs.getString("readingLanguage", null)
            ?: TranslateLanguage.VIETNAMESE,
        wifiOnly = prefs.getBoolean("wifiOnly", true)
    )

    fun write(settings: TranslateSettings) {
        prefs.edit()
            .putBoolean("enabled", settings.enabled)
            .putString("readingLanguage", settings.readingLanguage)
            .putBoolean("wifiOnly", settings.wifiOnly)
            .apply()
    }
}
