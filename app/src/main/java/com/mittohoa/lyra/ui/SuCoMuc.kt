package com.mittohoa.lyra.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.NhatKySuCo

/**
 * Kể lại những lần app tự tắt, và cho người dùng gửi đi nếu họ muốn.
 *
 * VÌ SAO CÓ. Cho tới giờ mọi bản đều đo trên đúng một chiếc máy của chính
 * người viết. Bản đầu tiên tới tay người lạ mà tắt ngang thì họ không có cách
 * nào kể lại — và lỗi ấy sẽ chỉ tồn tại trong đầu người gặp nó.
 *
 * KHÔNG TỰ GỬI ĐI ĐÂU. Nhật ký nằm trong bộ nhớ riêng của app và chỉ rời khỏi
 * máy khi người dùng tự bấm, tới nơi họ tự chọn. Bản này vừa tắt sao lưu ngầm
 * của Android vì không muốn dữ liệu tự đi khỏi máy; gắn một bộ tự gửi sự cố
 * vào đây là nói một đằng làm một nẻo.
 *
 * BÀY NGUYÊN VĂN RA TRƯỚC KHI GỬI. Vết lỗi có thể chứa đường dẫn tệp trong máy
 * — người ta có quyền đọc thứ mình sắp gửi đi.
 *
 * MỤC NÀY TỰ ẨN khi chưa có lần nào. Một mục tên "Sự cố" nằm chình ình trong
 * Cài đặt của một app chưa hề hỏng chỉ làm người dùng lo.
 */
@Composable
internal fun SuCoMuc(accent: Color, onXoaHet: () -> Unit) {
    val mau = LocalBangMau.current
    val context = LocalContext.current

    var doi by remember { mutableIntStateOf(0) }
    var xemNguyenVan by remember { mutableStateOf(false) }

    val soLan = remember(doi) { NhatKySuCo.dem(context) }
    val ganNhat = remember(doi) { NhatKySuCo.lanGanNhat(context) }

    Column {
        Text(
            if (soLan == 1) "AURA đã tự tắt một lần."
            else "AURA đã tự tắt $soLan lần.",
            color = mau.chu,
            fontSize = 14.sp
        )
        ganNhat?.let {
            Spacer(Modifier.height(2.dp))
            Text("Lần gần nhất: $it", color = mau.chuRatMo, fontSize = 12.5.sp)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Nhật ký này nằm trong máy bạn và không tự đi đâu cả. Gửi đi thì " +
                "bạn chọn gửi bằng gì và gửi cho ai — nên xem qua trước, trong " +
                "đó có thể có đường dẫn tệp trên máy.",
            color = mau.chuRatMo,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )

        if (xemNguyenVan) {
            Spacer(Modifier.height(10.dp))
            Text(
                NhatKySuCo.doc(context),
                color = mau.chuMo,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(mau.nenChim)
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NutManhSaoLuu(
                nhan = if (xemNguyenVan) "Thu lại" else "Xem",
                bat = true,
                accent = mau.nenChim,
                modifier = Modifier.weight(1f)
            ) { xemNguyenVan = !xemNguyenVan }

            NutManhSaoLuu(
                nhan = "Gửi đi",
                bat = true,
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                val chu = NhatKySuCo.doc(context)
                // `createChooser` chứ không mở thẳng: không đoán hộ người dùng
                // muốn gửi bằng gì, và trên máy không có app nhận thì hệ thống
                // nói ra thay vì app ném lỗi.
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "AURA — nhật ký sự cố")
                    putExtra(Intent.EXTRA_TEXT, chu)
                }
                context.startActivity(Intent.createChooser(i, "Gửi nhật ký sự cố"))
            }

            NutManhSaoLuu(
                nhan = "Xoá",
                bat = true,
                accent = mau.nenChim,
                modifier = Modifier.weight(1f)
            ) {
                NhatKySuCo.xoa(context)
                xemNguyenVan = false
                doi++
                // BÁO NGƯỢC LÊN, không chỉ tự dựng lại mình.
                //
                // Cái `if` quyết định có bày mục này ra hay không nằm ở màn
                // hình Chỉnh, không nằm ở đây. Chỉ dựng lại riêng mục này thì
                // sau khi xoá nó vẫn nằm nguyên đó và đọc ra "AURA đã tự tắt 0
                // lần" — đo được trên máy thật.
                onXoaHet()
            }
        }
    }
}
