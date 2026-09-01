package com.mittohoa.lyra.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.mittohoa.lyra.data.LyricEffect
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
    var showControls: Boolean = false

    /** Do dai bai va trang thai phat - chi dung cho dai dieu khien. */
    private var duration: Long = 0
    private var dangPhat: Boolean = false

    /** Vung da ve cua dai dieu khien, de doi mot cu cham thanh mot hanh dong. */
    private var controlsTop = Float.MAX_VALUE
    private var waveRect: ClosedFloatingPointRange<Float> = 0f..0f
    private var waveY: ClosedFloatingPointRange<Float> = 0f..0f

    private var lines: List<LyricLine> = emptyList()
    private var translations: List<String> = emptyList()
    private var offset: Long = 0
    private var position: Long = 0
    private var idleText: String = ""

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    /**
     * Bộ chữ người dùng chọn; `null` là để nguyên bộ chữ của máy.
     *
     * Khung nổi và trang Lời hiện cùng một thứ, nên chúng phải cùng dáng chữ.
     * Khung này sống ngoài cây Compose nên không tự đọc được lựa chọn đó —
     * `Lyra` đẩy sang, xem `Lyra.datKieuChu`.
     */
    var chuRieng: Typeface? = null
        set(value) {
            if (field === value) return
            field = value
            paint.typeface = value
            // Đổi bộ chữ là đổi bề rộng từng dòng, nên chỗ ngắt dòng và chiều
            // cao khung đều phải tính lại chứ không chỉ vẽ lại.
            drawnIndex = Int.MIN_VALUE
            requestLayout()
            invalidate()
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
        backgroundColorValue = look.backgroundColor
        backgroundOpacity = look.backgroundOpacity
        contextLines = look.contextLines
        val doiCao = showControls != look.showControls
        showControls = look.showControls
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

    /**
     * Do dai bai va trang thai phat, cho dai dieu khien.
     *
     * Ve lai NGAY khi doi trang thai phat - nut phai doi hinh cung luc nguoi
     * dung bam, khong cho toi lan doi dong tiep theo.
     */
    fun setTransport(duration: Long, dangPhat: Boolean) {
        val doi = this.dangPhat != dangPhat || this.duration != duration
        this.duration = duration
        this.dangPhat = dangPhat
        if (doi && showControls) invalidate()
    }

    /**
     * Cham vao dai dieu khien: tra ve viec can lam, hoac null neu cham cho khac.
     *
     * `Tua` mang ti le 0..1 chu khong mang mili-giay: view khong biet ben goi
     * se tua bo phat nao, va do dai o day chi de VE.
     */
    sealed interface ChamDieuKhien {
        data object Truoc : ChamDieuKhien
        data object PhatDung : ChamDieuKhien
        data object Sau : ChamDieuKhien
        data class Tua(val tiLe: Float) : ChamDieuKhien
    }

    fun chamDieuKhien(x: Float, y: Float): ChamDieuKhien? {
        if (!showControls || y < controlsTop) return null
        if (y in waveY) {
            if (x !in waveRect) return null
            val rong = waveRect.endInclusive - waveRect.start
            if (rong <= 0f) return null
            return ChamDieuKhien.Tua(((x - waveRect.start) / rong).coerceIn(0f, 1f))
        }
        // Hang nut: chia ba vung theo be ngang, khong doi cham dung tam nut -
        // nut nho ma vung cham nho nua thi bam mai khong trung.
        val giua = width / 2f
        return when {
            x < giua - dp(23f) -> ChamDieuKhien.Truoc
            x > giua + dp(23f) -> ChamDieuKhien.Sau
            else -> ChamDieuKhien.PhatDung
        }
    }

    /**
     * Ten bai hien khi CO bai nhung chua tim ra loi.
     *
     * Chuoi rong nghia la khong co bai nao - luc do khung tu thu minh lai het
     * co, xem `onMeasure`. Mot hop trong lo lung tren man hinh chinh ghi "chua
     * phat bai nao" thi khong noi duoc gi, va no chinh la thu khien nguoi dung
     * tuong app da tat ma cai khung con dinh lai.
     */
    fun setIdleText(text: String) {
        if (idleText == text) return
        val doiCao = idleText.isEmpty() != text.isEmpty()
        idleText = text
        if (doiCao) requestLayout()
        invalidate()
    }

    /** Khong co bai nao va cung khong co loi thi khong co gi de ve. */
    private fun rong(): Boolean = lines.isEmpty() && idleText.isEmpty()

    /** Dong dang hat o lan ve truoc - de biet khi nao that su can ve lai. */
    private var drawnIndex = Int.MIN_VALUE

    /** Hieu ung chu dang dung. Doi thi ve lai ngay. */
    var effect: LyricEffect = LyricEffect.SANG_DAN
        set(value) {
            if (field == value) return
            field = value
            doiDong = 1f
            invalidate()
        }

    /**
     * Tien do trong cau, 0..1. Chi dung cho hai hieu ung quet.
     *
     * Suy tu moc dong nay toi moc dong sau - thu biet chac. KHONG phai vi tri
     * tieng hat trong cau: loi tu LRCLIB khong co moc theo tu.
     */
    private var quet = 0f

    /** Hoat anh "vua doi dong": 0 -> 1 ngay sau moi lan sang cau moi. */
    private var doiDong = 1f
    private var animDoiDong: ValueAnimator? = null

    /**
     * Cap nhat vi tri phat.
     *
     * Duoc goi 10 lan moi giay. Voi phan lon hieu ung thi chi ve lai KHI DOI
     * DONG - ve lai vo ich moi 100ms la mot dong ho danh thuc GPU suot ca bai
     * hat, va day la mot cua so nam de len app khac nen he thong con phai tron
     * no vao tung khung hinh.
     *
     * Hai hieu ung quet la ngoai le co y: chung phai ve lai lien tuc thi moi
     * quet duoc. Trang Chinh noi thang cho nguoi dung biet chung ton pin hon.
     */
    fun setPosition(position: Long) {
        this.position = position
        val index = activeLineIndex(lines, position, offset)

        if (index != drawnIndex) {
            drawnIndex = index
            batDauDoiDong()
            capNhatQuet(index)
            invalidate()
            return
        }

        // Cung mot dong: chi ve lai neu hieu ung dang can quet
        if (effect == LyricEffect.SANG_DAN || effect == LyricEffect.HIEN_CHU) {
            val truoc = quet
            capNhatQuet(index)
            // Chenh duoi 1% thi mat khong thay - bo qua de bot mot lan ve
            if (kotlin.math.abs(quet - truoc) > 0.01f) invalidate()
        }
    }

    /** Cau da chay duoc bao nhieu phan, tinh tu moc dong nay toi dong sau. */
    private fun capNhatQuet(index: Int) {
        if (index < 0 || index > lines.lastIndex) { quet = 0f; return }
        val batDau = lines[index].time + offset
        val ketThuc = lines.getOrNull(index + 1)?.let { it.time + offset } ?: (batDau + 4_000L)
        val dai = (ketThuc - batDau).coerceAtLeast(1L)
        quet = ((position - batDau).toFloat() / dai).coerceIn(0f, 1f)
    }

    /**
     * Chay hoat anh ngan cho hai hieu ung "doi dong thi lam gi do".
     *
     * Chi song ~300ms moi lan sang cau moi roi tat han, nen no khong dung vao
     * ly do giu nhip ve thap o tren.
     */
    private fun batDauDoiDong() {
        animDoiDong?.cancel()
        if (effect != LyricEffect.NAY && effect != LyricEffect.TROI_LEN) {
            doiDong = 1f
            return
        }
        animDoiDong = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (effect == LyricEffect.NAY) 420L else 340L
            interpolator = if (effect == LyricEffect.NAY) OvershootInterpolator(2.2f)
            else DecelerateInterpolator()
            addUpdateListener { doiDong = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animDoiDong?.cancel()
        animDoiDong = null
        super.onDetachedFromWindow()
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

    /** Cao cua dai dieu khien o day khung, 0 khi khong hien. */
    private fun controlsHeight() = if (showControls) dp(58f) else 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // Khong co gi de noi thi khong chiem cho. Cong tac "dang bat" van bat;
        // khung chi thu lai het co roi tu no lon lai khi co bai.
        if (rong()) { setMeasuredDimension(0, 0); return }
        // Cao vua du so dong se ve, cong hang dich neu co, cong le tren duoi
        val rows = 1 + contextLines * 2
        var height = rows * lineHeight() + dp(20f)
        if (translations.isNotEmpty()) height += translationHeight()
        height += controlsHeight()
        setMeasuredDimension(width, height.toInt())
    }

    /**
     * Ve dai dieu khien o DAY khung: thanh song lam timeline, va ba nut.
     *
     * Song chi la HINH DANG, khong phai song am that cua bai. Song that doi
     * doc mau am thanh, ma am thanh cua app khac chi lay duoc qua `Visualizer`
     * - can quyen ghi am micro. Voi mot app loi bai hat thi do la cai gia
     * khong tra noi: chinh sach quyen rieng tu dang ghi "khong micro", va Play
     * Protect von da kho tinh voi app nay roi.
     *
     * Nen day la mot thanh tien do mang hinh song. No khong gia vo la gi khac.
     */
    private fun drawControls(canvas: Canvas, top: Float) {
        val h = controlsHeight()
        val giua = top + h / 2f

        // ---- Thanh song ----
        val leTrai = dp(16f)
        val rong = width - leTrai * 2
        if (rong <= 0f) return

        val tienDo = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        val soCot = (rong / dp(4.5f)).toInt().coerceIn(8, 96)
        val ysong = top + h * 0.30f
        val caoNhat = h * 0.30f

        for (i in 0 until soCot) {
            val x = leTrai + i * (rong / soCot) + dp(1f)
            // Hinh song co dinh, sinh tu chi so cot - khong doi theo bai, nen
            // khong ai tuong no dang do am thanh that.
            val n = kotlin.math.sin(i * 0.7f) * kotlin.math.cos(i * 0.31f)
            val cao = (caoNhat * (0.30f + 0.70f * kotlin.math.abs(n))).coerceAtLeast(dp(2f))
            val daQua = i.toFloat() / soCot <= tienDo
            backdrop.color = textColor
            backdrop.alpha = if (daQua) 230 else 70
            canvas.drawRoundRect(
                x, ysong - cao / 2f, x + rong / soCot - dp(2f), ysong + cao / 2f,
                dp(1.5f), dp(1.5f), backdrop
            )
        }

        // ---- Ba nut ----
        val ynut = top + h * 0.72f
        val co = dp(9f)
        backdrop.color = textColor
        backdrop.alpha = 235
        val giuaX = width / 2f
        val cach = dp(46f)

        veTamGiac(canvas, giuaX - cach, ynut, co, trai = true)
        if (dangPhat) {
            canvas.drawRect(giuaX - co * 0.55f, ynut - co, giuaX - co * 0.15f, ynut + co, backdrop)
            canvas.drawRect(giuaX + co * 0.15f, ynut - co, giuaX + co * 0.55f, ynut + co, backdrop)
        } else {
            veTamGiac(canvas, giuaX, ynut, co, trai = false)
        }
        veTamGiac(canvas, giuaX + cach, ynut, co, trai = false)
        // Vach dung canh nut bai truoc/sau cho ra hinh "tua"
        canvas.drawRect(giuaX - cach - co * 0.95f, ynut - co, giuaX - cach - co * 0.6f, ynut + co, backdrop)
        canvas.drawRect(giuaX + cach + co * 0.6f, ynut - co, giuaX + cach + co * 0.95f, ynut + co, backdrop)

        controlsTop = top
        waveRect = leTrai..(leTrai + rong)
        waveY = (top)..(top + h * 0.55f)
    }

    private fun veTamGiac(canvas: Canvas, cx: Float, cy: Float, r: Float, trai: Boolean) {
        val path = android.graphics.Path()
        if (trai) {
            path.moveTo(cx + r * 0.8f, cy - r); path.lineTo(cx + r * 0.8f, cy + r)
            path.lineTo(cx - r * 0.7f, cy)
        } else {
            path.moveTo(cx - r * 0.8f, cy - r); path.lineTo(cx - r * 0.8f, cy + r)
            path.lineTo(cx + r * 0.7f, cy)
        }
        path.close()
        canvas.drawPath(path, backdrop)
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
                drawRow(canvas, idleText, (height - controlsHeight()) / 2f, bold = false, alpha = 150)
            }
            if (showControls) drawControls(canvas, height - controlsHeight())
            return
        }

        // Dong dang hat luon nam giua PHAN LOI - khong phai giua ca cua so,
        // vi dai dieu khien chiem phan duoi va se keo lech chu len tren.
        val caoLoi = height - controlsHeight()
        val activeRow = rows.indexOfFirst { it.bold }.coerceAtLeast(0)
        var baseline = caoLoi / 2f + sp(fontSizeSp) * 0.35f
        for (i in 0 until activeRow) baseline -= rows[i].height

        for (row in rows) {
            paint.textSize = row.textSize

            // Hai hieu ung "doi dong thi lam gi do" chi tac dong len DONG DANG
            // HAT. Lam ca khoi nhay hay troi len thi khung nhu bi giat, va cai
            // nguoi ta can nhin lai la dong o giua.
            val nhun = row.bold && doiDong < 1f
            if (nhun) {
                canvas.save()
                when (effect) {
                    LyricEffect.NAY -> {
                        val s = 0.86f + 0.14f * doiDong
                        canvas.scale(s, s, width / 2f, baseline - row.height * 0.3f)
                    }
                    LyricEffect.TROI_LEN -> {
                        canvas.translate(0f, (1f - doiDong) * row.height * 0.7f)
                        paint.alpha = (row.alpha * doiDong).toInt().coerceIn(0, 255)
                    }
                    else -> Unit
                }
            }

            drawRow(canvas, row.text, baseline, row.bold, row.alpha)
            if (nhun) canvas.restore()

            drawnBands.add(
                (baseline - row.height * 0.75f)..(baseline + row.height * 0.25f) to row.lineIndex
            )
            baseline += row.height
        }

        if (showControls) drawControls(canvas, height - controlsHeight())
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

        // Toa sang: mot quang mo quanh net chu, mau lay tu chinh mau chu. Khong
        // ton lan ve nao them - chi la mot thuoc tinh cua net ve.
        if (bold && effect == LyricEffect.TOA_SANG) {
            paint.setShadowLayer(dp(9f), 0f, 0f, textColor)
        }

        // Quet sang / hien chu: mot dai mau co canh cung ngay tai vi tri tien
        // do. Chi dat cho DONG DANG HAT - cac dong phu khong quet.
        //
        // Day KHONG phai karaoke tung chu: loi tu LRCLIB khong co moc theo tu,
        // nen khong ai biet tieng hat dang toi chu nao. Cai chay o day la tien
        // do trong cau, thu suy duoc tu moc dong nay toi dong sau.
        val quetDuoc = bold &&
            (effect == LyricEffect.SANG_DAN || effect == LyricEffect.HIEN_CHU)
        if (quetDuoc) {
            val rong = paint.measureText(ve)
            if (rong > 0f) {
                val trai = x - rong / 2f
                val f = quet.coerceIn(0f, 1f)
                val sau = if (effect == LyricEffect.HIEN_CHU) (textColor and 0x00FFFFFF)
                else (textColor and 0x00FFFFFF) or (0x60 shl 24)
                paint.shader = LinearGradient(
                    trai, 0f, trai + rong, 0f,
                    intArrayOf(textColor, textColor, sau, sau),
                    floatArrayOf(0f, f, (f + 0.012f).coerceAtMost(1f), 1f),
                    Shader.TileMode.CLAMP
                )
            }
        }

        canvas.drawText(ve, x, y, paint)

        // Tra `paint` ve nguyen trang: no dung chung cho moi hang, de sot mot
        // shader hay mot quang sang lai la ca khung nhuom theo.
        paint.shader = null
        paint.clearShadowLayer()
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
