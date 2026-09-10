package com.mittohoa.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Nút mảng đặc, dùng cho mấy việc lớn trong trang Chỉnh.
 *
 * Tách ra một tệp riêng vì giờ có ba chỗ dùng: sao lưu, khôi phục, và tải lời
 * cho cả thư viện. Để nó nằm nhờ trong tệp của một mục cụ thể thì xoá mục đó
 * đi là kéo theo cả hai mục kia — đã suýt xảy ra thật.
 *
 * Nút TẮT thì vẫn bày ra chứ không giấu, chỉ mờ đi: giấu đi thì người dùng đi
 * tìm một thứ họ nhớ là có và kết luận app mất tính năng.
 */
@Composable
internal fun NutManhSaoLuu(
    nhan: String,
    bat: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onBam: () -> Unit
) {
    val mau = LocalBangMau.current
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (bat) accent else mau.nenChim)
            .then(if (bat) Modifier.clickable(onClick = onBam) else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            nhan,
            color = if (bat) Color.White else mau.chuRatMo,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
