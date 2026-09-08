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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.TheSua
import com.mittohoa.lyra.sources.Track

/**
 * Sửa thẻ một bài trong máy.
 *
 * VÌ SAO CẦN. Rất nhiều tệp nhạc gắn thẻ sai hoặc bỏ trống, và AURA thì dựa vào
 * thẻ cho gần như mọi việc: gom album, xếp thứ tự đĩa, và quan trọng nhất là đi
 * TÌM LỜI. Một tệp ghi ca sĩ là "Unknown Artist" thì không nguồn lời nào tìm ra
 * được — mà đó chính là thứ người dùng mở app này lên để xem.
 *
 * NÓI THẲNG LÀ KHÔNG SỬA TỆP. Màn hình này ghi một bảng nằm trong app chứ không
 * đụng vào thẻ ID3 của tệp, và câu đó phải nằm ngay trên màn hình chứ không
 * giấu trong ghi chú phát hành: người dùng sửa xong, mở tệp bằng trình phát
 * khác, thấy tên cũ, và kết luận app hỏng. Biết trước thì đó chỉ là một giới
 * hạn; không biết thì đó là một lời hứa bị bội.
 *
 * BỎ TRỐNG = GIỮ NGUYÊN THẺ GỐC, chứ không phải "xoá trường đó đi". Đây là cách
 * duy nhất để bỏ một chỗ sửa mà không cần thêm nút xoá riêng cho từng ô.
 */
@Composable
fun SuaTheManHinh(
    bai: Track,
    banDau: TheSua,
    accent: Color,
    onLuu: (TheSua) -> Unit,
    onDong: () -> Unit
) {
    var ten by remember { mutableStateOf(banDau.title) }
    var caSi by remember { mutableStateOf(banDau.artist) }
    var album by remember { mutableStateOf(banDau.album) }
    var thuTu by remember {
        mutableStateOf(if (banDau.soThuTu >= 0) banDau.soThuTu.toString() else "")
    }

    Box(
        Modifier
            .fillMaxSize()
            // Nuốt chạm rơi ra ngoài: màn hình này phủ lên danh sách, và chạm
            // xuyên qua là bấm nhầm vào một bài không nhìn thấy.
            .chanChamXuyen()
            .background(mau.nen)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sửa thẻ",
                    color = mau.chu,
                    fontFamily = boChu.loi,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Đóng",
                    color = mau.chuMo,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDong)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Chỉ đổi trong AURA — tệp nhạc không bị sửa, nên các trình phát " +
                    "khác vẫn thấy thẻ cũ.",
                color = mau.chuRatMo,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(20.dp))
            // Bày THẺ GỐC ra dưới mỗi ô, chứ không đổ sẵn vào ô. Đổ sẵn thì
            // không phân biệt được "để nguyên" với "sửa thành đúng cái cũ", mà
            // đó chính là cách bỏ một chỗ đã sửa.
            O("Tên bài", ten, bai.title, accent) { ten = it }
            O("Ca sĩ", caSi, bai.artist, accent) { caSi = it }
            O("Album", album, bai.album, accent) { album = it }
            O(
                "Số thứ tự trong album",
                thuTu,
                if (bai.soThuTu > 0) bai.soThuTu.toString() else "",
                accent,
                so = true
            ) { thuTu = it.filter(Char::isDigit).take(3) }

            Spacer(Modifier.height(26.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .clickable {
                        onLuu(
                            TheSua(
                                title = ten.trim(),
                                artist = caSi.trim(),
                                album = album.trim(),
                                // Chuỗi rỗng là "không sửa", KHÁC với số 0 —
                                // số 0 nghĩa là "bài này không đánh số", và đó
                                // là một cách sửa hợp lệ.
                                soThuTu = thuTu.trim().toIntOrNull() ?: -1
                            )
                        )
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Lưu",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Bỏ trống một ô nghĩa là giữ nguyên thẻ trong tệp. Xoá hết rồi " +
                    "lưu là bỏ mọi chỗ đã sửa cho bài này.",
                color = mau.chuRatMo,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun O(
    nhan: String,
    giaTri: String,
    goc: String,
    accent: Color,
    so: Boolean = false,
    onDoi: (String) -> Unit
) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(nhan, color = mau.chuMo, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = giaTri,
            onValueChange = onDoi,
            singleLine = true,
            textStyle = TextStyle(color = mau.chu, fontSize = 16.sp),
            cursorBrush = SolidColor(accent),
            keyboardOptions = if (so) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            },
            decorationBox = { o ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(mau.nenChim)
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    if (giaTri.isEmpty()) {
                        Text(
                            goc.ifBlank { "— trống —" },
                            color = mau.chuRatMo,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    o()
                }
            }
        )
        if (giaTri.isNotBlank() && goc.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Row {
                Text("Thẻ trong tệp: ", color = mau.chuRatMo, fontSize = 12.sp)
                Text(
                    goc,
                    color = mau.chuRatMo,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
