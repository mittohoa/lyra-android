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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mittohoa.lyra.data.LanNghe
import java.util.Calendar

/**
 * Hàng "Nghe gần đây", cuộn ngang, đặt trên đầu trang Tìm.
 *
 * VÌ SAO CÓ. Thư viện xếp theo album hay theo tên trả lời được câu "bài X nằm
 * đâu". Câu hay gặp hơn là "bài hôm qua nghe trên đường về, tên gì ấy nhỉ" — và
 * với câu đó thì cả album lẫn bảng chữ cái đều vô dụng. Chỉ thứ tự thời gian
 * mới trả lời được.
 *
 * CUỘN NGANG chứ không xếp dọc, cùng lẽ với hàng danh sách phát ngay dưới: đây
 * là thứ người ta lướt qua để NHẬN RA, không phải để đọc từng dòng. Xếp dọc thì
 * mươi bài vừa nghe đã chiếm hết màn hình và đẩy cả thư viện xuống dưới.
 *
 * Muốn xem hết thì có [LichSuManHinh] — hàng này chỉ bày mấy bài đầu.
 */
@Composable
fun HangNgheGanDay(
    lichSu: List<LanNghe>,
    accent: Color,
    onChon: (LanNghe) -> Unit,
    onXemHet: () -> Unit
) {
    if (lichSu.isEmpty()) return

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Nghe gần đây",
            color = mau.chuRatMo,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        // LUÔN có lối vào màn hình đầy đủ, kể cả khi hàng này đã bày hết. Nút
        // "Xoá hết" nằm trong đó, và đó là thứ duy nhất trong app cần với tới
        // được ngay lúc đưa máy cho người khác mượn. Chỉ hiện nó khi lịch sử đã
        // dài quá mười bài thì đúng lúc mới nghe vài bài lại là lúc không xoá
        // được — mà mới vài bài mới chính là lúc người ta muốn xoá nhất.
        Text(
            "Xem hết",
            color = accent,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onXemHet)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(lichSu.take(TRONG_HANG), key = { it.khoa }) { lan ->
            Column(
                Modifier
                    .width(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(mau.nenChim)
                    .clickable { onChon(lan) }
                    .padding(14.dp)
            ) {
                Text(
                    lan.ten.ifBlank { lan.caSi },
                    color = mau.chu,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 19.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    nhanDuoi(lan),
                    color = mau.chuRatMo,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

/**
 * Cả lịch sử, xếp theo ngày.
 *
 * Phủ lên trang Tìm chứ không thành một trang riêng của bộ điều hướng — cùng lẽ
 * với màn hình danh sách phát: mở lịch sử ra là một việc NGẮN, xem rồi đóng.
 *
 * XOÁ ĐƯỢC NGAY TRÊN MÀN HÌNH NÀY. Đây là bản ghi những gì một người đã nghe,
 * riêng tư hơn hẳn mọi dữ liệu khác trong app. Chôn nút xoá trong Cài đặt thì
 * người đưa máy cho bạn mượn không kịp tìm ra nó.
 */
@Composable
fun LichSuManHinh(
    lichSu: List<LanNghe>,
    accent: Color,
    onChon: (LanNghe) -> Unit,
    onXoaMot: (String) -> Unit,
    onXoaHet: () -> Unit,
    onDong: () -> Unit
) {
    // Hỏi lại TRONG CHÍNH NÚT chứ không dựng một hộp thoại: câu hỏi chỉ có một
    // vế, và một hộp thoại phủ kín màn hình cho một vế thì nặng hơn việc nó hỏi.
    var hoiXoa by remember { mutableStateOf(false) }

    val nhom = remember(lichSu) { chiaTheoNgay(lichSu) }

    Column(Modifier.fillMaxSize().background(mau.nen)) {
        Row(
            Modifier.padding(start = 24.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Nghe gần đây",
                    color = mau.chu,
                    fontFamily = boChu.loi,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${lichSu.size} bài",
                    color = mau.chuRatMo,
                    fontSize = 12.5.sp
                )
            }
            Text(
                if (hoiXoa) "Xoá thật?" else "Xoá hết",
                color = if (hoiXoa) accent else mau.chuMo,
                fontSize = 14.sp,
                fontWeight = if (hoiXoa) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        if (hoiXoa) {
                            onXoaHet()
                            hoiXoa = false
                            onDong()
                        } else {
                            hoiXoa = true
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Text(
                "Đóng",
                color = mau.chuMo,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDong)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
            nhom.forEach { (ngay, ds) ->
                item(key = "ngay:$ngay") {
                    Text(
                        ngay,
                        color = mau.chuMo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp
                        )
                    )
                }
                items(ds, key = { it.khoa }) { lan ->
                    DongLichSu(lan, accent, onChon = { onChon(lan) }, onXoa = { onXoaMot(lan.khoa) })
                }
            }
        }
    }
}

@Composable
private fun DongLichSu(
    lan: LanNghe,
    accent: Color,
    onChon: () -> Unit,
    onXoa: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onChon)
            .padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                lan.ten.ifBlank { lan.caSi },
                color = mau.chu,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                nhanDuoi(lan),
                color = mau.chuRatMo,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Bài của app khác thì bấm vào là ĐI TÌM, không phải phát. Nói ra bằng
        // một chữ ngay trên dòng chứ không để người dùng bấm rồi mới biết.
        if (!lan.ngheLaiDuoc) {
            Text(
                "Tìm",
                color = accent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onXoa)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("×", color = mau.chuRatMo, fontSize = 17.sp)
        }
    }
}

/** Bao nhiêu thẻ bày ra ở hàng ngang. Quá số này thì mới có nút "Xem hết". */
private const val TRONG_HANG = 10

/**
 * Dòng chữ nhỏ dưới tên bài: ca sĩ, và app nào phát nếu không phải AURA.
 *
 * Ca sĩ đứng trước vì đó là thứ giúp nhận ra bài. Tên app chỉ là chú thích cho
 * biết vì sao dòng này không phát lại được — nên nó đứng sau, và biến mất hẳn ở
 * những bài do chính AURA phát, nơi nó chẳng nói thêm được gì.
 */
private fun nhanDuoi(lan: LanNghe): String {
    val app = if (lan.app.isBlank()) "" else appLabel(lan.app)
    return when {
        lan.caSi.isBlank() && app.isBlank() -> "Trong máy"
        lan.caSi.isBlank() -> app
        app.isBlank() -> lan.caSi
        else -> lan.caSi + " · " + app
    }
}

/**
 * Chia lịch sử thành từng ngày, giữ nguyên thứ tự mới-trước.
 *
 * Nhóm theo NGÀY LỊCH chứ không theo "bao lâu trước": hai bài nghe cách nhau
 * một phút mà vắt qua nửa đêm thì đúng là hai ngày khác nhau, và người ta nhớ
 * theo ngày chứ không theo số giờ đã trôi.
 */
private fun chiaTheoNgay(lichSu: List<LanNghe>): List<Pair<String, List<LanNghe>>> {
    if (lichSu.isEmpty()) return emptyList()
    val homNay = dauNgay(System.currentTimeMillis())
    val ra = ArrayList<Pair<String, MutableList<LanNghe>>>()
    for (lan in lichSu) {
        val nhan = tenNgay(lan.luc, homNay)
        val cuoi = ra.lastOrNull()
        if (cuoi != null && cuoi.first == nhan) cuoi.second.add(lan)
        else ra.add(nhan to mutableListOf(lan))
    }
    return ra.map { it.first to it.second }
}

/** Mốc 0 giờ của ngày chứa `luc`, theo múi giờ máy. */
private fun dauNgay(luc: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = luc
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun tenNgay(luc: Long, homNay: Long): String {
    // Chưa từng ghi giờ — dữ liệu từ bản cũ hoặc một dòng hỏng. Nói thẳng là
    // không biết, đừng gán bừa cho hôm nay.
    if (luc <= 0L) return "Không rõ khi nào"
    val ngay = dauNgay(luc)
    val cach = (homNay - ngay) / 86_400_000L
    return when {
        cach <= 0L -> "Hôm nay"
        cach == 1L -> "Hôm qua"
        cach < 7L -> "$cach ngày trước"
        cach < 30L -> "${cach / 7} tuần trước"
        else -> {
            val c = Calendar.getInstance()
            c.timeInMillis = ngay
            "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.YEAR)}"
        }
    }
}
