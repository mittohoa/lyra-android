package com.mittohoa.lyra.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.scale
import com.mittohoa.lyra.R
import com.mittohoa.lyra.data.KieuChu

/**
 * Vẽ một câu hát thành một tấm ảnh chia sẻ được.
 *
 * Vẽ bằng `Canvas` chứ không chụp lại giao diện Compose, và đó là chủ ý: MỘT
 * hàm duy nhất dựng cả tấm xem trước lẫn file xuất ra, nên thứ người dùng nhìn
 * thấy đúng là thứ được gửi đi. Chụp màn hình thì tấm ảnh phụ thuộc vào cỡ máy,
 * mật độ điểm ảnh và cả thanh trạng thái — mỗi máy một kiểu.
 *
 * Sáu mẫu, xem `MauThe`. Chúng khác nhau về BỐ CỤC chứ không phải bộ màu: một
 * câu hát dữ dội và một câu hát buồn không nên trông như nhau.
 */
object TheLoi {

    /** 4:5 — tỉ lệ dọc mà mọi chỗ đăng ảnh đều nhận, và đọc chữ thoải mái. */
    const val RONG = 1080
    const val CAO = 1350

    /** Dòng ở góc dưới. Giữ nguyên trên cả sáu mẫu. */
    private const val THUONG_HIEU = "AURA by #mittoHOA"

    private const val LE = 16f
    private const val TRAI = 104f
    private const val PHAI = 88f

    fun ve(
        context: Context,
        cauHat: String,
        tenBai: String,
        caSi: String,
        mauNhan: Int,
        laGiay: Boolean,
        kieuChu: KieuChu,
        mau: MauThe = MauThe.GIAY,
        bia: Bitmap? = null
    ): Bitmap {
        val anh = Bitmap.createBitmap(RONG, CAO, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(anh)
        val coChan = fontCoChan(context, kieuChu)
        val khongChan = fontKhongChan(context, kieuChu)

        // Mẫu cần bìa mà không có bìa thì lùi về mẫu mặc định, đừng vẽ ra một
        // tấm thẻ trống hoác.
        val thuc = if (mau.canBia && bia == null) MauThe.GIAY else mau

        when (thuc) {
            MauThe.GIAY -> veGiay(canvas, cauHat, tenBai, caSi, mauNhan, laGiay, coChan, khongChan, false)
            MauThe.KE_DONG -> veGiay(canvas, cauHat, tenBai, caSi, mauNhan, laGiay, coChan, khongChan, true)
            MauThe.CHU_LON -> veChuLon(canvas, cauHat, tenBai, caSi, laGiay, coChan, khongChan)
            MauThe.KHOI_MAU -> veKhoiMau(canvas, cauHat, tenBai, caSi, mauNhan, coChan, khongChan)
            MauThe.BIA_MO -> veBiaMo(canvas, cauHat, tenBai, caSi, mauNhan, bia!!, coChan, khongChan)
            MauThe.BIA_TREN -> veBiaTren(canvas, cauHat, tenBai, caSi, mauNhan, laGiay, bia!!, coChan, khongChan)
        }
        return anh
    }

    // ---------- Sáu mẫu ----------

    /**
     * Trang giấy — dáng mặc định, và bản kẻ dòng dùng chung khung này.
     *
     * Kẻ dòng chỉ thêm mấy nét mảnh phía sau chữ: cùng bố cục, khác cảm giác.
     * Tách thành hai hàm riêng thì hai bản sẽ trôi xa nhau sau vài lần sửa.
     */
    private fun veGiay(
        c: Canvas, cauHat: String, tenBai: String, caSi: String,
        mauNhan: Int, laGiay: Boolean, coChan: Typeface, khongChan: Typeface,
        keDong: Boolean
    ) {
        val nen = nenGiay(laGiay)
        val muc = mucGiay(laGiay)
        c.drawColor(nen)

        c.drawRect(0f, 0f, LE, CAO.toFloat(), Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CAO.toFloat(),
                pha(mauNhan, nen, 0.92f), pha(mauNhan, nen, 0.30f), Shader.TileMode.CLAMP
            )
        })

        val rong = (RONG - TRAI - PHAI).toInt()
        val son = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = coChan; color = muc }
        val bocCuc = coVua(cauHat, son, rong, 88f, 620)
        val chuY = (CAO - bocCuc.height) / 2f - 90f

        if (keDong) {
            // Nét kẻ chạy theo đúng nhịp dòng của khối chữ, không phải một lưới
            // kẻ sẵn rồi thả chữ lên trên.
            val sonKe = Paint().apply { color = pha(muc, nen, 0.14f); strokeWidth = 2f }
            for (i in 0 until bocCuc.lineCount) {
                val y = chuY + bocCuc.getLineBottom(i) - 6f
                c.drawLine(TRAI, y, RONG - PHAI, y, sonKe)
            }
        }

        c.save(); c.translate(TRAI, chuY); bocCuc.draw(c); c.restore()

        var y = chuY + bocCuc.height + 78f
        c.drawRect(TRAI, y, TRAI + 104f, y + 6f, Paint().apply { color = mauNhan })
        y += 58f
        veTen(c, tenBai, caSi, TRAI, y, rong, muc, nen, coChan, khongChan)
        veThuongHieu(c, khongChan, pha(muc, nen, 0.34f))
    }

    /** Chữ lớn — bỏ hết trang trí, câu hát chiếm gần cả tấm. */
    private fun veChuLon(
        c: Canvas, cauHat: String, tenBai: String, caSi: String,
        laGiay: Boolean, coChan: Typeface, khongChan: Typeface
    ) {
        val nen = nenGiay(laGiay)
        val muc = mucGiay(laGiay)
        c.drawColor(nen)

        val rong = (RONG - PHAI * 2).toInt()
        val son = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = coChan; color = muc }
        // Cho phép to hơn hẳn mẫu khác và chiếm cao hơn: ở đây câu hát là tất cả.
        val bocCuc = coVua(cauHat, son, rong, 132f, 900)
        val chuY = (CAO - bocCuc.height) / 2f - 60f
        c.save(); c.translate(PHAI, chuY); bocCuc.draw(c); c.restore()

        val sonTen = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = khongChan; color = pha(muc, nen, 0.46f)
            textSize = 26f; letterSpacing = 0.12f
        }
        val ten = if (caSi.isBlank()) tenBai else "$tenBai · $caSi"
        c.drawText(ten.uppercase(), PHAI, CAO - 130f, sonTen)
        veThuongHieu(c, khongChan, pha(muc, nen, 0.34f))
    }

    /** Khối màu — nền đặc màu lấy từ bìa. */
    private fun veKhoiMau(
        c: Canvas, cauHat: String, tenBai: String, caSi: String,
        mauNhan: Int, coChan: Typeface, khongChan: Typeface
    ) {
        // Ép màu về đủ tối để chữ ngà nổi lên, dù ảnh bìa cho ra vàng chanh hay
        // hồng phấn.
        val nen = toiLai(mauNhan)
        val chu = 0xFFF8F3E8.toInt()
        c.drawColor(nen)

        val rong = (RONG - TRAI - PHAI).toInt()
        val son = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = coChan; color = chu }
        val bocCuc = coVua(cauHat, son, rong, 92f, 640)
        val chuY = (CAO - bocCuc.height) / 2f - 80f
        c.save(); c.translate(TRAI, chuY); bocCuc.draw(c); c.restore()

        var y = chuY + bocCuc.height + 74f
        c.drawRect(TRAI, y, TRAI + 104f, y + 6f, Paint().apply { color = pha(chu, nen, 0.75f) })
        y += 56f
        veTen(c, tenBai, caSi, TRAI, y, rong, chu, nen, coChan, khongChan)
        veThuongHieu(c, khongChan, pha(chu, nen, 0.42f))
    }

    /** Bìa mờ — ảnh bìa nhoè phủ kín, chữ nổi lên trên. */
    private fun veBiaMo(
        c: Canvas, cauHat: String, tenBai: String, caSi: String,
        mauNhan: Int, bia: Bitmap, coChan: Typeface, khongChan: Typeface
    ) {
        veBiaPhuKin(c, bia, RectF(0f, 0f, RONG.toFloat(), CAO.toFloat()), nhoe = true)

        // Hai lớp phủ: một lớp màu đều kéo cả tấm về một tông, và một lớp đậm
        // dần xuống dưới để chữ ở nửa dưới luôn đọc được dù bìa sáng.
        c.drawColor((pha(toiLai(mauNhan), Color.BLACK, 0.55f) and 0x00FFFFFF) or (0xA0 shl 24))
        c.drawRect(0f, 0f, RONG.toFloat(), CAO.toFloat(), Paint().apply {
            shader = LinearGradient(
                0f, CAO * 0.25f, 0f, CAO.toFloat(),
                0x00000000, 0xB0000000.toInt(), Shader.TileMode.CLAMP
            )
        })

        val chu = Color.WHITE
        val rong = (RONG - TRAI - PHAI).toInt()
        val son = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = coChan; color = chu
            setShadowLayer(18f, 0f, 3f, 0x80000000.toInt())
        }
        val bocCuc = coVua(cauHat, son, rong, 92f, 620)
        val chuY = (CAO - bocCuc.height) / 2f - 70f
        c.save(); c.translate(TRAI, chuY); bocCuc.draw(c); c.restore()

        var y = chuY + bocCuc.height + 70f
        c.drawRect(TRAI, y, TRAI + 104f, y + 6f, Paint().apply { color = chu })
        y += 56f
        veTen(c, tenBai, caSi, TRAI, y, rong, chu, Color.BLACK, coChan, khongChan)
        veThuongHieu(c, khongChan, 0xB0FFFFFF.toInt())
    }

    /** Bìa trên — ảnh bìa vuông ở trên, lời ở dưới như một trang sách. */
    private fun veBiaTren(
        c: Canvas, cauHat: String, tenBai: String, caSi: String,
        mauNhan: Int, laGiay: Boolean, bia: Bitmap, coChan: Typeface, khongChan: Typeface
    ) {
        val nen = nenGiay(laGiay)
        val muc = mucGiay(laGiay)
        c.drawColor(nen)

        val caoBia = 660f
        veBiaPhuKin(c, bia, RectF(0f, 0f, RONG.toFloat(), caoBia), nhoe = false)
        // Nét kẻ màu ngay dưới bản in, giống hệt trang Bài trong app
        c.drawRect(0f, caoBia, RONG.toFloat(), caoBia + 8f, Paint().apply { color = mauNhan })

        val rong = (RONG - TRAI - PHAI).toInt()
        val son = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = coChan; color = muc }
        val bocCuc = coVua(cauHat, son, rong, 74f, 360)
        val chuY = caoBia + 8f + 78f
        c.save(); c.translate(TRAI, chuY); bocCuc.draw(c); c.restore()

        val y = chuY + bocCuc.height + 56f
        veTen(c, tenBai, caSi, TRAI, y, rong, muc, nen, coChan, khongChan)
        veThuongHieu(c, khongChan, pha(muc, nen, 0.34f))
    }

    // ---------- Mảnh dùng chung ----------

    /**
     * Cỡ chữ co lại cho tới khi khối chữ vừa chiều cao cho phép.
     *
     * Một câu bốn chữ đặt ở cỡ của một câu hai mươi chữ thì lọt thỏm giữa tấm
     * thẻ, còn ngược lại thì tràn ra ngoài.
     */
    private fun coVua(chu: String, son: TextPaint, rong: Int, coDau: Float, caoToiDa: Int): StaticLayout {
        var co = coDau
        var bc: StaticLayout
        while (true) {
            son.textSize = co
            bc = dungBoCuc(chu, son, rong)
            if (bc.height <= caoToiDa || co <= 40f) return bc
            co -= 4f
        }
    }

    /** Tên bài rồi tên ca sĩ. */
    private fun veTen(
        c: Canvas, tenBai: String, caSi: String, x: Float, y: Float, rong: Int,
        muc: Int, nen: Int, coChan: Typeface, khongChan: Typeface
    ) {
        val sonTen = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = coChan
            color = pha(muc, nen, 0.74f)
            textSize = 42f
        }
        val ten = dungBoCuc(tenBai, sonTen, rong, 2)
        c.save(); c.translate(x, y); ten.draw(c); c.restore()

        if (caSi.isNotBlank()) {
            val sonCaSi = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = khongChan
                color = pha(muc, nen, 0.56f)
                textSize = 27f
                letterSpacing = 0.14f
            }
            c.drawText(caSi.uppercase(), x, y + ten.height + 41f, sonCaSi)
        }
    }

    /**
     * Dòng thương hiệu ở góc dưới phải.
     *
     * Nhỏ và mờ: tấm thẻ là của câu hát, không phải của app. Nhưng phải có —
     * chia sẻ đi mà không ai biết nó từ đâu ra thì cũng chẳng để làm gì.
     */
    private fun veThuongHieu(c: Canvas, khongChan: Typeface, mau: Int) {
        c.drawText(THUONG_HIEU, RONG - PHAI, CAO - 62f, TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = khongChan
            color = mau
            textSize = 25f
            letterSpacing = 0.08f
            textAlign = Paint.Align.RIGHT
        })
    }

    /**
     * Vẽ ảnh bìa phủ kín một vùng, cắt bớt phần thừa chứ không bóp méo.
     *
     * `nhoe` làm nhoè bằng cách thu nhỏ rồi phóng to lại có lọc — rẻ hơn nhiều
     * so với một bộ làm nhoè thật, và ở mức nhoè mạnh thì mắt không phân biệt
     * được. `RenderEffect` thì chỉ có từ Android 12.
     */
    private fun veBiaPhuKin(c: Canvas, bia: Bitmap, vung: RectF, nhoe: Boolean) {
        val nguon = if (nhoe) {
            runCatching { bia.scale(40, 40, filter = true) }.getOrDefault(bia)
        } else bia

        val tiLeVung = vung.width() / vung.height()
        val tiLeAnh = nguon.width.toFloat() / nguon.height
        val cat = if (tiLeAnh > tiLeVung) {
            val w = (nguon.height * tiLeVung).toInt()
            Rect((nguon.width - w) / 2, 0, (nguon.width + w) / 2, nguon.height)
        } else {
            val h = (nguon.width / tiLeVung).toInt()
            Rect(0, (nguon.height - h) / 2, nguon.width, (nguon.height + h) / 2)
        }
        c.drawBitmap(nguon, cat, vung, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun dungBoCuc(chu: String, son: TextPaint, rong: Int, toiDaDong: Int = 8): StaticLayout =
        StaticLayout.Builder.obtain(chu, 0, chu.length, son, rong)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.26f)
            .setIncludePad(false)
            .setMaxLines(toiDaDong)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

    private fun nenGiay(laGiay: Boolean) = if (laGiay) 0xFFFBF6EC.toInt() else 0xFF13110D.toInt()
    private fun mucGiay(laGiay: Boolean) = if (laGiay) 0xFF191510.toInt() else 0xFFF6F1E6.toInt()

    /** Trộn `tren` lên `duoi` với độ đục `a`, trả về một màu ĐẶC. */
    internal fun pha(tren: Int, duoi: Int, a: Float): Int = Color.rgb(
        (Color.red(tren) * a + Color.red(duoi) * (1 - a)).toInt(),
        (Color.green(tren) * a + Color.green(duoi) * (1 - a)).toInt(),
        (Color.blue(tren) * a + Color.blue(duoi) * (1 - a)).toInt()
    )

    /**
     * Ép một màu về đủ tối để chữ ngà nổi lên trên.
     *
     * Ảnh bìa cho ra màu tuỳ ý — một bìa vàng chanh làm nền thì chữ ngà biến
     * mất. Hạ độ sáng nhưng giữ nguyên sắc, đúng như `BangMau.mucMau`.
     */
    internal fun toiLai(mau: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(mau, hsv)
        hsv[1] = (hsv[1] * 1.1f).coerceAtMost(1f)
        hsv[2] = hsv[2].coerceAtMost(0.30f)
        return Color.HSVToColor(hsv)
    }

    private fun fontCoChan(context: Context, kieuChu: KieuChu): Typeface = when (kieuChu) {
        KieuChu.SACH -> font(context, R.font.newsreader)
        KieuChu.MOT_BO -> font(context, R.font.be_vietnam_pro_semibold)
        KieuChu.MAY -> Typeface.SERIF
    }

    private fun fontKhongChan(context: Context, kieuChu: KieuChu): Typeface = when (kieuChu) {
        KieuChu.MAY -> Typeface.DEFAULT_BOLD
        else -> font(context, R.font.be_vietnam_pro_semibold)
    }

    private fun font(context: Context, id: Int): Typeface =
        runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: Typeface.SERIF
}

/**
 * Ghi tấm thẻ ra file tạm rồi mở bảng chia sẻ của hệ thống.
 *
 * Luôn ghi đè MỘT file duy nhất chứ không đặt tên theo thời gian: mỗi lần chia
 * sẻ mà sinh một file mới thì thư mục tạm phình mãi, và không có ai dọn.
 */
fun guiTheLoi(context: android.content.Context, anh: android.graphics.Bitmap, tenBai: String) {
    val thuMuc = java.io.File(context.cacheDir, "the-loi").apply { mkdirs() }
    val file = java.io.File(thuMuc, "lyra-loi.png")
    java.io.FileOutputStream(file).use {
        anh.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
    }
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.chiase", file
    )
    val gui = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        putExtra(android.content.Intent.EXTRA_TITLE, tenBai)
        // `clipData` KHÔNG thừa dù đã có `EXTRA_STREAM`.
        //
        // `FLAG_GRANT_READ_URI_PERMISSION` chỉ trao quyền cho app ĐƯỢC CHỌN, mà
        // bảng chọn của hệ thống là một tiến trình khác và nó cần đọc file để vẽ
        // ảnh xem trước. Không có `clipData` thì bảng chia sẻ hiện ra không có
        // tấm ảnh nào — trông như app gửi đi một file rỗng. Trong logcat là
        // "Permission Denial: opening provider ... from com.android.intentresolver".
        clipData = android.content.ClipData.newUri(context.contentResolver, tenBai, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(gui, null))
}
