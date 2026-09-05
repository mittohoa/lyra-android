package com.mittohoa.lyra.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.mittohoa.lyra.lyrics.DocChuTuAnh
import kotlinx.coroutines.launch
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

    val ngucanh = LocalContext.current
    val pham = rememberCoroutineScope()
    var dangDoc by remember { mutableStateOf(false) }
    var baoDoc by remember { mutableStateOf<String?>(null) }

    // Bộ chọn ảnh của hệ thống — KHÔNG xin quyền đọc ảnh.
    //
    // `PickVisualMedia` trả về đúng một tấm người dùng chỉ, và app chỉ được
    // đọc tấm đó. Xin quyền đọc cả thư viện chỉ để nhận một tấm ảnh là xin
    // thừa, và là thứ người dùng có lý khi từ chối.
    val chonAnh = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        dangDoc = true
        pham.launch {
            val doc = DocChuTuAnh.doc(ngucanh, uri)
            dangDoc = false
            // Noi that so dong them vao, va noi that rang no co the con rac.
            // OCR tren mot anh chup ca man hinh se doc luon ca ten nut va dong
            // ho; giau chuyen do di thi nguoi dung phat hien ra o buoc can gio,
            // luc da mat cong hon nhieu.
            baoDoc = if (doc == null) "Không đọc được chữ nào từ ảnh đó"
                else "Đã thêm ${doc.lines().size} dòng — xoá bớt dòng thừa nếu " +
                    "ảnh chụp cả giao diện."
            if (doc != null) {
                // NỐI vào chứ không thay: lời dài thường phải chụp mấy tấm, và
                // tấm thứ hai mà xoá mất tấm thứ nhất thì không ai dùng nổi.
                text = if (text.isBlank()) doc else text.trimEnd() + "\n" + doc
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .chanChamXuyen()
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
                    "Dán lời vào đây, hoặc bấm “Từ ảnh” để đọc chữ ra khỏi một " +
                        "ảnh chụp.\n\n[00:12.34]Câu đầu tiên\n[00:18.00]Câu thứ hai\n…",
                    color = mau.chuRatMo,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )
            }
        }

        baoDoc?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = mau.chuMo, fontSize = 13.sp, lineHeight = 19.sp)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Pillbtn(
                if (dangDoc) "Đang đọc…" else "Từ ảnh",
                mau.nenChim,
                Modifier.weight(1.1f)
            ) { if (!dangDoc) chonAnh.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
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
