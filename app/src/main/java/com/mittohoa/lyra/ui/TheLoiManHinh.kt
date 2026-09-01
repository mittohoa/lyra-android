package com.mittohoa.lyra.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.KieuChu
import com.mittohoa.lyra.lyrics.LyricLine
import com.mittohoa.lyra.share.TheLoi
import com.mittohoa.lyra.share.guiTheLoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Xem trước tấm thẻ lời rồi gửi đi.
 *
 * Mở ra ở câu đang hát vì đó là câu người ta vừa nghe thấy và muốn giữ lại,
 * nhưng đổi được sang câu khác bằng hai nút ‹ ›: câu đáng chia sẻ thường là câu
 * vừa trôi qua, không phải câu đang trôi.
 */
@Composable
fun TheLoiManHinh(
    cacDong: List<LyricLine>,
    dongDau: Int,
    tenBai: String,
    caSi: String,
    accent: Color,
    kieuChu: KieuChu,
    onDong: () -> Unit
) {
    val context = LocalContext.current
    val bangMau = mau

    // Bỏ qua các dòng trống và dòng chỉ có dấu nhạc: không ai chia sẻ một tấm
    // thẻ in mỗi chữ "♪".
    val dungDuoc = remember(cacDong) {
        cacDong.indices.filter { cacDong[it].text.isNotBlank() && cacDong[it].text.trim() != "♪" }
    }
    if (dungDuoc.isEmpty()) { onDong(); return }

    var viTri by remember {
        val gan = dungDuoc.indexOfFirst { it >= dongDau }
        mutableIntStateOf(if (gan >= 0) gan else dungDuoc.lastIndex)
    }
    val cauHat = cacDong[dungDuoc[viTri]].text

    // Vẽ trên luồng nền: một tấm 1080×1350 kèm bố cục chữ là việc của CPU, làm
    // trên luồng chính thì mỗi lần bấm ‹ › là một cú khựng.
    val anh by produceState<Bitmap?>(null, cauHat, bangMau.laGiay, kieuChu, accent) {
        value = withContext(Dispatchers.Default) {
            TheLoi.ve(
                context = context,
                cauHat = cauHat,
                tenBai = tenBai,
                caSi = caSi,
                mauNhan = accent.toArgb(),
                laGiay = bangMau.laGiay,
                kieuChu = kieuChu
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(mau.nen)
            // Man hinh nay ve tran ra sat vien nhu ca app, nen phai tu chua le
            // cho hai dai he thong. Khong chua thi dong "The loi" chui len duoi
            // dong ho va vach song.
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Thẻ lời", color = mau.chu, fontFamily = boChu.loi,
                fontSize = 21.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDong)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Đóng", color = mau.chuMo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val a = anh
            if (a != null) {
                Image(
                    bitmap = a.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(TheLoi.RONG.toFloat() / TheLoi.CAO)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, mau.vien, RoundedCornerShape(6.dp))
                )
            } else {
                LyraMark(size = 44.dp, busy = true)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NutDoiCau("‹", viTri > 0) { viTri-- }
            Text(
                "Câu ${viTri + 1}/${dungDuoc.size}",
                color = mau.chuMo,
                fontSize = 13.5.sp,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            NutDoiCau("›", viTri < dungDuoc.lastIndex) { viTri++ }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 22.dp)
                .clip(RoundedCornerShape(50))
                .background(if (anh != null) accent else mau.nenChim)
                .clickable(enabled = anh != null) {
                    anh?.let { guiTheLoi(context, it, tenBai) }
                }
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Chia sẻ",
                color = if (anh != null) Color.White else mau.chuRatMo,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NutDoiCau(nhan: String, bamDuoc: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(50))
            .background(mau.nenChim)
            .clickable(enabled = bamDuoc, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            nhan,
            color = if (bamDuoc) mau.chu else mau.chuRatMo,
            fontSize = 22.sp
        )
    }
}
