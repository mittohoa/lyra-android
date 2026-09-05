package com.mittohoa.lyra.ui

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.ThuMucNhac
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.sources.ThuVienNgoai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Trỏ AURA vào một thư mục nhạc.
 *
 * Màn hình này tồn tại vì một câu hỏi rất hay gặp: "tôi tải nhạc về rồi mà app
 * không thấy". Có đúng hai nguyên nhân, và chúng cần hai câu trả lời trái
 * ngược nhau — nên chỗ này phải nói thẳng cả hai, thay vì để người dùng thử đi
 * thử lại một thứ không bao giờ chạy.
 *
 *   - Tệp nằm trong bộ nhớ chung mà danh mục hệ thống bỏ sót → chọn thư mục ở
 *     đây là xong.
 *   - Tệp nằm trong bộ nhớ riêng của app đã tải nó → không app nào chạm tới
 *     được, kể cả AURA, kể cả khi đã cấp mọi quyền. Bộ chọn thư mục của Android
 *     cũng sẽ TỪ CHỐI cho chọn `Android/data`. Nói thật chuyện này còn hơn để
 *     họ tưởng mình cấu hình sai.
 */
@Composable
internal fun ThuMucNhacMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val kho = remember { ThuMucNhac(context) }
    var cacThuMuc by remember { mutableStateOf(kho.danhSach()) }
    var soBai by remember { mutableIntStateOf(0) }
    var dangQuet by remember { mutableStateOf(false) }
    var bao by remember { mutableStateOf<String?>(null) }

    /** Quét lại và nạp lại thư viện. Gọi sau mỗi lần thêm hoặc bỏ thư mục. */
    fun quetLai() {
        cacThuMuc = kho.danhSach()
        if (cacThuMuc.isEmpty()) {
            soBai = 0
            Lyra.loadLibrary(context)
            return
        }
        dangQuet = true
        scope.launch {
            soBai = withContext(Dispatchers.IO) { ThuVienNgoai.tatCa(context).size }
            dangQuet = false
            // Nạp lại thư viện để bài mới hiện ra ngay, không đợi mở lại app.
            Lyra.loadLibrary(context)
        }
    }

    LaunchedEffect(Unit) { if (cacThuMuc.isNotEmpty()) quetLai() }

    val chon = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        bao = if (kho.them(uri)) null
        else "Máy không cho giữ quyền đọc thư mục này. Thử chọn một thư mục " +
            "trong bộ nhớ trong thay vì trong ứng dụng tệp của bên thứ ba."
        quetLai()
    }

    Column {
        Text(
            when {
                cacThuMuc.isEmpty() -> "Chưa trỏ vào thư mục nào. AURA đang chỉ đọc " +
                    "danh mục nhạc của hệ thống."
                dangQuet -> "Đang quét…"
                soBai == 0 -> "Không tìm thấy tệp nhạc nào trong thư mục đã chọn."
                else -> "Tìm thấy $soBai bài trong thư mục bạn đã trỏ."
            },
            color = mau.chuMo,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        bao?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = mau.chu, fontSize = 13.sp, lineHeight = 19.sp)
        }

        if (cacThuMuc.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            for (uri in cacThuMuc) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(mau.nenChim)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tenDeDoc(uri),
                        color = mau.chu,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Bỏ",
                        color = accent,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                bao = null
                                kho.bo(uri)
                                quetLai()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Dùng khi bạn thấy tệp nhạc trong trình quản lý tệp mà AURA thì " +
                "không thấy. Thường là do thư mục có tệp .nomedia, hoặc máy " +
                "chưa quét lại sau khi bạn chép nhạc sang.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        Nut(nhan = "Chọn thư mục nhạc", accent = accent, modifier = Modifier.fillMaxWidth()) {
            bao = null
            chon.launch(null)
        }

        Spacer(Modifier.height(14.dp))
        // Đây là đoạn quan trọng nhất của cả màn hình. Nhạc tải trong Spotify,
        // YouTube Music, và bản tải offline của nhiều app khác nằm trong bộ nhớ
        // riêng của chính app đó. Không có cách nào - không phải AURA thiếu
        // quyền, mà Android không mở cửa đó cho bất kỳ app nào.
        Text(
            "Nhạc tải trong Spotify hay YouTube Music thì cách này không giúp " +
                "được. Bản offline của các app đó nằm trong bộ nhớ riêng của " +
                "chúng (Android/data), đã mã hoá, và từ Android 11 thì không " +
                "app nào đọc được vùng đó — bộ chọn thư mục cũng sẽ không cho " +
                "bạn chọn vào đấy. Nhạc tải ở Zing MP3 và NhacCuaTui thì tuỳ " +
                "phiên bản: nếu app lưu ra thư mục dùng chung thì chọn nó ở " +
                "đây là AURA đọc được, còn nếu lưu vào bộ nhớ riêng thì cũng " +
                "chịu như trên.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

/**
 * Tên thư mục cho người đọc, không phải địa chỉ máy đọc.
 *
 * Mã tài liệu có dạng `primary:Music/Zing`. Bỏ phần ổ đĩa đi và thay dấu hai
 * chấm bằng dấu gạch chéo là ra đúng đường dẫn người dùng thấy trong trình
 * quản lý tệp. Không đoán được thì trả về nguyên địa chỉ, còn hơn trả về rỗng.
 */
private fun tenDeDoc(uri: Uri): String = try {
    val id = DocumentsContract.getTreeDocumentId(uri)
    val duong = id.substringAfter(':', id)
    if (duong.isBlank()) id else duong
} catch (e: IllegalArgumentException) {
    uri.toString()
}

@Composable
private fun Nut(
    nhan: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onBam: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent)
            .clickable(onClick = onBam)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            nhan,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
