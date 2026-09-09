package com.mittohoa.lyra.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.service.Lyra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sao lưu và khôi phục lịch sử nghe.
 *
 * VÌ SAO CẦN, và vì sao nó khác mục lời tự nhập ngay bên trên: lời gõ tay thì
 * mất rồi vẫn gõ lại được — mất công, nhưng làm lại được. Còn "tháng trước tôi
 * nghe những gì" thì không có cách nào dựng lại. Không nhớ thì thôi, và không
 * ai nhớ hộ. Đây là dữ liệu duy nhất trong AURA mà mất là mất hẳn.
 *
 * Dùng BỘ CHỌN TỆP CỦA HỆ THỐNG, cùng lẽ với [SaoLuuLoiMuc]: người dùng chọn
 * chỗ để, AURA không xin thêm quyền nào, và tệp nằm ngoài vùng app nên gỡ app
 * đi nó vẫn còn. Một bản sao lưu bị xoá cùng lúc với thứ nó đang sao lưu thì
 * không phải bản sao lưu.
 *
 * NÓI TRƯỚC LÀ TỆP NÀY RIÊNG TƯ. Nó ghi lại bạn đã nghe gì và lúc nào — thứ
 * riêng tư hơn hẳn mọi tệp khác app này tạo ra. Người dùng phải biết điều đó
 * TRƯỚC KHI bấm lưu, chứ không phải sau khi đã gửi tệp cho ai đó.
 */
@Composable
internal fun SaoLuuLichSuMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current

    var soLan by remember { mutableIntStateOf(0) }
    var bao by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { soLan = withContext(Dispatchers.IO) { Lyra.demLichSu(context) } }

    val ghi = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            bao = withContext(Dispatchers.IO) {
                try {
                    val chu = Lyra.xuatLichSu(context)
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(chu.toByteArray())
                    } ?: error("không mở được tệp")
                    "Đã lưu $soLan lần nghe ra tệp."
                } catch (e: Exception) {
                    "Không ghi được tệp: ${e.message ?: "lỗi không rõ"}"
                }
            }
        }
    }

    val doc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val chu = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().decodeToString()
                    } ?: error("không mở được tệp")
                }
            }.getOrElse {
                bao = "Không đọc được tệp: ${it.message ?: "lỗi không rõ"}"
                return@launch
            }

            val kq = withContext(Dispatchers.IO) { Lyra.nhapLichSu(context, chu) }
            soLan = withContext(Dispatchers.IO) { Lyra.demLichSu(context) }
            bao = when {
                kq.hong > 0 -> "Tệp này không phải bản sao lưu lịch sử của AURA."
                kq.them == 0 && kq.daCo > 0 ->
                    "${kq.daCo} lần nghe trong tệp đều đã có sẵn trên máy."
                kq.daCo > 0 -> "Thêm ${kq.them} lần nghe. ${kq.daCo} lần đã có sẵn."
                else -> "Đã khôi phục ${kq.them} lần nghe."
            }
        }
    }

    Column {
        Text(
            if (soLan == 0) "Chưa ghi lại lần nghe nào."
            else "Đang giữ $soLan lần nghe gần đây.",
            color = mau.chuMo,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        bao?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = mau.chu, fontSize = 13.sp, lineHeight = 19.sp)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Lời bạn gõ tay mất rồi thì gõ lại được. Còn tháng trước bạn nghe " +
                "những gì thì không dựng lại được bằng cách nào — gỡ app hoặc " +
                "đổi máy là mất hẳn.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NutManhSaoLuu(
                nhan = "Sao lưu ra tệp",
                bat = soLan > 0,
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                bao = null
                ghi.launch("aura-nghe-" + homNayNghe() + ".txt")
            }
            NutManhSaoLuu(
                nhan = "Khôi phục",
                bat = true,
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                bao = null
                // Nhieu may gan cho tep .txt kieu MIME khac nhau - xem ghi chu
                // cung cho trong `SaoLuuLoiMuc`.
                doc.launch(arrayOf("text/plain", "text/*", "application/octet-stream"))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Khôi phục là TRỘN, không xoá cái đang có: bài trùng thì giữ lần " +
                "nghe gần nhất. Tệp lưu ra ghi rõ bạn nghe gì lúc nào — cất nó " +
                "như một thứ riêng tư.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

private fun homNayNghe(): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
