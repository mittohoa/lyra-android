package com.mittohoa.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mấy việc làm được với bài đang phát, gom vào một chỗ.
 *
 * VÌ SAO CÓ. Mỗi việc trong đây từng là một ô lời nhắc riêng nằm trên đầu trang
 * Bài. Thêm dần thì tới lúc năm ô chồng lên nhau chiếm nửa màn hình, ảnh bìa bị
 * ép xuống, và lời — thứ người ta mở app ra để đọc — trôi hẳn khỏi tầm nhìn.
 *
 * Chỗ hỏng nằm ở chỗ TRỘN HAI LOẠI vào một dải:
 *
 *   - TIN về bài lời đang hiện: "lời từ đâu", "mốc có thể lệch", "chưa có mốc".
 *     Chúng nói về đúng thứ đang nằm trước mắt, nên thuộc về trang.
 *   - LỜI MỜI làm một việc: ghi ra tệp, sửa thẻ, chọn bản khác, tải gói dịch.
 *     Chúng không phải tin — chúng luôn đúng, luôn có, và không đổi theo bài.
 *     Bày một lời mời cố định lên chỗ dành cho tin là ngày nào cũng báo một
 *     chuyện mà người đọc đã biết từ lần mở app đầu tiên.
 *
 * Loại thứ hai vào đây. Đổi lại một cú chạm, được lại nửa màn hình — và mỗi
 * việc có một chỗ CỐ ĐỊNH để tìm, thay vì một tấm thẻ trôi lên trôi xuống tuỳ
 * bài đang phát có bao nhiêu lời nhắc.
 *
 * VIỆC KHÔNG DÙNG ĐƯỢC THÌ VẪN BÀY RA, chỉ mờ đi kèm một dòng nói vì sao. Giấu
 * đi thì người dùng đi tìm một thứ họ nhớ là có và kết luận app mất tính năng;
 * bày ra kèm lý do thì họ biết cần làm gì để dùng được.
 */
@Composable
fun MucLucBai(
    accent: Color,
    muc: List<MucViec>,
    onDong: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .chanChamXuyen()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDong)
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(mau.nen)
                // Chặn chạm RIÊNG cho tấm: nền phía sau đóng khi bấm, mà tấm
                // thì không được đóng khi bấm vào giữa nó.
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 14.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(mau.vien)
            )

            muc.forEach { m -> Muc(m, accent) }
        }
    }
}

/**
 * Một việc trong mục lục.
 *
 * `viSao` chỉ có nghĩa khi việc đang KHÔNG dùng được — nó là câu trả lời cho
 * "sao cái này mờ", và không có nó thì mục mờ chỉ là một chỗ cụt.
 */
class MucViec(
    val nhan: String,
    val mota: String,
    val dungDuoc: Boolean = true,
    val viSao: String = "",
    val onChon: () -> Unit
)

@Composable
private fun Muc(m: MucViec, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = m.dungDuoc, onClick = m.onChon)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                m.nhan,
                color = if (m.dungDuoc) mau.chu else mau.chuRatMo,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (m.dungDuoc) m.mota else m.viSao.ifBlank { m.mota },
                color = mau.chuRatMo,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }
        if (m.dungDuoc) {
            Spacer(Modifier.width(12.dp))
            Text("›", color = accent, fontSize = 20.sp)
        }
    }
}
