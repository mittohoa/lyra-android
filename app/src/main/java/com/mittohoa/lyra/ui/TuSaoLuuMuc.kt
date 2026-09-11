package com.mittohoa.lyra.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.SaoLuuTuDong
import com.mittohoa.lyra.service.Lyra
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bật cho AURA tự ghi một bản sao lưu vào thư mục bạn chỉ.
 *
 * NẰM NGAY DƯỚI CÂU CẢNH BÁO. Mục Sao lưu nói "gỡ app hoặc đổi máy là mất
 * hết" — và từ bản này câu ấy đúng hoàn toàn, vì sao lưu ngầm của Android đã
 * tắt. Một lời cảnh báo không kèm lối thoát thì chỉ là đùn việc sang người
 * đọc; lối thoát phải nằm ngay cạnh chỗ nói ra vấn đề, không phải ở một mục
 * khác.
 *
 * KHÔNG BẬT SẴN. Nó cần một thư mục người dùng tự chọn, và việc chọn ấy chính
 * là lời đồng ý. Không có cách nào bật hộ mà vẫn giữ được ý nghĩa đó.
 */
@Composable
internal fun TuSaoLuuMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kho = remember { Lyra.khoTuSaoLuu(context) }

    // Đếm để ép đọc lại kho sau mỗi lần đổi. Kho này nằm trên `SharedPreferences`
    // chứ không phải một dòng chảy, nên không có gì tự báo là nó đã đổi.
    var doi by remember { mutableIntStateOf(0) }
    var bao by remember { mutableStateOf<String?>(null) }

    val thuMuc = remember(doi) { kho.thuMuc() }
    val soNgay = remember(doi) { kho.soNgay() }
    val lanCuoi = remember(doi) { kho.lanCuoi() }

    val chonThuMuc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { kho.datThuMuc(uri) }
            .onFailure { bao = "Máy không trao quyền ghi lâu dài cho thư mục này." }
            .onSuccess { bao = null }
        doi++
    }

    Column {
        if (thuMuc == null) {
            Text(
                "AURA có thể tự ghi một bản ra thư mục bạn chỉ, mỗi tuần một " +
                    "lần, để bạn không phải nhớ bấm nút. Tệp nằm trong máy " +
                    "bạn, không đi đâu cả.",
                color = mau.chuRatMo,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            NutManhSaoLuu(
                nhan = "Chọn thư mục để tự lưu",
                bat = true,
                accent = accent,
                modifier = Modifier
            ) { chonThuMuc.launch(null) }
        } else {
            Text(
                "Đang tự lưu vào " + tenThuMuc(thuMuc.toString()) + ", " +
                    nhipChu(soNgay) + ". " + lanCuoiChu(lanCuoi),
                color = mau.chuMo,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(10.dp))
            Text(
                "Giữ ${SaoLuuTuDong.SO_BAN_GIU} bản gần nhất rồi mới xoá bớt — " +
                    "một lần hỏng dữ liệu mà cả tháng sau mới nhận ra thì vẫn " +
                    "còn đường lùi.",
                color = mau.chuRatMo,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(12.dp))
            Text("Mấy ngày một bản", color = mau.chuMo, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in NHIP) {
                    ONhip(n, n == soNgay, accent) {
                        kho.datSoNgay(n)
                        doi++
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NutManhSaoLuu(
                    nhan = "Ghi ngay một bản",
                    bat = true,
                    accent = accent,
                    modifier = Modifier.weight(1f)
                ) {
                    bao = null
                    scope.launch {
                        bao = when (val kq = Lyra.ghiSaoLuuNgay(context)) {
                            is SaoLuuTuDong.KetQua.DaGhi ->
                                "Đã ghi ${kq.ten} (${kq.soByte / 1024} KB)."
                            is SaoLuuTuDong.KetQua.Hong ->
                                "Không ghi được: ${kq.vi}"
                            SaoLuuTuDong.KetQua.ChuaToiHan -> null
                        }
                        doi++
                    }
                }
                NutManhSaoLuu(
                    nhan = "Thôi tự lưu",
                    bat = true,
                    accent = mau.nenChim,
                    modifier = Modifier.weight(1f)
                ) {
                    kho.tat()
                    bao = null
                    doi++
                }
            }
        }

        bao?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = mau.chu, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun ONhip(n: Int, dangChon: Boolean, accent: Color, onBam: () -> Unit) {
    val mau = LocalBangMau.current
    Text(
        text = "$n ngày",
        color = if (dangChon) Color.White else mau.chuMo,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (dangChon) accent else mau.nenChim)
            .clickable(onClick = onBam)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

private val NHIP = listOf(1, 3, 7, 14)

private fun nhipChu(n: Int) = if (n == 1) "mỗi ngày" else "$n ngày một bản"

private fun lanCuoiChu(luc: Long): String =
    if (luc <= 0L) "Chưa ghi bản nào."
    else "Bản gần nhất: " + GIO.format(Date(luc)) + "."

/**
 * Lấy đoạn cuối của địa chỉ SAF cho người đọc.
 *
 * Địa chỉ đầy đủ là một chuỗi mã hoá dài không ai đọc nổi; đoạn sau dấu hai
 * chấm cuối cùng là đường thư mục thật. Không đọc ra được thì thà nói "thư mục
 * đã chọn" còn hơn bày một chuỗi `content://com.android.externalstorage…`.
 */
private fun tenThuMuc(uri: String): String {
    val sau = uri.substringAfterLast("%3A", "").ifEmpty { uri.substringAfterLast(':', "") }
    val ten = java.net.URLDecoder.decode(sau, "UTF-8").trim('/')
    return if (ten.isBlank()) "thư mục đã chọn" else ten
}

private val GIO = SimpleDateFormat("d/M/yyyy HH:mm", Locale.US)
