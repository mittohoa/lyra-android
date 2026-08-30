package com.mittohoa.lyra.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mittohoa.lyra.service.Lyra

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ve tran ra sat vien: nen mau lay tu anh bia phai chay het man hinh,
        // khong bi cat ngang boi hai dai he thong
        enableEdgeToEdge()

        setContent {
            val now by Lyra.now.collectAsStateWithLifecycle()
            val lyrics by Lyra.lyrics.collectAsStateWithLifecycle()
            val loading by Lyra.loading.collectAsStateWithLifecycle()

            var overlayOn by remember { mutableStateOf(Lyra.overlay.isShowing) }
            val position = rememberPlaybackPosition()

            HomeScreen(
                now = now,
                lyrics = lyrics,
                loading = loading,
                position = position,
                hasNotificationAccess = hasNotificationAccess(),
                canDrawOverlay = Settings.canDrawOverlays(this),
                overlayOn = overlayOn,
                onOpenNotificationSettings = {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onOpenOverlaySettings = {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                },
                onToggleOverlay = { overlayOn = Lyra.toggleOverlay(this) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Nguoi dung vua bat quyen xong quay lai - doc lai ngay, khong bat doi
        if (hasNotificationAccess()) Lyra.refresh(this)
    }

    private fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
}

/**
 * Vi tri phat, cap nhat theo nhip ve cua he thong.
 *
 * Dung `withFrameMillis` chu khong dung bo dem rieng: no chay dung nhip ve, va
 * TU DUNG khi man hinh khong con ve (app xuong nen, man hinh tat). Mot bo dem
 * 100ms thi cu chay tiep du khong ai nhin - hao pin ma khong duoc gi.
 *
 * Chi doc lai moi 200ms: hoi day hon khong lam loi chay muot hon, vi cai quyet
 * dinh la DONG nao dang hat chu khong phai mili-giay thu bao nhieu.
 */
@Composable
private fun rememberPlaybackPosition(): State<Long> {
    val position = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameMillis { frame ->
                if (frame - last >= 200) {
                    last = frame
                    position.longValue = Lyra.watcher.livePosition()
                }
            }
        }
    }

    return position
}
