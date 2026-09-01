package com.mittohoa.lyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mittohoa.lyra.R
import com.mittohoa.lyra.data.KieuChu

/*
 * Bộ chữ và bảng màu của Lyra.
 *
 * Lyra không phát nhạc thay người dùng chọn — nó ĐỌC nhạc đang phát ở chỗ khác
 * và biến nó thành chữ. Thứ nó bán là con chữ, không phải ảnh bìa. Nên nó được
 * dựng như một trang in chứ không như một cái máy phát: nền giấy, chữ mực, một
 * lề màu ở mép trái, và ảnh bìa nằm như một bản in dán vào trang — có viền, có
 * chú thích — chứ không phải một ô vuông bo tròn phát sáng giữa màn hình.
 *
 * Lý do không phải để cho khác người. Mọi app nhạc hôm nay đều là nền tối +
 * gradient lấy từ ảnh bìa + phông mặc định của máy, nên mở app nào cũng thấy
 * như nhau. Một app mà nhân vật chính là chữ thì đi hướng ngược lại vừa đúng
 * chức năng vừa nhận ra được từ xa.
 */

/**
 * Hai bộ chữ, và lý do dùng hai bộ.
 *
 * Sách và báo tách đôi: một bộ có chân cho phần ĐỌC, một bộ không chân cho phần
 * ĐIỀU HƯỚNG. Mắt đọc một đoạn dài bằng bộ có chân đỡ mỏi hơn, còn nhãn nút thì
 * cần gọn và rõ ở cỡ nhỏ. Lyra có đúng hai loại chữ đó — lời bài hát, và mọi
 * thứ còn lại — nên tách đôi là tách đúng chỗ.
 *
 * Cả hai đều giấy phép OFL, nhúng thẳng vào APK chứ không tải về khi chạy: một
 * app có thể mở lúc mất mạng, và bộ mặt của nó không nên phụ thuộc vào sóng.
 * Cả hai đã kiểm đủ 134 chữ cái tiếng Việt có dấu.
 */
object BoChu {

    /**
     * Be Vietnam Pro — cho giao diện.
     *
     * Do người Việt vẽ, và dấu tiếng Việt được vẽ chứ không phải ghép máy móc
     * từ một bộ Latin có sẵn. Với một app mà chữ hiển thị hầu hết là tiếng Việt
     * có dấu, đó không phải chi tiết trang trí: dấu đặt sai chỗ ở cỡ 12sp là
     * thứ nhìn thấy ngay.
     */
    val Sans = FontFamily(
        Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
        Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold)
    )

    /**
     * Newsreader — cho lời bài hát và tên bài.
     *
     * Một bộ chữ biến thiên: một file duy nhất chứa mọi độ đậm, nên rẻ hơn hẳn
     * so với nhúng ba bốn file tĩnh. Trục `wght` được đặt tay ở dưới.
     *
     * Chọn bộ có chân vì lời bài hát là thứ để ĐỌC. Và vì không app nhạc nào
     * làm thế — đó là nửa còn lại của lý do.
     */
    @OptIn(ExperimentalTextApi::class)
    val Serif = FontFamily(
        Font(R.font.newsreader, FontWeight.Normal, variationSettings = nang(400)),
        Font(R.font.newsreader, FontWeight.Medium, variationSettings = nang(500)),
        Font(R.font.newsreader, FontWeight.SemiBold, variationSettings = nang(600)),
        Font(R.font.newsreader, FontWeight.Bold, variationSettings = nang(700))
    )

    @OptIn(ExperimentalTextApi::class)
    private fun nang(w: Int) = FontVariation.Settings(FontVariation.weight(w))
}

/**
 * Bảng màu một mặt giấy.
 *
 * Chỉ có sáu ô, và đó là cố ý. Một bảng màu ba chục ô thì không ai nhớ nổi ô
 * nào dùng ở đâu, và kết quả là mỗi màn hình một kiểu. Sáu ô thì thuộc, và
 * thuộc thì cả app trông như một thứ.
 */
@Immutable
data class BangMau(
    /** Mặt giấy. */
    val nen: Color,
    /** Ô lõm trên giấy: thẻ, ô nhập, hàng danh sách được chọn. */
    val nenChim: Color,
    /** Mực chính — chữ để đọc. */
    val chu: Color,
    /** Mực nhạt — nhãn, chú thích, dòng lời chưa tới. */
    val chuMo: Color,
    /** Mực rất nhạt — chữ chờ trong ô nhập, dòng lời ở xa. */
    val chuRatMo: Color,
    /** Đường kẻ, viền, gạch ngang. */
    val vien: Color,
    /** `true` khi đang là mặt giấy sáng. */
    val laGiay: Boolean
) {
    /**
     * Màu lấy từ ảnh bìa, chỉnh lại cho hợp mặt giấy đang dùng.
     *
     * Ảnh bìa cho ra màu tuỳ ý — một bìa album vàng chanh trên nền giấy ngà thì
     * không đọc được chữ nào. Trên giấy thì dìm màu xuống thành MỰC MÀU: giữ
     * nguyên sắc, hạ độ sáng. Trên nền mực thì giữ nguyên, vì ở đó màu sáng mới
     * là thứ nổi lên.
     *
     * Hạ sáng theo HSL chứ KHÔNG trộn với màu đen.
     *
     * Trộn tím với gần-đen theo RGB thì ra bùn: cả ba kênh cùng bị kéo về gần
     * nhau, mà ba kênh gần nhau nghĩa là xám. Bản đầu làm đúng kiểu đó và mọi
     * nút trên trang Chỉnh biến thành nâu xám — nhìn như app hỏng màu chứ không
     * như mực màu. Đổi sang HSL thì sắc và độ tươi giữ nguyên, chỉ có độ sáng
     * tụt xuống: đúng nghĩa "cùng màu ấy, nhưng là mực".
     *
     * Còn nâng độ tươi lên một chút, vì mắt đọc màu tối là kém tươi hơn màu
     * sáng cùng độ tươi — không bù thì mực trông nhợt.
     */
    fun mucMau(accent: Color): Color {
        if (!laGiay) return accent
        val (h, s, l) = tachHsl(accent)
        if (s < 0.06f) return Color(0xFF3A342B)          // bìa gần như xám: mực nâu
        return tuHsl(h, (s * 1.22f).coerceAtMost(1f), l.coerceAtMost(0.34f))
    }

    companion object {
        /*
         * Hai mặt KHÔNG dùng chung một bộ độ mờ, và đó là chỗ bản đầu làm sai.
         *
         * Mờ đi thì mực đen trên giấy mất tương phản nhanh hơn hẳn chữ sáng
         * trên nền tối — cùng độ mờ 0,56 cho ra 4,08:1 trên giấy nhưng 6,16:1
         * trên mực. Bản đầu chép y nguyên bộ số từ mặt này sang mặt kia, nên
         * mặt giấy có hai bậc chữ đều KHÔNG đạt chuẩn WCAG 4,5:1: chú thích ở
         * 4,08 và chữ mờ nhất ở 2,05. Trên máy thật thì nó hiện ra đúng như
         * con số nói: khó đọc.
         *
         * Các con số dưới đây được đo chứ không ước lượng. Mọi bậc chữ ở cả
         * hai mặt đều đạt 4,5:1 trở lên.
         */

        /** Giấy ngà, mực đen. Hơi ấm chứ không trắng tinh — trắng tinh chói mắt. */
        val Giay = BangMau(
            nen = Color(0xFFFBF6EC),
            nenChim = Color(0xFFF1E9DA),
            chu = Color(0xFF191510),                                  // 16,9:1
            chuMo = Color(0xFF191510).copy(alpha = 0.74f),            //  7,4:1
            chuRatMo = Color(0xFF191510).copy(alpha = 0.60f),         //  4,6:1
            vien = Color(0xFF191510).copy(alpha = 0.22f),
            laGiay = true
        )

        /** Mực đen, chữ trắng ngà. Cùng bộ chữ, cùng bố cục — chỉ lật mặt. */
        val Muc = BangMau(
            nen = Color(0xFF13110D),
            nenChim = Color(0xFF1D1A15),
            chu = Color(0xFFF6F1E6),                                  // 16,7:1
            chuMo = Color(0xFFF6F1E6).copy(alpha = 0.62f),            //  6,9:1
            chuRatMo = Color(0xFFF6F1E6).copy(alpha = 0.50f),         //  4,7:1
            vien = Color(0xFFF6F1E6).copy(alpha = 0.20f),
            laGiay = false
        )
    }
}

/**
 * Mặt giấy đang dùng, đọc được từ bất kỳ chỗ nào trong cây giao diện.
 *
 * Dùng `CompositionLocal` chứ không truyền tham số: bảng màu đi tới từng cái
 * `Text` ở tầng sâu nhất, mà luồn một tham số qua ba chục hàm chỉ để tới đó thì
 * mỗi lần thêm một màn hình lại phải luồn lại một lần nữa.
 */
val LocalBangMau: ProvidableCompositionLocal<BangMau> = compositionLocalOf { BangMau.Giay }

/** Bảng màu đang dùng. Viết tắt cho `LocalBangMau.current`. */
val mau: BangMau
    @Composable get() = LocalBangMau.current

/**
 * Tách một màu ra sắc / độ tươi / độ sáng.
 *
 * Có một bản trong `Artwork.kt` nhưng bản đó nhận ba số nguyên kênh màu và là
 * `private` của việc dò màu ảnh bìa. Ở đây cần bản nhận thẳng một `Color`, nên
 * viết riêng thay vì mở rộng phạm vi của bản kia ra cả gói.
 */
private fun tachHsl(c: Color): Triple<Float, Float, Float> {
    val r = c.red; val g = c.green; val b = c.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
    return Triple(h, s, l)
}

/** Dựng lại màu từ sắc / độ tươi / độ sáng. `h` tính theo vòng 0..1. */
private fun tuHsl(h: Float, s: Float, l: Float): Color {
    if (s <= 0f) return Color(l, l, l)
    val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
    val p = 2 * l - q
    fun kenh(t0: Float): Float {
        var t = t0
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    return Color(kenh(h + 1f / 3f), kenh(h), kenh(h - 1f / 3f))
}

/**
 * Hai bộ chữ đang dùng: một cho giao diện, một cho lời bài hát.
 *
 * `null` nghĩa là để nguyên bộ chữ của máy — đó là một lựa chọn thật, không
 * phải trạng thái thiếu.
 */
@Immutable
data class BoChuDung(
    val giaoDien: FontFamily?,
    val loi: FontFamily?
) {
    companion object {
        fun tu(kieu: KieuChu): BoChuDung = when (kieu) {
            KieuChu.SACH -> BoChuDung(BoChu.Sans, BoChu.Serif)
            KieuChu.MOT_BO -> BoChuDung(BoChu.Sans, BoChu.Sans)
            KieuChu.MAY -> BoChuDung(null, null)
        }
    }
}

val LocalBoChu: ProvidableCompositionLocal<BoChuDung> =
    compositionLocalOf { BoChuDung.tu(KieuChu.SACH) }

/** Bộ chữ đang dùng. Viết tắt cho `LocalBoChu.current`. */
val boChu: BoChuDung
    @Composable get() = LocalBoChu.current
