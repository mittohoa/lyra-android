package com.mittohoa.lyra.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
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

    fun show(context: Context, x: Int = 0, y: Int = 0) {
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) {
            Log.i(TAG, "Chua co quyen ve de len app khac")
            return
        }

        val wm = context.getSystemService(WindowManager::class.java) ?: return
        val overlay = OverlayView(context)
        val lp = buildParams(x, y)

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

        target.setOnTouchListener { _, event ->
            val lp = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - touchX).roundToInt()
                    lp.y = startY + (event.rawY - touchY).roundToInt()
                    runCatching { windowManager?.updateViewLayout(target, lp) }
                    true
                }

                else -> false
            }
        }
    }

    /** Vi tri hien tai, de nho lai cho lan mo sau. */
    fun currentPosition(): Pair<Int, Int>? = params?.let { it.x to it.y }

    private companion object {
        const val TAG = "LyraOverlay"
    }
}
