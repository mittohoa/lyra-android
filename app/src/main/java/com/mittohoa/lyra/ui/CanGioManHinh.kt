package com.mittohoa.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.lyrics.LyricLine
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.sources.LrclibPublish
import kotlinx.coroutines.delay

/**
 * Căn giờ cho lời chữ trơn: bật nhạc, tới câu nào thì chạm một cái.
 *
 * Đây là mảnh còn thiếu để khép vòng. Lời CHỮ TRƠN thì đầy trên mạng; lời có
 * MỐC THỜI GIAN thì hiếm, nhất là tiếng Việt — hôm nay tìm ra "Sóng Gió" mà
 * LRCLIB không có lời nào. Mà mọi thứ Lyra làm đều đứng trên chỗ có mốc: khung
 * lời nổi, sáu hiệu ứng, lặp A–B, thẻ lời.
 *
 * Lyra là thứ hiếm hoi có đủ ba mảnh để tự tạo ra chúng — nó đang phát bài, nó
 * có lời, nó có bộ đếm thời gian. Người dùng chỉ phải cung cấp mảnh thứ tư:
 * đôi tai. Căn xong thì góp lên LRCLIB được, và lúc đó là góp đúng thứ đang
 * thiếu chứ không phải thứ vốn đã đầy.
 *
 * ## Vì sao phải căn HẾT mới lưu được
 *
 * Bộ đọc `.lrc` xếp các dòng theo thời gian. Một file trộn — vài dòng có mốc,
 * vài dòng không — thì dòng không mốc mang thời gian 0 và bị đẩy hết lên đầu
 * bài. Lời đảo lộn còn tệ hơn hẳn lời chưa căn: người đọc tin vào thứ tự, và
 * thứ tự sai thì họ không nhận ra là sai.
 *
 * Nên nút Lưu chỉ mở khi mọi dòng đã có mốc. Đổi lại là hoàn tác nhiều bậc và
 * tạm dừng thoải mái.
 */
@Composable
fun CanGioManHinh(
    cacDong: List<LyricLine>,
    tenBai: String,
    caSi: String,
    accent: Color,
    dangPhat: Boolean,
    onLuu: (String) -> Unit,
    onDong: () -> Unit
) {
    val context = LocalContext.current

    // Mốc đã căn cho từng dòng; -1 là chưa căn.
    val mocs = remember(cacDong) { mutableStateListOf(*Array(cacDong.size) { -1L }) }
    var toi by remember(cacDong) { mutableIntStateOf(0) }
    var viTri by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            viTri = Lyra.livePosition()
            delay(120)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(toi) {
        if (toi in cacDong.indices) listState.animateScrollToItem(toi, scrollOffset = -220)
    }

    val xong = toi >= cacDong.size

    Column(
        Modifier
            .fillMaxSize()
            .background(mau.nen)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Căn giờ", color = mau.chu, fontFamily = boChu.loi,
                    fontSize = 21.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (caSi.isBlank()) tenBai else "$caSi — $tenBai",
                    color = mau.chuMo, fontSize = 13.sp
                )
            }
            Box(
                Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onDong)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Đóng", color = mau.chuMo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            if (xong) "Đã căn xong ${cacDong.size} câu. Lưu lại nhé."
            else "Bật nhạc, tới câu nào thì chạm nút bên dưới. Đã căn " +
                "$toi/${cacDong.size} câu.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(cacDong) { i, dong ->
                val daCan = mocs[i] >= 0
                val sapToi = i == toi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (daCan) nhan(mocs[i]) else if (sapToi) "▸" else "",
                        color = if (daCan) accent else mau.chuRatMo,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(76.dp)
                    )
                    Text(
                        dong.text.ifBlank { "♪" },
                        color = when {
                            sapToi -> mau.chu
                            daCan -> mau.chuMo
                            else -> mau.chuRatMo
                        },
                        fontFamily = boChu.loi,
                        fontSize = if (sapToi) 20.sp else 16.sp,
                        fontWeight = if (sapToi) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = if (sapToi) 27.sp else 23.sp
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NutNho("⏮ 5s", true) {
                    Lyra.seekTo(context, (Lyra.livePosition() - 5_000L).coerceAtLeast(0L))
                }
                NutNho(if (dangPhat) "Tạm dừng" else "Phát", true) { Lyra.playPause(context) }
                NutNho("Hoàn tác", toi > 0) {
                    // Lùi một bậc VÀ tua về đúng chỗ câu đó bắt đầu: bấm nhầm
                    // rồi thì phải nghe lại được đoạn ấy, không thì hoàn tác
                    // xong vẫn không căn lại được.
                    val truoc = toi - 1
                    val moc = mocs[truoc]
                    mocs[truoc] = -1L
                    toi = truoc
                    if (moc >= 0) Lyra.seekTo(context, moc)
                }
                Text(
                    clockLabel(viTri),
                    color = mau.chuMo,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (xong) accent else mau.nenChim)
                    .clickable(enabled = !xong) {
                        // Trừ đi thời gian phản xạ.
                        //
                        // Người ta chạm SAU khi nghe thấy câu bắt đầu, nên mốc
                        // ghi thẳng sẽ luôn muộn. Không bù thì cả bài lệch đều
                        // một phần tư giây — đủ để lời chạy sau tiếng hát.
                        val t = (Lyra.livePosition() - BU_PHAN_XA).coerceAtLeast(0L)
                        if (toi in cacDong.indices) {
                            mocs[toi] = t
                            toi++
                        }
                    }
                    .padding(vertical = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (xong) "Đã căn hết" else "Chạm khi tới câu",
                    color = if (xong) Color.White else mau.chu,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (xong) accent else mau.nenChim)
                    .clickable(enabled = xong) {
                        onLuu(LrclibPublish.dungLrc(
                            cacDong.mapIndexed { i, d -> LyricLine(mocs[i], d.text) }
                        ))
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (xong) "Lưu lời đã căn" else "Căn hết mới lưu được",
                    color = if (xong) Color.White else mau.chuRatMo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Người ta chạm sau khi nghe thấy; 250ms là khoảng phản xạ thường gặp. */
private const val BU_PHAN_XA = 250L

private fun nhan(ms: Long): String {
    val tong = ms / 1000
    return "%d:%02d.%02d".format(tong / 60, tong % 60, (ms % 1000) / 10)
}

@Composable
private fun NutNho(nhan: String, bamDuoc: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(mau.nenChim)
            .clickable(enabled = bamDuoc, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            nhan,
            color = if (bamDuoc) mau.chu else mau.chuRatMo,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
