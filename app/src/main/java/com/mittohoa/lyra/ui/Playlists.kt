package com.mittohoa.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.mittohoa.lyra.data.Playlist

/**
 * Hang the danh sach phat, cuon ngang.
 *
 * Cuon ngang chu khong xep doc: danh sach phat la thu nguoi dung luot qua de
 * NHAN RA cai minh muon, khong phai doc tung dong. Va xep doc thi vai danh sach
 * da chiem het man hinh, day nhac trong may xuong duoi.
 */
@Composable
fun PlaylistRow(
    playlists: List<Playlist>,
    accent: Color,
    onOpen: (String) -> Unit
) {
    if (playlists.isEmpty()) return

    Text(
        "Danh sách phát",
        color = mau.chuRatMo,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Column(
                Modifier
                    .width(132.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .clickable { onOpen(playlist.id) }
                    .padding(14.dp)
            ) {
                Text(
                    playlist.name,
                    color = mau.chu,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${playlist.tracks.size} bài",
                    color = mau.chuMo,
                    fontSize = 13.sp
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

/**
 * Man hinh mot danh sach phat.
 *
 * Phu len tren trang Tim thay vi la mot trang rieng: mo mot danh sach la mot
 * viec NGAN - xem co gi, bam phat, roi ra. Bien no thanh mot trang cua bo dieu
 * huong thi nguoi dung phai tim duong quay lai.
 */
@Composable
fun PlaylistScreen(
    playlist: Playlist,
    accent: Color,
    playingUri: String?,
    onClose: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .background(mau.nen)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = mau.chuMo, fontSize = 20.sp)
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    color = mau.chu,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    "${playlist.tracks.size} bài",
                    color = mau.chuRatMo,
                    fontSize = 13.5.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Phát tất cả", accent, Color.White, Modifier.weight(1.4f)) { onPlayAt(0) }
            Pill("Đổi tên", mau.nenChim, mau.chu, Modifier.weight(1f)) {
                renaming = true
            }
            Pill("Xoá", mau.nenChim, mau.chu, Modifier.weight(1f)) {
                confirmingDelete = true
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    if (renaming) {
        NameDialog(
            title = "Đổi tên danh sách",
            initial = playlist.name,
            accent = accent,
            onCancel = { renaming = false },
            onConfirm = {
                onRename(it)
                renaming = false
            }
        )
    }

    if (confirmingDelete) {
        // Hoi lai truoc khi xoa: mot danh sach la cong suc xep dat, va khong co
        // duong hoan tac
        ConfirmDialog(
            title = "Xoá \"${playlist.name}\"?",
            body = "Các bài trong đó không bị xoá khỏi máy hay khỏi nguồn — chỉ danh sách này mất đi.",
            confirm = "Xoá",
            accent = accent,
            onCancel = { confirmingDelete = false },
            onConfirm = {
                confirmingDelete = false
                onDelete()
            }
        )
    }
}

@Composable
private fun Pill(
    label: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Hop nhap ten.
 *
 * Tu ve chu khong dung `AlertDialog` cua Material: ca app khong co mot mang
 * Material nao khac, va mot hop thoai theo phong cach he thong roi vao giua man
 * hinh nay se lo ra ngay.
 */
@Composable
fun NameDialog(
    title: String,
    initial: String,
    accent: Color,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1A1622))
                .padding(22.dp)
                .imePadding()
        ) {
            Text(title, color = mau.chu, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(mau.nenChim)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = mau.chu, fontSize = 15.sp),
                    cursorBrush = SolidColor(accent)
                )
                if (text.isEmpty()) {
                    Text(
                        "Tên danh sách",
                        color = mau.chuRatMo,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Huỷ", mau.nenChim, mau.chu, Modifier.weight(1f)) {
                    onCancel()
                }
                Pill("Lưu", accent, Color.White, Modifier.weight(1.3f)) { onConfirm(text) }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    accent: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1A1622))
                .padding(22.dp)
        ) {
            Text(title, color = mau.chu, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                color = mau.chuMo,
                fontSize = 14.5.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Huỷ", mau.nenChim, mau.chu, Modifier.weight(1.3f)) {
                    onCancel()
                }
                Pill(confirm, accent, Color.White, Modifier.weight(1f)) { onConfirm() }
            }
        }
    }
}
