package com.mittohoa.lyra.lyrics

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Lời nằm ngay cạnh tệp nhạc.
 *
 * Ai để nhạc trong máy lâu năm thì gần như chắc chắn có sẵn một mớ `.lrc` nằm
 * cạnh từng tệp — đó là cách cả thế giới lưu lời bài hát suốt hai chục năm nay,
 * và mọi trình phát trên máy tính đều đọc kiểu đó. Không đọc thì Lyra bắt người
 * ta gõ lại thứ họ đã có sẵn trên máy, hoặc tệ hơn, đi tra mạng để lấy về đúng
 * cái đang nằm trong cùng thư mục.
 *
 * Đây là NGUỒN ĐÁNG TIN gần bằng lời tự nhập: người dùng tự để tệp đó ở đấy.
 * Nên nó đứng trên bộ nhớ đệm và trên mọi nguồn mạng, chỉ dưới lời tự nhập.
 *
 * ĐỌC THẲNG BẰNG `File`, KHÔNG QUA MediaStore. Đo trên Android 17 (Pixel 6 Pro,
 * chỉ có quyền `READ_MEDIA_AUDIO`):
 *
 *     MediaStore biết bao nhiêu tệp .lrc      0
 *     đọc thẳng /storage/emulated/0/Music/    được
 *
 * MediaStore chỉ đánh chỉ mục tệp phương tiện; `.lrc` là tệp chữ nên nó không
 * bao giờ thấy. Không có đường nào khác ngoài đọc thẳng — trừ khi bắt người
 * dùng chỉ thư mục qua bộ chọn, việc mà họ không hiểu tại sao phải làm khi
 * "app đã thấy bài hát rồi mà".
 *
 * Đọc hụt thì im lặng trả `null`: không có tệp `.lrc` là chuyện thường, và
 * chuyện thường thì không được phép làm phiền ai.
 */
object LrcCanhTep {

    /**
     * Tìm lời cho tệp phương tiện đang phát, theo `uri` của nó.
     *
     * Nhận cả `content://media/...` lẫn `file://`. Trả về chuỗi thô đúng như
     * trong tệp — việc đọc thành dòng là của `parseLrc`, chạy lại mỗi lần.
     */
    fun doc(context: Context, uri: String): String? {
        val duong = duongDan(context, uri) ?: return null
        return docCanh(duong)
    }

    /**
     * Đường dẫn thật của một tệp phương tiện, hoặc `null` khi hỏi không ra.
     *
     * Hỏi bảng `Files` chứ không hỏi riêng bảng `Audio`: bảng này phủ cả nhạc
     * lẫn video, nên khi Lyra phát được video thì chỗ này không phải sửa lại.
     */
    private fun duongDan(context: Context, uri: String): String? = try {
        val u = Uri.parse(uri)
        when {
            u.scheme == "file" -> u.path

            // Hang doi cua Lyra mang dia chi rieng `lyra://<nguon>/<ma>` chu
            // khong mang duong that - xem `StreamResolver`. Voi nguon "may"
            // thi <ma> chinh la ma MediaStore.
            u.scheme == "lyra" && u.host == NGUON_TRONG_MAY ->
                u.lastPathSegment?.toLongOrNull()?.let { hoiDuong(context, it) }

            u.scheme == "content" -> hoiDuong(context, ContentUris.parseId(u))

            // Nguon mang: khong co tep nao tren dia de tim canh ca.
            else -> null
        }
    } catch (e: Exception) {
        Log.d(TAG, "khong hoi duoc duong dan", e)
        null
    }

    /**
     * Hoi bang `Files` chu khong hoi rieng bang `Audio`: bang nay phu ca nhac
     * lan video, nen khi Lyra phat duoc video thi cho nay khong phai sua lai.
     */
    private fun hoiDuong(context: Context, id: Long): String? =
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.Files.FileColumns.DATA),
            "${MediaStore.Files.FileColumns._ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }

    /**
     * Đọc tệp lời nằm cạnh một tệp phương tiện.
     *
     * Thử hai kiểu tên, vì hai kiểu này đều đang tồn tại ngoài đời:
     *
     *     bài hát.mp3  ->  bài hát.lrc       (thay đuôi - phổ biến nhất)
     *     bài hát.mp3  ->  bài hát.mp3.lrc   (nối thêm đuôi)
     *
     * và với mỗi kiểu thì thử cả `.lrc` lẫn `.LRC`: Windows không phân biệt hoa
     * thường nên tệp chép từ máy tính sang rất hay mang đuôi viết hoa, còn
     * Android thì phân biệt.
     */
    fun docCanh(duongNhac: String): String? {
        val khongDuoi = duongNhac.substringBeforeLast('.')
        for (ten in listOf(
            "$khongDuoi.lrc", "$khongDuoi.LRC",
            "$duongNhac.lrc", "$duongNhac.LRC"
        )) {
            val chu = try {
                File(ten).takeIf { it.isFile }?.readText()
            } catch (e: Exception) {
                // Không đọc được là chuyện bình thường trên Android hiện đại:
                // tệp nằm ngoài vùng app được phép đọc. Ghi lại một dòng để còn
                // lần ra khi có người báo "máy tôi không thấy lời", rồi đi tiếp.
                Log.d(TAG, "khong doc duoc $ten", e)
                null
            }
            if (chu != null && chu.isNotBlank()) {
                Log.i(TAG, "doc duoc loi canh tep: $ten")
                return chu
            }
        }
        return null
    }

    /** Khop voi `MusicSource.LOCAL.key`. */
    private const val NGUON_TRONG_MAY = "may"

    private const val TAG = "LyraLrcCanhTep"
}
