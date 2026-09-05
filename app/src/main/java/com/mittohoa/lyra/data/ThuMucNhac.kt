package com.mittohoa.lyra.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Những thư mục người dùng đã chỉ đích danh cho AURA đọc.
 *
 * ĐÂY LÀ TOÀN BỘ PHẠM VI. Danh sách này rỗng thì AURA không đọc một tệp nào
 * trên máy — không phải "đọc ít đi", mà là không đọc gì cả.
 *
 * Khác hẳn cách thông thường, và là có chủ ý. Mặc định của gần như mọi trình
 * phát là quét sạch máy rồi bày ra, và người dùng chỉ được hỏi đúng một câu
 * "cho phép đọc nhạc?" — bấm Đồng ý là trao cả bộ sưu tập phương tiện. Ở đây
 * thứ họ trao là một thư mục CỤ THỂ, chọn bằng tay, thu lại được bất cứ lúc
 * nào ngay tại màn hình Cài đặt.
 *
 * Đi kèm một cái lợi không nhỏ: thư mục đã chỉ thì đọc THẲNG, không qua danh
 * mục hệ thống, nên với tới được cả những tệp danh mục bỏ sót — thư mục có
 * `.nomedia`, đuôi lạ, hoặc tệp vừa chép sang mà máy chưa quét lại. Người dùng
 * nhìn thấy tệp trong trình quản lý tệp mà app không thấy là chuyện không giải
 * thích cho xuôi được.
 *
 * KHÔNG xin thêm quyền nào. `ACTION_OPEN_DOCUMENT_TREE` là người dùng tự chọn
 * và tự trao, mỗi thư mục một lần.
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
     * Trần số bài cho một lần quét thư mục.
     *
     * Phải có trần: người dùng hoàn toàn có thể trỏ vào gốc thẻ nhớ, và đọc thẻ
     * từng tệp trên vài chục nghìn tệp là hàng phút đứng hình.
     *
     * Nhưng trần phải **đổi được**. Người có thư viện lớn hơn trần mà không có
     * cách nâng thì mất bài vĩnh viễn, và mất một cách im lặng — không lỗi,
     * không danh sách rỗng, chỉ là vài album biến mất.
     */
    fun tranSoBai(): Int = prefs.getInt(KEY_TRAN, TRAN_MAC_DINH)

    /** Nâng trần thêm một bậc. Trả về trần mới. */
    fun nangTran(): Int {
        val moi = tranSoBai() + TRAN_MAC_DINH
        prefs.edit().putInt(KEY_TRAN, moi).apply()
        return moi
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
        const val KEY_TRAN = "tran-so-bai"

        /** Cũng là bước nâng mỗi lần bấm — xem `nangTran`. */
        const val TRAN_MAC_DINH = 2000
        const val TAG = "AuraThuMuc"
    }
}
