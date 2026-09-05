package com.mittohoa.lyra.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Những thư mục người dùng đã tự trỏ AURA vào.
 *
 * VÌ SAO CẦN, khi đã đọc `MediaStore` rồi: MediaStore chỉ chứa thứ nó đã đánh
 * chỉ mục. Ba trường hợp rơi ra ngoài, và cả ba đều là nhạc thật của người dùng:
 *
 *   - Thư mục có tệp `.nomedia` — máy quét bỏ qua cả thư mục. Nhiều app tải
 *     nhạc đặt sẵn tệp này để nhạc của họ không lẫn vào thư viện chung.
 *   - Đuôi tệp lạ hoặc kiểu MIME máy không nhận, nên không vào bảng audio.
 *   - Tệp vừa chép sang qua cáp mà máy chưa quét lại.
 *
 * Người dùng nhìn thấy tệp trong trình quản lý tệp nhưng AURA thì không, và
 * không có cách nào giải thích chuyện đó cho xuôi. Trỏ thẳng vào thư mục là
 * đường vòng qua cả bộ máy đánh chỉ mục.
 *
 * KHÔNG xin thêm quyền nào. `ACTION_OPEN_DOCUMENT_TREE` là người dùng tự chọn
 * và tự trao, mỗi thư mục một lần, thu lại được bất cứ lúc nào ở đây.
 *
 * Quyền được trao là VĨNH VIỄN (`takePersistableUriPermission`) — không giữ thì
 * khởi động lại máy là mất, và người dùng phải chọn lại thư mục mỗi sáng.
 */
class ThuMucNhac(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("thu-muc-nhac", Context.MODE_PRIVATE)

    /**
     * Các thư mục còn dùng được.
     *
     * Lọc theo quyền hệ thống ĐANG giữ chứ không tin danh sách đã lưu: người
     * dùng có thể gỡ quyền trong Cài đặt, hoặc thẻ nhớ chứa thư mục đã bị rút.
     * Trả về một địa chỉ đã chết thì mọi lần quét sau đều ném lỗi ở chỗ khác.
     */
    fun danhSach(): List<Uri> {
        val daLuu = prefs.getStringSet(KEY, emptySet()).orEmpty()
        if (daLuu.isEmpty()) return emptyList()

        val conQuyen = app.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri.toString() }
            .toSet()

        val song = daLuu.filter { it in conQuyen }
        // Dọn luôn những địa chỉ đã mất quyền, để danh sách trong Cài đặt không
        // bày ra một thư mục bấm vào không có gì.
        if (song.size != daLuu.size) prefs.edit().putStringSet(KEY, song.toSet()).apply()

        return song.map(Uri::parse)
    }

    /**
     * Nhận một thư mục vừa được chọn.
     *
     * Trả về false khi hệ thống từ chối trao quyền lâu dài — hiếm, nhưng có
     * thật với vài trình cung cấp tệp của bên thứ ba. Nuốt lỗi rồi vẫn ghi vào
     * danh sách thì lần quét sau mới hỏng, xa chỗ gây ra.
     */
    fun them(uri: Uri): Boolean {
        return try {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val moi = prefs.getStringSet(KEY, emptySet()).orEmpty() + uri.toString()
            prefs.edit().putStringSet(KEY, moi).apply()
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "He thong khong trao quyen lau dai cho $uri", e)
            false
        }
    }

    /** Bỏ một thư mục, và trả lại quyền cho hệ thống. */
    fun bo(uri: Uri) {
        val moi = prefs.getStringSet(KEY, emptySet()).orEmpty() - uri.toString()
        prefs.edit().putStringSet(KEY, moi).apply()
        try {
            app.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Quyền có thể đã bị gỡ từ phía Cài đặt. Không phải lỗi.
            Log.d(TAG, "Quyen cho $uri da khong con", e)
        }
    }

    private companion object {
        const val KEY = "cac-thu-muc"
        const val TAG = "AuraThuMuc"
    }
}
