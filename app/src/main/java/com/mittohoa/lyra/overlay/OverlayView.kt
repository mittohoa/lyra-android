package com.mittohoa.lyra.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import com.mittohoa.lyra.data.OverlayLook
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

    /** Cai dat hien hanh; doi thi goi `applyLook` roi `invalidate`. */
    var fontSizeSp: Float = 26f
    var textColor: Int = Color.WHITE
    var strokeColor: Int = Color.BLACK
    var strokeWidthDp: Float = 2f
    var backgroundColorValue: Int = Color.BLACK
    var backgroundOpacity: Float = 0.35f
    var contextLines: Int = 1

    private var lines: List<LyricLine> = emptyList()
    private var translations: List<String> = emptyList()
    private var offset: Long = 0
    private var position: Long = 0
    private var idleText: String = ""

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val backdrop = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    /**
     * Mot hang chu tren khung.
     *
     * `lineIndex` la dong loi ma hang nay ung voi - hang dich mang chi so cua
     * chinh dong no dich, vi cham vao ban dich thi trong y nguoi dung van la
     * cham vao cau ay.
     */
    private class Row(
        val text: String,
        val textSize: Float,
        val bold: Boolean,
        val alpha: Int,
        val lineIndex: Int,
        val height: Float
    )

    /**
     * Dai y va chi so dong cua lan ve gan nhat, de doi mot cu cham thanh chi so
     * dong.
     *
     * Phai ghi lai luc VE chu khong tinh lai luc cham: hai ben ma tinh doc lap
     * thi chi can mot ben doi bo cuc la cham tro nham dong, va khong ai phat
     * hien ra vi no van "co ve dung". Cang dung tu khi co them hang dich - no
     * chen vao giua va day cac dong duoi xuong.
     */
    private val drawnBands = ArrayList<Pair<ClosedFloatingPointRange<Float>, Int>>(8)

    /** Ap ca bo hinh thuc mot lan, roi ve lai. */
    fun applyLook(look: OverlayLook) {
        fontSizeSp = look.fontSizeSp
        textColor = look.textColor
        strokeColor = look.strokeColor
        strokeWidthDp = look.strokeWidthDp
        backgroundOpacity = look.backgroundOpacity
        contextLines = look.contextLines
        drawnIndex = Int.MIN_VALUE
        requestLayout()
        invalidate()
    }

    fun setLyrics(lines: List<LyricLine>, offset: Long) {
        this.lines = lines
        this.offset = offset
        // Loi moi thi ban dich cu khong con ung voi gi ca - bo ngay, ban dich
        // cua bai nay se toi sau
        this.translations = emptyList()
        drawnIndex = Int.MIN_VALUE // bai khac roi, buoc ve lai
        requestLayout()
        invalidate()
    }

    /**
     * Ban dich, dat song song voi `lines`.
     *
     * Toi SAU loi vai tram mili-giay, va co the khong bao gio toi. Khung phai
     * chay binh thuong trong ca hai truong hop - nen day la mot loi goi rieng
     * chu khong phai mot tham so cua `setLyrics`.
     */
    fun setTranslations(translations: List<String>) {
        val next = if (translations.size == lines.size) translations else emptyList()
        if (next == this.translations) return
        val hadRow = this.translations.isNotEmpty()
        this.translations = next
        // Them hay bot hang dich la doi CHIEU CAO cua ca cua so
        if (hadRow != next.isNotEmpty()) requestLayout()
        invalidate()
    }

    /** Ten bai hien khi chua co loi, cho do trong. */
    fun setIdleText(text: String) {
        if (idleText == text) return
        idleText = text
        invalidate()
    }

    /** Dong dang hat o lan ve truoc - de biet khi nao that su can ve lai. */
    private var drawnIndex = Int.MIN_VALUE

    /**
     * Cap nhat vi tri phat.
     *
     * Duoc goi 10 lan moi giay, nhung chi ve lai khi DOI DONG. Ve lai vo ich
     * moi 100ms la mot dong ho danh thuc GPU suot ca bai hat - dung thu ta doi
     * mot dong chu thi khong dang.
     */
    fun setPosition(position: Long) {
        this.position = position
        val index = activeLineIndex(lines, position, offset)
        if (index == drawnIndex) return
        drawnIndex = index
        invalidate()
    }

    private fun sp(value: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics
    )

    private fun dp(value: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

    private fun lineHeight() = sp(fontSizeSp) * 1.34f

    /** Hang dich thap hon hang loi: no la chu thich, khong phai loi bai hat. */
    private fun translationHeight() = sp(fontSizeSp) * TRANSLATION_SCALE * 1.34f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // Cao vua du so dong se ve, cong hang dich neu co, cong le tren duoi
        val rows = 1 + contextLines * 2
        var height = rows * lineHeight() + dp(20f)
        if (translations.isNotEmpty()) height += translationHeight()
        setMeasuredDimension(width, height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (backgroundOpacity > 0f) {
            backdrop.color = backgroundColorValue
            backdrop.alpha = (backgroundOpacity * 255).toInt().coerceIn(0, 255)
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, dp(14f), dp(14f), backdrop)
        }

        drawnBands.clear()

        val active = activeLineIndex(lines, position, offset)
        val rows = buildRows(active)

        if (rows.isEmpty()) {
            if (idleText.isNotEmpty()) {
                paint.textSize = sp(fontSizeSp) * 0.82f
                drawRow(canvas, idleText, height / 2f, bold = false, alpha = 150)
            }
            return
        }

        // Dong dang hat luon nam giua khung; cac hang khac xep quanh no
        val activeRow = rows.indexOfFirst { it.bold }.coerceAtLeast(0)
        var baseline = height / 2f + sp(fontSizeSp) * 0.35f
        for (i in 0 until activeRow) baseline -= rows[i].height

        for (row in rows) {
            paint.textSize = row.textSize
            drawRow(canvas, row.text, baseline, row.bold, row.alpha)
            drawnBands.add(
                (baseline - row.height * 0.75f)..(baseline + row.height * 0.25f) to row.lineIndex
            )
            baseline += row.height
        }
    }

    /**
     * Cham vao toa do nay la cham vao dong nao. Tra ve -1 neu khong trung dong nao.
     *
     * Day la duong sua lech NGAY TAI CHO phat hien ra no: dang xem YouTube,
     * thay loi chay lech, cham vao cau minh dang nghe la xong - khong phai
     * thoat app nhac ra, mo Lyra, roi tim toi trang Loi.
     */
    fun lineIndexAt(y: Float): Int {
        for ((band, index) in drawnBands) if (y in band) return index
        return -1
    }

    /**
     * Cac hang se ve: dong dang hat, may dong truoc/sau, va ban dich neu co.
     *
     * Ban dich chi hien cho DONG DANG HAT chu khong cho ca cac dong phu. Khung
     * noi la thu de liec mat; gap doi so hang thi no thanh mot khoi chu chan
     * mat man hinh - ma nghia cua cau dang hat moi la thu nguoi ta can.
     */
    private fun buildRows(active: Int): List<Row> {
        if (lines.isEmpty() || active < 0) return emptyList()

        val from = (active - contextLines).coerceAtLeast(0)
        val to = (active + contextLines).coerceAtMost(lines.lastIndex)
        val rows = ArrayList<Row>(to - from + 2)

        for (i in from..to) {
            val isActive = i == active
            rows.add(
                Row(
                    // Doan nhac dao khong co chu - ve ky hieu nhac cho do trong
                    text = lines[i].text.ifEmpty { "♪" },
                    textSize = if (isActive) sp(fontSizeSp) else sp(fontSizeSp) * 0.82f,
                    bold = isActive,
                    alpha = if (isActive) 255 else 110,
                    lineIndex = i,
                    height = lineHeight()
                )
            )

            if (!isActive) continue
            val translation = translations.getOrNull(i)?.trim().orEmpty()
            // Ban dich trung y het dong goc thi hien no chi lam chat khung
            if (translation.isEmpty() || translation == lines[i].text.trim()) continue
            rows.add(
                Row(
                    text = translation,
                    textSize = sp(fontSizeSp) * TRANSLATION_SCALE,
                    bold = false,
                    alpha = 190,
                    lineIndex = i,
                    height = translationHeight()
                )
            )
        }

        return rows
    }

    /**
     * Ve mot hang, vien truoc than chu sau.
     *
     * Ve nguoc lai thi vien an lem vao net chu - dung ly do ban Windows phai
     * dat `paint-order: stroke fill`. Va vi nua trong cua net vien bi than chu
     * de len, phai nhan doi do day thi nhin moi dung nhu da dat.
     */
    private fun drawRow(canvas: Canvas, text: String, y: Float, bold: Boolean, alpha: Int) {
        paint.isFakeBoldText = bold
        val x = width / 2f
        val ve = fitToWidth(text)

        if (strokeWidthDp > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(strokeWidthDp) * 2f
            paint.color = strokeColor
            paint.alpha = if (alpha > 200) 255 else 140
            canvas.drawText(ve, x, y, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = textColor
        paint.alpha = alpha
        canvas.drawText(ve, x, y, paint)
    }

    /**
     * Thu chu lai cho vua be ngang khung.
     *
     * `drawText` khong tu xuong dong: cau nao dai hon khung thi bi cat cut o
     * mep, va cau bi cat thuong lai la cau nguoi ta dang muon doc. Xuong dong
     * thi giai quyet duoc, nhung no lam so hang thay doi theo tung cau - khung
     * se pho phong len xep xuong suot ca bai, va phep doi cham thanh so dong
     * cung khong con dung.
     *
     * Thu chu lai giu duoc ca hai: bo cuc khong doi, va khong mat chu nao. Chi
     * thu toi mot muc - duoi nua thi doc khong noi, va luc ay thu chu tiep chi
     * la mot cach khac de mat cau do.
     */
    private fun fitToWidth(text: String): String {
        val available = width - dp(24f)
        if (available <= 0f) return text
        val measured = paint.measureText(text)
        if (measured <= available) return text

        val ratio = (available / measured).coerceAtLeast(MIN_SHRINK)
        paint.textSize = paint.textSize * ratio

        // Thu toi day roi van khong vua - lyric tieng Viet dai gap luon. Ve
        // nguyen van thi hong: `drawText` can giua nen cau bi cat CA HAI DAU,
        // mat luon chu dau cau, ma dau cau moi la cho de bat nhip. Cat bot duoi
        // va dat dau ba cham thi giu duoc dau cau va noi ro la con nua.
        if (paint.measureText(text) <= available) return text
        return TextUtils
            .ellipsize(text, TextPaint(paint), available, TextUtils.TruncateAt.END)
            .toString()
    }

    private companion object {
        /** Chu cua hang dich, tinh theo co chu loi. */
        const val TRANSLATION_SCALE = 0.66f

        /**
         * Cau dai may cung khong thu chu nho hon ngan nay lan co da dat.
         *
         * Ha tu 0,55 xuong 0,45 sau khi do tren may that: lyric tieng Viet dai
         * hon lyric tieng Anh dang ke, va o 0,55 thi nhung cau nhu "Cam on nha
         * vi da luon la noi de khoi hanh nhung chuyen di" van tran ra ngoai.
         */
        const val MIN_SHRINK = 0.45f
    }
}
