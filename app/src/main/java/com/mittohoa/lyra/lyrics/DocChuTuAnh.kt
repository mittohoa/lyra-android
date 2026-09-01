package com.mittohoa.lyra.lyrics

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Đọc lời bài hát ra khỏi một tấm ảnh.
 *
 * Lời chữ trơn ngoài kia phần nhiều nằm trong ảnh chụp màn hình — người ta chụp
 * lại từ một app khác, một trang web, hay một tấm hình bìa đĩa. Gõ tay lại một
 * bài bốn mươi câu là việc không ai làm; đọc từ ảnh thì mất vài giây, rồi đưa
 * thẳng sang công cụ căn giờ.
 *
 * Dùng bản NHÚNG SẴN mô hình chữ Latin chứ không phải bản tải qua Play Services.
 * Nặng hơn, nhưng máy không có Play Services vẫn dùng được, và không bắt người
 * dùng đợi tải mô hình ngay lần đầu — với một app mà điểm bán là chạy được lúc
 * mất mạng thì đó là đánh đổi đúng chiều.
 *
 * Mô hình chữ Latin đọc được dấu tiếng Việt.
 */
object DocChuTuAnh {

    /**
     * Trả về chữ đọc được, hoặc `null` khi không đọc nổi.
     *
     * Chuỗi trả về đã xuống dòng theo đúng thứ tự đọc — xem `sapTheoDongDoc`.
     */
    suspend fun doc(context: Context, anh: Uri): String? = try {
        val img = InputImage.fromFilePath(context, anh)
        val may = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val ketQua = suspendCancellableCoroutine { tiep ->
            may.process(img)
                .addOnSuccessListener { tiep.resume(it) }
                .addOnFailureListener {
                    Log.i(TAG, "khong doc duoc anh", it)
                    tiep.resume(null)
                }
            tiep.invokeOnCancellation { may.close() }
        }
        ketQua?.let { sapTheoDongDoc(it) }?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.i(TAG, "khong mo duoc anh", e)
        null
    }

    /**
     * Xếp các dòng theo thứ tự ĐỌC, không theo thứ tự khối mà ML Kit trả về.
     *
     * ML Kit gom chữ thành "khối" theo khoảng cách hình học, và thứ tự khối
     * không hứa hẹn gì về thứ tự đọc. Với một tấm ảnh chụp lời bài hát — chữ
     * xếp thành một cột dài — nối đuôi các khối lại rất dễ cho ra lời đảo đoạn.
     *
     * Sắp theo mép trên của từng dòng thì đúng cho mọi bố cục một cột, và đó là
     * bố cục của gần như mọi tấm ảnh chụp lời.
     */
    private fun sapTheoDongDoc(kq: com.google.mlkit.vision.text.Text): String =
        kq.textBlocks
            .flatMap { it.lines }
            .sortedBy { it.boundingBox?.top ?: 0 }
            .joinToString("\n") { it.text.trim() }
            .lines()
            .map { it.trim() }
            .filter { giuLai(it) }
            .joinToString("\n")

    /**
     * Bo nhung dong ro rang khong phai loi bai hat.
     *
     * Phan lon nguoi ta chup CA MAN HINH chu khong cat rieng phan loi, nen
     * anh mang theo dong ho, ten nut, so phut giay, huy hieu thong bao. Doc
     * het roi de nguyen thi nguoi dung phai ngoi xoa tung dong - va moi dong
     * rac con ton them mot cu cham nua o buoc can gio.
     *
     * Chi loc theo HINH DANG, khong doan theo noi dung: khong co danh sach tu
     * cam nao ca. Mot cau hat ngan that su van co the bi vut nham, nhung "Oh"
     * hay "Yeah" thi go lai trong hai giay, con doan theo tu thi sai kieu
     * khong ai luong truoc duoc.
     */
    private fun giuLai(dong: String): Boolean {
        if (dong.isBlank()) return false
        // "4:06", "2:07 Co" - dong ho va so thoi luong
        if (dong.matches(DONG_HO)) return false
        // Qua ngan: "AB", "Loi", "Bia", "Gop" deu la nhan nut
        if (dong.length <= 4) return false
        // Phan lon khong phai chu cai: ":!! 100+", huy hieu, bieu tuong
        return dong.count { it.isLetter() } * 2 >= dong.length
    }

    private val DONG_HO = Regex("""\d{1,2}:\d{2}(\s.{0,3})?""")

    private const val TAG = "LyraDocAnh"
}
