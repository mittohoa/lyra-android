package com.mittohoa.lyra.data

import android.content.Context
import android.graphics.Color

/**
 * Hinh thuc cua khung loi noi.
 *
 * Bo cai dat nay co doi tuong doc rat cu the: nguoi dung dang xem mot thu khac
 * (Spotify, YouTube) va khung loi de chen len tren. Nen thu can chinh khong
 * phai la mau me, ma la ba dieu:
 *
 *   - Chu du to de doc luot khong?
 *   - Nen co du mo de doc tren mot man hinh sang khong?
 *   - Khung co che mat thu dang xem khong?
 */
data class OverlayLook(
    val fontSizeSp: Float = 26f,
    val textColor: Int = Color.WHITE,
    val strokeColor: Int = Color.BLACK,
    val strokeWidthDp: Float = 2f,
    val backgroundOpacity: Float = 0.35f,
    /** So dong hien them truoc va sau dong dang hat. */
    val contextLines: Int = 1,
    /** Cham co di xuyen qua khung xuong app ben duoi khong. */
    val clickThrough: Boolean = false,
    /** Vi tri da keo toi, nho lai cho lan mo sau. */
    val x: Int = 0,
    val y: Int = 240
)

/**
 * Doc va ghi hinh thuc khung noi.
 *
 * Dung `SharedPreferences` chu khong dung DataStore: khung noi duoc dung tu
 * mot service co the bi he thong giet va dung lai bat cu luc nao, va luc do no
 * can doc cai dat NGAY trong ham `show()` - khong co cho de doi mot luong bat
 * dong bo.
 */
class OverlayPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("overlay", Context.MODE_PRIVATE)

    fun read(): OverlayLook = OverlayLook(
        fontSizeSp = prefs.getFloat("fontSize", 26f),
        textColor = prefs.getInt("textColor", Color.WHITE),
        strokeColor = prefs.getInt("strokeColor", Color.BLACK),
        strokeWidthDp = prefs.getFloat("strokeWidth", 2f),
        backgroundOpacity = prefs.getFloat("bgOpacity", 0.35f),
        contextLines = prefs.getInt("contextLines", 1),
        clickThrough = prefs.getBoolean("clickThrough", false),
        x = prefs.getInt("x", 0),
        y = prefs.getInt("y", 240)
    )

    fun write(look: OverlayLook) {
        prefs.edit()
            .putFloat("fontSize", look.fontSizeSp)
            .putInt("textColor", look.textColor)
            .putInt("strokeColor", look.strokeColor)
            .putFloat("strokeWidth", look.strokeWidthDp)
            .putFloat("bgOpacity", look.backgroundOpacity)
            .putInt("contextLines", look.contextLines)
            .putBoolean("clickThrough", look.clickThrough)
            .putInt("x", look.x)
            .putInt("y", look.y)
            .apply()
    }

    /** Chi ghi vi tri - goi sau moi lan keo tha, khong dung cham cac muc khac. */
    fun writePosition(x: Int, y: Int) {
        prefs.edit().putInt("x", x).putInt("y", y).apply()
    }
}
