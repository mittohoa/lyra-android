package com.mittohoa.lyra.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mittohoa.lyra.lyrics.RawNowPlaying
import com.mittohoa.lyra.lyrics.candidatesFrom
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.service.Lyra

/**
 * Mốc 1: chứng minh đường khó nhất đi được.
 *
 * Màn hình này chỉ làm đúng một việc — hiện thứ đang phát ở app khác, và hiện
 * luôn các phương án nhận diện mà `Identify` bóc ra. Nếu bước này không xong
 * thì cả dự án không có nghĩa, nên nó được làm trước tiên.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                HomeScreen(
                    hasNotificationAccess = hasNotificationAccess(),
                    onOpenNotificationSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Người dùng vừa bật quyền xong quay lại — đọc lại ngay, không bắt đợi
        if (hasNotificationAccess()) Lyra.refresh(this)
    }

    private fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
}

@Composable
private fun HomeScreen(
    hasNotificationAccess: Boolean,
    onOpenNotificationSettings: () -> Unit
) {
    val now by Lyra.now.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131019))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Lyra",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF0EDF6)
        )
        Text(
            "Lời bài hát cho nhạc đang phát ở app khác",
            fontSize = 14.sp,
            color = Color(0xFF9A92A9)
        )

        Spacer(Modifier.height(6.dp))

        if (!hasNotificationAccess) {
            PermissionCard(onOpenNotificationSettings)
        } else {
            NowPlayingCard(now)
        }
    }
}

@Composable
private fun PermissionCard(onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cần quyền đọc thông báo", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                "Android chỉ cho đọc thông tin bài đang phát ở app khác khi bạn đã bật quyền " +
                    "này. Lyra không đọc nội dung thông báo của bạn — chỉ đọc tên bài và vị " +
                    "trí phát.",
                fontSize = 13.sp,
                color = Color(0xFFC9C2D6)
            )
            Button(onClick = onOpen) { Text("Mở Cài đặt để bật") }
        }
    }
}

@Composable
private fun NowPlayingCard(now: NowPlaying?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (now == null) {
                Text("Chưa thấy app nào đang phát", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Mở Spotify, YouTube Music, Zing hay NhacCuaTui rồi phát một bài, " +
                        "sau đó quay lại đây.",
                    fontSize = 13.sp,
                    color = Color(0xFFC9C2D6)
                )
                return@Card
            }

            Text(now.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(
                listOf(now.artist, now.album).filter { it.isNotEmpty() }.joinToString(" · "),
                fontSize = 13.sp,
                color = Color(0xFFC9C2D6)
            )
            Text(
                "${now.packageName} · ${if (now.isPlaying) "đang phát" else "tạm dừng"} · " +
                    "${now.position / 1000}s / ${now.duration / 1000}s",
                fontSize = 12.sp,
                color = Color(0xFF9A92A9),
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(6.dp))
            Text("Nhận diện ra", fontSize = 12.sp, color = Color(0xFF9A92A9))

            // Hiện luôn các phương án để lúc dò sai còn biết vì sao
            val candidates = candidatesFrom(
                RawNowPlaying(
                    title = now.title,
                    artist = now.artist.ifEmpty { null },
                    album = now.album.ifEmpty { null }
                )
            )
            candidates.take(3).forEach { c ->
                Text(
                    "${c.weight}  ${c.artist.ifEmpty { "(không rõ)" }} — ${c.title}",
                    fontSize = 12.sp,
                    color = Color(0xFFC4B5FD),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
