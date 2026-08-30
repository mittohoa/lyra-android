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
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mittohoa.lyra.service.Lyra
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    /**
     * Trang thai hai quyen, giu o day chu KHONG hoi lai trong than composable.
     *
     * Ca hai ham kiem quyen deu la loi goi qua binder toi he thong -
     * `getEnabledListenerPackages` con phai truy van mot content provider. Goi
     * chung trong than composable nghia la moi lan dung lai giao dien lai hoi
     * he thong mot lan; voi mot man hinh co dong ho chay thi thanh hang chuc
     * lan moi giay tren LUONG CHINH, va app treo (ANR).
     *
     * Hai quyen nay chi doi khi nguoi dung roi app di bat, nen doc lai o
     * `onResume` la du va dung.
     */
    private var hasNotificationAccess by mutableStateOf(false)
    private var canDrawOverlay by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ve tran ra sat vien: nen mau lay tu anh bia phai chay het man hinh,
        // khong bi cat ngang boi hai dai he thong
        enableEdgeToEdge()
        readPermissions()

        setContent {
            val now by Lyra.now.collectAsStateWithLifecycle()
            val lyrics by Lyra.lyrics.collectAsStateWithLifecycle()
            val loading by Lyra.loading.collectAsStateWithLifecycle()

            var overlayOn by remember { mutableStateOf(Lyra.overlay.isShowing) }
            var look by remember { mutableStateOf(Lyra.overlay.currentLook(this)) }
            val position = rememberPlaybackPosition()

            HomeScreen(
                now = now,
                lyrics = lyrics,
                loading = loading,
                position = position,
                hasNotificationAccess = hasNotificationAccess,
                canDrawOverlay = canDrawOverlay,
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
                onToggleOverlay = { overlayOn = Lyra.toggleOverlay(this) },
                onSyncToLine = { Lyra.syncToLine(it) },
                onClearOffset = { Lyra.clearOffset() },
                look = look,
                onLookChange = {
                    // Ve lai khung dang noi NGAY, roi moi ghi xuong. Cho ghi
                    // xong moi ve thi keo thanh truot bi giat tung nac.
                    look = it
                    Lyra.overlay.applyLook(this, it)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        readPermissions()
        // Nguoi dung vua bat quyen xong quay lai - doc lai ngay, khong bat doi
        if (hasNotificationAccess) Lyra.refresh(this)
    }

    private fun readPermissions() {
        hasNotificationAccess =
            NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        canDrawOverlay = Settings.canDrawOverlays(this)
    }
}

/**
 * Vi tri phat, cap nhat moi 200ms va chi khi man hinh dang hien.
 *
 * `repeatOnLifecycle(STARTED)` tu dung vong lap khi app xuong nen va tu chay
 * lai khi quay ve - khong ton pin dem thoi gian cho mot man hinh khong ai nhin.
 *
 * Da thu dung `withFrameMillis` de bam theo nhip ve, nhung no bat he thong ve
 * lien tuc 60 khung mot giay suot ca bai hat du chu chi doi vai giay mot lan.
 * `delay` de he thong ranh giua hai lan doc.
 *
 * 200ms la du: cai quyet dinh la DONG nao dang hat, chu khong phai mili-giay
 * thu bao nhieu.
 */
@Composable
private fun rememberPlaybackPosition(): State<Long> {
    val position = remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                position.longValue = Lyra.watcher.livePosition()
                delay(200)
            }
        }
    }

    return position
}
