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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.service.Lyra

/**
 * Chọn một bản lời khác cho bài đang phát.
 *
 * VÌ SAO CÓ. Cửa chặn khi app tự tìm lời là ĐỘ GIỐNG CỦA TÊN BÀI — tên ca sĩ
 * không phải lúc nào cũng đối chiếu được, vì rất nhiều tệp nhạc gắn thẻ sai
 * hoặc bỏ trống. Nên một bài tên "Hours" nhận được lời của "Hours and Hours"
 * của một người hoàn toàn khác. Bản 0.3.25 đã nói ra chuyện đó, nhưng nói xong
 * rồi để đấy: người dùng chỉ còn cách tự gõ lại cả bài.
 *
 * Mà DỮ LIỆU ĐÃ NẰM SẴN TRONG TAY. LRCLIB trả về cả danh sách trong một lần
 * gọi; app chọn một bản rồi vứt phần còn lại. Bày phần còn lại ra là rẻ nhất
 * trong mọi cách sửa, và nó không đoán thêm điều gì — chính người dùng nhìn tên
 * bài, tên ca sĩ, độ dài rồi chỉ vào bản đúng.
 *
 * BÀY ĐỘ DÀI RA, và đó không phải chi tiết trang trí: hai bản thu của cùng một
 * bài lệch nhau vài chục giây thì lời chạy sai từ đầu tới cuối. Độ dài là manh
 * mối rẻ nhất và đáng tin nhất để người dùng tự loại — họ biết bài mình đang
 * nghe dài bao nhiêu, còn app thì chỉ đoán được.
 */
@Composable
fun ChonBanLoi(
    accent: Color,
    /** Bài đang phát, để người dùng còn đối chiếu. */
    tenBai: String,
    caSi: String,
    /** Độ dài bài đang phát, mili-giây; 0 khi không biết. */
    doDai: Long,
    /** Đang dùng bản do người dùng tự chọn hay không — có thì cho bỏ. */
    dangDungBanChon: Boolean,
    onXong: () -> Unit
) {
    // `null` = đang hỏi. Danh sách rỗng và "hỏi không được" là HAI chuyện, và
    // hai chuyện đó đòi người dùng làm hai việc khác nhau — xem `baoHong`.
    var ds by remember { mutableStateOf<List<Lyrics>?>(null) }
    var baoHong by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val ra = Lyra.banLoiKhac()
        baoHong = ra == null
        ds = ra ?: emptyList()
    }

    Box(
        Modifier
            .fillMaxSize()
            // Nuốt mọi cú chạm rơi ra ngoài: màn hình này che trang Lời, mà
            // chạm xuyên qua một thứ đang che là bấm nhầm vào chỗ không nhìn
            // thấy. Xem `chanChamXuyen`.
            .chanChamXuyen()
            .background(mau.nen)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Chọn bản lời",
                        color = mau.chu,
                        fontFamily = boChu.loi,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        danhTinh(tenBai, caSi, doDai),
                        color = mau.chuRatMo,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Đóng",
                    color = mau.chuMo,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onXong)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            if (dangDungBanChon) {
                Notice(
                    accent = accent,
                    text = "Đang dùng bản bạn tự chọn cho bài này.",
                    action = "Bỏ chọn",
                    onAction = {
                        Lyra.boBanLoiDaChon()
                        onXong()
                    }
                )
            }

            val danhSach = ds
            when {
                danhSach == null -> Giua {
                    LyraMark(size = 48.dp, busy = true)
                    Spacer(Modifier.height(14.dp))
                    Text("Đang tìm các bản lời…", color = mau.chuMo, fontSize = 14.5.sp)
                }

                // HAI câu khác nhau cho hai chuyện khác nhau. "Hỏi không được"
                // thì thử lại là xong; "không có bản nào" thì thử lại bao nhiêu
                // lần cũng thế, và đường đi tiếp là tự nhập.
                baoHong -> Giua {
                    Text(
                        "Không hỏi được kho lời. Kiểm tra mạng rồi mở lại.",
                        color = mau.chuMo,
                        fontSize = 14.5.sp
                    )
                }

                danhSach.isEmpty() -> Giua {
                    Text(
                        "Kho lời không có bản nào cho bài này.",
                        color = mau.chuMo,
                        fontSize = 14.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đóng lại rồi bấm “Tự nhập” để tự dán lời vào.",
                        color = mau.chuRatMo,
                        fontSize = 13.sp
                    )
                }

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 22.dp, end = 22.dp, top = 8.dp, bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(danhSach) { ban ->
                        MotBanLoi(ban, doDai, accent) {
                            Lyra.chonBanLoi(ban)
                            onXong()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Giua(noiDung: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) { noiDung() }
    }
}

@Composable
private fun MotBanLoi(ban: Lyrics, doDaiBai: Long, accent: Color, onChon: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(mau.nenChim)
            .clickable(onClick = onChon)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            ban.matchedTitle.ifBlank { "Không rõ tên bài" },
            color = mau.chu,
            fontFamily = boChu.loi,
            fontSize = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            ban.matchedArtist.ifBlank { "Không rõ ca sĩ" },
            color = mau.chuMo,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Có mốc giờ hay chỉ chữ trơn là khác biệt LỚN NHẤT giữa hai bản
            // lời, lớn hơn cả tên: gần hết những gì AURA làm — tô sáng câu đang
            // hát, khung lời nổi, lặp A–B, thẻ lời — đều đứng trên chỗ có mốc.
            The(if (ban.synced) "Có mốc giờ" else "Chữ trơn", ban.synced, accent)
            val lech = doLech(ban.sourceDuration, doDaiBai)
            if (lech != null) {
                Spacer(Modifier.width(8.dp))
                The(lech.chu, false, accent, canhBao = lech.dangNgo)
            }
        }
    }
}

@Composable
private fun The(nhan: String, noiBat: Boolean, accent: Color, canhBao: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    noiBat -> accent.copy(alpha = 0.30f)
                    canhBao -> mau.chu.copy(alpha = 0.10f)
                    else -> mau.vien.copy(alpha = 0.5f)
                }
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            nhan,
            color = if (noiBat) mau.chu else mau.chuMo,
            fontSize = 12.sp,
            fontWeight = if (noiBat) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private class Lech(val chu: String, val dangNgo: Boolean)

/**
 * Bản lời này dài bằng bài đang nghe không.
 *
 * Trả `null` khi một trong hai bên không nói độ dài — im lặng thì không so được
 * gì, và bịa ra một con số ở đây là mời người dùng loại nhầm một bản đúng.
 *
 * Ngưỡng 15 giây lấy đúng ngưỡng `LyricsRepository` dùng để đánh dấu "khác bản
 * thu": hai bản phát hành của cùng một bài hiếm khi lệch quá chừng đó, còn bản
 * live hay bản có đoạn dạo dài thì gần như luôn vượt.
 */
private fun doLech(nguon: Long, dangNghe: Long): Lech? {
    if (nguon <= 0L || dangNghe <= 0L) return null
    val giay = nguon / 1000
    val chu = "%d:%02d".format(giay / 60, giay % 60)
    val lech = kotlin.math.abs(nguon - dangNghe)
    return Lech(chu, dangNgo = lech > 15_000L)
}

private fun danhTinh(tenBai: String, caSi: String, doDai: Long): String {
    val phan = mutableListOf<String>()
    if (tenBai.isNotBlank()) phan += tenBai
    if (caSi.isNotBlank()) phan += caSi
    if (doDai > 0L) {
        val giay = doDai / 1000
        phan += "%d:%02d".format(giay / 60, giay % 60)
    }
    return phan.joinToString("  ·  ")
}
