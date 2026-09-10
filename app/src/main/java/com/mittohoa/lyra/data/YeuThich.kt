package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Những bài người dùng đánh dấu yêu thích.
 *
 * VÌ SAO KHÔNG DÙNG DANH SÁCH PHÁT. App đã có danh sách phát, và một danh sách
 * tên "Yêu thích" thì làm được gần hết việc này. Khác nhau ở chỗ số cú chạm:
 * thêm vào danh sách phát là mở hàng đợi, chọn danh sách, xác nhận; còn đánh
 * dấu yêu thích phải là MỘT cú chạm ngay lúc bài đang hát — vì đó đúng là lúc
 * người ta biết mình thích nó. Bắt đi ba bước ở đúng khoảnh khắc ấy thì gần như
 * không ai làm.
 *
 * CHỈ GIỮ ĐỊA CHỈ, không giữ cả bài. Tên bài, ca sĩ, độ dài đều đọc lại được từ
 * thư viện, và chép chúng vào đây nghĩa là sửa thẻ xong thì danh sách yêu thích
 * vẫn hiện tên cũ. Cái giá là bài bị xoá khỏi máy sẽ biến mất khỏi danh sách —
 * đúng, vì nó cũng đã biến mất khỏi máy.
 *
 * Một địa chỉ mỗi dòng, chữ trơn: cả bộ dữ liệu này là một danh sách chuỗi, mà
 * gói một danh sách chuỗi vào JSON thì chỉ được thêm mấy dấu ngoặc.
 *
 * Nằm ở `filesDir`: đây là việc người dùng đã làm, không dựng lại được.
 */
class YeuThich(context: Context) {

    private val file = File(context.filesDir, "yeu-thich.txt")

    private val _bo = MutableStateFlow(doc())
    val bo: StateFlow<Set<String>> = _bo.asStateFlow()

    private fun doc(): Set<String> = try {
        if (!file.exists()) emptySet()
        else file.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toCollection(
            LinkedHashSet()
        )
    } catch (e: Exception) {
        Log.w(TAG, "Khong doc duoc danh sach yeu thich", e)
        emptySet()
    }

    fun co(diaChi: String): Boolean = diaChi.isNotBlank() && diaChi in _bo.value

    /** Bật/tắt một bài. Trả về trạng thái SAU khi đổi. */
    fun doi(diaChi: String): Boolean {
        if (diaChi.isBlank()) return false
        val moi = LinkedHashSet(_bo.value)
        val bat = if (!moi.remove(diaChi)) {
            // Thêm vào ĐẦU: bài vừa đánh dấu là bài người ta muốn thấy trước.
            val gop = LinkedHashSet<String>(moi.size + 1)
            gop.add(diaChi)
            gop.addAll(moi)
            _bo.value = gop
            true
        } else {
            _bo.value = moi
            false
        }
        ghiDia(_bo.value)
        return bat
    }

    private fun ghiDia(bo: Set<String>) {
        try {
            file.writeText(bo.joinToString("\n"))
        } catch (e: Exception) {
            Log.w(TAG, "Khong ghi duoc danh sach yeu thich", e)
        }
    }

    private companion object {
        const val TAG = "AuraYeuThich"
    }
}
