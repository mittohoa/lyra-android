package com.mittohoa.lyra.ui

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.CaiAmThanh

/**
 * Hai lựa chọn đụng thẳng vào tiếng nhạc AURA tự phát.
 *
 * CHỈ ÁP CHO NHẠC CỦA AURA, và mục này nói thẳng điều đó ra. Cả hai đều nằm
 * trong bộ phát; nhạc phát ở Zing hay YouTube thì AURA chỉ đứng ngoài đọc thẻ
 * media, không chạm được vào dòng tiếng. Bày một công tắc không làm gì cho nửa
 * số trường hợp mà không nói ra là cách chắc chắn để người dùng kết luận app
 * hỏng.
 */
@Composable
internal fun AmThanhMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current
    val cai = remember { CaiAmThanh(context) }

    var doi by remember { mutableIntStateOf(0) }
    val boLang = remember(doi) { cai.boKhoangLang() }
    val moDan = remember(doi) { cai.moDanMs() }

    Column {
        Text(
            "Chỉ áp cho nhạc AURA tự phát. Nhạc ở app khác thì AURA đứng " +
                "ngoài đọc thẻ, không chạm được vào tiếng.",
            color = mau.chuRatMo,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(14.dp))
        Text("Mờ dần khi đổi bài", color = mau.chu, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Nhỏ tiếng dần ở cuối bài rồi to lại ở đầu bài sau. Hai bài không " +
                "chồng lên nhau — chồng tiếng cần hai bộ phát, và bộ phát ở " +
                "đây còn gánh hàng đợi, thẻ màn hình khoá và Android Auto.",
            color = mau.chuRatMo,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (ms in CaiAmThanh.CAC_MUC) {
                OChon(nhanMoDan(ms), ms == moDan, accent) {
                    cai.datMoDanMs(ms)
                    doi++
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Bỏ khoảng lặng", color = mau.chu, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Cắt những đoạn im lặng kéo dài trong bài. Hợp với album thu liền " +
                "mạch chừa vài giây trống ở cuối mỗi bài — nhưng có bài thì " +
                "khoảng lặng nằm trong ý đồ người làm, cắt đi là hỏng.",
            color = mau.chuRatMo,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OChon("Tắt", !boLang, accent) { cai.datBoKhoangLang(false); doi++ }
            OChon("Bật", boLang, accent) { cai.datBoKhoangLang(true); doi++ }
        }
    }
}

@Composable
private fun OChon(nhan: String, dangChon: Boolean, accent: Color, onBam: () -> Unit) {
    val mau = LocalBangMau.current
    Text(
        text = nhan,
        color = if (dangChon) Color.White else mau.chuMo,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (dangChon) accent else mau.nenChim)
            .clickable(onClick = onBam)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

private fun nhanMoDan(ms: Int): String = when (ms) {
    0 -> "Tắt"
    else -> {
        val giay = ms / 1000.0
        if (giay % 1.0 == 0.0) "${giay.toInt()}s" else "0,5s"
    }
}
