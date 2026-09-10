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
 * Sao lưu và khôi phục MỌI THỨ người dùng tự tạo, trong một tệp.
 *
 * VÌ SAO GỘP LÀM MỘT. Trước đây lời tự nhập và lịch sử nghe mỗi thứ một mục
 * riêng, mỗi mục một nút — mà yêu thích và cân bằng âm thì chẳng có mục nào.
 * Bốn kho dữ liệu, hai nút, và không chỗ nào nói ra hai kho còn lại đang không
 * được sao lưu. Ai cũng sẽ bấm hai cái rồi tưởng đã xong, tới lúc đổi máy mới
 * biết mất gì.
 *
 * Một tệp, một nút, và dòng đếm nói rõ trong đó có gì.
 *
 * TỆP SAO LƯU CŨ VẪN KHÔI PHỤC ĐƯỢC — xem `Lyra.nhapTatCa`. Ra một định dạng
 * mới rồi bỏ rơi tệp cũ thì đúng vào lúc người ta cần khôi phục nhất lại là
 * lúc app nói không đọc được.
 */
@Composable
internal fun SaoLuuTatCaMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current

    var dem by remember { mutableStateOf(Lyra.DemSaoLuu(0, 0, 0)) }
    var bao by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Đếm là đọc đĩa nên đẩy sang luồng nền: trang Chỉnh mở ra không được khựng.
    LaunchedEffect(bao) {
        dem = withContext(Dispatchers.IO) { Lyra.demSaoLuu(context) }
    }

    val ghi = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            bao = withContext(Dispatchers.IO) {
                try {
                    val chu = Lyra.xuatTatCa(context)
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(chu.toByteArray())
                    } ?: error("không mở được tệp")
                    "Đã lưu ra tệp."
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

            val kq = withContext(Dispatchers.IO) { Lyra.nhapTatCa(context, chu) }
            bao = if (kq.hong) "Tệp này không phải bản sao lưu của AURA."
            else keChiTiet(kq)
        }
    }

    Column {
        Text(
            "Đang giữ ${dem.loi} bài lời tự nhập, ${dem.nghe} lần nghe, " +
                "${dem.thich} bài yêu thích.",
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
            "Tất cả nằm trong bộ nhớ riêng của AURA: gỡ app hoặc đổi máy là mất " +
                "hết. Lời gõ tay thì gõ lại được, còn tháng trước bạn nghe những " +
                "gì thì không dựng lại được bằng cách nào.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NutManhSaoLuu(
                nhan = "Sao lưu ra tệp",
                bat = dem.loi > 0 || dem.nghe > 0 || dem.thich > 0,
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                bao = null
                ghi.launch("aura-sao-luu-" + homNayTatCa() + ".txt")
            }
            NutManhSaoLuu(
                nhan = "Khôi phục",
                bat = true,
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                bao = null
                // Nhieu may gan cho tep .txt kieu MIME khac nhau — xem ghi chú
                // cùng chỗ trong `SaoLuuLoiMuc` đời trước.
                doc.launch(arrayOf("text/plain", "text/*", "application/octet-stream"))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Khôi phục là TRỘN, không xoá cái đang có. Tệp sao lưu cũ chỉ có " +
                "lời hoặc chỉ có lịch sử vẫn đọc được. Tệp lưu ra ghi rõ bạn " +
                "nghe gì lúc nào — cất nó như một thứ riêng tư.",
            color = mau.chuRatMo,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
    }
}

/**
 * Kể ra từng con số thay vì báo "xong".
 *
 * Khôi phục xong mà chỉ thấy chữ "xong" thì không biết tệp ấy có đúng tệp mình
 * tìm không — mà đó lại là điều duy nhất muốn biết ở đúng lúc ấy.
 */
private fun keChiTiet(kq: com.mittohoa.lyra.data.SaoLuuTatCa.KetQua): String {
    val phan = buildList {
        if (kq.loi.them > 0) add("${kq.loi.them} bài lời")

        // TÁCH "THÊM MỚI" KHỎI "LÀM MỚI".
        //
        // Gộp làm một con số thì câu báo nói hẹp, và người đọc hiểu sai theo
        // đúng hướng đáng lo nhất: khôi phục mười chín dòng mà đa số là bài máy
        // đã có, dòng đếm ở trên chỉ nhích một, và họ tưởng mười tám dòng kia
        // đã mất. Cả hai đều là việc thật — một cái làm danh sách DÀI ra, một
        // cái làm nó MỚI hơn — nên nói ra cả hai.
        //
        // Hai MỤC RIÊNG chứ không phải một mục có dấu hai chấm: câu báo này là
        // một danh sách ngăn bằng dấu phẩy, nên "lần nghe: thêm 1, làm mới 2"
        // nằm lẫn giữa "3 bài lời" và "5 bài yêu thích" thì không biết dấu
        // phẩy nào thuộc về ai.
        val n = kq.nghe
        if (n.themMoi > 0) add("${n.themMoi} lần nghe mới")
        if (n.capNhat > 0) add("${n.capNhat} lần nghe được cập nhật")

        if (kq.thich > 0) add("${kq.thich} bài yêu thích")
        if (kq.coCanBang) add("lựa chọn cân bằng âm")
    }
    if (phan.isEmpty()) return "Tệp đọc được, nhưng mọi thứ trong đó máy đã có sẵn."
    return "Đã khôi phục " + phan.joinToString(", ") + "."
}

private fun homNayTatCa(): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
