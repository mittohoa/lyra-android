package com.mittohoa.lyra.service

import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.mittohoa.lyra.data.LyricCache
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.data.LyricEffectPrefs
import com.mittohoa.lyra.data.ManualLyricStore
import com.mittohoa.lyra.data.SaoLuuLoi
import com.mittohoa.lyra.data.OffsetStore
import com.mittohoa.lyra.data.Playlist
import com.mittohoa.lyra.data.PlaylistStore
import com.mittohoa.lyra.data.OverlayPrefs
import com.mittohoa.lyra.data.TranslatePrefs
import com.mittohoa.lyra.data.TranslateSettings
import com.mittohoa.lyra.data.TranslationCache
import com.mittohoa.lyra.data.UpdateChecker
import com.mittohoa.lyra.update.ApkInstaller
import androidx.core.content.res.ResourcesCompat
import com.mittohoa.lyra.data.ChuDePrefs
import com.mittohoa.lyra.data.KieuChu
import com.mittohoa.lyra.R
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.lyrics.LrcCanhTep
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
import com.mittohoa.lyra.sources.LrclibPublish
import com.mittohoa.lyra.sources.Track
import com.mittohoa.lyra.translate.TranslationRepository
import com.mittohoa.lyra.translate.TranslationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
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

    /** Ten goi gia cho bai chi XEM LOI, khong phat. */
    private const val XEM = "lyra-xem"

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
            ?: LyricsRepository(scope, cache, offsets, manual, ::loiCanhTep)
                .also { lyricsRepoOrNull = it }

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

    /** Ảnh bìa Lyra tự tải về cho bài CHÍNH NÓ phát. */
    private val _artwork = MutableStateFlow<Bitmap?>(null)

    /**
     * Ảnh bìa dùng được cho bài đang phát — null khi bài không phải của Lyra.
     *
     * `_artwork` chỉ được nạp lại khi bộ phát của Lyra đổi bài, nên nó SỐNG DAI
     * hơn lượt phát của Lyra: nghe một bài trong Lyra rồi chuyển sang Zing thì
     * ảnh cũ vẫn còn nguyên trong đó. Màn hình Đang phát ưu tiên ảnh này hơn
     * ảnh kèm bản tin media, nên nó hiện bìa của bài TRƯỚC bên cạnh tên bài
     * MỚI — và màu nền của cả app cũng lấy từ đúng cái bìa sai đó.
     *
     * Lọc ngay tại đây thay vì bắt từng màn hình tự nhớ kiểm tra: chỉ cần một
     * chỗ quên là lỗi quay lại.
     */
    val artwork: StateFlow<Bitmap?> =
        combine(_artwork, _now) { bia, dangPhat ->
            if (dangPhat?.packageName == OWN) bia else null
        }.stateIn(scope, SharingStarted.Eagerly, null)

    private var artworkJob: Job? = null

    fun toggleShuffle(context: Context) = Playback.toggleShuffle(context)

    fun cycleRepeat(context: Context) = Playback.cycleRepeat(context)

    fun seekTo(context: Context, positionMs: Long) {
        if (tuPhat()) Playback.seekTo(context, positionMs)
        else watcher.dieuKhien { seekTo(positionMs) }
    }

    /**
     * Lyra co phai la ben dang phat khong.
     *
     * Quyet dinh moi nut bam di duong nao: bo phat cua chinh minh, hay bo phat
     * cua app khac qua `MediaController`.
     */
    private fun tuPhat(): Boolean = localPlayer?.isPlaying == true

    /**
     * Nhac o app khac co dieu khien duoc khong.
     *
     * Truoc day ca app tin la KHONG - co han mot dong chu thich trong
     * `PlayerPane` noi "khong app nao dieu khien duoc bo phat cua app khac", va
     * trang Dang phat giau het nut khi nhac o app khac. Sai: quyen doc thong
     * bao cho ta cac `MediaController`, moi cai mang mot bo `TransportControls`,
     * va do la duong chinh thuc ma dong ho thong minh va man hinh xe hoi dung.
     */
    fun dieuKhienDuoc(): Boolean = tuPhat() || watcher.dieuKhien { }

    /** Phien hien tai co cho tua khong. Khong phai app nao cung cho. */
    fun tuaDuoc(): Boolean = tuPhat() || watcher.tuaDuoc()

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
        // Ban Play khong hoi gi ca. Play tu lo viec cap nhat, va mot app tren
        // Play ma tu di hoi noi khac roi moi nguoi dung sang do tai la dung vao
        // chinh sach ve phat hanh ngoai cua hang. Chan o day thi R8 cung thay
        // `UpdateChecker` khong ai goi va bo han no khoi ban dung.
        if (!ApkInstaller.SUPPORTED) return
        if (daKiemBanMoi) return
        daKiemBanMoi = true
        scope.launch { _banMoi.value = UpdateChecker.kiem(phienBanDangChay) }
    }

    /**
     * Cai dang xay ra voi viec cap nhat.
     *
     * Phai co mot pha CHO_HE_THONG rieng. Tai xong byte moi la het viec cua
     * Lyra, chua het viec cua may: he thong con chuan bi, va Play Protect con
     * chan lai de GUI CA FILE 20 MB LEN GOOGLE QUET - doan lau nhat trong ca
     * chuoi. Van hien "dang tai 100%" suot doan do thi nguoi dung nhin mot thanh
     * day dung im hang chuc giay, roi ket luan dung theo nhung gi ho thay: treo.
     */
    sealed interface TrangThaiCapNhat {
        /** `phanTram` la -1 khi may chu khong noi truoc do dai. */
        data class DangTai(val phanTram: Int) : TrangThaiCapNhat
        data object ChoHeThong : TrangThaiCapNhat
        data class Hong(val vi: String) : TrangThaiCapNhat
    }

    private val _capNhat = MutableStateFlow<TrangThaiCapNhat?>(null)
    val capNhat: StateFlow<TrangThaiCapNhat?> = _capNhat.asStateFlow()

    /**
     * Hieu ung chu, dung chung cho trang Loi va khung noi.
     *
     * Giu o `Lyra` chu khong o rieng man hinh: khung noi song ngoai vong doi
     * cua Activity, va no can biet lua chon nay ke ca khi khong ai mo app.
     */
    private val _hieuUng = MutableStateFlow(LyricEffect.SANG_DAN)
    val hieuUng: StateFlow<LyricEffect> = _hieuUng.asStateFlow()

    fun datHieuUng(context: Context, effect: LyricEffect) {
        _hieuUng.value = effect
        overlay.effect = effect
        LyricEffectPrefs(context.applicationContext).write(effect)
    }

    /** Doc lua chon da luu. Goi khi dung app hoac dung khung noi. */
    /**
     * Đổi bộ chữ của KHUNG NỔI theo lựa chọn ở trang Chỉnh.
     *
     * Phần app tự đổi qua `LocalBoChu`; khung nổi thì không, vì nó là một
     * `View` thuần nằm trong cửa sổ của `WindowManager`, ngoài cây Compose.
     */
    fun datKieuChu(context: Context, kieu: KieuChu) {
        overlay.update { chuRieng = typefaceCho(context, kieu) }
    }

    fun napKieuChu(context: Context) {
        datKieuChu(context, ChuDePrefs(context.applicationContext).docKieuChu())
    }

    /** `null` = bộ chữ của máy, và đó là một lựa chọn chứ không phải thiếu sót. */
    private fun typefaceCho(context: Context, kieu: KieuChu): android.graphics.Typeface? =
        runCatching {
            when (kieu) {
                KieuChu.SACH -> ResourcesCompat.getFont(context, R.font.newsreader)
                KieuChu.MOT_BO -> ResourcesCompat.getFont(context, R.font.be_vietnam_pro_regular)
                KieuChu.MAY -> null
            }
        }.getOrNull()

    fun napHieuUng(context: Context) {
        val e = LyricEffectPrefs(context.applicationContext).read()
        _hieuUng.value = e
        overlay.effect = e
    }

    /** Ban dung nay tu tai va cai ban moi duoc khong. */
    val tuCaiDuoc: Boolean get() = ApkInstaller.SUPPORTED

    fun duocPhepCai(context: Context): Boolean = ApkInstaller.duocPhepCai(context)

    fun moTrangCapQuyenCai(context: Context) = ApkInstaller.moTrangCapQuyen(context)

    /**
     * He thong bao ket qua ve day, qua `KetQuaCaiDat`.
     *
     * `session.commit` tra ve ngay, con ket qua that toi sau vai chuc giay bang
     * mot ban tin rieng. Truoc day ket qua do chi di vao nhat ky - nen khi
     * Android tu choi (da gap that: "Self update is blocked by unknown source
     * package") thi nguoi dung bam nut xong ngoi nhin mot man hinh khong doi gi,
     * va khong co cach nao biet chuyen gi da xay ra.
     */
    fun ketQuaCaiDat(thanhCong: Boolean, vi: String?) {
        _capNhat.value =
            if (thanhCong) null
            else TrangThaiCapNhat.Hong(vi ?: "Hệ thống từ chối cài bản mới")
    }

    /** Nguoi dung da doc bao loi. */
    fun quenLoiCapNhat() {
        if (_capNhat.value is TrangThaiCapNhat.Hong) _capNhat.value = null
    }

    /**
     * Tai ban moi ve roi giao cho he thong cai.
     *
     * Chua duoc cap quyen cai dat thi mo thang trang cai dat de nguoi dung bat -
     * hon la bao mot loi ma ho khong biet phai lam gi.
     */
    fun taiVaCaiBanMoi(context: Context) {
        val ban = _banMoi.value ?: return
        val dang = _capNhat.value
        if (dang is TrangThaiCapNhat.DangTai || dang is TrangThaiCapNhat.ChoHeThong) return

        if (!ApkInstaller.duocPhepCai(context)) {
            ApkInstaller.moTrangCapQuyen(context)
            return
        }

        val app = context.applicationContext
        _capNhat.value = TrangThaiCapNhat.DangTai(0)
        scope.launch {
            val loi = ApkInstaller.taiVaCai(app, ban.duongTai) { phanTram ->
                _capNhat.value = TrangThaiCapNhat.DangTai(phanTram)
            }
            // Giao xong cho he thong thi CHUA xong: hop thoai xac nhan con chua
            // hien. Giu pha cho cho toi khi `ketQuaCaiDat` bao ve.
            _capNhat.value =
                if (loi == null) TrangThaiCapNhat.ChoHeThong
                else TrangThaiCapNhat.Hong(loi)
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

    /**
     * Hàng đợi hiện tại lấy từ đâu ra — "Nhạc trong máy", tên một danh sách…
     *
     * Chỉ là một dòng chữ, nhưng nó trả lời câu hỏi mà màn hình Đang phát không
     * trả lời được: bài này ở đâu ra, và mấy bài xếp sau nó là của cái gì. Zing
     * ghi "PHÁT TỪ #zingchart Tuần 36" ngay đầu trang phát, và đó là thứ đáng
     * lấy — nó biến một hàng đợi vô danh thành một thứ hiểu được.
     *
     * `null` khi nhạc phát ở app khác: lúc đó ta thấy bài đang phát nhưng không
     * thấy hàng đợi của họ, nên cũng không biết nó từ đâu.
     */
    private val _nguonHangDoi = MutableStateFlow<String?>(null)
    val nguonHangDoi: StateFlow<String?> = _nguonHangDoi.asStateFlow()

    /** Phat ca danh sach tu mot bai. */
    fun playPlaylist(context: Context, id: String, index: Int = 0) {
        _dangXem.value = null
        val ds = playlistStore?.byId(id) ?: return
        _nguonHangDoi.value = ds.name
        Playback.playQueue(context, ds.tracks, index)
    }

    /** Doi cho hai bai trong hang doi. */
    fun doiChoTrongHangDoi(context: Context, tu: Int, den: Int) =
        Playback.doiChoTrongHangDoi(context, tu, den)

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
    fun playFromLibrary(context: Context, index: Int) {
        _dangXem.value = null
        _nguonHangDoi.value = "Nhạc trong máy"
        Playback.playQueue(context, _library.value, index)
    }

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

    fun playFromResults(context: Context, index: Int) {
        _dangXem.value = null
        _nguonHangDoi.value = "Kết quả tìm"
        Playback.playQueue(context, _results.value, index)
    }

    fun enqueue(context: Context, track: Track) = Playback.enqueue(context, track)

    /**
     * Bài đang XEM LỜI mà không phát.
     *
     * Bản Play tìm được nhạc ở Zing và NhacCuaTui nhưng không phát được — xem
     * `NguonNgoai`. Chạm vào một kết quả ở đó thì việc đúng để làm là TRA LỜI
     * cho bài ấy, chứ không phải bày ra một nút phát không bao giờ ăn gì.
     *
     * Dựng một `NowPlaying` giả với `isPlaying = false`: cả bộ máy tra lời, dịch
     * và hiển thị đã chạy quanh `NowPlaying` sẵn rồi, nên không cần một đường
     * riêng nào cả. Tên gói đặt là `XEM` để mọi chỗ phân biệt được "đang xem"
     * với "đang phát" — nhất là để giấu thanh tua và hàng nút, hai thứ vô nghĩa
     * khi không có gì đang chạy.
     */
    private val _dangXem = MutableStateFlow<NowPlaying?>(null)

    fun xemLoi(track: Track) {
        _dangXem.value = NowPlaying(
            packageName = XEM,
            title = track.title,
            artist = track.artist,
            album = "",
            duration = track.durationMs,
            position = 0L,
            isPlaying = false
        )
    }

    fun thoiXemLoi() {
        _dangXem.value = null
    }

    /** Bài đang hiện có phải là bài chỉ xem lời không. */
    fun laDangXem(): Boolean = _now.value?.packageName == XEM

    fun skipInQueue(context: Context, index: Int) = Playback.skipToIndex(context, index)

    fun removeFromQueue(context: Context, index: Int) = Playback.removeFromQueue(context, index)

    /**
     * Ba nut nay di mot trong hai duong.
     *
     * Lyra dang phat thi bam thang vao bo phat cua minh - chinh xac hon va
     * khong qua trung gian nao. Nhac o app khac thi gui lenh qua
     * `MediaController` cua ho.
     */
    fun next(context: Context) {
        if (tuPhat()) Playback.next(context) else watcher.dieuKhien { skipToNext() }
    }

    fun previous(context: Context) {
        if (tuPhat()) Playback.previous(context) else watcher.dieuKhien { skipToPrevious() }
    }

    fun playPause(context: Context) {
        if (tuPhat()) { Playback.playPause(context); return }
        // Tam dung roi thi `tuPhat` la false, nhung neu Lyra van la ben giu hang
        // doi thi nut Phat phai danh thuc bo phat cua Lyra chu khong phai app
        // khac - nguoi dung vua nghe bai cua Lyra, khong doi y giua chung.
        if (_queueIndex.value >= 0 && watcher.now.value?.isPlaying != true) {
            Playback.playPause(context); return
        }
        watcher.dieuKhien { if (now.value?.isPlaying == true) pause() else play() }
    }

    private var wired = false

    /** Nhip ve lai khung noi. 10 lan/giay du muot ma khong ton pin. */
    private const val TICK_MS = 100L

    /**
     * Nhip khi man hinh tat.
     *
     * Luc do khung noi khong duoc ve, chi con the media can dung cau. Mot giay
     * mot lan la du cho mot dong chu vai giay moi doi.
     */
    private const val TICK_NGU_MS = 1_000L

    /**
     * Cau da dua len the media lan truoc.
     *
     * Giu lai de chi cap nhat khi DOI CAU. The media di qua he thong toi giao
     * dien khoa man hinh, va day mot ban mo ta moi 10 lan mot giay la lam phien
     * ca ba tang do de doi mot dong chu vai giay moi thay.
     */
    private var cardLine: String? = null

    /**
     * Man hinh dang sang hay khong.
     *
     * Giu lai chu khong hoi `PowerManager` moi nhip: hoi he thong 10 lan mot
     * giay chinh la kieu lang phi ma cho nay sinh ra de chan.
     */
    @Volatile
    private var manHinhSang = true

    /**
     * Bao lâu nữa thì gọi nhịp lần sau.
     *
     * Màn hình sáng thì nhịp dày: khung lời nổi có quét sáng chạy trong câu,
     * và cái đó cần vẽ liên tục.
     *
     * Màn hình tắt thì thứ duy nhất còn phải đúng là CÂU trên thẻ media ở màn
     * hình khoá. Một nhịp một giây phẳng lì khiến thẻ trễ tới gần một giây so
     * với lúc câu thật sự đổi — nhìn ra được, và đó chính là kiểu "thẻ chạy sau
     * một dòng" mà người dùng hay thấy.
     *
     * Nên thay vì một giây phẳng, ngủ tới ĐÚNG lúc câu sau bắt đầu — hoặc một
     * giây, tuỳ cái nào tới trước. Không tốn thêm lần thức nào so với trước:
     * vẫn tối đa một lần mỗi giây, chỉ là rơi đúng chỗ có việc để làm.
     */
    private fun nhipToi(viTri: Long): Long {
        if (manHinhSang) return TICK_MS
        val loi = lyricsRepoOrNull?.lyrics?.value ?: return TICK_NGU_MS
        if (!loi.synced || loi.timingSuspect) return TICK_NGU_MS
        val t = viTri + loi.offset
        val sau = loi.lines.firstOrNull { it.time > t } ?: return TICK_NGU_MS
        // Thêm 30ms cho chắc là đã qua mốc chứ không đứng ngay trên nó.
        return (sau.time - t + 30L).coerceIn(50L, TICK_NGU_MS)
    }

    private val tick = object : Runnable {
        override fun run() {
            val position = livePosition()

            // Man hinh tat thi KHONG ve khung noi: no dang vo hinh, va ve mot
            // cua so khong ai nhin la dot pin thang. Truoc day khong co dieu
            // kien nay - bat khung roi khoa may la Lyra ve lai 10 lan moi giay
            // suot dem.
            //
            // The media thi van cap nhat: no hien tren man hinh khoa, va do
            // dung la luc man hinh vua bat len.
            if (manHinhSang && overlay.isShowing) overlay.update {
                setPosition(position)
                val n = _now.value
                setTransport(n?.duration ?: 0L, n?.isPlaying == true)
            }
            pushLineToCard(position)
            appContext?.let { ngoDoanLap(it, position) }

            // Chay tiep chung nao con viec de lam. Truoc day nhip chi song theo
            // khung noi; gio Lyra tu phat duoc, va luc do the media van can duoc
            // cap nhat du khung noi dang tat.
            //
            // Man hinh tat thi CHAM lai chu khong dung han: cau dang hat van
            // phai dung tren the media o man hinh khoa. Mot giay mot lan la du -
            // khong ai doc nhanh hon the.
            // Dang lap mot doan thi nhip phai chay du khung noi tat va du Lyra
            // khong phai ben phat: cai quyet dinh la vi tri da toi cuoi doan
            // chua, va cau hoi do chi tra loi duoc bang cach hoi lien tuc.
            if (overlay.isShowing || localPlayer?.isPlaying == true || _doanLap.value != null) {
                handler.postDelayed(this, nhipToi(position))
            }
        }
    }

    /**
     * Bao cho nhip biet man hinh vua tat hay vua bat.
     *
     * Goi tu `LyraNotificationListener` - dich vu do song lau hon moi Activity,
     * va la cho duy nhat con song khi nguoi dung da roi app.
     */
    fun manHinhDoi(sang: Boolean) {
        if (manHinhSang == sang) return
        manHinhSang = sang
        // Vua bat lai: ve ngay chu khong doi het nhip cham - mo may len ma nhin
        // mot khung loi da chet mot giay thi thay lien.
        if (sang) startTick()
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
            // Thu tu uu tien: cai gi DANG KEU thi thang do.
            //
            // "Xem loi" xep chot va chi thang khi khong con gi khac - no la mot
            // bai nguoi dung tra cuu, khong phai mot bai dang chay. Bat mot bai
            // len thi no bien mat ngay, va do la dung y.
            combine(watcher.now, localNow, _dangXem) { external, local, xem ->
                when {
                    local != null && local.isPlaying -> local
                    external != null && external.isPlaying -> external
                    xem != null -> xem
                    external != null -> external
                    else -> local
                }
            }.collect { _now.value = it }
        }

        scope.launch {
            _now.collect { now ->
                // Doi bai thi ket qua gop cua bai truoc khong con y nghia gi.
                if (_gop.value != null) thoiGopLoi()
                lyricsRepo.onNowPlaying(now)
                overlay.update { setIdleText(idleText()) }
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
        chuanBi(context)
        wire()
        watcher.start(context.applicationContext, LyraNotificationListener::class.java)
        loadLibrary(context)
    }

    /**
     * Mo cac kho tren dia. GOI DUOC MOI LUC, khong doi quyen nao.
     *
     * Truoc day phan nay nam trong `refresh`, ma `refresh` chi chay khi nguoi
     * dung DA CAP QUYEN DOC THONG BAO. Ai chi nghe nhac trong may - dung Lyra
     * nhu mot trinh phat, khong cho no doc thong bao app khac - thi khong bao
     * gio chay toi day, va mat sach:
     *
     *     bo nho dem loi   khong nho gi, lan nao cung goi mang lai
     *     LOI TU NHAP      `manual` con null nen luu vao la ROI MAT, im lang
     *     do lech nhip     khong nho
     *     danh sach phat   khong doc, khong luu
     *
     * Ba thu do khong lien quan gi toi quyen doc thong bao ca. Quyen do chi de
     * BIET app khac dang phat bai nao - nen chi phan `watcher` moi phai doi no.
     */
    fun chuanBi(context: Context) {
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
    }

    /**
     * Khung noi dang hien hay khong.
     *
     * Phai la mot dong chay chu khong phai mot lan doc `overlay.isShowing`:
     * khung tu tat duoc - giu tay len no la tat - va luc do khong ai goi
     * `toggleOverlay` ca. Man hinh Chinh doc mot lan roi nho mai thi nut van
     * ghi "Tat loi noi" trong khi khung da bien mat.
     */
    private val _overlayOn = MutableStateFlow(false)
    val overlayOn: StateFlow<Boolean> = _overlayOn.asStateFlow()

    /** Chu hien tren khung khi chua co loi. */
    private fun idleText(): String {
        val n = _now.value
        return when {
            // Rong = khung tu an. Khong co bai nao thi khong co loi nao, va
            // mot hop trong lo lung khong phuc vu ai.
            n == null -> ""
            n.artist.isNotEmpty() -> "${n.artist} — ${n.title}"
            else -> n.title
        }
    }

    fun showOverlay(context: Context) {
        wire()
        napHieuUng(context)
        napKieuChu(context)
        // Cham vao mot cau tren khung noi = can lai loi theo cau dang nghe.
        // Gan o day chu khong o `wire`: khung noi co the bi dung roi dung lai
        // nhieu lan trong mot phien.
        overlay.onLineTap = { index -> lyricsRepo.syncToLine(index, livePosition()) }
        // Giu tay tren khung = tat khung. Gan cung cho voi `onLineTap` va vi
        // cung mot ly do: khung co the bi dung roi dung lai nhieu lan.
        overlay.onDismiss = { hideOverlay() }
        // Ba nut va thanh song tren dai dieu khien. Chung di qua dung cac ham
        // dinh tuyen o tren, nen bam tren khung noi hay bam trong app deu ra
        // cung mot hanh vi.
        val app = context.applicationContext
        overlay.onTruoc = { previous(app) }
        overlay.onPhatDung = { playPause(app) }
        overlay.onSau = { next(app) }
        overlay.onTua = { tiLe ->
            val dai = _now.value?.duration ?: 0L
            if (dai > 0L) seekTo(app, (dai * tiLe).toLong())
        }
        overlay.show(context.applicationContext)

        // Do trang thai hien tai vao khung VUA DUNG XONG.
        //
        // Khung moi la mot `OverlayView` moi tinh, khong biet gi. Loi chi toi
        // duoc no qua cac luong gan trong `wire()`, ma nhung luong ay da phat
        // gia tri hien tai tu truoc - se khong phat lai chi vi vua co mot khung
        // moi sinh ra. Thieu doan nay thi bat khung giua bai se thay mot khung
        // trong ron cho toi khi doi bai, du trang Loi trong app van chay binh
        // thuong. Da gap that.
        val loi = lyricsRepo.lyrics.value
        overlay.update {
            setIdleText(idleText())
            setLyrics(loi.lines, loi.offset)
            setTranslations((translationRepo.state.value as? TranslationState.Done)?.lines.orEmpty())
            setPosition(livePosition())
        }

        startTick()

        // Chi ghi la "dang bat" khi dung duoc that. Thieu quyen ve de len app
        // khac thi `show` lang le khong lam gi, va ghi bua se thanh mot lan thu
        // dung khung vo ich moi lan he thong noi lai service.
        if (overlay.isShowing) prefs(context).setEnabled(true)
        _overlayOn.value = overlay.isShowing
    }

    fun hideOverlay() {
        handler.removeCallbacks(tick)
        overlay.hide()
        overlayPrefs?.setEnabled(false)
        _overlayOn.value = false
    }

    /** Nguoi dung cham vao dong dang hat de can lai ca bai. */
    fun syncToLine(index: Int) = lyricsRepo.syncToLine(index, livePosition())

    // ---- Luyện tập: lặp một đoạn, và đổi tốc độ ----

    /** Đoạn đang lặp, tính theo CHỈ SỐ DÒNG lời chứ không theo mili-giây. */
    data class DoanLap(val tuDong: Int, val denDong: Int)

    private val _doanLap = MutableStateFlow<DoanLap?>(null)
    val doanLap: StateFlow<DoanLap?> = _doanLap.asStateFlow()

    private val _tocDo = MutableStateFlow(1f)
    val tocDo: StateFlow<Float> = _tocDo.asStateFlow()

    /**
     * Chọn đoạn theo DÒNG, không theo thời gian.
     *
     * Đây là chỗ tính năng này khác mọi bộ lặp A–B khác: người ta chọn "từ câu
     * này tới câu kia" chứ không kéo hai cái mốc trên một thanh thời gian. Lyra
     * biết câu nào bắt đầu lúc nào, nên nó dịch giúp.
     *
     * Nhận hai dòng theo thứ tự nào cũng được — bấm nhầm thứ tự là chuyện thường.
     */
    fun datDoanLap(context: Context, a: Int, b: Int) {
        val doan = DoanLap(minOf(a, b), maxOf(a, b))
        _doanLap.value = doan

        // NHẢY TỚI ĐẦU ĐOẠN NGAY, đừng đợi bài chạy tới đó.
        //
        // Bản đầu chỉ đặt đoạn rồi thôi. Chọn đoạn ở phút thứ ba trong lúc bài
        // đang ở giây hai mươi thì hai phút liền không có gì xảy ra — người dùng
        // kết luận là tính năng hỏng, và họ đúng: chọn một đoạn để luyện tập
        // nghĩa là muốn nghe nó BÂY GIỜ.
        val loi = lyricsRepo.lyrics.value
        loi.lines.getOrNull(doan.tuDong)?.let {
            seekTo(context, (it.time - loi.offset).coerceAtLeast(0L))
        }

        // Nhịp có thể đang ngủ (khung nổi tắt, Lyra không phát). Đánh thức nó,
        // không thì đoạn vừa chọn không bao giờ được kiểm.
        startTick()
    }

    fun boDoanLap() {
        _doanLap.value = null
    }

    fun datTocDo(context: Context, giaTri: Float) {
        _tocDo.value = giaTri
        Playback.datTocDo(context, giaTri)
    }

    /** Lyra có phải là bên đang phát không — tốc độ chỉ đổi được khi đúng. */
    fun laLyraPhat(): Boolean = _now.value?.packageName == OWN

    /**
     * Tới cuối đoạn thì quay lại đầu đoạn.
     *
     * Gọi từ nhịp chung. Kiểm bằng mốc thời gian của DÒNG SAU dòng cuối, chứ
     * không phải mốc của chính dòng cuối: dòng cuối phải được hát hết đã, không
     * thì nghe được đúng một chữ rồi nhảy.
     */
    private fun ngoDoanLap(context: Context, viTri: Long) {
        val doan = _doanLap.value ?: return
        val cacDong = lyricsRepo.lyrics.value.lines
        val offset = lyricsRepo.lyrics.value.offset
        val batDau = cacDong.getOrNull(doan.tuDong)?.time ?: return
        // Hết đoạn = lúc dòng sau dòng cuối bắt đầu. Không có dòng sau thì để
        // chạy tới hết bài rồi mới quay lại.
        val ketThuc = cacDong.getOrNull(doan.denDong + 1)?.time ?: return
        if (viTri + offset >= ketThuc) {
            seekTo(context, (batDau - offset).coerceAtLeast(0L))
        }
    }

    /**
     * Tua tới đúng chỗ câu thứ `index` bắt đầu.
     *
     * Trừ đi `offset` chứ không cộng: `activeLineIndex` coi câu i là câu đang
     * hát khi `lines[i].time <= vịTrí + offset`, nên muốn câu i thành câu đang
     * hát thì vị trí phải là `time - offset`. Cộng nhầm dấu thì mỗi lần chạm
     * lại nhảy lệch gấp đôi độ lệch đã căn.
     */
    fun seekToLine(context: Context, index: Int) {
        val loi = lyricsRepo.lyrics.value
        val cau = loi.lines.getOrNull(index) ?: return
        seekTo(context, (cau.time - loi.offset).coerceAtLeast(0L))
    }

    /** Bo do lech da chinh. */
    fun clearOffset() = lyricsRepo.clearOffset()

    /** Nguoi dung dong y tai goi ngon ngu ve may. */
    fun downloadTranslationModel() = translationRepo.downloadAndTranslate()

    fun updateTranslateSettings(settings: TranslateSettings) =
        translationRepo.update(settings)

    /** Luu loi nguoi dung tu go hoac dan vao. */
    fun saveManualLyrics(raw: String) = lyricsRepo.saveManual(raw)

    // ---- Góp lời ngược lại cho LRCLIB ----

    sealed interface TrangThaiGop {
        /** `daThu` là số lần bằm đã chạy, để màn hình nói được là đang làm gì. */
        data class DangGiai(val daThu: Long) : TrangThaiGop
        data object DangGui : TrangThaiGop
        data object Xong : TrangThaiGop
        data class Hong(val vi: String) : TrangThaiGop
    }

    private val _gop = MutableStateFlow<TrangThaiGop?>(null)
    val gop: StateFlow<TrangThaiGop?> = _gop.asStateFlow()
    private var gopJob: Job? = null

    /**
     * Đăng bản lời đang xem lên LRCLIB.
     *
     * Chỉ chạy khi người dùng bấm. Đây là đăng lên một kho công cộng ai cũng
     * đọc được và không rút lại được — không bao giờ được là mặc định.
     */
    fun gopLoiChoLrclib() {
        if (gopJob?.isActive == true) return
        val n = _now.value ?: return
        val loi = lyricsRepo.lyrics.value
        gopJob = scope.launch {
            _gop.value = TrangThaiGop.DangGiai(0)
            val kq = LrclibPublish.gop(
                tenBai = n.title,
                caSi = n.artist,
                album = n.album,
                doDaiMs = n.duration,
                cacDong = loi.lines,
                coMoc = loi.synced,
                tienDo = { _gop.value = TrangThaiGop.DangGiai(it) },
                dangGui = { _gop.value = TrangThaiGop.DangGui }
            )
            _gop.value = when (kq) {
                LrclibPublish.KetQua.Xong -> TrangThaiGop.Xong
                is LrclibPublish.KetQua.Hong -> TrangThaiGop.Hong(kq.vi)
            }
        }
    }

    fun thoiGopLoi() {
        gopJob?.cancel()
        gopJob = null
        _gop.value = null
    }

    /**
     * Loi nam canh tep dang phat, hoac null.
     *
     * CHI cho thu Lyra tu phat: bai phat o app khac thi Lyra khong biet no doc
     * tep nao, ma cung khong co quyen hoi.
     */
    private fun loiCanhTep(): String? {
        val ctx = appContext ?: return null
        val bai = Playback.currentTrack ?: return null
        return LrcCanhTep.doc(ctx, bai.uri)
    }

    /** Chuoi de mo ra sua - loi da nhap, hoac loi dang co de sua lai. */
    fun manualDraft(): String = lyricsRepo.manualDraft()

    // ---- Sao luu loi tu nhap ----

    /** So bai dang giu loi tu nhap; 0 khi chua dung toi bao gio. */
    fun demLoiTuNhap(context: Context): Int = khoLoi(context).demBai()

    /** Toan bo loi tu nhap, da xep sang dang tep sao luu. */
    fun xuatLoiTuNhap(context: Context): String =
        SaoLuuLoi.xuat(khoLoi(context).tatCa())

    /**
     * Doc mot tep sao luu vao kho.
     *
     * Khong ghi de bai da co: xem `ManualLyricStore.dat`. Sau khi doc xong thi
     * bao lai kho loi tra lai bai dang phat - bai dang mo co the vua co loi.
     */
    fun nhapLoiTuNhap(context: Context, raw: String): SaoLuuLoi.KetQua {
        val cac = SaoLuuLoi.nhap(raw)
        if (cac.isEmpty()) return SaoLuuLoi.KetQua(0, 0, 1)

        val kho = khoLoi(context)
        var them = 0
        var daCo = 0
        for (b in cac) if (kho.dat(b)) them++ else daCo++
        if (them > 0) lyricsRepo.lamMoi()
        return SaoLuuLoi.KetQua(them, daCo, 0)
    }

    /**
     * Kho loi tu nhap, mo duoc ca khi dich vu chua chay.
     *
     * Trang Chinh vao duoc truoc khi nguoi dung phat bai nao, ma `refresh` chi
     * chay khi man hinh chinh mo len - nen khong the dua vao `manual` da duoc
     * dung san. Kho nay chi la mot thu muc, dung them mot cai khong ton gi.
     */
    private fun khoLoi(context: Context): ManualLyricStore =
        manual ?: ManualLyricStore(context.applicationContext).also { manual = it }

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
