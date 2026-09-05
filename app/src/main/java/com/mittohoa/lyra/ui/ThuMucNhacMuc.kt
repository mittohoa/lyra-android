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
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mittohoa.lyra.data.ThuMucNhac
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.sources.MediaKind
import kotlinx.coroutines.launch

/**
 * Chỉ ra cho AURA những thư mục nó được phép đọc.
 *
 * Đây là CỬA DUY NHẤT để nhạc trong máy vào được AURA. Chưa chọn gì thì thư
 * viện rỗng — không phải "ít bài", mà là không đọc gì cả. Màn hình này vì thế
 * không phải một tuỳ chọn nâng cao, nó là bước đầu tiên.
 *
 * Đổi lại người dùng được hai thứ:
 *
 *   - **Biết chính xác app nhìn thấy gì.** Thứ họ trao là một thư mục cụ thể
 *     chứ không phải cả bộ sưu tập phương tiện sau một ô quyền bấm cho xong.
 *   - **Thấy được nhiều hơn.** Thư mục đã chỉ thì đọc thẳng, không đợi danh
 *     mục hệ thống, nên với tới cả tệp danh mục bỏ sót: thư mục có `.nomedia`,
 *     đuôi lạ, hoặc nhạc vừa chép sang mà máy chưa quét lại.
 *
 * Cùng một danh sách thư mục lo cả nhạc lẫn video. Không tách hai danh sách:
 * bộ quét phân loại theo NỘI DUNG THẬT của tệp chứ không theo đuôi, nên một
 * thư mục lẫn lộn vẫn ra đúng — nhạc vào mục nhạc, phim vào mục phim. Bắt người
 * dùng khai hai lần cho cùng một thư mục là bắt họ trả lời một câu mà máy tự
 * biết.
 *
 * Còn một chuyện màn hình này phải nói thẳng, vì không nói thì người dùng thử
 * mãi một thứ không bao giờ chạy: nhạc tải trong Spotify hay YouTube Music nằm
 * trong bộ nhớ riêng của chính app đó, và từ Android 11 thì không app nào đọc
 * được vùng ấy — bộ chọn thư mục cũng sẽ không cho chọn vào đấy.
 */
@Composable
internal fun ThuMucNhacMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val kho = remember { ThuMucNhac(context) }
    var cacThuMuc by remember { mutableStateOf(kho.danhSach()) }
    var dangQuet by remember { mutableStateOf(false) }
    var bao by remember { mutableStateOf<String?>(null) }

    // Đếm từ chính thư viện đang dùng, không tự quét lấy một lần nữa: con số
    // người dùng cần thấy là "tôi sẽ thấy bao nhiêu bài", mà đó đúng là thư
    // viện. Tự quét riêng vừa tốn hai lần vừa có thể ra số khác thư viện thật.
    val thuVien by Lyra.library.collectAsStateWithLifecycle()
    val soNhac = thuVien.count { it.kind == MediaKind.AUDIO }
    val soVideo = thuVien.count { it.kind == MediaKind.VIDEO }

    /** Nạp lại thư viện theo phạm vi mới. Gọi sau mỗi lần thêm hoặc bỏ. */
    fun napLai() {
        cacThuMuc = kho.danhSach()
        dangQuet = true
        scope.launch {
            Lyra.napThuVien(context)
            dangQuet = false
        }
    }

    val chon = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        bao = if (kho.them(uri)) null
        else "Máy không cho giữ quyền đọc thư mục này. Thử chọn một thư mục " +
            "trong bộ nhớ trong thay vì trong ứng dụng tệp của bên thứ ba."
        napLai()
    }

    Column {
        Text(
            when {
                dangQuet -> "Đang quét…"
                cacThuMuc.isEmpty() ->
                    "Chưa chọn thư mục nào, nên AURA chưa đọc gì trên máy bạn."
                soNhac == 0 && soVideo == 0 ->
                    "Không tìm thấy nhạc hay video nào trong thư mục đã chọn."
                else ->
                    "Chỉ đọc trong ${cacThuMuc.size} thư mục dưới đây: " +
                        "$soNhac bài hát, $soVideo video."
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
                                napLai()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (cacThuMuc.isEmpty())
                "AURA không tự quét máy bạn. Nó chỉ đọc trong thư mục bạn chỉ " +
                    "đích danh ở đây — và đọc thẳng, nên thấy được cả những tệp " +
                    "danh mục hệ thống bỏ sót: thư mục có .nomedia, đuôi tệp lạ, " +
                    "hoặc nhạc vừa chép sang mà máy chưa quét lại."
            else
                "Bỏ hết thư mục thì AURA không còn đọc gì trên máy nữa.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        Nut(nhan = "Thêm thư mục", accent = accent, modifier = Modifier.fillMaxWidth()) {
            bao = null
            chon.launch(null)
        }

        Spacer(Modifier.height(14.dp))
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
 * Mã tài liệu có dạng `primary:Music/Zing`. Bỏ phần ổ đĩa đi và giữ lại phần
 * đường dẫn là ra đúng thứ người dùng thấy trong trình quản lý tệp. Không đoán
 * được thì trả về nguyên địa chỉ, còn hơn trả về rỗng.
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
