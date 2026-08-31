package com.mittohoa.lyra.translate

import com.google.mlkit.nl.translate.TranslateLanguage

/**
 * Cac ngon ngu app cho chon lam ngon ngu DOC.
 *
 * Khong liet ke het hon nam muoi thu tieng ML Kit ho tro: mot danh sach dai la
 * mot danh sach khong ai cuon het. Day la nhung thu tieng thuc te co nguoi Viet
 * doc, con nhac o NGON NGU NAO thi khong gioi han - phan nhan dien tu lo.
 */
val READING_LANGUAGES: List<Pair<String, String>> = listOf(
    TranslateLanguage.VIETNAMESE to "Tiếng Việt",
    TranslateLanguage.ENGLISH to "English",
    TranslateLanguage.JAPANESE to "日本語",
    TranslateLanguage.KOREAN to "한국어",
    TranslateLanguage.CHINESE to "中文"
)

/**
 * Ten tieng Viet cua mot ma ngon ngu, de con bao cho nguoi dung biet ho sap tai
 * goi gi ve may.
 *
 * Chi phu nhung thu tieng hay gap trong nhac. Ma la khong dich duoc thi tra ve
 * chinh cai ma - "Lời đang là tiếng 'sw'" van con hon la khong noi gi.
 */
fun languageName(code: String): String = NAMES[code] ?: code

private val NAMES: Map<String, String> = mapOf(
    TranslateLanguage.VIETNAMESE to "Việt",
    TranslateLanguage.ENGLISH to "Anh",
    TranslateLanguage.JAPANESE to "Nhật",
    TranslateLanguage.KOREAN to "Hàn",
    TranslateLanguage.CHINESE to "Trung",
    TranslateLanguage.THAI to "Thái",
    TranslateLanguage.FRENCH to "Pháp",
    TranslateLanguage.SPANISH to "Tây Ban Nha",
    TranslateLanguage.GERMAN to "Đức",
    TranslateLanguage.RUSSIAN to "Nga",
    TranslateLanguage.INDONESIAN to "Indonesia",
    TranslateLanguage.PORTUGUESE to "Bồ Đào Nha",
    TranslateLanguage.ITALIAN to "Ý",
    TranslateLanguage.HINDI to "Hindi"
)
