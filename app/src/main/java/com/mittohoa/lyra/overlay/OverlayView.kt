package com.mittohoa.lyra.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import com.mittohoa.lyra.lyrics.LyricLine
import com.mittohoa.lyra.lyrics.activeLineIndex

/**
 * Khung loi noi. View thuan, tu ve.
 *
 * Khong dung Compose o day co ly do cu the: `ComposeView` dat trong cua so cua
 * `WindowManager` khong co san `ViewTreeLifecycleOwner` va
 * `SavedStateRegistryOwner`, phai tu gan tay - them mot tang de hong ma doi lai
 * chang duoc gi, vi cho nay chi ve vai dong chu.
 */
class OverlayView(context: Context) : View(context) {

    /** Cai dat hien hanh; doi thi goi `applySettings` roi `invalidate`. */
    var fontSizeSp: Float = 26f
    var textColor: Int = Color.WHITE
    var strokeColor: Int = Color.BLACK
    var strokeWidthDp: Float = 2f
    var backgroundColorValue: Int = Color.BLACK
    var backgroundOpacity: Float = 0.35f
    var contextLines: Int = 1

    private var lines: List<LyricLine> = emptyList()
    private var offset: Long = 0
    private var position: Long = 0
    private var idleText: String = ""

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val backdrop = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun setLyrics(lines: List<LyricLine>, offset: Long) {
        this.lines = lines
        this.offset = offset
        invalidate()
    }

    /** Ten bai hien khi chua co loi, cho do trong. */
    fun setIdleText(text: String) {
        idleText = text
        invalidate()
    }

    fun setPosition(position: Long) {
        this.position = position
        invalidate()
    }

    private fun sp(value: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics
    )

    private fun dp(value: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // Cao vua du so dong se ve, cong le tren duoi
        val rows = 1 + contextLines * 2
        val lineHeight = sp(fontSizeSp) * 1.34f
        val height = (rows * lineHeight + dp(20f)).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (backgroundOpacity > 0f) {
            backdrop.color = backgroundColorValue
            backdrop.alpha = (backgroundOpacity * 255).toInt().coerceIn(0, 255)
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, dp(14f), dp(14f), backdrop)
        }

        val active = activeLineIndex(lines, position, offset)
        val shown = visibleLines(active)

        if (shown.isEmpty()) {
            if (idleText.isNotEmpty()) drawLine(canvas, idleText, height / 2f, isActive = false)
            return
        }

        val lineHeight = sp(fontSizeSp) * 1.34f
        // Dong dang hat luon nam giua khung, cac dong phu deu hai ben
        val centerY = height / 2f + sp(fontSizeSp) * 0.35f
        val activeSlot = shown.indexOfFirst { it.second }

        shown.forEachIndexed { i, (text, isActive) ->
            drawLine(canvas, text, centerY + (i - activeSlot) * lineHeight, isActive)
        }
    }

    /** Dong dang hat cong may dong truoc/sau theo cai dat. */
    private fun visibleLines(active: Int): List<Pair<String, Boolean>> {
        if (lines.isEmpty() || active < 0) return emptyList()
        val from = (active - contextLines).coerceAtLeast(0)
        val to = (active + contextLines).coerceAtMost(lines.lastIndex)
        return (from..to).map { i ->
            // Doan nhac dao khong co chu - ve ky hieu nhac cho do trong
            (lines[i].text.ifEmpty { "♪" }) to (i == active)
        }
    }

    /**
     * Ve mot dong, vien truoc than chu sau.
     *
     * Ve nguoc lai thi vien an lem vao net chu - dung ly do ban Windows phai
     * dat `paint-order: stroke fill`. Va vi nua trong cua net vien bi than chu
     * de len, phai nhan doi do day thi nhin moi dung nhu da dat.
     */
    private fun drawLine(canvas: Canvas, text: String, y: Float, isActive: Boolean) {
        paint.textSize = if (isActive) sp(fontSizeSp) else sp(fontSizeSp) * 0.82f
        paint.isFakeBoldText = isActive
        val x = width / 2f

        if (strokeWidthDp > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(strokeWidthDp) * 2f
            paint.color = strokeColor
            paint.alpha = if (isActive) 255 else 140
            canvas.drawText(text, x, y, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = textColor
        paint.alpha = if (isActive) 255 else 110
        canvas.drawText(text, x, y, paint)
    }
}
