package com.mittohoa.lyra.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.mittohoa.lyra.data.ThuMucNhac
import com.mittohoa.lyra.service.Lyra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mở bộ chọn thư mục của hệ thống, nhận kết quả, nạp lại thư viện.
 *
 * Nằm riêng vì có HAI chỗ cần đúng việc này: mục Thư mục quét trong Cài đặt, và
 * tấm thẻ mời ở màn hình thư viện rỗng. Hai bản chép tay thì sớm muộn lệch nhau
 * — một bên nhớ nạp lại thư viện, bên kia quên, và lỗi ấy trông y như "chọn
 * thư mục xong chẳng thấy gì".
 *
 * Trả về một hàm để bấm. Bên gọi tự lo phần giao diện của mình; chỗ này chỉ lo
 * việc: xin quyền lâu dài, ghi vào danh sách, rồi nạp lại thư viện theo phạm vi
 * mới.
 *
 * `null` từ bộ chọn nghĩa là người dùng bấm quay lại — không phải lỗi, và
 * không đụng gì tới danh sách đang có.
 */
@Composable
internal fun nhoBoChonThuMuc(
    /** Gọi ngay khi người dùng vừa chọn xong, trước lúc quét. */
    onBatDau: () -> Unit = {},
    /**
     * Gọi khi đã nạp lại thư viện xong.
     *
     * `nhanDuoc = false` nghĩa là máy từ chối trao quyền lâu dài — hiếm, nhưng
     * có thật với vài trình cung cấp tệp của bên thứ ba.
     */
    onXong: (nhanDuoc: Boolean) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kho = remember { ThuMucNhac(context) }

    val boChon = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        onBatDau()
        scope.launch {
            // `them` chạm đĩa và gọi qua binder. Nhỏ, nhưng nó nằm ngay trên
            // đường người dùng vừa bấm xong — đẩy sang luồng nền thì khung hình
            // kế tiếp không phải đợi nó.
            val nhan = withContext(Dispatchers.IO) { kho.them(uri) }
            Lyra.napThuVien(context)
            onXong(nhan)
        }
    }

    // `null` = mở ở chỗ hệ thống tự chọn. Không gợi ý sẵn một thư mục nào: thứ
    // ta đoán gần như luôn sai, và nó chỉ tổ đẩy người dùng đi lạc một nhánh.
    return { boChon.launch(null) }
}
