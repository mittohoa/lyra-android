package com.mittohoa.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Man hinh tu nhap loi.
 *
 * Duong cuu khi ca ba nguon deu khong co, hoac co ma sai. Voi nhung bai it
 * nguoi nghe, day la cach duy nhat de app con dung duoc.
 *
 * Mo ra la da co san loi dang hien (neu co), de nguoi dung SUA thay vi go lai
 * tu dau - phan lon truong hop chi sai vai dong.
 *
 * Nhan ca hai kieu: chu tron, hoac .lrc day du moc thoi gian dan tu noi khac.
 * Khong bat nguoi dung chon kieu nao - `parseLrc` tu nhan ra.
 */
@Composable
fun LyricEditor(
    initial: String,
    accent: Color,
    songLabel: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    Column(
        Modifier
            .fillMaxSize()
            .background(mau.nen)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Tự nhập lời", color = mau.chu, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            songLabel,
            color = mau.chuMo,
            fontSize = 14.sp,
            maxLines = 2
        )

        Spacer(Modifier.height(10.dp))
        Text(
            "Dán lời vào đây. Có mốc thời gian dạng [00:12.34] thì lời sẽ chạy theo " +
                "nhạc; không có thì hiện dạng chữ trơn.",
            color = mau.chuRatMo,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(mau.nenChim)
                .padding(14.dp)
        ) {
            // `BasicTextField` chu khong phai `TextField` cua Material: o day
            // can mot o chu tran vien, khong nhan, khong duong ke - de no hoa
            // vao man hinh khong chrome cua ca app
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(color = mau.chu, fontSize = 15.sp, lineHeight = 23.sp),
                cursorBrush = SolidColor(accent)
            )
            if (text.isEmpty()) {
                Text(
                    "[00:12.34]Câu đầu tiên\n[00:18.00]Câu thứ hai\n…",
                    color = mau.chuRatMo,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Pillbtn("Huỷ", mau.nenChim, Modifier.weight(1f), onCancel)
            if (initial.isNotEmpty()) {
                // Xoa loi tu nhap thi app quay lai tra tu ba nguon nhu binh thuong
                Pillbtn("Xoá", mau.nenChim, Modifier.weight(1f)) { onSave("") }
            }
            Pillbtn("Lưu", accent, Modifier.weight(1.4f)) { onSave(text) }
        }
    }
}

@Composable
private fun Pillbtn(
    label: String,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = mau.chu, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
