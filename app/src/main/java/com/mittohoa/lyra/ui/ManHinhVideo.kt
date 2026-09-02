package com.mittohoa.lyra.ui

import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mittohoa.lyra.player.Playback

/**
 * Ô hình cho video đang phát.
 *
 * `SurfaceView` chứ không `TextureView`: bộ giải mã vẽ thẳng vào một lớp riêng
 * của hệ thống, không phải đi qua lớp vẽ của app — nhẹ pin hơn hẳn và không
 * rớt hình khi màn hình bận. Cái giá là nó không xoay, không làm mờ, không hoà
 * trộn được với thứ vẽ đè lên; Lyra không cần thứ nào trong số đó.
 *
 * Gắn và bỏ theo vòng đời của ô: bộ giải mã giữ tham chiếu tới bề mặt vẽ, nên
 * quên bỏ ra thì nó vẽ vào một bề mặt đã chết — màn hình đen, và không có lỗi
 * nào báo cả.
 */
@Composable
internal fun ManHinhVideo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = remember { SurfaceView(context) }

    DisposableEffect(view) {
        Playback.ganManHinh(view)
        onDispose { Playback.boManHinh(view) }
    }

    AndroidView(factory = { view }, modifier = modifier.fillMaxSize())
}
