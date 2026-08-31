package com.mittohoa.lyra.service

import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.mittohoa.lyra.data.LyricCache
import com.mittohoa.lyra.data.ManualLyricStore
import com.mittohoa.lyra.data.OffsetStore
import com.mittohoa.lyra.data.Playlist
import com.mittohoa.lyra.data.PlaylistStore
import com.mittohoa.lyra.data.OverlayPrefs
import com.mittohoa.lyra.data.TranslatePrefs
import com.mittohoa.lyra.data.TranslateSettings
import com.mittohoa.lyra.data.TranslationCache
import com.mittohoa.lyra.data.UpdateChecker
import com.mittohoa.lyra.update.ApkInstaller
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.lyrics.LyricsRepository
import com.mittohoa.lyra.media.MediaSessionWatcher
import com.mittohoa.lyra.media.NowPlaying
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.mittohoa.lyra.overlay.OverlayHost
import com.mittohoa.lyra.download.DownloadResult
import com.mittohoa.lyra.download.Downloads
import com.mittohoa.lyra.player.Artwork
import com.mittohoa.lyra.player.Playback
import com.mittohoa.lyra.sources.Catalog
import com.mittohoa.lyra.sources.LocalLibrary
import com.mittohoa.lyra.sources.Track
import com.mittohoa.lyra.translate.TranslationRepository
import com.mittohoa.lyra.translate.TranslationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private const val TAG = "LyraNoi"

    /** Ten goi gia cho bai do chinh Lyra phat - de phan biet voi app khac. */
    private const val OWN = "lyra"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    val watcher = MediaSessionWatcher()

    /**
     * Bo may phat cua CHINH Lyra, khi no dang chay.
     *
     * Khong dung de biet dang phat bai gi - viec do van la cua `watcher`, va
     * phien cua Lyra hien ra o do y het moi app khac. Cai duy nhat lay tu day
     * la VI TRI PHAT: hoi thang bo giai ma thi dung tung mili-giay, con qua
     * ban tin media thi luon la mot con so suy ra tu lan bao gan nhat.
     */
    private var localPlayer: Player? = null

    /**
     * Bai chinh Lyra dang phat.
     *
     * Dung mot dong RIENG chu khong di qua `watcher`: phien media cua Lyra da
     * bi ta thay phan mo ta bang cau dang hat, nen doc lai chinh no la doc lai
     * ket qua cua minh. Duong nay lay thang tu bo may phat - ten bai, nghe si va
     * do dai deu la ban goc tu nguon nhac, khong phai doan tu mot chuoi tho.
     */
    private val localNow = MutableStateFlow<NowPlaying?>(null)
    val overlay = OverlayHost()

    /**
     * Bo nho dem gan sau, vi no can Context ma singleton thi khong co.
     * Chua gan thi van chay duoc, chi la lan nao cung phai goi mang.
     */
    private var cache: LyricCache? = null
    private var offsets: OffsetStore? = null
    private var manual: ManualLyricStore? = null
    private var overlayPrefs: OverlayPrefs? = null
    private var translationCache: TranslationCache? = null
    private var translatePrefs: TranslatePrefs? = null
    private var playlistStore: PlaylistStore? = null
    private var translationRepoOrNull: TranslationRepository? = null
    private var lyricsRepoOrNull: LyricsRepository? = null

    private val lyricsRepo: LyricsRepository
        get() = lyricsRepoOrNull
            ?: LyricsRepository(scope, cache, offsets, manual).also { lyricsRepoOrNull = it }

    private val translationRepo: TranslationRepository
        get() = translationRepoOrNull
            ?: TranslationRepository(scope, translationCache, translatePrefs)
                .also { translationRepoOrNull = it }

    private val _now = MutableStateFlow<NowPlaying?>(null)

    /**
     * Bai dang phat, du la Lyra tu phat hay mot app khac phat.
     *
     * Lyra dang phat thi Lyra thang: no la ben nguoi dung vua bam nut. Lyra
     * tam dung ma app khac dang phat thi nhuong - nguoi ta chuyen sang nghe cho
     * khac roi, va lyra khong nen bam lay man hinh.
     */
    val now: StateFlow<NowPlaying?> = _now.asStateFlow()
    val lyrics: StateFlow<Lyrics> get() = lyricsRepo.lyrics
    val loading: StateFlow<Boolean> get() = lyricsRepo.loading
    val translation: StateFlow<TranslationState> get() = translationRepo.state
    val translateSettings: StateFlow<TranslateSettings> get() = translationRepo.settings

    fun attachPlayer(player: Player) {
        localPlayer = player
        wire()

        // Bat lai nhip moi lan bat dau phat. Khong co cai nay thi nhip chi song
        // theo khung noi: bo may phat duoc dung len TRUOC khi co bai nao, luc do
        // `isPlaying` con la false, va neu khung noi dang tat thi nhip tat ngay
        // sau vong dau - roi khong con gi danh thuc no day nua.
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                refreshLocalNow()
                if (isPlaying) startTick()
            }

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                // Doi bai that su, khong phai lan thay phan mo ta de hien loi
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    cardLine = null
                    refreshLocalNow()
                }
            }

            override fun onPlaybackStateChanged(state: Int) = refreshLocalNow()

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                _shuffle.value = enabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                _repeat.value = mode
            }
        })
        startTick()
    }

    fun detachPlayer() {
        localPlayer = null
    }

    /**
     * Vi tri phat hien tai.
     *
     * Lyra dang tu phat thi hoi thang bo giai ma cua minh; con lai thi hoi qua
     * ban tin media nhu cu. Phai goi tren luong chinh - `Player` chi cho doc o
     * dung luong da dung no.
     */
    fun livePosition(): Long {
        val player = localPlayer
        if (player != null && player.isPlaying) return player.currentPosition
        return watcher.livePosition()
    }

    // ---- Tim bai va hang doi ----

    private val _results = MutableStateFlow<List<Track>>(emptyList())
    val results: StateFlow<List<Track>> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    /** Vi tri bai dang phat trong hang doi; -1 khi Lyra khong phat gi. */
    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    val queue: StateFlow<List<Track>> get() = Playback.queueFlow

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    /** 0 = tat, 1 = lap ca hang doi, 2 = lap mot bai (theo Media3). */
    private val _repeat = MutableStateFlow(0)
    val repeat: StateFlow<Int> = _repeat.asStateFlow()

    /** Anh bia cua bai dang phat; null khi chua tai xong hoac khong co. */
    private val _artwork = MutableStateFlow<Bitmap?>(null)
    val artwork: StateFlow<Bitmap?> = _artwork.asStateFlow()

    private var artworkJob: Job? = null

    fun toggleShuffle(context: Context) = Playback.toggleShuffle(context)

    fun cycleRepeat(context: Context) = Playback.cycleRepeat(context)

    fun seekTo(context: Context, positionMs: Long) = Playback.seekTo(context, positionMs)

    private var searchJob: Job? = null

    /**
     * Tim bai o ca hai nguon.
     *
     * Huy lan tim truoc: nguoi dung go them chu la cau hoi da khac, va ket qua
     * cua cau hoi cu ve sau lai de len cau moi thi danh sach nhay lung tung.
     */
    // ---- Ban moi ----

    private val _banMoi = MutableStateFlow<UpdateChecker.BanMoi?>(null)
    val banMoi: StateFlow<UpdateChecker.BanMoi?> = _banMoi.asStateFlow()

    private var daKiemBanMoi = false

    /**
     * Hoi xem co ban moi khong. Chi hoi MOT lan moi lan mo app.
     *
     * Ban moi ra vai tuan mot lan, nen hoi lai moi lan nguoi dung quay ve man
     * hinh chinh la mot lan goi mang khong ai yeu cau.
     */
    fun kiemBanMoi(phienBanDangChay: String) {
        if (daKiemBanMoi) return
        daKiemBanMoi = true
        scope.launch { _banMoi.value = UpdateChecker.kiem(phienBanDangChay) }
    }

    /** Tien trinh tai ban moi: 0..100, -1 khi khong biet do dai, null khi khong tai. */
    private val _tienDoCapNhat = MutableStateFlow<Int?>(null)
    val tienDoCapNhat: StateFlow<Int?> = _tienDoCapNhat.asStateFlow()

    /** Ban dung nay tu tai va cai ban moi duoc khong. */
    val tuCaiDuoc: Boolean get() = ApkInstaller.SUPPORTED

    fun duocPhepCai(context: Context): Boolean = ApkInstaller.duocPhepCai(context)

    fun moTrangCapQuyenCai(context: Context) = ApkInstaller.moTrangCapQuyen(context)

    /**
     * Tai ban moi ve roi giao cho he thong cai.
     *
     * Chua duoc cap quyen cai dat thi mo thang trang cai dat de nguoi dung bat -
     * hon la bao mot loi ma ho khong biet phai lam gi.
     */
    fun taiVaCaiBanMoi(context: Context) {
        val ban = _banMoi.value ?: return
        if (_tienDoCapNhat.value != null) return

        if (!ApkInstaller.duocPhepCai(context)) {
            ApkInstaller.moTrangCapQuyen(context)
            return
        }

        val app = context.applicationContext
        _tienDoCapNhat.value = 0
        scope.launch {
            val loi = ApkInstaller.taiVaCai(app, ban.duongTai) { phanTram ->
                _tienDoCapNhat.value = phanTram
            }
            _tienDoCapNhat.value = null
            if (loi != null) Log.w(TAG, "Cap nhat that bai: $loi")
        }
    }

    // ---- Tai xuong ----

    /** Trang thai tai cua tung bai, khoa theo `Track.playbackUri`. */
    sealed interface Downloading {
        /** `percent` = -1 khi nguon khong noi truoc do dai. */
        data class Working(val percent: Int) : Downloading
        data object Done : Downloading
        data class Failed(val why: String) : Downloading
    }

    private val _downloads = MutableStateFlow<Map<String, Downloading>>(emptyMap())
    val downloads: StateFlow<Map<String, Downloading>> = _downloads.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    /**
     * Tai mot bai ve may, kem loi neu tim duoc.
     *
     * Loi duoc tra RIENG chu khong lay tu `lyricsRepo`: bai duoc tai co the
     * khong phai bai dang phat - nguoi dung bam tai ngay tren danh sach ket qua.
     * Tra khong ra loi thi van tai nhac; mot bai khong loi van hon la khong co
     * bai nao.
     */
    /** Ban dung nay co tai nhac hay khong. Giao dien doc de an han nut di. */
    val downloadsSupported: Boolean get() = Downloads.SUPPORTED

    fun downloadTrack(context: Context, track: Track) {
        if (!Downloads.SUPPORTED) return
        val key = track.playbackUri
        if (downloadJobs[key]?.isActive == true) return

        val app = context.applicationContext
        _downloads.value = _downloads.value + (key to Downloading.Working(0))

        downloadJobs[key] = scope.launch {
            val lyrics = lyricsFor(track)
            val result = Downloads.download(app, track, lyrics) { percent ->
                _downloads.value = _downloads.value + (key to Downloading.Working(percent))
            }
            _downloads.value = _downloads.value + when (result) {
                is DownloadResult.Done -> key to Downloading.Done
                is DownloadResult.Failed -> key to Downloading.Failed(result.why)
            }
            if (result is DownloadResult.Done) {
                // Bai vua tai nam trong Music/Lyra, tuc no da thuoc thu vien
                // trong may - doc lai de no hien ra ngay
                loadLibrary(app)
            }
            downloadJobs.remove(key)
        }
    }

    /**
     * Loi dang .lrc cho mot bai, de nhung vao file tai ve.
     *
     * Uu tien loi nguoi dung tu nhap: ho da bo cong sua thi ban tai ve phai
     * mang chinh ban do, khong phai ban may tu do lai.
     */
    private suspend fun lyricsFor(track: Track): String? {
        manual?.get(track.artist, track.title)?.let { return it }
        val found = cache?.get(track.artist, track.title)
            ?: lyricsRepo.lookup(track.artist, track.title, track.durationMs)
        if (found == null || found.isEmpty || !found.synced) return found?.plainText()
        return found.lines.joinToString(System.lineSeparator()) { line ->
            "[%02d:%02d.%02d]%s".format(
                line.time / 60_000,
                (line.time / 1000) % 60,
                (line.time % 1000) / 10,
                line.text
            )
        }
    }

    // ---- Danh sach phat ----

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    /**
     * Luu hang doi dang co thanh mot danh sach.
     *
     * Hang doi la cho lam viec - xep vao, bo ra, nghe xong roi thoi. Danh sach
     * phat la thu giu lai. Mot nut noi hai cai do lai la duong ngan nhat tu
     * "toi vua xep duoc mot chuoi hay" toi "toi muon nghe lai chuoi nay".
     */
    fun saveQueueAsPlaylist(name: String): String? {
        val tracks = Playback.queueFlow.value
        if (tracks.isEmpty()) return null
        return playlistStore?.create(name, tracks)
    }

    fun renamePlaylist(id: String, name: String) = playlistStore?.rename(id, name)

    fun deletePlaylist(id: String) = playlistStore?.delete(id)

    fun addToPlaylist(id: String, track: Track) = playlistStore?.add(id, track)

    fun removeFromPlaylist(id: String, index: Int) = playlistStore?.removeAt(id, index)

    /** Phat ca danh sach tu mot bai. */
    fun playPlaylist(context: Context, id: String, index: Int = 0) {
        val tracks = playlistStore?.byId(id)?.tracks ?: return
        Playback.playQueue(context, tracks, index)
    }

    private val _library = MutableStateFlow<List<Track>>(emptyList())
    val library: StateFlow<List<Track>> = _library.asStateFlow()

    /**
     * Doc nhac trong may.
     *
     * Goi lai duoc nhieu lan - vd. ngay sau khi nguoi dung vua cap quyen, hoac
     * khi ho quay lai app sau khi tai them nhac. Doc lai ca thu vien re hon
     * nhieu so voi theo doi tung thay doi cua `MediaStore`.
     */
    fun loadLibrary(context: Context) {
        val app = context.applicationContext
        scope.launch {
            val found = LocalLibrary.all(app)
            _library.value = found
            Catalog.library = found
            Log.i(TAG, "Thu vien trong may: ${found.size} bai")
        }
    }

    /** Phat ca thu vien tu mot bai. */
    fun playFromLibrary(context: Context, index: Int) =
        Playback.playQueue(context, _library.value, index)

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            _searching.value = false
            return
        }
        _searching.value = true
        searchJob = scope.launch {
            val found = Catalog.search(query)
            _results.value = found
            _searching.value = false
            Log.i(TAG, "Tim \"$query\": ${found.size} ket qua")
        }
    }

    fun playFromResults(context: Context, index: Int) =
        Playback.playQueue(context, _results.value, index)

    fun enqueue(context: Context, track: Track) = Playback.enqueue(context, track)

    fun skipInQueue(context: Context, index: Int) = Playback.skipToIndex(context, index)

    fun removeFromQueue(context: Context, index: Int) = Playback.removeFromQueue(context, index)

    fun next(context: Context) = Playback.next(context)

    fun previous(context: Context) = Playback.previous(context)

    fun playPause(context: Context) = Playback.playPause(context)

    private var wired = false

    /** Nhip ve lai khung noi. 10 lan/giay du muot ma khong ton pin. */
    private const val TICK_MS = 100L

    /**
     * Cau da dua len the media lan truoc.
     *
     * Giu lai de chi cap nhat khi DOI CAU. The media di qua he thong toi giao
     * dien khoa man hinh, va day mot ban mo ta moi 10 lan mot giay la lam phien
     * ca ba tang do de doi mot dong chu vai giay moi thay.
     */
    private var cardLine: String? = null

    private val tick = object : Runnable {
        override fun run() {
            val position = livePosition()
            if (overlay.isShowing) overlay.update { setPosition(position) }
            pushLineToCard(position)

            // Chay tiep chung nao con viec de lam. Truoc day nhip chi song theo
            // khung noi; gio Lyra tu phat duoc, va luc do the media van can duoc
            // cap nhat du khung noi dang tat.
            if (overlay.isShowing || localPlayer?.isPlaying == true) {
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    /** Doc lai bai dang phat tu bo may phat cua chinh Lyra. */
    private fun refreshLocalNow() {
        val player = localPlayer
        val track = Playback.currentTrack
        _queueIndex.value = Playback.queueIndex
        loadArtwork(track?.artworkUri)
        localNow.value = if (player == null || track == null ||
            player.playbackState == Player.STATE_IDLE
        ) {
            null
        } else {
            NowPlaying(
                packageName = OWN,
                title = track.title,
                artist = track.artist,
                album = "",
                duration = player.duration.coerceAtLeast(0L),
                position = player.currentPosition,
                isPlaying = player.isPlaying
            )
        }
    }

    /**
     * Tai anh bia cua bai dang phat.
     *
     * Huy lan tai truoc khi doi bai: anh cua bai cu ve sau ma van gan vao thi
     * man hinh hien bia mot dang, ten bai mot neo - va cang de xay ra khi doi
     * bai nhanh, tuc dung luc nguoi dung dang luot qua hang doi.
     */
    private fun loadArtwork(url: String?) {
        if (url == artworkUrl) return
        artworkUrl = url
        artworkJob?.cancel()
        _artwork.value = null
        if (url == null) return
        val context = appContext ?: return
        artworkJob = scope.launch { _artwork.value = Artwork.load(context, url) }
    }

    private var artworkUrl: String? = null
    private var appContext: Context? = null

    /** Bat nhip neu chua chay. Goi duoc nhieu lan. */
    private fun startTick() {
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    /**
     * Dua cau dang hat len the media cua chinh Lyra.
     *
     * Chi lam khi CHINH LYRA dang phat. Nhac o app khac thi the media la cua ho,
     * ta khong ghi vao duoc - va do la dung: khong app nao duoc phep sua the cua
     * app khac.
     *
     * Moc dang ngo thi khong dua gi len. Tren man hinh khoa nguoi dung khong co
     * cach nao doi chieu xem cau do co dung khong, nen mot cau sai o day dang
     * tin hon han mot cau sai tren khung noi - va vi the tai hai hon.
     */
    private fun pushLineToCard(position: Long) {
        val player = localPlayer
        if (player == null || !player.isPlaying) {
            cardLine = null
            return
        }

        val lyrics = lyricsRepoOrNull?.lyrics?.value
        if (lyrics == null || !lyrics.synced || lyrics.timingSuspect) {
            if (cardLine == null) return
            cardLine = null
            Playback.showLyricLine(null)
            return
        }

        val index = activeLineIndex(lyrics.lines, position, lyrics.offset)
        val line = lyrics.lines.getOrNull(index)?.text?.takeIf { it.isNotBlank() }

        // Dong trong giua hai doan thi GIU NGUYEN cau vua hat, khong tra the ve
        // ten bai. File .lrc nao cung co nhung dong trong nhu vay, va tra ve roi
        // hien lai cu vai giay mot lan bien the tren man hinh khoa thanh mot cho
        // nhap nhay - trong khi cai nguoi ta muon chi la doc duoc cau vua nghe.
        if (line == null || line == cardLine) return

        cardLine = line
        cardLine = line
        Playback.showLyricLine(line)
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
            combine(watcher.now, localNow) { external, local ->
                when {
                    local != null && local.isPlaying -> local
                    external != null -> external
                    else -> local
                }
            }.collect { _now.value = it }
        }

        scope.launch {
            _now.collect { now ->
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
                translationRepo.onLyrics(lyrics)
            }
        }

        // Ban dich di duong RIENG toi khung noi, khong gop vao `setLyrics`:
        // no toi sau loi vai tram mili-giay, va khong duoc de khung trong
        // trong luc cho.
        scope.launch {
            translationRepo.state.collect { state ->
                val lines = (state as? TranslationState.Done)?.lines ?: emptyList()
                overlay.update { setTranslations(lines) }
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
        appContext = context.applicationContext
        if (cache == null) cache = LyricCache(context.applicationContext)
        if (offsets == null) offsets = OffsetStore(context.applicationContext)
        if (manual == null) manual = ManualLyricStore(context.applicationContext)
        if (translationCache == null) translationCache = TranslationCache(context.applicationContext)
        if (translatePrefs == null) translatePrefs = TranslatePrefs(context.applicationContext)
        if (playlistStore == null) {
            playlistStore = PlaylistStore(context.applicationContext).also {
                scope.launch { it.playlists.collect { list -> _playlists.value = list } }
            }
        }
        wire()
        watcher.start(context.applicationContext, LyraNotificationListener::class.java)
        loadLibrary(context)
    }

    fun showOverlay(context: Context) {
        wire()
        // Cham vao mot cau tren khung noi = can lai loi theo cau dang nghe.
        // Gan o day chu khong o `wire`: khung noi co the bi dung roi dung lai
        // nhieu lan trong mot phien.
        overlay.onLineTap = { index -> lyricsRepo.syncToLine(index, livePosition()) }
        // Giu tay tren khung = tat khung. Gan cung cho voi `onLineTap` va vi
        // cung mot ly do: khung co the bi dung roi dung lai nhieu lan.
        overlay.onDismiss = { hideOverlay() }
        overlay.show(context.applicationContext)
        startTick()

        // Chi ghi la "dang bat" khi dung duoc that. Thieu quyen ve de len app
        // khac thi `show` lang le khong lam gi, va ghi bua se thanh mot lan thu
        // dung khung vo ich moi lan he thong noi lai service.
        if (overlay.isShowing) prefs(context).setEnabled(true)
    }

    fun hideOverlay() {
        handler.removeCallbacks(tick)
        overlay.hide()
        overlayPrefs?.setEnabled(false)
    }

    /** Nguoi dung cham vao dong dang hat de can lai ca bai. */
    fun syncToLine(index: Int) = lyricsRepo.syncToLine(index, livePosition())

    /** Bo do lech da chinh. */
    fun clearOffset() = lyricsRepo.clearOffset()

    /** Nguoi dung dong y tai goi ngon ngu ve may. */
    fun downloadTranslationModel() = translationRepo.downloadAndTranslate()

    fun updateTranslateSettings(settings: TranslateSettings) =
        translationRepo.update(settings)

    /** Luu loi nguoi dung tu go hoac dan vao. */
    fun saveManualLyrics(raw: String) = lyricsRepo.saveManual(raw)

    /** Chuoi de mo ra sua - loi da nhap, hoac loi dang co de sua lai. */
    fun manualDraft(): String = lyricsRepo.manualDraft()

    private fun prefs(context: Context): OverlayPrefs =
        overlayPrefs ?: OverlayPrefs(context.applicationContext).also { overlayPrefs = it }

    /**
     * Dung lai khung noi sau khi he thong giet roi noi lai tien trinh.
     *
     * Goi tu `onListenerConnected`. Khong phai luc nao cung dung: chi khi lan
     * truoc nguoi dung that su dang bat no.
     */
    fun restoreOverlay(context: Context) {
        if (overlay.isShowing) return
        if (!prefs(context).isEnabled()) {
            Log.i(TAG, "Lan truoc nguoi dung tat khung noi - khong dung lai")
            return
        }
        showOverlay(context)
        Log.i(TAG, "Da dung lai khung noi: dang hien = ${overlay.isShowing}")
    }

    fun toggleOverlay(context: Context): Boolean {
        if (overlay.isShowing) hideOverlay() else showOverlay(context)
        return overlay.isShowing
    }
}
