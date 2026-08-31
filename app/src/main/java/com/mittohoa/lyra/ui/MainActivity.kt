package com.mittohoa.lyra.ui

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.app.StatusBarManager
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
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
import androidx.annotation.RequiresApi
import com.mittohoa.lyra.R
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.service.LyraTileService
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
    private var canReadLibrary by mutableStateOf(false)

    /**
     * Ten quyen doc nhac, khac nhau theo doi may.
     *
     * Tu Android 13 quyen doc bo nho duoc tach theo loai noi dung. Xin dung
     * quyen NHAC la mot khac biet nguoi dung THAY duoc trong hop thoai - va
     * xin it hon thi ho dong y de hon.
     */
    private val audioPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private val askAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canReadLibrary = granted
        if (granted) Lyra.loadLibrary(this)
    }

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
            val translation by Lyra.translation.collectAsStateWithLifecycle()
            val translateSettings by Lyra.translateSettings.collectAsStateWithLifecycle()
            val results by Lyra.results.collectAsStateWithLifecycle()
            val searching by Lyra.searching.collectAsStateWithLifecycle()
            val queue by Lyra.queue.collectAsStateWithLifecycle()
            val queueIndex by Lyra.queueIndex.collectAsStateWithLifecycle()
            val artwork by Lyra.artwork.collectAsStateWithLifecycle()
            val shuffle by Lyra.shuffle.collectAsStateWithLifecycle()
            val repeat by Lyra.repeat.collectAsStateWithLifecycle()
            val library by Lyra.library.collectAsStateWithLifecycle()
            val playlists by Lyra.playlists.collectAsStateWithLifecycle()
            val downloads by Lyra.downloads.collectAsStateWithLifecycle()
            var openedPlaylistId by remember { mutableStateOf<String?>(null) }
            // Doc lai tu danh sach that moi lan no doi: doi ten hay xoa mot bai
            // phai thay ngay tren man hinh dang mo, khong phai dong ra mo lai
            val openedPlaylist = playlists.firstOrNull { it.id == openedPlaylistId }
            var searchQuery by remember { mutableStateOf("") }

            var overlayOn by remember { mutableStateOf(Lyra.overlay.isShowing) }
            var look by remember { mutableStateOf(Lyra.overlay.currentLook(this)) }
            // Chuoi dang soan; null nghia la khong mo man hinh soan
            var draft by remember { mutableStateOf<String?>(null) }
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
                onEditLyrics = { draft = Lyra.manualDraft() },
                look = look,
                suggestedFontSize = Lyra.overlay.suggestedFontSize(this),
                onLookChange = {
                    // Ve lai khung dang noi NGAY, roi moi ghi xuong. Cho ghi
                    // xong moi ve thi keo thanh truot bi giat tung nac.
                    look = it
                    Lyra.overlay.applyLook(this, it)
                },
                translation = translation,
                translateSettings = translateSettings,
                onDownloadModel = { Lyra.downloadTranslationModel() },
                onTranslateChange = { Lyra.updateTranslateSettings(it) },
                canAddTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                onAddTile = { requestAddTile() },
                searchQuery = searchQuery,
                results = results,
                searching = searching,
                queue = queue,
                queueIndex = queueIndex,
                onSearchQueryChange = { searchQuery = it },
                onSearchSubmit = { Lyra.search(searchQuery) },
                onPlayResult = { Lyra.playFromResults(this, it) },
                onEnqueue = { Lyra.enqueue(this, it) },
                onSkipInQueue = { Lyra.skipInQueue(this, it) },
                onRemoveFromQueue = { Lyra.removeFromQueue(this, it) },
                onPrevious = { Lyra.previous(this) },
                onPlayPause = { Lyra.playPause(this) },
                onNext = { Lyra.next(this) },
                artwork = artwork,
                shuffle = shuffle,
                repeat = repeat,
                onSeek = { Lyra.seekTo(this, it) },
                onToggleShuffle = { Lyra.toggleShuffle(this) },
                onCycleRepeat = { Lyra.cycleRepeat(this) },
                library = library,
                canReadLibrary = canReadLibrary,
                onAskLibrary = { askAudio.launch(audioPermission) },
                onPlayFromLibrary = { Lyra.playFromLibrary(this, it) },
                playlists = playlists,
                openedPlaylist = openedPlaylist,
                onOpenPlaylist = { openedPlaylistId = it },
                onPlayPlaylistAt = { i -> openedPlaylistId?.let { Lyra.playPlaylist(this, it, i) } },
                onRemoveFromPlaylist = { i -> openedPlaylistId?.let { Lyra.removeFromPlaylist(it, i) } },
                onRenamePlaylist = { name -> openedPlaylistId?.let { Lyra.renamePlaylist(it, name) } },
                onDeletePlaylist = { openedPlaylistId?.let { Lyra.deletePlaylist(it) } },
                onSaveQueue = { Lyra.saveQueueAsPlaylist(it) },
                downloads = downloads,
                onDownload = { Lyra.downloadTrack(this, it) }
            )

            // Go xong ngung mot chut la tu tim, khoi phai bam phim tim
            DebouncedSearch(searchQuery) { Lyra.search(it) }

            // Man hinh soan phu len tren, khong phai mot Activity rieng: no chi
            // la mot trang thai cua man hinh nay, va thoat ra thi ve dung cho cu
            draft?.let { current ->
                LyricEditor(
                    initial = current,
                    accent = androidx.compose.ui.graphics.Color(0xFF6D28D9),
                    songLabel = now?.let { n ->
                        if (n.artist.isNotEmpty()) n.artist + " — " + n.title else n.title
                    } ?: "Chưa phát bài nào",
                    onSave = {
                        Lyra.saveManualLyrics(it)
                        draft = null
                    },
                    onCancel = { draft = null }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        readPermissions()
        // Nguoi dung vua bat quyen xong quay lai - doc lai ngay, khong bat doi
        if (hasNotificationAccess) Lyra.refresh(this)
    }

    /**
     * Xin he thong them o "Loi noi" vao bang Cai dat nhanh.
     *
     * Truoc Android 13 khong co duong nao: app khong duoc tu them o, va dung
     * ra la vay - bang Cai dat nhanh la cho cua nguoi dung. Tu 13 tro len he
     * thong hien mot hop thoai de ho dong y bang mot cham, va do la muc do
     * dung: app hoi, nguoi dung quyet.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAddTile() {
        val manager = getSystemService(StatusBarManager::class.java) ?: return
        runCatching {
            manager.requestAddTileService(
                ComponentName(this, LyraTileService::class.java),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_tile),
                { it.run() },
                {}
            )
        }
    }

    private fun readPermissions() {
        hasNotificationAccess =
            NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        canDrawOverlay = Settings.canDrawOverlays(this)

        val had = canReadLibrary
        canReadLibrary = ContextCompat.checkSelfPermission(this, audioPermission) ==
            PackageManager.PERMISSION_GRANTED
        // Vua cap quyen o man hinh Cai dat roi quay lai - doc thu vien ngay,
        // khong bat nguoi dung phai bam them mot lan nua
        if (canReadLibrary && !had) Lyra.loadLibrary(this)
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
