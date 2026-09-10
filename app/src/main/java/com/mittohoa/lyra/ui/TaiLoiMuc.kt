package com.mittohoa.lyra.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mittohoa.lyra.service.Lyra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tải sẵn lời cho cả thư viện.
 *
 * VÌ SAO CẦN. Ô tìm đọc được cả lời bài hát, nhưng chỉ đọc được lời ĐÃ NẰM
 * TRONG KHO — tức những bài đã từng mở. Một thư viện năm trăm bài mà mới nghe
 * hai chục thì ô tìm gần như trống, và màn hình phải đứng ra xin lỗi bằng một
 * dòng chữ nhỏ. Lần tải này biến nó từ một mẹo hay thành một tính năng thật.
 *
 * BÀY RA TỈ LỆ TRƯỚC KHI BẤM. "Đang có lời cho 42 trên 380 bài" nói được ngay
 * việc này đáng làm hay không, mà không cần đọc một dòng giải thích nào.
 *
 * ĐI TỪNG BÀI MỘT, có nghỉ giữa hai lần — xem `Lyra.taiLoiChoThuVien`. LRCLIB
 * là kho mở, miễn phí, chạy bằng tiền quyên góp; bắn hàng trăm lần gọi song
 * song vào đó là cách nhanh nhất để cả app bị chặn.
 */
@Composable
internal fun TaiLoiMuc(accent: Color) {
    val mau = LocalBangMau.current
    val context = LocalContext.current

    val thuVien by Lyra.library.collectAsStateWithLifecycle()
    val tien by Lyra.tienTaiLoi.collectAsStateWithLifecycle()

    var daCo by remember { mutableIntStateOf(0) }

    // Đếm lại mỗi khi thư viện đổi HOẶC lần tải chạy xong. Đếm là đọc đĩa từng
    // bài nên đẩy sang luồng nền: trang Chỉnh mở ra không được khựng một nhịp
    // nào, mà một thư viện hai nghìn bài là hai nghìn lần hỏi kho.
    LaunchedEffect(thuVien, tien?.xong) {
        daCo = withContext(Dispatchers.IO) { Lyra.demBaiCoLoi(context) }
    }

    val dangChay = tien != null && !tien!!.xong

    Column {
        Text(
            when {
                thuVien.isEmpty() -> "Chưa có bài nào trong thư viện."
                else -> "Đang có lời cho $daCo trên ${thuVien.size} bài."
            },
            color = mau.chuMo,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        tien?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                if (it.xong) "Xong — tìm được lời cho ${it.daCo} trên ${it.tong} bài."
                else "Đang tải… ${it.daXet}/${it.tong}, tìm được ${it.daCo}.",
                color = mau.chu,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Tải xong thì ô tìm đọc được cả lời: gõ một câu bạn nhớ là ra bài. " +
                "Bài nào không có trên kho lời thì vẫn không có — lần tải này " +
                "không tự chép lời ra từ nhạc.",
            color = mau.chuRatMo,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        NutManhSaoLuu(
            nhan = if (dangChay) "Dừng" else "Tải lời cho cả thư viện",
            bat = thuVien.isNotEmpty(),
            accent = accent,
            modifier = Modifier
        ) {
            if (dangChay) Lyra.thoiTaiLoi() else Lyra.taiLoiChoThuVien(context)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Đi từng bài một và có nghỉ giữa hai lần, nên một thư viện lớn mất " +
                "vài phút. LRCLIB là kho lời mở chạy bằng tiền quyên góp — hỏi " +
                "dồn dập vào đó thì cả app bị chặn, và lúc ấy không ai tra được " +
                "lời nữa.",
            color = mau.chuRatMo,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
    }
}
