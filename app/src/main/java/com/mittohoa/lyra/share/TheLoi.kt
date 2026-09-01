package com.mittohoa.lyra.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
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
 * Tấm thẻ mang đúng bộ mặt của app: nền giấy hoặc nền mực, lề mực màu lấy từ
 * ảnh bìa chạy dọc mép trái, câu hát đặt bằng chữ có chân. Đó là lý do tính
 * năng này chỉ đáng làm SAU khi có bộ mặt riêng — trước đó Lyra không có gì để
 * in lên tấm thẻ cả.
 */
object TheLoi {

    /** 4:5 — tỉ lệ dọc mà mọi chỗ đăng ảnh đều nhận, và đọc chữ thoải mái. */
    const val RONG = 1080
    const val CAO = 1350

    private const val LE = 16f          // bề ngang dải lề mực
    private const val TRAI = 104f       // chữ bắt đầu ở đâu
    private const val PHAI = 88f

    fun ve(
        context: Context,
        cauHat: String,
        tenBai: String,
        caSi: String,
        /** Màu lấy từ ảnh bìa, đã chỉnh cho hợp mặt giấy đang dùng. */
        mauNhan: Int,
        laGiay: Boolean,
        kieuChu: KieuChu
    ): Bitmap {
        val nen = if (laGiay) 0xFFFBF6EC.toInt() else 0xFF13110D.toInt()
        val muc = if (laGiay) 0xFF191510.toInt() else 0xFFF6F1E6.toInt()
        val mucMo = pha(muc, nen, if (laGiay) 0.74f else 0.62f)
        val mucRatMo = pha(muc, nen, if (laGiay) 0.60f else 0.50f)

        val coChan = when (kieuChu) {
            KieuChu.SACH -> font(context, R.font.newsreader)
            KieuChu.MOT_BO -> font(context, R.font.be_vietnam_pro_semibold)
            KieuChu.MAY -> Typeface.SERIF
        }
        val khongChan = when (kieuChu) {
            KieuChu.MAY -> Typeface.DEFAULT_BOLD
            else -> font(context, R.font.be_vietnam_pro_semibold)
        }

        val anh = Bitmap.createBitmap(RONG, CAO, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(anh)
        canvas.drawColor(nen)

        // Lề mực dọc mép trái - dấu hiệu riêng của Lyra, nhạt dần xuống dưới
        // như mực in thấm vào giấy.
        val leSon = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CAO.toFloat(),
                pha(mauNhan, nen, 0.92f), pha(mauNhan, nen, 0.30f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, LE, CAO.toFloat(), leSon)

        val rongChu = RONG - TRAI - PHAI

        // --- Câu hát ---
        //
        // Cỡ chữ co lại theo độ dài câu chứ không cố định: một câu bốn chữ đặt
        // ở cỡ của một câu hai mươi chữ thì lọt thỏm giữa tấm thẻ, còn ngược
        // lại thì tràn ra ngoài.
        var coChu = 88f
        var boCuc: StaticLayout
        val sonChu = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = coChan
            color = muc
        }
        while (true) {
            sonChu.textSize = coChu
            boCuc = dungBoCuc(cauHat, sonChu, rongChu.toInt())
            if (boCuc.height <= 620 || coChu <= 44f) break
            coChu -= 4f
        }

        // Câu hát đặt hơi cao hơn giữa tấm: phần dưới còn phải chứa tên bài,
        // và một khối chữ đặt đúng giữa hình học thì mắt thấy như bị tụt xuống.
        val chuY = (CAO - boCuc.height) / 2f - 90f
        canvas.save()
        canvas.translate(TRAI, chuY)
        boCuc.draw(canvas)
        canvas.restore()

        // --- Nét kẻ màu, rồi tên bài và ca sĩ ---
        var y = chuY + boCuc.height + 78f
        canvas.drawRect(TRAI, y, TRAI + 104f, y + 6f, Paint().apply { color = mauNhan })
        y += 6f + 52f

        val sonTen = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = coChan
            color = mucMo
            textSize = 42f
        }
        val ten = dungBoCuc(tenBai, sonTen, rongChu.toInt(), 2)
        canvas.save()
        canvas.translate(TRAI, y)
        ten.draw(canvas)
        canvas.restore()
        y += ten.height + 14f

        if (caSi.isNotBlank()) {
            val sonCaSi = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = khongChan
                color = mucRatMo
                textSize = 27f
                letterSpacing = 0.14f
            }
            canvas.drawText(caSi.uppercase(), TRAI, y + 27f, sonCaSi)
        }

        // Dấu hiệu ở góc dưới phải, nhỏ và mờ: tấm thẻ là của câu hát, không
        // phải của app. Nhưng phải có, không thì chia sẻ đi mà chẳng ai biết
        // nó từ đâu ra.
        val sonDau = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = khongChan
            color = pha(muc, nen, 0.30f)
            textSize = 24f
            letterSpacing = 0.30f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("LYRA", RONG - PHAI, CAO - 66f, sonDau)

        return anh
    }

    private fun dungBoCuc(chu: String, son: TextPaint, rong: Int, toiDaDong: Int = 8): StaticLayout =
        StaticLayout.Builder.obtain(chu, 0, chu.length, son, rong)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.26f)
            .setIncludePad(false)
            .setMaxLines(toiDaDong)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

    /** Trộn `tren` lên `duoi` với độ đục `a`, trả về một màu ĐẶC. */
    private fun pha(tren: Int, duoi: Int, a: Float): Int = Color.rgb(
        (Color.red(tren) * a + Color.red(duoi) * (1 - a)).toInt(),
        (Color.green(tren) * a + Color.green(duoi) * (1 - a)).toInt(),
        (Color.blue(tren) * a + Color.blue(duoi) * (1 - a)).toInt()
    )

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
