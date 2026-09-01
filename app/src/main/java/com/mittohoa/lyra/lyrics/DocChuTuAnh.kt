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
 * Dùng bản TẢI QUA PLAY SERVICES chứ không phải bản nhúng sẵn mô hình. Bản nhúng
 * chạy được cả khi máy không có Play Services và không phải đợi tải lần đầu, nghe
 * thì đúng chiều hơn — nhưng đo ra thì nó thêm 12 MB cho bản arm64 và 40 MB cho
 * bản universal, tức tăng hơn nửa dung lượng app. Bản universal lại đúng là bản
 * dành cho máy cũ, nhóm máy chật bộ nhớ nhất. Đổi 40 MB lấy một tính năng phần
 * lớn người dùng chạy một lần rồi thôi là đổi hớ.
 *
 * Cái giá phải trả: máy phải có Play Services, và lần đọc đầu tiên phải đợi tải
 * mô hình. Phần dịch trong app cũng đã tải gói ngôn ngữ khi chạy rồi.
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
     * Bỏ những dòng rõ ràng không phải lời bài hát.
     *
     * Phần lớn người ta chụp CẢ MÀN HÌNH chứ không cắt riêng phần lời, nên ảnh
     * mang theo đồng hồ, tên nút, số phút giây, huy hiệu thông báo. Đọc hết rồi
     * để nguyên thì người dùng phải ngồi xoá từng dòng — và mỗi dòng rác còn tốn
     * thêm một cú chạm nữa ở bước căn giờ.
     *
     * Chỉ lọc theo HÌNH DÁNG, không đoán theo nội dung: không có danh sách từ
     * cấm nào cả. Một câu hát ngắn thật sự vẫn có thể bị vứt nhầm, nhưng "Oh"
     * hay "Yeah" thì gõ lại trong hai giây, còn đoán theo từ thì sai kiểu không
     * ai lường trước được.
     */
    private fun giuLai(dong: String): Boolean {
        if (dong.isBlank()) return false
        // "4:06", "2:07 Có" — đồng hồ và số thời lượng
        if (dong.matches(DONG_HO)) return false
        // Quá ngắn: "AB", "Lời", "Bìa", "Góp" đều là nhãn nút
        if (dong.length <= 4) return false
        // Phần lớn không phải chữ cái: ":!! 100+", huy hiệu, biểu tượng
        return dong.count { it.isLetter() } * 2 >= dong.length
    }

    private val DONG_HO = Regex("""\d{1,2}:\d{2}(\s.{0,3})?""")

    private const val TAG = "LyraDocAnh"
}
