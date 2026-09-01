package com.mittohoa.lyra.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.data.OverlayLook
import com.mittohoa.lyra.data.OverlayPrefs
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dung va giu khung loi noi.
 *
 * Cua so nay khong thuoc Activity nao ca - no gan thang vao `WindowManager` cua
 * he thong, nen song doc lap voi moi man hinh cua app. Do la ca diem manh (hien
 * de len app khac) va diem phai can than (khong ai don ho, phai tu go).
 */
class OverlayHost {

    private var windowManager: WindowManager? = null
    private var view: OverlayView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())

    /** Dang hien hay khong. */
    val isShowing: Boolean get() = view != null

    /** Chuot/ngon tay di xuyen qua khung xuong app ben duoi. */
    var clickThrough: Boolean = false
        set(value) {
            field = value
            applyFlags()
        }

    /**
     * Hieu ung chu. Nho o day chu khong chi dat vao view: khung co the bi dung
     * roi dung lai nhieu lan, va moi lan la mot `OverlayView` moi tinh khong
     * biet gi ve lua chon cua nguoi dung.
     */
    var effect: LyricEffect = LyricEffect.SANG_DAN
        set(value) {
            field = value
            view?.effect = value
        }

    /** Noi luu hinh thuc; gan khi mo, de con ghi lai vi tri sau moi lan keo. */
    private var prefs: OverlayPrefs? = null

    /** Cham vao mot cau tren khung: dung de can lai loi theo cau dang nghe. */
    var onLineTap: ((Int) -> Unit)? = null

    /**
     * Giu tay tren khung: tat khung noi.
     *
     * Day la duong tat NGAN NHAT co the - ngon tay dang o ngay tren cai can
     * tat. Cac duong khac deu bat nguoi dung roi cho: mo Lyra ra bam nut, hoac
     * vuot thanh thong bao xuong tim o Quick Settings.
     */
    var onDismiss: (() -> Unit)? = null

    fun show(context: Context) {
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) {
            Log.i(TAG, "Chua co quyen ve de len app khac")
            return
        }

        val store = prefs ?: OverlayPrefs(context.applicationContext).also { prefs = it }
        val look = store.read()

        val wm = context.getSystemService(WindowManager::class.java) ?: return
        val overlay = OverlayView(context).apply {
            applyLook(look)
            effect = this@OverlayHost.effect
        }
        clickThrough = look.clickThrough
        val lp = buildParams(look.x, look.y)

        try {
            wm.addView(overlay, lp)
        } catch (e: Exception) {
            // Quyen vua bi rut, hoac may cua hang chan kieu cua so nay
            Log.w(TAG, "Khong dung duoc khung loi noi", e)
            return
        }

        windowManager = wm
        view = overlay
        params = lp
        attachDrag(overlay)
    }

    fun hide() {
        val wm = windowManager
        val v = view
        if (wm != null && v != null) {
            runCatching { wm.removeView(v) }
        }
        windowManager = null
        view = null
        params = null
    }

    /** Doi noi dung dang hien; goi tren luong chinh. */
    fun update(block: OverlayView.() -> Unit) {
        handler.post { view?.apply(block) }
    }

    /** Ap hinh thuc moi ngay lap tuc, va ghi lai de lan sau mo van vay. */
    fun applyLook(context: Context, look: OverlayLook) {
        val store = prefs ?: OverlayPrefs(context.applicationContext).also { prefs = it }
        // Giu nguyen vi tri dang co - no thuoc ve lan keo tha, khong thuoc bang chinh
        val current = store.read()
        store.write(look.copy(x = current.x, y = current.y))

        clickThrough = look.clickThrough
        handler.post {
            view?.applyLook(look)
            view?.requestLayout()
        }
    }

    /** Co chu hop voi man hinh may nay - de giao dien moi nguoi dung quay ve. */
    fun suggestedFontSize(context: Context): Float =
        (prefs ?: OverlayPrefs(context.applicationContext).also { prefs = it }).suggestedFontSizeSp

    /** Doc hinh thuc dang luu, de giao dien hien dung gia tri. */
    fun currentLook(context: Context): OverlayLook =
        (prefs ?: OverlayPrefs(context.applicationContext).also { prefs = it }).read()

    private fun buildParams(x: Int, y: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        // API 26+. Cac kieu cua so overlay cu hon deu da bi Android chan.
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        baseFlags(),
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        this.x = x
        this.y = y
    }

    /**
     * NOT_FOCUSABLE: khong cuop ban phim cua app dang dung - thieu cai nay thi
     *   dang go tin nhan ma khung hien len la mat ban phim.
     * NOT_TOUCH_MODAL: cham ra ngoai khung thi roi xuong app ben duoi.
     * LAYOUT_NO_LIMITS: cho phep keo ra sat vien va ra vung tai tho.
     */
    private fun baseFlags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    private fun applyFlags() {
        val wm = windowManager ?: return
        val v = view ?: return
        val lp = params ?: return

        lp.flags = if (clickThrough) {
            baseFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            baseFlags()
        }
        runCatching { wm.updateViewLayout(v, lp) }
    }

    /**
     * Keo tha bang mot ngon tay.
     *
     * Tinh theo `rawX/rawY` chu khong phai `x/y`: toa do trong view thay doi
     * ngay khi cua so bi dich, nen dung no thi khung rung giat va truot khoi
     * ngon tay.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachDrag(target: OverlayView) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var downAt = 0L
        var moved = false

        // Giu tay = tat khung. Hen gio o `ACTION_DOWN` roi huy o `MOVE`/`UP`,
        // chu khong doi toi luc tha tay moi xet: nguoi dung phai biet minh da giu
        // DU LAU ngay trong luc con dang giu, bang cu rung bao - khong thi ho chi
        // biet minh giu hut sau khi da nhac tay.
        val dismissAfterHold = Runnable {
            if (moved) return@Runnable
            target.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onDismiss?.invoke()
        }
        val holdMs = ViewConfiguration.getLongPressTimeout().toLong() + HOLD_EXTRA_MS

        // Nguong "coi la da keo", tinh theo mat do man hinh chu khong phai
        // pixel: cung mot ngon tay run tren may 1080p va may 1440p sinh ra so
        // pixel khac han nhau
        val slop = ViewConfiguration.get(target.context).scaledTouchSlop

        target.setOnTouchListener { _, event ->
            val lp = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    downAt = System.currentTimeMillis()
                    moved = false
                    handler.postDelayed(dismissAfterHold, holdMs)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (!moved && abs(dx) + abs(dy) > slop) {
                        moved = true
                        // Da xe dich thi day la keo, khong phai giu
                        handler.removeCallbacks(dismissAfterHold)
                    }
                    if (moved) {
                        lp.x = startX + dx.roundToInt()
                        lp.y = startY + dy.roundToInt()
                        runCatching { windowManager?.updateViewLayout(target, lp) }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(dismissAfterHold)
                    if (moved) {
                        // Nho cho vua keo toi. Ghi luc THA TAY chu khong phai
                        // moi lan di chuyen: keo mot doan la hang tram lan ghi dia.
                        prefs?.writePosition(lp.x, lp.y)
                    } else if (
                        event.action == MotionEvent.ACTION_UP &&
                        System.currentTimeMillis() - downAt < TAP_MS
                    ) {
                        // Khong xe dich va nhac tay nhanh = mot cu cham
                        val index = target.lineIndexAt(event.y)
                        if (index >= 0) {
                            // Rung nhe de bao da nhan: nguoi dung dang nhin app
                            // khac, khong the trong cho phan hoi bang mat
                            target.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onLineTap?.invoke(index)
                        }
                    }
                    false
                }

                else -> false
            }
        }
    }

    /** Vi tri hien tai, de nho lai cho lan mo sau. */
    fun currentPosition(): Pair<Int, Int>? = params?.let { it.x to it.y }

    private companion object {
        /**
         * Giu them ngan nay nua moi coi la muon tat.
         *
         * Dai hon nguong giu thong thuong cua he thong mot chut: tat nham khung
         * loi giua bai la kho chiu hon han la phai giu them mot phan tu giay.
         */
        const val HOLD_EXTRA_MS = 250L

        const val TAG = "LyraOverlay"

        /** Nhac tay trong khoang nay va khong xe dich thi coi la mot cu cham. */
        const val TAP_MS = 400L
    }
}
