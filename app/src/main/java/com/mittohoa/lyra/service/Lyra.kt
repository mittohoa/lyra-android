package com.mittohoa.lyra.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.LyricsRepository
import com.mittohoa.lyra.media.MediaSessionWatcher
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.overlay.OverlayHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Trang thai dung chung cua ca app.
 *
 * Phai la singleton ngoai service: `NotificationListenerService` bi he thong
 * dung len roi giet di theo y no, con man hinh Cai dat thi can doc cung mot
 * dong du lieu ay. De trang thai trong service thi moi lan he thong dung lai
 * la mat sach.
 */
object Lyra {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    val watcher = MediaSessionWatcher()
    val lyricsRepo = LyricsRepository(scope)
    val overlay = OverlayHost()

    val now: StateFlow<NowPlaying?> get() = watcher.now
    val lyrics: StateFlow<Lyrics> get() = lyricsRepo.lyrics
    val loading: StateFlow<Boolean> get() = lyricsRepo.loading

    private var wired = false

    /** Nhip ve lai khung noi. 10 lan/giay du muot ma khong ton pin. */
    private const val TICK_MS = 100L

    private val tick = object : Runnable {
        override fun run() {
            if (overlay.isShowing) {
                overlay.update { setPosition(watcher.livePosition()) }
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    /**
     * Noi cac manh lai voi nhau. Goi duoc nhieu lan, chi lam that mot lan.
     *
     * Tach khoi `refresh` vi hai viec khac nhau: cai nay noi day, con kia mo
     * lai duong doc phien media sau khi nguoi dung vua cap quyen.
     */
    private fun wire() {
        if (wired) return
        wired = true

        scope.launch {
            watcher.now.collect { now ->
                lyricsRepo.onNowPlaying(now)
                overlay.update {
                    setIdleText(
                        when {
                            now == null -> "Chưa phát bài nào"
                            now.artist.isNotEmpty() -> "${now.artist} — ${now.title}"
                            else -> now.title
                        }
                    )
                }
            }
        }

        scope.launch {
            lyricsRepo.lyrics.collect { lyrics ->
                overlay.update { setLyrics(lyrics.lines, lyrics.offset) }
            }
        }
    }

    /**
     * Thu doc phien media ngay, khong doi he thong noi vao service.
     *
     * Dung cho man hinh chinh: nguoi dung vua bat quyen xong quay lai app thi
     * thay ket qua luon, khong phai doi.
     */
    fun refresh(context: Context) {
        wire()
        watcher.start(context.applicationContext, LyraNotificationListener::class.java)
    }

    fun showOverlay(context: Context) {
        wire()
        overlay.show(context.applicationContext)
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    fun hideOverlay() {
        handler.removeCallbacks(tick)
        overlay.hide()
    }

    fun toggleOverlay(context: Context): Boolean {
        if (overlay.isShowing) hideOverlay() else showOverlay(context)
        return overlay.isShowing
    }
}
