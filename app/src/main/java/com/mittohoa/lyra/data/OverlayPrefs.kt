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
    /**
     * Chi la con so cuoi cung khi khong biet gi ve may. Duong di binh thuong
     * la `OverlayPrefs.read()`, va no do man hinh that - xem `suggestFontSizeSp`.
     */
    val fontSizeSp: Float = 26f,
    val textColor: Int = Color.WHITE,
    val strokeColor: Int = Color.BLACK,
    val strokeWidthDp: Float = 2f,
    val backgroundOpacity: Float = 0.35f,
    /** So dong hien them truoc va sau dong dang hat. */
    val contextLines: Int = 1,
    /**
     * Hien thanh dieu khien o DAY khung.
     *
     * Dat duoi cung chu khong de len loi: loi la thu nguoi ta dang doc, va mot
     * hang nut noi giua no la lay cho cua chinh minh.
     */
    val showControls: Boolean = false,

    /**
     * Lam mo ca man hinh phia sau khung loi, 0 = tat.
     *
     * Mot lop phu rieng nam DUOI khung loi, cham xuyen qua duoc. Dung de doc
     * loi ma khong bi phan con lai cua man hinh keo mat di.
     */
    val dimBackground: Float = 0f,

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

    /**
     * Co chu hop voi may nay. Do mot lan luc dung `OverlayPrefs`: be ngang nho
     * nhat cua man hinh khong doi trong doi mot tien trinh.
     */
    val suggestedFontSizeSp: Float = suggestFontSizeSp(context)

    fun read(): OverlayLook = OverlayLook(
        fontSizeSp = prefs.getFloat("fontSize", suggestedFontSizeSp),
        textColor = prefs.getInt("textColor", Color.WHITE),
        strokeColor = prefs.getInt("strokeColor", Color.BLACK),
        strokeWidthDp = prefs.getFloat("strokeWidth", 2f),
        backgroundOpacity = prefs.getFloat("bgOpacity", 0.35f),
        contextLines = prefs.getInt("contextLines", 1),
        showControls = prefs.getBoolean("showControls", false),
        dimBackground = prefs.getFloat("dimBackground", 0f),
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
            .putBoolean("showControls", look.showControls)
            .putFloat("dimBackground", look.dimBackground)
            .putBoolean("clickThrough", look.clickThrough)
            .putInt("x", look.x)
            .putInt("y", look.y)
            .apply()
    }

    /**
     * Nguoi dung co dang bat khung noi khong.
     *
     * Phai luu, khong duoc de trong bo nho: tien trinh cua app chi duoc neo boi
     * `NotificationListenerService`, va Android giet no bat cu luc nao khi may
     * thieu bo nho - dung luc nguoi dung dang nghe nhac o app khac, tuc dung luc
     * khung noi can co mat nhat. Giet xong he thong noi lai service, va chi co
     * cai co nay moi biet co phai dung lai khung hay khong.
     */
    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
    }

    /** Chi ghi vi tri - goi sau moi lan keo tha, khong dung cham cac muc khac. */
    fun writePosition(x: Int, y: Int) {
        prefs.edit().putInt("x", x).putInt("y", y).apply()
    }
}
