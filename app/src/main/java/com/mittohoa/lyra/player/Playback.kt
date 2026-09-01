package com.mittohoa.lyra.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.Player
import com.mittohoa.lyra.sources.MediaKind
import com.mittohoa.lyra.sources.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.common.util.concurrent.MoreExecutors

/**
 * Duong noi tu giao dien toi bo may phat.
 *
 * Dung `MediaController` chu khong giu thang `ExoPlayer`, du ca hai nam cung
 * mot tien trinh. Ly do: he thong co the giet va dung lai service bat cu luc
 * nao, va `MediaController` tu noi lai duoc - con mot tham chieu thang toi
 * `ExoPlayer` thi sau lan giet do la mot tham chieu tro vao hu vo.
 *
 * Noi la KHONG DONG BO: `play()` goi truoc khi noi xong thi bai duoc xep vao
 * `pending` roi phat ngay khi noi duoc. Bat nguoi dung doi mot ket noi noi bo
 * xong moi cho bam nut la de lo mot chi tiet ky thuat khong lien quan gi toi ho.
 */
object Playback {

    private var controller: MediaController? = null
    private var connecting = false
    private var pending: (MediaController.() -> Unit)? = null

    /** Bai dang phat, giu de con dung lai the media khi loi doi cau. */
    private var current: Playable? = null

    /**
     * Hang doi hien tai, theo dung thu tu trong bo giai ma.
     *
     * Giu ban sao rieng vi bo giai ma chi nho `MediaItem` - ma trong do phan mo
     * ta da bi ta thay bang cau dang hat. Danh sach nay moi la ten bai that.
     */
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queueFlow: StateFlow<List<Track>> = _queue.asStateFlow()
    private var queue: List<Track>
        get() = _queue.value
        set(value) { _queue.value = value }

    /**
     * Bai Lyra dang phat, NGUYEN BAN.
     *
     * Doc o day chu khong doc `player.mediaMetadata`: phan mo ta cua phien da
     * bi ta thay bang cau dang hat de hien tren the man hinh khoa. Day moi la
     * ten bai that.
     */
    val currentTrack: Playable?
        get() {
            val c = controller
            if (c != null && c.mediaItemCount > 0) {
                queue.getOrNull(c.currentMediaItemIndex)?.let { return it.toPlayable() }
            }
            return current
        }

    /** Vi tri bai dang phat trong hang doi, -1 khi khong co gi. */
    val queueIndex: Int get() = controller?.takeIf { it.mediaItemCount > 0 }?.currentMediaItemIndex ?: -1

    private val handler = Handler(Looper.getMainLooper())

    /** Mot bai co the phat, du tu trong may hay tu nguon online. */
    data class Playable(
        val id: String,
        val title: String,
        val artist: String,
        val uri: String,
        val artworkUri: String? = null,
        val durationMs: Long = 0,
        val kind: MediaKind = MediaKind.AUDIO
    )

    /**
     * Xep ca danh sach vao hang doi roi phat tu bai duoc chon.
     *
     * Xep CA danh sach chu khong chi bai vua bam: bam vao bai thu ba trong ket
     * qua tim thi y nguoi dung gan nhu luon la "phat tu day tro di", khong phai
     * "phat dung mot bai nay roi im". Muon mot bai thi ho bam dung xong.
     */
    fun playQueue(context: Context, tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val index = startIndex.coerceIn(0, tracks.lastIndex)
        queue = tracks
        run(context) {
            current = tracks[index].toPlayable()
            setMediaItems(tracks.map { it.toPlayable().toMediaItem() }, index, 0L)
            prepare()
            play()
        }
    }

    /** Them mot bai vao cuoi hang doi, khong dung bai dang phat. */
    fun enqueue(context: Context, track: Track) {
        queue = queue + track
        run(context) {
            addMediaItem(track.toPlayable().toMediaItem())
            if (!isPlaying && mediaItemCount == 1) {
                current = track.toPlayable()
                prepare()
                play()
            }
        }
    }

    fun skipToIndex(context: Context, index: Int) = run(context) {
        if (index !in 0 until mediaItemCount) return@run
        seekTo(index, 0L)
        play()
    }

    fun next(context: Context) = run(context) { seekToNextMediaItem() }

    /**
     * Bat/tat tron bai.
     *
     * ExoPlayer tron bang mot thu tu NGAM chu khong xao lai danh sach that.
     * Nghia la tat tron di thi hang doi tro lai dung thu tu cu - nguoi dung
     * khong mat cai ho da xep.
     */
    fun toggleShuffle(context: Context) = run(context) {
        shuffleModeEnabled = !shuffleModeEnabled
    }

    /**
     * Doi kieu lap theo vong: tat -> ca hang doi -> mot bai -> tat.
     *
     * Mot nut ba trang thai chu khong phai hai nut. Ba trang thai nay loai tru
     * nhau, va moi dan CD nao cung lam vay - nguoi dung da biet cach dung no tu
     * truoc khi mo app.
     */
    fun cycleRepeat(context: Context) = run(context) {
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun previous(context: Context) = run(context) {
        // Qua 5 giay thi "bai truoc" nghia la ve dau bai dang nghe - dung nhu
        // moi dan CD tu truoc toi nay, va nguoi dung da quen the
        if (currentPosition > 5_000L) seekTo(0L) else seekToPreviousMediaItem()
    }

    fun removeFromQueue(context: Context, index: Int) {
        queue = queue.filterIndexed { i, _ -> i != index }
        run(context) { if (index in 0 until mediaItemCount) removeMediaItem(index) }
    }

    fun play(context: Context, track: Playable) = run(context) {
        current = track
        setMediaItem(track.toMediaItem())
        prepare()
        play()
    }

    fun playPause(context: Context) = run(context) {
        if (isPlaying) pause() else play()
    }

    fun seekTo(context: Context, positionMs: Long) = run(context) { seekTo(positionMs) }

    /**
     * Đổi tốc độ phát. Chỉ áp được cho bộ phát CỦA LYRA.
     *
     * `MediaController` của hệ thống — thứ dùng để bấm nút cho nhạc ở app khác —
     * không có đường nào đổi tốc độ. Nên tính năng này chỉ hiện khi Lyra là bên
     * đang phát; bày một thanh trượt không làm gì cho nhạc ở Spotify thì tệ hơn
     * là không bày.
     */
    fun datTocDo(context: Context, tocDo: Float) = run(context) { setPlaybackSpeed(tocDo) }

    fun stop(context: Context) = run(context) {
        current = null
        stop()
        clearMediaItems()
    }

    /**
     * Dua cau dang hat len the media - cai the tren man hinh khoa va trong
     * thanh keo xuong.
     *
     * `replaceMediaItem` chu khong `setMediaItem`: cung mot duong dan thi
     * ExoPlayer chi doi phan mo ta, khong dung lai va khong nap lai. Doi bang
     * `setMediaItem` la ngat nhac mot nhip moi cau hat - tuc pha nat chinh bai
     * dang nghe de hien loi cua no.
     *
     * `null` = tra the ve nguyen trang: chua co loi, loi khong co moc, hoac moc
     * dang ngo. Hien mot cau sai voi ve chac chan tren man hinh khoa con te hon
     * la khong hien gi.
     */
    fun showLyricLine(line: String?) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        val index = c.currentMediaItemIndex
        val track = queue.getOrNull(index)?.toPlayable() ?: current ?: return
        runCatching { c.replaceMediaItem(index, track.toMediaItem(line)) }
    }

    /**
     * Vi tri phat hien tai, hoac -1 khi Lyra khong phai la ben dang phat.
     *
     * -1 chu khong phai 0: 0 la mot vi tri hop le, va tra 0 khi khong biet thi
     * ben goi khong the phan biet duoc "dang o dau bai" voi "khong biet".
     */
    fun positionOrUnknown(): Long {
        val c = controller ?: return -1
        return if (c.isPlaying) c.currentPosition else -1
    }

    /** Goi khi thoat han - vd. app bi dong. */
    fun release() {
        controller?.release()
        controller = null
        pending = null
        current = null
    }

    /**
     * Chay mot viec tren bo dieu khien, noi truoc neu chua noi.
     *
     * Chi giu MOT viec dang cho: neu nguoi dung bam ba bai lien tiep truoc khi
     * noi xong thi cai ho bam CUOI CUNG moi la cai ho muon.
     */
    private fun run(context: Context, block: MediaController.() -> Unit) {
        controller?.let {
            block(it)
            return
        }

        pending = block
        if (connecting) return
        connecting = true

        val token = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, LyraPlaybackService::class.java)
        )
        val future = MediaController.Builder(context.applicationContext, token).buildAsync()
        future.addListener({
            connecting = false
            try {
                val c = future.get()
                controller = c
                pending?.let { work ->
                    pending = null
                    handler.post { work(c) }
                }
            } catch (e: Exception) {
                pending = null
                Log.w(TAG, "Khong noi duoc vao bo may phat", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun Track.toPlayable() = Playable(
        kind = kind,
        id = playbackUri,
        title = title,
        artist = artist,
        uri = playbackUri,
        artworkUri = artworkUrl,
        durationMs = durationMs
    )

    /**
     * Dong dau cua the media la CAU DANG HAT khi co, khong phai ten bai.
     *
     * Do la cho chu to nhat va sang nhat tren the, va cau hat moi la thu nguoi
     * ta doc - ten bai thi ho da biet tu luc bam phat. Ten bai va nghe si don
     * xuong dong duoi, khong mat di.
     */
    private fun Playable.toMediaItem(line: String? = null): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(line?.takeIf { it.isNotBlank() } ?: title)
                .setArtist(
                    if (line.isNullOrBlank()) artist
                    else listOf(title, artist).filter { it.isNotBlank() }.joinToString(" — ")
                )
                .setArtworkUri(artworkUri?.let(Uri::parse))
                // He thong doc co nay de biet day la nhac chu khong phai
                // podcast hay sach noi - the tren man hinh khoa hien khac nhau
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()

    private const val TAG = "LyraPhat"
}
