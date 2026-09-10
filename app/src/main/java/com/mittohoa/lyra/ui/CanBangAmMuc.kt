package com.mittohoa.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mittohoa.lyra.service.Lyra

/**
 * Cân bằng âm — chọn một bộ mẫu có sẵn của máy.
 *
 * NÓI TRƯỚC HAI GIỚI HẠN, cả hai đều làm người dùng tưởng app hỏng nếu không
 * nói:
 *
 *   - Chỉ áp cho nhạc AURA tự phát. Hiệu ứng âm thanh của Android gắn vào một
 *     phiên cụ thể, mà phiên của Zing hay YouTube là của app đó.
 *   - Phải phát một bài rồi mới biết máy có dùng được không. Phiên âm thanh chỉ
 *     có thật khi bộ phát đã mở đường ra, nên trước bài đầu tiên thì chưa hỏi
 *     được máy có bộ mẫu nào.
 *
 * Bộ mẫu do chính bộ xử lý âm thanh của máy khai ra, nên tên và số lượng khác
 * nhau tuỳ hãng — xem [com.mittohoa.lyra.player.CanBangAm].
 */
@Composable
internal fun CanBangAmMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current

    // Đọc lại mỗi lần nhịp đổi. `CanBangAm` không phải dòng chảy — nó là một
    // lớp bọc quanh một thứ của hệ thống, và trạng thái của nó chỉ đổi khi
    // chính app gọi vào. Một con số đếm là đủ để màn hình vẽ lại.
    val nhip by Lyra.nhipCanBang.collectAsStateWithLifecycle()
    val bo = remember(nhip) { Lyra.boCanBang(context) }

    Column {
        if (!bo.coDung) {
            Text(
                "Chưa dùng được. Phát một bài trong AURA rồi mở lại mục này — " +
                    "máy chỉ cho hỏi bộ cân bằng âm khi đang có tiếng ra.",
                color = mau.chuMo,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Có máy không có sẵn bộ cân bằng âm nào. Lúc đó mục này vẫn " +
                    "báo như trên, và nhạc phát bình thường.",
                color = mau.chuRatMo,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            return@Column
        }

        Text(
            if (bo.dangBat) "Đang bật · ${bo.mau.getOrNull(bo.mauDangChon) ?: ""}"
            else "Đang tắt.",
            color = mau.chuMo,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VienCanBang("Tắt", !bo.dangBat, accent) { Lyra.datBatCanBang(context, false) }
            bo.mau.forEachIndexed { i, ten ->
                VienCanBang(ten, bo.dangBat && bo.mauDangChon == i, accent) {
                    Lyra.datMauCanBang(context, i)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Chỉ áp cho nhạc AURA tự phát. Nhạc ở Zing, YouTube hay app khác thì " +
                "tiếng là của bên đó — AURA không sửa được, và cũng không nên.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun VienCanBang(nhan: String, dangChon: Boolean, accent: Color, onBam: () -> Unit) {
    val mau = LocalBangMau.current
    Text(
        nhan,
        color = if (dangChon) Color.White else mau.chuMo,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (dangChon) accent else mau.nenChim)
            .clickable(onClick = onBam)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
