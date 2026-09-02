package com.mittohoa.lyra.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import android.view.WindowManager
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Video xem toàn màn hình.
 *
 * Nằm trong một `Dialog` chiếm hết màn hình chứ không phải một `Box` đè lên
 * trang: chỉ cửa sổ riêng mới trốn được hai dải hệ thống mà không kéo theo cả
 * phần còn lại của app, và thoát ra thì mọi thứ về đúng như cũ mà không phải
 * dọn tay chỗ nào.
 *
 * XOAY THEO VIDEO, không xoay theo máy: video ngang thì ép ngang, video dọc thì
 * để nguyên. Ép ngang một video quay dọc là biến nó thành một dải hẹp giữa hai
 * vùng đen rộng gấp đôi phần hình — và đó đúng là thứ người ta bấm nút này để
 * thoát khỏi.
 */
@Composable
internal fun ToanManHinh(
    tiLe: Float?,
    accent: Color,
    dangPhat: Boolean,
    viTri: State<Long>,
    doDai: Long,
    onPhatDung: () -> Unit,
    onTruoc: () -> Unit,
    onSau: () -> Unit,
    onTua: (Long) -> Unit,
    onDong: () -> Unit
) {
    val view = LocalView.current
    val nam = (tiLe ?: 16f / 9f) >= 1f

    // Ép chiều xoay theo video, rồi trả lại đúng chiều cũ lúc thoát. Giữ lại
    // giá trị cũ chứ không đặt bừa về UNSPECIFIED: máy có thể đang bị khoá
    // xoay bởi chính người dùng.
    DisposableEffect(nam) {
        val activity = view.context as? Activity
        val cu = activity?.requestedOrientation
        activity?.requestedOrientation =
            if (nam) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        onDispose {
            activity?.requestedOrientation = cu ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = onDong,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Cua so cua Dialog, KHONG phai cua so cua Activity: an hai dai he
        // thong phai an tren dung cua so dang phu len tren, khong thi lenh roi
        // vao cua so nam duoi va khong thay gi doi.
        val cuaSoDialog = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(cuaSoDialog) {
            // Cho phep ve TRAN CA vung khuyet. Khong dat thi o che do ngang,
            // he thong chua mot dai trong ben phia co lo camera - va dai do de
            // lo trang nam duoi, den nham mot mau khac han.
            cuaSoDialog?.attributes = cuaSoDialog?.attributes?.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            val dieuKhien = cuaSoDialog?.let {
                WindowCompat.getInsetsController(it, it.decorView)
            }
            dieuKhien?.hide(WindowInsetsCompat.Type.systemBars())
            dieuKhien?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            onDispose { dieuKhien?.show(WindowInsetsCompat.Type.systemBars()) }
        }

        BackHandler(onBack = onDong)

        // Thanh điều khiển ẩn đi sau khi hiện, và một cú chạm bất kỳ gọi nó về.
        // Xem phim mà có một thanh nút nằm đè suốt thì thà xem ở khung nhỏ.
        var hienNut by remember { mutableStateOf(true) }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { hienNut = !hienNut },
            contentAlignment = Alignment.Center
        ) {
            // Video phai VUA TRON trong man hinh, khong tran ra.
            //
            // `fillMaxWidth().aspectRatio(r)` thi chi dung khi man hinh be ngang
            // hon video. Man hinh ngang cua dien thoai be NGANG hon 16:9 that
            // (3120x1440 la 2,17:1), nen keo het chieu rong cho ra mot khung cao
            // 1755 tren mot man hinh cao 1440 - mat mot dai tren va mot dai duoi.
            // So hai ti le roi chon chieu de bam vao moi dung ca hai phia.
            BoxWithConstraints(contentAlignment = Alignment.Center) {
                val tiLeVideo = (tiLe ?: 16f / 9f).coerceIn(0.4f, 2.5f)
                val tiLeKhung = maxWidth / maxHeight
                ManHinhVideo(
                    modifier =
                        if (tiLeKhung > tiLeVideo) Modifier.fillMaxHeight().aspectRatio(tiLeVideo)
                        else Modifier.fillMaxWidth().aspectRatio(tiLeVideo),
                    dangPhat = dangPhat
                )
            }

            if (hienNut) {
                Box(Modifier.fillMaxSize().padding(20.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(onClick = onDong),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 18.sp)
                    }

                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Seek(
                            accent = accent,
                            position = viTri,
                            duration = doDai,
                            enabled = doDai > 0,
                            onSeek = onTua
                        )
                        Spacer(Modifier.height(2.dp))
                        // Hàng nút riêng chứ không dùng lại `Transport`: hàng kia
                        // có thêm trộn bài và lặp, mà hai thứ đó không có nghĩa
                        // gì khi đang xem một video. Bày ra rồi nối vào chỗ
                        // không làm gì thì tệ hơn là không bày.
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Ghost("◀◀", active = false, accent = accent, onClick = onTruoc)
                            PlayButton(accent = accent, playing = dangPhat, onClick = onPhatDung)
                            Ghost("▶▶", active = false, accent = accent, onClick = onSau)
                        }
                    }
                }
            }
        }
    }
}
