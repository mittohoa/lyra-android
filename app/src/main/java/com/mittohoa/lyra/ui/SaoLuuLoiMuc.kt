package com.mittohoa.lyra.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import com.mittohoa.lyra.service.Lyra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sao luu va khoi phuc loi tu nhap.
 *
 * Go tay mot bai loi mat vai phut, can gio tung cau con lau hon - va tat ca
 * nam trong bo nho rieng cua app. Go app la mat, doi may la mat, va nguoi dung
 * khong co duong nao lay ra. Mot tep sao luu la cach duy nhat de cong do song
 * lau hon lan cai dat nay.
 *
 * Dung BO CHON TEP CUA HE THONG chu khong tu ghi vao mot thu muc nao ca: nguoi
 * dung chon cho de, AURA khong xin them mot quyen nao, va tep nam ngoai vung
 * app - tuc go app di no van con. Mot ban sao luu bi xoa cung luc voi thu no
 * dang sao luu thi khong phai ban sao luu.
 */
@Composable
internal fun SaoLuuLoiMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current

    var soBai by remember { mutableIntStateOf(0) }
    var bao by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Doc thu muc cung la cham dia, nen dem cung day sang luong nen. So bai
    // khong nhieu, nhung trang Chinh mo ra khong duoc phep khung mot nhip nao.
    LaunchedEffect(Unit) { soBai = withContext(Dispatchers.IO) { Lyra.demLoiTuNhap(context) } }

    val ghi = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Doc het kho loi roi ghi ra tep: viec cua dia, khong phai cua luong ve.
        scope.launch {
            bao = withContext(Dispatchers.IO) {
                try {
                    val chu = Lyra.xuatLoiTuNhap(context)
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(chu.toByteArray())
                    } ?: error("không mở được tệp")
                    "Đã lưu $soBai bài ra tệp."
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

            val kq = withContext(Dispatchers.IO) { Lyra.nhapLoiTuNhap(context, chu) }
            soBai = withContext(Dispatchers.IO) { Lyra.demLoiTuNhap(context) }
            bao = when {
                kq.hong > 0 -> "Tệp này không phải bản sao lưu của AURA."
                kq.them == 0 && kq.daCo > 0 ->
                    "${kq.daCo} bài trong tệp đều đã có sẵn trên máy — giữ nguyên bản đang có."
                kq.daCo > 0 -> "Thêm ${kq.them} bài. ${kq.daCo} bài đã có sẵn nên giữ nguyên."
                else -> "Đã khôi phục ${kq.them} bài."
            }
        }
    }

    Column {
        Text(
            if (soBai == 0) "Chưa có bài nào bạn tự nhập lời."
            else "Đang giữ $soBai bài lời bạn tự nhập hoặc tự căn giờ.",
            color = mau.chuMo,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        // Cau bao dat NGAY DUOI dong dem chu khong duoi hang nut. Thu tay tren
        // may cho thay hang nut nam sat day man hinh, nen mot cau bao dat duoi
        // no roi xuong duoi vung nhin: bam xong khong thay gi bao ca, phai cuon
        // moi biet la da xong. Dat canh dung con so vua doi thi ca hai cung
        // loc vao mat mot luc.
        bao?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = mau.chu, fontSize = 13.sp, lineHeight = 19.sp)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Số lời này nằm trong bộ nhớ riêng của AURA: gỡ app hoặc đổi máy là " +
                "mất hết. Lưu ra một tệp rồi cất đi thì công gõ và công căn giờ " +
                "còn lại.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Nut(
                nhan = "Sao lưu ra tệp",
                bat = soBai > 0,
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                bao = null
                ghi.launch("lyra-loi-" + homNay() + ".txt")
            }
            Nut(
                nhan = "Khôi phục",
                bat = true,
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                bao = null
                // Nhieu may gan cho tep .txt kieu MIME khac nhau, co may tra ve
                // "application/octet-stream". Loc chat theo "text/plain" thi tep
                // cua chinh minh vua ghi ra lai hien mo nhat va bam khong duoc.
                doc.launch(arrayOf("text/plain", "text/*", "application/octet-stream"))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Khôi phục không ghi đè: bài nào máy đang có lời tự nhập thì giữ " +
                "nguyên bản trên máy.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun Nut(
    nhan: String,
    bat: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onBam: () -> Unit
) {
    val mau = LocalBangMau.current
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (bat) accent else mau.nenChim)
            .then(if (bat) Modifier.clickable(onClick = onBam) else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            nhan,
            color = if (bat) Color.White else mau.chuRatMo,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun homNay(): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
