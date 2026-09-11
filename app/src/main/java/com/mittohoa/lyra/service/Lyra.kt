package com.mittohoa.lyra.service

import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.mittohoa.lyra.data.LanNghe
import com.mittohoa.lyra.data.LichSuNghe
import com.mittohoa.lyra.data.LyricCache
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.data.LyricEffectPrefs
import com.mittohoa.lyra.data.ManualLyricStore
import com.mittohoa.lyra.data.SaoLuuLichSu
import com.mittohoa.lyra.data.SaoLuuLoi
import com.mittohoa.lyra.data.SaoLuuTatCa
import com.mittohoa.lyra.data.SaoLuuTuDong
import com.mittohoa.lyra.data.OffsetStore
import com.mittohoa.lyra.data.Playlist
import com.mittohoa.lyra.data.PlaylistStore
import com.mittohoa.lyra.data.OverlayPrefs
import com.mittohoa.lyra.data.TranslatePrefs
import com.mittohoa.lyra.data.TranslateSettings
import com.mittohoa.lyra.data.TranslationCache
import com.mittohoa.lyra.data.UpdateChecker
import com.mittohoa.lyra.data.YeuThich
import com.mittohoa.lyra.update.ApkInstaller
import androidx.core.content.res.ResourcesCompat
import com.mittohoa.lyra.data.ChuDePrefs
import com.mittohoa.lyra.data.KieuChu
import com.mittohoa.lyra.R
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.lyrics.tenBaiDeHien
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.graphics.toArgb
import com.mittohoa.lyra.ui.dominantColor
import com.mittohoa.lyra.player.CanBangAm
import com.mittohoa.lyra.player.Playback
import com.mittohoa.lyra.sources.Catalog
import com.mittohoa.lyra.sources.KieuXep
import com.mittohoa.lyra.sources.LocLoai
import com.mittohoa.lyra.widget.KhungLoiWidget
import com.mittohoa.lyra.data.SuaThe
import com.mittohoa.lyra.data.TheSua
import com.mittohoa.lyra.data.ThuMucNhac
import com.mittohoa.lyra.sources.LocalLibrary
import com.mittohoa.lyra.sources.ThuVienNgoai
import com.mittohoa.lyra.sources.LrclibPublish
import com.mittohoa.lyra.sources.Track
import com.mittohoa.lyra.translate.TranslationRepository
import com.mittohoa.lyra.translate.TranslationState
import kotlinx.coroutines.CoroutineScope
import com.mittohoa.lyra.lyrics.normalizeForCompare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private const val TAG = "AuraNoi"

    /** Ten goi gia cho bai do chinh AURA phat - de phan biet voi app khac. */
    private const val OWN = "lyra"

    /** Ten goi gia cho bai chi XEM LOI, khong phat. */
    private const val XEM = "lyra-xem"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    val watcher = MediaSessionWatcher()

    /**
     * Bo may phat cua CHINH AURA, khi no dang chay.
     *
     * Khong dung de biet dang phat bai gi - viec do van la cua `watcher`, va
     * phien cua AURA hien ra o do y het moi app khac. Cai duy nhat lay tu day
     * la VI TRI PHAT: hoi thang bo giai ma thi dung tung mili-giay, con qua
     * ban tin media thi luon la mot con so suy ra tu lan bao gan nhat.
     */
    private var localPlayer: Player? = null

    /**
     * Bai chinh AURA dang phat.
     *
     * Dung mot dong RIENG chu khong di qua `watcher`: phien media cua AURA da
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
    private var banDaChon: ManualLyricStore? = null
    private var overlayPrefs: OverlayPrefs? = null
    private var translationCache: TranslationCache? = null
    private var translatePrefs: TranslatePrefs? = null
    private var playlistStore: PlaylistStore? = null
    private var lichSu: LichSuNghe? = null
    private var translationRepoOrNull: TranslationRepository? = null
    private var lyricsRepoOrNull: LyricsRepository? = null

    private val lyricsRepo: LyricsRepository
        get() = lyricsRepoOrNull
            ?: LyricsRepository(scope, cache, offsets, manual, ::loiCanhTep, banDaChon)
                .also { lyricsRepoOrNull = it }

    private val translationRepo: TranslationRepository
        get() = translationRepoOrNull
            ?: TranslationRepository(scope, translationCache, translatePrefs)
                .also { translationRepoOrNull = it }

    private val _now = MutableStateFlow<NowPlaying?>(null)

    /**
     * Bai dang phat, du la AURA tu phat hay mot app khac phat.
     *
     * AURA dang phat thi AURA thang: no la ben nguoi dung vua bam nut. AURA
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
                // Gan can bang am o day chu khong luc dung bo phat: phien am
                // thanh chi co that khi bo phat da mo duong ra, ma truoc bai
                // dau tien thi no chua mo. Gan som la gan vao so 0 - khong tac
                // dung, va khong bao gi.
                if (isPlaying) ganCanBang()
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
     * AURA dang tu phat thi hoi thang bo giai ma cua minh; con lai thi hoi qua
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

    /** Vi tri bai dang phat trong hang doi; -1 khi AURA khong phat gi. */
    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    val queue: StateFlow<List<Track>> get() = Playback.queueFlow

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    /** 0 = tat, 1 = lap ca hang doi, 2 = lap mot bai (theo Media3). */
    private val _repeat = MutableStateFlow(0)
    val repeat: StateFlow<Int> = _repeat.asStateFlow()

    /** Ảnh bìa AURA tự tải về cho bài CHÍNH NÓ phát. */
    private val _artwork = MutableStateFlow<Bitmap?>(null)

    /**
     * Ảnh bìa dùng được cho bài đang phát — null khi bài không phải của AURA.
     *
     * `_artwork` chỉ được nạp lại khi bộ phát của AURA đổi bài, nên nó SỐNG DAI
     * hơn lượt phát của AURA: nghe một bài trong AURA rồi chuyển sang Zing thì
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
     * AURA co phai la ben dang phat khong.
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
     * AURA, chua het viec cua may: he thong con chuan bi, va Play Protect con
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
     * Giu o `AURA` chu khong o rieng man hinh: khung noi song ngoai vong doi
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

    // ---- Hen gio tat nhac ----

    /** Cach dat gio: sau bao nhieu phut, hay khi bai dang phat het. */
    sealed class KieuHen {
        data class Phut(val so: Int) : KieuHen()
        data object HetBai : KieuHen()
    }

    /**
     * Mot lan hen gio dang chay.
     *
     * `hetLuc` theo dong ho `elapsedRealtime` chu khong phai gio thuc: gio thuc
     * nhay duoc khi may dong bo lai voi mang, va mot cu nhay nhu the bien "con
     * mười phút" thanh "da qua han" giua chung.
     */
    data class HenGio(val kieu: KieuHen, val hetLuc: Long)

    private val _henGio = MutableStateFlow<HenGio?>(null)
    val henGio: StateFlow<HenGio?> = _henGio.asStateFlow()

    private val chuongHenGio = Runnable {
        _henGio.value = null
        appContext?.let { dungPhat(it) }
    }

    /**
     * Dat gio tat nhac.
     *
     * TAT DUOC CA NHAC CUA APP KHAC. Day la cho AURA lam duoc thu ma mot hen
     * gio thong thuong khong lam: no dieu khien duoc phien media cua Zing hay
     * YouTube, nen mot lan hen gio o day phu len bat ke ai dang phat. Nguoi ngu
     * quen khong can biet luc do minh dang mo app nao.
     *
     * GIOI HAN da biet: dem gio bang `Handler` cua tien trinh. Tien trinh nay
     * song vi dang co nhac phat - may khong ngu sau khi dang phat am thanh -
     * nen dong ho chay. Nhung neu he thong giet tien trinh (het bo nho, nguoi
     * dung vuot app khoi danh sach gan day) thi lan hen gio do mat luon, im
     * lang. Doi lay mot bao thuc cua he thong thi phai xin them quyen, ma quyen
     * do thi dat hon thu no mua.
     */
    fun datHenGio(context: Context, kieu: KieuHen) {
        appContext = context.applicationContext
        handler.removeCallbacks(chuongHenGio)

        val conLai = doDaiHen(kieu)
        if (conLai == null) {
            // "Het bai nay" ma khong biet bai dai bao nhieu thi khong hen duoc.
            // Bo han con hon dat mot cai gio doan bua roi tat giua bai.
            _henGio.value = null
            return
        }

        _henGio.value = HenGio(kieu, SystemClock.elapsedRealtime() + conLai)
        handler.postDelayed(chuongHenGio, conLai)
    }

    fun boHenGio() {
        handler.removeCallbacks(chuongHenGio)
        _henGio.value = null
    }

    /** Bao lau nua thi tat, tinh tu bay gio. `null` khi chua tinh duoc. */
    private fun doDaiHen(kieu: KieuHen): Long? = when (kieu) {
        is KieuHen.Phut -> kieu.so * 60_000L
        KieuHen.HetBai -> {
            val n = _now.value
            val conLai = if (n == null || n.duration <= 0L) null
            else (n.duration - livePosition()).coerceAtLeast(0L)
            conLai
        }
    }

    /**
     * Tinh lai han cho kieu "het bai nay" moi khi ban tin media doi.
     *
     * Khong tinh lai thi tua mot cai la han sai han: dat gio o giay thu 10 cua
     * mot bai bon phut, roi keo toi giay thu 200, thi con lai 30 giay that ma
     * dong ho van dem tiep 230 giay - nhac tat giua bai sau.
     *
     * Doi BAI thi thoi han han: "het bai nay" da lam xong viec cua no. Nhung
     * bai doi vi bai truoc HET, ma luc do chuong da reo va nhac da dung roi -
     * nen duong nay chi cham toi khi nguoi dung tu bam sang bai khac.
     */
    private fun soatHenGio() {
        val hen = _henGio.value ?: return
        if (hen.kieu !is KieuHen.HetBai) return
        val context = appContext ?: return
        datHenGio(context, hen.kieu)
    }

    /** Dung nhac, di dung con duong cua vai dang dong. */
    private fun dungPhat(context: Context) {
        if (tuPhat()) {
            Playback.pause(context)
            return
        }
        watcher.dieuKhien { if (now.value?.isPlaying == true) pause() }
    }

    // ---- Lich su nghe ----

    private val _lichSuNghe = MutableStateFlow<List<LanNghe>>(emptyList())
    val lichSuNghe: StateFlow<List<LanNghe>> = _lichSuNghe.asStateFlow()

    /** Xoa sach lich su. Man hinh phai hoi truoc - khong co duong hoan tac. */
    fun xoaLichSu() {
        lichSu?.xoaHet()
    }

    /**
     * Kho lich su, dung ngay ca khi `chuanBi` chua chay.
     *
     * Man hinh Chinh mo duoc truoc khi mot bai nao duoc phat, va luc do
     * `chuanBi` co the chua tao kho. Doi kho co san moi cho sao luu thi nut
     * "Sao luu" ngoi im khong ly do.
     */
    private fun khoLichSu(context: Context): LichSuNghe =
        lichSu ?: LichSuNghe(context.applicationContext).also { kho ->
            lichSu = kho
            scope.launch { kho.lichSu.collect { ds -> _lichSuNghe.value = ds } }
        }

    /** Bao nhieu lan nghe dang giu. */
    fun demLichSu(context: Context): Int = khoLichSu(context).lichSu.value.size

    /** Toan bo lich su, da xep sang dang tep sao luu. */
    fun xuatLichSu(context: Context): String =
        SaoLuuLichSu.xuat(khoLichSu(context).lichSu.value)

    /** Doc mot tep sao luu vao kho. Tron chu khong ghi de - xem `LichSuNghe.gop`. */
    fun nhapLichSu(context: Context, raw: String): SaoLuuLichSu.KetQua {
        val cac = SaoLuuLichSu.nhap(raw)
        if (cac.isEmpty()) return SaoLuuLichSu.KetQua(0, 0, 0, 1)
        return khoLichSu(context).gop(cac)
    }

    // ---- Sao luu GOP: moi thu nguoi dung tu tao, mot tep ----

    /** Bao nhieu bai yeu thich, bao nhieu lan nghe, bao nhieu bai loi tu nhap. */
    data class DemSaoLuu(val loi: Int, val nghe: Int, val thich: Int)

    fun demSaoLuu(context: Context): DemSaoLuu {
        chuanBi(context)
        return DemSaoLuu(
            loi = khoLoi(context).demBai(),
            nghe = khoLichSu(context).lichSu.value.size,
            thich = khoYeu(context).bo.value.size
        )
    }

    fun xuatTatCa(context: Context): String {
        chuanBi(context)
        val cb = boCanBang(context)
        return SaoLuuTatCa.xuat(
            loi = xuatLoiTuNhap(context),
            nghe = xuatLichSu(context),
            thich = khoYeu(context).bo.value,
            // Chi ghi phan can bang am khi may THAT SU co no. Ghi mot lua chon
            // doc ra tu mot bo khong ton tai la ghi mot con so bia.
            canBang = if (cb.coDung) SaoLuuTatCa.CanBang(cb.dangBat, cb.mauDangChon) else null
        )
    }

    /**
     * Doc mot tep sao luu, KIEU NAO CUNG NHAN.
     *
     * Tep gop thi mo tung phan; tep chi co loi hoac chi co lich su - thu da nam
     * trong may nguoi dung tu may ban truoc - thi van doc duoc. Ra mot dinh dang
     * moi roi bo roi tep cu thi dung vao luc nguoi ta can khoi phuc nhat lai la
     * luc app noi khong doc duoc.
     */
    fun nhapTatCa(context: Context, raw: String): SaoLuuTatCa.KetQua {
        chuanBi(context)
        return when (SaoLuuTatCa.loai(raw)) {
            SaoLuuTatCa.Loai.CHI_LOI ->
                SaoLuuTatCa.KetQua(loi = nhapLoiTuNhap(context, raw))

            SaoLuuTatCa.Loai.CHI_NGHE ->
                SaoLuuTatCa.KetQua(nghe = nhapLichSu(context, raw))

            SaoLuuTatCa.Loai.KHONG_BIET -> SaoLuuTatCa.KetQua(hong = true)

            SaoLuuTatCa.Loai.TAT_CA -> {
                val phan = SaoLuuTatCa.tach(raw)
                val loi = phan[SaoLuuTatCa.PHAN_LOI]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { nhapLoiTuNhap(context, it) }
                    ?: SaoLuuLoi.KetQua(0, 0, 0)
                val nghe = phan[SaoLuuTatCa.PHAN_NGHE]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { nhapLichSu(context, it) }
                    ?: SaoLuuLichSu.KetQua(0, 0, 0, 0)

                // Yeu thich: THEM vao chu khong thay the, cung le voi lich su.
                // Khoi phuc tren mot may da danh dau vai bai ma xoa sach di thi
                // ban sao luu lai la thu lam mat du lieu.
                var themThich = 0
                phan[SaoLuuTatCa.PHAN_THICH]?.let { p ->
                    val kho = khoYeu(context)
                    for (dc in SaoLuuTatCa.docThich(p)) {
                        if (!kho.co(dc)) {
                            kho.doi(dc)
                            themThich++
                        }
                    }
                }

                var coCanBang = false
                phan[SaoLuuTatCa.PHAN_CANBANG]?.let { p ->
                    SaoLuuTatCa.docCanBang(p)?.let { cb ->
                        val bo = boCanBang(context)
                        // May nay khong co can bang am thi bo qua, khong bao
                        // hong: tep van dung, chi la phan cuoi khong ap duoc.
                        if (bo.coDung) {
                            bo.datMau(cb.mau)
                            bo.datBat(cb.bat)
                            _nhipCanBang.value++
                            coCanBang = true
                        }
                    }
                }

                SaoLuuTatCa.KetQua(loi, nghe, themThich, coCanBang)
            }
        }
    }

    /** Xoa dung mot dong, cho luc nguoi dung chi muon giau mot bai. */
    fun xoaMotLanNghe(khoa: String) {
        lichSu?.xoa(khoa)
    }

    /**
     * Nghe lai mot bai trong lich su. Tra `false` khi khong mo lai duoc.
     *
     * KHONG PHAT THANG TU DIA CHI DA LUU. Dia chi trong lich su co the tro toi
     * mot tep da bi xoa, doi ten, hoac nam tren the nho da rut ra - va phat mot
     * dia chi chet thi bo may phat bao mot loi kho hieu roi im. Tim lai trong
     * thu vien dang co truoc, tim khong thay thi tra `false` de man hinh chuyen
     * sang di TIM theo ten, mot duong con dan toi dau do.
     */
    fun ngheLai(context: Context, lan: LanNghe): Boolean {
        if (lan.diaChi.isBlank()) return false
        val thu = _library.value

        var i = thu.indexOfFirst { it.playbackUri == lan.diaChi }

        // KHONG TIM THAY DIA CHI THI TIM THEO TEN BAI VA CA SI.
        //
        // Do duoc tren may that: CUNG MOT TEP mang hai dia chi khac nhau tuy
        // duong quet ra no - `lyra://may/<so>` khi doc qua MediaStore, va
        // `lyra://may/saf-<...>` khi doc thang tu thu muc nguoi dung chi. Nen
        // mot dong lich su chep tu may khac sang, hoac ghi tu truoc khi doi
        // duong quet, tro toi mot dia chi khong con ai nhan.
        //
        // Dia chi chi la CHO DE, con ten bai voi ca si moi la bai hat. Doi
        // duong quet thi cho de doi, bai hat thi khong.
        //
        // Van uu tien dia chi: hai ban thu am cung ten cung ca si nam trong hai
        // thu muc khac nhau la chuyen co that, va dia chi la thu duy nhat phan
        // biet duoc chung.
        if (i < 0 && lan.ten.isNotBlank()) {
            i = thu.indexOfFirst { it.title == lan.ten && it.artist == lan.caSi }
        }

        if (i < 0) return false
        playFromLibrary(context, i)
        return true
    }

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

    private val _coThuMuc = MutableStateFlow(false)

    /**
     * Nguoi dung da chi cho AURA thu muc nao chua.
     *
     * Man hinh can phan biet HAI chuyen rat khac nhau ma deu ra mot thu vien
     * rong: chua cho phep doc o dau (loi thoat la vao Cai dat chon thu muc), va
     * da cho phep nhung trong do khong co bai nao (loi thoat la chon thu muc
     * khac). Gop lam mot thi cau bao luon sai mot nua so nguoi doc no.
     */
    val coThuMuc: StateFlow<Boolean> = _coThuMuc.asStateFlow()

    private val _chamTranThuMuc = MutableStateFlow(false)

    /**
     * Lan quet gan nhat co dung vi cham tran khong.
     *
     * Man hinh Cai dat phai noi ra: dung o tran ma im lang thi nguoi co thu vien
     * lon hon tran mat bai KHONG DAU HIEU GI - khong loi, khong danh sach rong,
     * chi la vai album bien mat ma ho khong doan duoc tai sao.
     */
    val chamTranThuMuc: StateFlow<Boolean> = _chamTranThuMuc.asStateFlow()

    /**
     * Doc nhac trong may.
     *
     * Goi lai duoc nhieu lan - vd. ngay sau khi nguoi dung vua cap quyen, hoac
     * khi ho quay lai app sau khi tai them nhac. Doc lai ca thu vien re hon
     * nhieu so voi theo doi tung thay doi cua `MediaStore`.
     */
    fun loadLibrary(context: Context) {
        val app = context.applicationContext
        scope.launch { napThuVien(app) }
    }

    /**
     * Nhu `loadLibrary`, nhung CHO XONG moi tra ve.
     *
     * Man hinh Cai dat can biet luc nao quet xong de con tat chu "Dang quet".
     * Khong co ban treo thi no phai tu quet lay mot lan nua chi de dem, tuc
     * quet hai lan cho mot lan nguoi dung bam.
     */
    suspend fun napThuVien(context: Context) {
        val app = context.applicationContext

        val kho = ThuMucNhac(app)
        val thuMuc = withContext(Dispatchers.IO) { kho.danhSach() }
        _coThuMuc.value = thuMuc.isNotEmpty()

        // Doc lai lua chon da luu. `enumValueOf` NEM khi ten khong con - ban cu
        // ghi mot kieu xep ma ban nay da bo di thi app sap ngay luc mo. Ten bi
        // bo di thi quay ve mac dinh, va do la dieu duy nhat dung.
        _kieuXep.value = runCatching { kho.kieuXep()?.let { enumValueOf<KieuXep>(it) } }
            .getOrNull() ?: KieuXep.ALBUM
        _locLoai.value = runCatching { kho.locLoai()?.let { enumValueOf<LocLoai>(it) } }
            .getOrNull() ?: LocLoai.TAT_CA

        // KHONG TU QUET GI CA. Chua chon thu muc thi thu vien rong, va AURA
        // khong doc mot dong nao cua danh muc he thong.
        //
        // Doi lai han so voi cach thong thuong, va la co y: mac dinh "quet sach
        // may roi bay ra" nghia la app cam ca bo suu tap phuong tien cua nguoi
        // dung ma chua ai cho phep gi ngoai mot o quyen ho bam cho xong. Khong
        // doc gi cho toi khi duoc chi dich danh thi cai ho trao la mot thu muc
        // CU THE, khong phai ca chiec dien thoai.
        if (thuMuc.isEmpty()) {
            thuVienTho = emptyList()
            _library.value = emptyList()
            _chamTranThuMuc.value = false
            Catalog.library = emptyList()
            Log.i(TAG, "Thu vien: chua chon thu muc nao, khong doc gi")
            return
        }

        val pham = thuMuc.mapNotNull(ThuVienNgoai::duongTuyetDoi)
        val danhMuc = LocalLibrary.all(app, pham)
        // Danh muc he thong truoc, roi moi toi cac thu muc nguoi dung tu tro
        // vao. Chua tro thu muc nao thi `tatCa` tra rong ngay va khong cham dia
        // lan nao - duong pho thong khong phai tra gia cho mot tinh nang phan
        // lon nguoi dung khong bat.
        //
        // Hai duong cung doc mot thu muc la co y: danh muc he thong nhanh va co
        // san anh bia, con duong quet thang nhat nhung tep danh muc bo sot.
        // `gop` chong trung phan giao nhau.
        val tuTro = ThuVienNgoai.tatCa(app)
        _chamTranThuMuc.value = tuTro.chamTran
        val found = ThuVienNgoai.gop(danhMuc, tuTro.bai)
        thuVienTho = found
        dungLaiThuVien()
        Log.i(
            TAG,
            "Thu vien: ${danhMuc.size} tu danh muc he thong, " +
                "them ${found.size - danhMuc.size} tu thu muc tu tro" +
                (if (pham.isEmpty()) "" else " (gioi han ${pham.size} thu muc)") +
                // Ghi ra chu khong de im: khi co nguoi bao "thieu bai", day la
                // dong duy nhat phan biet duoc "thu muc chi co chung ay" voi
                // "da doc toi tran roi dung".
                (if (tuTro.chamTran) " - DUNG O TRAN ${kho.tranSoBai()} BAI" else "")
        )
    }

    /**
     * Thu vien NGUYEN BAN, truoc khi loc va xep.
     *
     * Giu rieng de doi cach xep khong phai quet lai dia. Voi hai nghin bai thi
     * mot lan quet la vai giay dung hinh, ma nguoi dung doi cach xep chi de
     * nhin cung ngan ay bai theo thu tu khac.
     */
    private var thuVienTho: List<Track> = emptyList()

    private val _kieuXep = MutableStateFlow(KieuXep.ALBUM)
    val kieuXep: StateFlow<KieuXep> = _kieuXep.asStateFlow()

    private val _locLoai = MutableStateFlow(LocLoai.TAT_CA)
    val locLoai: StateFlow<LocLoai> = _locLoai.asStateFlow()

    /**
     * Loc roi xep lai tu ban nguyen, KHONG doc lai dia.
     *
     * `_library` la danh sach DANG HIEN RA, va do la co y: man hinh xep hang doi
     * tu chinh no bang CHI SO. Neu man hinh tu xep lay mot thu tu khac roi dua
     * chi so cua thu tu do xuong day, thi bam vao bai thu ba se phat mot bai
     * khac han - va cang lech nhieu khi nguoi dung doi cach xep.
     */
    private fun dungLaiThuVien() {
        // Áp bảng sửa thẻ TRƯỚC khi lọc và xếp. Sửa xong mà vẫn xếp theo thẻ
        // gốc thì bài vừa đổi album vẫn nằm ở nhóm cũ, và người dùng tưởng bản
        // sửa không ăn.
        val daSua = apThe(thuVienTho)
        val ra = ThuVienNgoai.sapXep(
            daSua.filter { _locLoai.value.hop(it) },
            _kieuXep.value
        )
        _library.value = ra
        // `Catalog.library` la thu bo tim doi chieu, khong phai thu de hien ra -
        // no phai la BAN DAY DU, khong dinh gi toi bo loc cua man hinh. Loc o
        // day nghia la go mot bo loc hien thi ma lam hong ca o tim.
        //
        // Nhung VAN AP BANG SUA: nguoi dung sua ten mot bai roi go dung cai ten
        // vua sua vao o tim ma khong ra gi thi ho ket luan bang sua khong an.
        Catalog.library = daSua
    }

    /**
     * Ap bang sua the len danh sach bai.
     *
     * Doc bang MOT LAN cho ca luot chu khong hoi tung bai: ham nay chay lai moi
     * lan doi cach xep, va mo mot tep JSON cho moi bai trong hai nghin bai la
     * dung kieu lang phi ma cho nay sinh ra de tranh.
     *
     * Bang rong thi tra ve NGUYEN danh sach cu, khong dung lai mot danh sach
     * moi y het: phan lon nguoi dung khong sua the bai nao ca.
     */
    private fun apThe(bai: List<Track>): List<Track> {
        val kho = suaThe ?: return bai
        val bang = kho.tatCa()
        if (bang.isEmpty()) return bai
        return bai.map { t ->
            val s = bang[t.playbackUri] ?: return@map t
            t.copy(
                title = s.title.ifBlank { t.title },
                artist = s.artist.ifBlank { t.artist },
                album = s.album.ifBlank { t.album },
                soThuTu = if (s.soThuTu >= 0) s.soThuTu else t.soThuTu
            )
        }
    }

    private var suaThe: SuaThe? = null

    /**
     * Bai trong thu vien, NGUYEN BAN - chua ap bang sua the.
     *
     * Man hinh sua the phai bay ra the THAT trong tep de nguoi dung con doi
     * chieu. Lay tu `_library` hay tu hang doi thi ca hai deu da mang ban sua
     * roi, va o "the trong tep" se hien lai chinh cai nguoi dung vua go vao -
     * mot cai guong, khong phai mot manh moi.
     */
    fun baiTho(khoa: String): Track? = thuVienTho.firstOrNull { it.playbackUri == khoa }

    /** Thẻ người dùng đã sửa cho bài này, hoặc rỗng. */
    fun theDaSua(context: Context, khoa: String): TheSua {
        val kho = suaThe ?: SuaThe(context.applicationContext).also { suaThe = it }
        return kho.cua(khoa) ?: TheSua()
    }

    /**
     * Sửa thẻ một bài, rồi dựng lại thư viện ngay.
     *
     * Không quét lại đĩa: bảng sửa nằm chồng lên bản nguyên đã giữ sẵn, nên
     * người dùng bấm Lưu là thấy tên mới ngay chứ không phải chờ một vòng quét.
     */
    fun datTheSua(context: Context, khoa: String, sua: TheSua) {
        val kho = suaThe ?: SuaThe(context.applicationContext).also { suaThe = it }
        kho.dat(khoa, sua)
        dungLaiThuVien()

        // ÁP LUÔN CHO BÀI ĐANG PHÁT, không đợi lần phát sau.
        //
        // Người dùng sửa thẻ CHÍNH VÌ lời không tìm ra, và họ sửa trong lúc bài
        // đó đang chạy. Chỉ đổi mỗi danh sách thì màn hình vẫn treo cái tên sai
        // và vẫn không có lời — đúng cái họ vừa bỏ công đi sửa.
        //
        // Đổi mô tả trong hàng đợi rồi đọc lại bài đang phát: `_now` mang tên
        // mới, khoá của nó đổi theo, và kho lời tự đi tìm lại một vòng.
        _library.value.firstOrNull { it.playbackUri == khoa }?.let { moi ->
            if (Playback.suaBaiTrongHangDoi(khoa, moi)) refreshLocalNow()
        }
        Log.i(TAG, "Sua the cho $khoa")
    }

    fun datKieuXep(context: Context, kieu: KieuXep) {
        if (_kieuXep.value == kieu) return
        _kieuXep.value = kieu
        ThuMucNhac(context).datKieuXep(kieu.name)
        dungLaiThuVien()
    }

    fun datLocLoai(context: Context, loc: LocLoai) {
        if (_locLoai.value == loc) return
        _locLoai.value = loc
        ThuMucNhac(context).datLocLoai(loc.name)
        dungLaiThuVien()
    }

    /** Phat ca thu vien tu mot bai. */
    /**
     * Phat mot bai, xep ca thu vien lam hang doi tu bai do tro di.
     *
     * Bai khong con trong thu vien thi khong lam gi: tep co the vua bi xoa hoac
     * rut the nho ra, va phat mot dia chi chet thi bo may phat bao mot loi kho
     * hieu roi im.
     */
    fun phatTrongThuVien(context: Context, bai: Track) {
        val i = _library.value.indexOfFirst { it.playbackUri == bai.playbackUri }
        if (i < 0) return
        playFromLibrary(context, i)
    }

    fun playFromLibrary(context: Context, index: Int) {
        _dangXem.value = null
        _nguonHangDoi.value = "Nhạc trong máy"
        Playback.playQueue(context, _library.value, index)
    }

    // ---- Can bang am ----

    private var canBang: CanBangAm? = null

    /** Doi moi lan gan lai hay doi bo mau, de man hinh Chinh ve lai. */
    private val _nhipCanBang = MutableStateFlow(0)
    val nhipCanBang: StateFlow<Int> = _nhipCanBang.asStateFlow()

    fun boCanBang(context: Context): CanBangAm =
        canBang ?: CanBangAm(context.applicationContext).also { canBang = it }

    private fun ganCanBang() {
        val p = localPlayer as? ExoPlayer ?: return
        val kho = canBang ?: appContext?.let { boCanBang(it) } ?: return
        kho.gan(p.audioSessionId)
        _nhipCanBang.value++
    }

    fun datBatCanBang(context: Context, bat: Boolean) {
        boCanBang(context).datBat(bat)
        _nhipCanBang.value++
    }

    fun datMauCanBang(context: Context, i: Int) {
        boCanBang(context).datMau(i)
        _nhipCanBang.value++
    }

    // ---- Yeu thich ----

    private var khoYeuThich: YeuThich? = null

    private val _yeuThich = MutableStateFlow<Set<String>>(emptySet())
    val yeuThich: StateFlow<Set<String>> = _yeuThich.asStateFlow()

    private fun khoYeu(context: Context): YeuThich =
        khoYeuThich ?: YeuThich(context.applicationContext).also { kho ->
            khoYeuThich = kho
            scope.launch { kho.bo.collect { _yeuThich.value = it } }
        }

    /**
     * Danh dau hoac bo danh dau bai DANG PHAT.
     *
     * Chi lam duoc voi nhac AURA tu phat: dau moc la dia chi tep, ma nhac o
     * Zing hay YouTube thi khong co dia chi nao ben nay giu duoc.
     */
    fun doiYeuThich(context: Context) {
        val diaChi = Playback.currentTrack?.uri ?: return
        if (!laLyraPhat()) return
        khoYeu(context).doi(diaChi)
    }

    fun doiYeuThich(context: Context, bai: Track) {
        khoYeu(context).doi(bai.playbackUri)
    }

    /** Nhung bai yeu thich con thay trong thu vien, moi danh dau len truoc. */
    fun baiYeuThich(): List<Track> {
        val thu = _library.value.associateBy { it.playbackUri }
        return _yeuThich.value.mapNotNull { thu[it] }
    }

    // ---- Tai loi san cho ca thu vien ----

    /**
     * Tien do mot lan tai loi cho ca thu vien.
     *
     * `daCo` dem so bai kho DA CO LOI, khong dem so lan goi mang: nguoi dung
     * muon biet "tim duoc bao nhieu bai", con so lan goi mang la chuyen cua may.
     */
    data class TienTaiLoi(val daXet: Int, val tong: Int, val daCo: Int, val xong: Boolean)

    private val _tienTaiLoi = MutableStateFlow<TienTaiLoi?>(null)
    val tienTaiLoi: StateFlow<TienTaiLoi?> = _tienTaiLoi.asStateFlow()

    private var jobTaiLoi: Job? = null

    /**
     * Tai san loi cho moi bai trong thu vien.
     *
     * VI SAO CAN. O tim doc duoc ca loi bai hat, nhung chi doc duoc loi DA NAM
     * TRONG KHO - tuc nhung bai da tung mo. Mot thu vien nam tram bai ma moi
     * nghe hai chuc thi o tim gan nhu trong, va man hinh phai dung ra xin loi.
     * Lan tai nay bien no tu mot meo hay thanh mot tinh nang that.
     *
     * DI TUNG BAI MOT, CO NGHI GIUA HAI LAN. LRCLIB la kho mo, mien phi, chay
     * bang tien quyen gop; ban ba tram lan goi song song vao do la cach nhanh
     * nhat de ca app bi chan. Cham hon thi chi la doi lau hon mot chut, ma lan
     * nay von la viec chay nen.
     *
     * BO QUA BAI KHO DA CO. Chay lan hai chi ton mang cho phan con thieu, nen
     * bam lai sau khi them nhac vao thu vien la viec re.
     */
    fun taiLoiChoThuVien(context: Context) {
        if (jobTaiLoi?.isActive == true) return
        appContext = context.applicationContext
        chuanBi(context)

        val bai = _library.value
        if (bai.isEmpty()) {
            _tienTaiLoi.value = TienTaiLoi(0, 0, 0, xong = true)
            return
        }

        jobTaiLoi = scope.launch {
            val kho = lyricsRepo
            var daCo = 0
            _tienTaiLoi.value = TienTaiLoi(0, bai.size, 0, xong = false)
            for ((i, b) in bai.withIndex()) {
                if (!isActive) break
                // Da co san thi khong goi mang, va cung khong nghi.
                val sanCo = kho.daCoLoi(b.artist, b.title)
                val co = if (sanCo) true
                else {
                    val duoc = runCatching { kho.taiVaNho(b.artist, b.title, b.durationMs) }
                        .getOrDefault(false)
                    delay(NGHI_GIUA_HAI_LAN_MS)
                    duoc
                }
                if (co) daCo++
                _tienTaiLoi.value = TienTaiLoi(i + 1, bai.size, daCo, xong = false)
            }
            _tienTaiLoi.value = _tienTaiLoi.value?.copy(xong = true)
            Log.i(TAG, "Tai loi thu vien xong: $daCo/${bai.size}")
        }
    }

    fun thoiTaiLoi() {
        jobTaiLoi?.cancel()
        _tienTaiLoi.value = _tienTaiLoi.value?.copy(xong = true)
    }

    /** Bao nhieu bai trong thu vien da co loi trong kho. */
    fun demBaiCoLoi(context: Context): Int {
        chuanBi(context)
        val kho = lyricsRepo
        return _library.value.count { kho.daCoLoi(it.artist, it.title) }
    }

    /**
     * Nghi giua hai lan goi mang khi tai ca thu vien.
     *
     * LRCLIB khong cong bo mot han muc cu the, nen con so nay chon theo le
     * thuong cua mot ben dung nho: cham hon han mot nguoi dung binh thuong co
     * the sinh ra. Nam tram bai mat chung hai phut - dai, nhung day la viec
     * chay nen va nguoi dung khong ngoi nhin.
     */
    private const val NGHI_GIUA_HAI_LAN_MS = 250L

    /** Một bài tìm ra nhờ LỜI của nó, kèm đúng câu đã khớp. */
    data class BaiKhopLoi(val bai: Track, val cau: String)

    private val _ketQuaLoi = MutableStateFlow<List<BaiKhopLoi>>(emptyList())
    val ketQuaLoi: StateFlow<List<BaiKhopLoi>> = _ketQuaLoi.asStateFlow()

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            _ketQuaLoi.value = emptyList()
            _searching.value = false
            return
        }
        _searching.value = true
        searchJob = scope.launch {
            // Tim trong LOI chay truoc va o luong nen: no chi doc dia, khong
            // goi mang, nen xong truoc phan tim online va nguoi dung thay ket
            // qua som hon.
            _ketQuaLoi.value = withContext(Dispatchers.IO) { timTheoLoi(query) }
            val found = Catalog.search(query)
            _results.value = found
            _searching.value = false
            Log.i(TAG, "Tim \"$query\": ${found.size} ket qua, ${_ketQuaLoi.value.size} theo loi")
        }
    }

    /**
     * Nhung bai trong may co LOI chua chuoi dang tim.
     *
     * Noi tu kho loi ve bai bang cach bam ten: kho loi khong giu duong dan tep,
     * no chi biet ca si va ten bai. Nen di tu THU VIEN sang - bam ten tung bai
     * roi hoi kho xem ban ghi do co nam trong so vua khop khong.
     *
     * Lay ca LOI TU NHAP. Do la lời người dùng tự gõ, tức là bài họ quan tâm
     * nhất; bỏ nó ra thì đúng những bài ấy lại là những bài không tìm được.
     */
    private fun timTheoLoi(query: String): List<BaiKhopLoi> {
        val thu = _library.value
        if (thu.isEmpty()) return emptyList()

        val theoKhoa = HashMap<String, String>()
        cache?.timTrongLoi(query)?.forEach { theoKhoa[it.khoa] = it.cau }

        val kim = normalizeForCompare(query)
        // Loi tu nhap khong nam trong kho dem, phai hoi rieng. Doi chieu theo
        // TEN chu khong theo khoa: hai kho bam ten theo hai cach khac nhau.
        val theoTen = HashMap<String, String>()
        if (kim.isNotBlank()) {
            manual?.tatCa()?.forEach { ban ->
                val cau = ban.loi.split('\n')
                    .firstOrNull { normalizeForCompare(it).contains(kim) } ?: return@forEach
                theoTen[normalizeForCompare(ban.caSi) + "|" + normalizeForCompare(ban.tenBai)] =
                    cau.trim()
            }
        }
        if (theoKhoa.isEmpty() && theoTen.isEmpty()) return emptyList()

        val ra = ArrayList<BaiKhopLoi>()
        for (bai in thu) {
            val cau = theoTen[
                normalizeForCompare(bai.artist) + "|" + normalizeForCompare(bai.title)
            ] ?: cache?.khoaCua(bai.artist, bai.title)?.let { theoKhoa[it] } ?: continue
            ra += BaiKhopLoi(bai, cau)
            if (ra.size >= TRAN_KHOP_LOI) break
        }
        return ra
    }

    /**
     * Nhieu nhat chung nay bai tim ra theo loi.
     *
     * Mot chuoi ngan nhu "yeu" khop hang tram bai, va mot danh sach nhu the
     * khong tra loi duoc cau hoi nao - no chi day phan ket qua theo TEN, thu
     * nguoi dung nhieu kha nang dang tim hon, xuong duoi tam nhin.
     */
    private const val TRAN_KHOP_LOI = 30

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
     * AURA dang phat thi bam thang vao bo phat cua minh - chinh xac hon va
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
        // Tam dung roi thi `tuPhat` la false, nhung neu AURA van la ben giu hang
        // doi thi nut Phat phai danh thuc bo phat cua AURA chu khong phai app
        // khac - nguoi dung vua nghe bai cua AURA, khong doi y giua chung.
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
     * Bao lau ghi lai cho dang nghe mot lan.
     *
     * Nam giay: du thua de khong lam ban dia, du day de mat nhieu nhat nam giay
     * khi tien trinh bi giet dot ngot.
     */
    private const val GHI_CHO_MOI_MS = 5_000L

    /**
     * Nghi bao lau moi ngo dong ho sao luu mot lan. Xem `soatSaoLuuTuDong`.
     *
     * Mot tieng: cho goi day dac nhat la moi thong bao nhac moi, ma nhip ghi
     * sao luu it nhat cung la MOT NGAY. Nghi mot tieng thi mot ngay chi soat
     * chung hai muoi bon lan - khong dang ke - va van bat duoc moc han trong
     * vong mot tieng ke tu luc no toi.
     */
    private const val NGHI_GIUA_HAI_LAN_SOAT_MS = 60L * 60 * 1000

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
     * Bai va cau da dua len widget lan truoc.
     *
     * Cung ly do voi `cardLine`: moi lan day `RemoteViews` la mot lan vuot qua
     * ranh gioi tien trinh, nen chi day khi chu doi that.
     *
     * Giu RIENG voi `cardLine` chu khong dung chung mot bien: the media chi cap
     * nhat khi CHINH AURA phat, con widget bam theo ca nhac o app khac. Gop lam
     * mot thi mot trong hai luon sai.
     */
    private var widgetBai: String? = null
    private var widgetCau: String? = null
    private var widgetDangTai: Boolean? = null
    private var widgetBiaDaTinh: android.graphics.Bitmap? = null
    private var widgetMau: Int? = null

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
            // kien nay - bat khung roi khoa may la AURA ve lai 10 lan moi giay
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
            pushLineToWidget(position)
            nhoChoNgheDo(position)
            nhoLichSu()
            appContext?.let { ngoDoanLap(it, position) }

            // Chay tiep chung nao con viec de lam. Truoc day nhip chi song theo
            // khung noi; gio AURA tu phat duoc, va luc do the media van can duoc
            // cap nhat du khung noi dang tat.
            //
            // Man hinh tat thi CHAM lai chu khong dung han: cau dang hat van
            // phai dung tren the media o man hinh khoa. Mot giay mot lan la du -
            // khong ai doc nhanh hon the.
            // Dang lap mot doan thi nhip phai chay du khung noi tat va du AURA
            // khong phai ben phat: cai quyet dinh la vi tri da toi cuoi doan
            // chua, va cau hoi do chi tra loi duoc bang cach hoi lien tuc.
            if (overlay.isShowing || localPlayer?.isPlaying == true ||
                _doanLap.value != null || widgetCanNhip()
            ) {
                handler.postDelayed(this, nhipToi(position))
            }
        }
    }

    /**
     * Bao cho nhip biet man hinh vua tat hay vua bat.
     *
     * Goi tu `AuraNotificationListener` - dich vu do song lau hon moi Activity,
     * va la cho duy nhat con song khi nguoi dung da roi app.
     */
    fun manHinhDoi(sang: Boolean) {
        if (manHinhSang == sang) return
        manHinhSang = sang
        // Vua bat lai: ve ngay chu khong doi het nhip cham - mo may len ma nhin
        // mot khung loi da chet mot giay thi thay lien.
        if (sang) startTick()
    }

    /** Doc lai bai dang phat tu bo may phat cua chinh AURA. */
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
     * Dua cau dang hat len the media cua chinh AURA.
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
     * Nguoi dung vua them hoac vua go mot widget.
     *
     * Goi tu `KhungLoiWidget.onUpdate`. Hai viec: quen so widget da dem, va BAT
     * LAI NHIP. Viec thu hai moi la viec that - neu luc nay nhac dang phat o
     * Zing ma khung loi noi dang tat thi nhip da dung tu lau, va khong con ai
     * danh thuc no day nua; widget vua tha xuong se dung yen mai mai.
     *
     * Tien trinh vua bi danh thuc chi de nhan broadcast thi `appContext` con
     * rong: luc do khong co nhac nao dang phat de ma bam theo, va khong lam gi
     * la dung.
     */
    fun widgetDoi(context: Context) {
        KhungLoiWidget.demLai()
        if (appContext != null) startTick()
    }

    /** Lan ghi cho nghe do gan nhat, theo dong ho `elapsedRealtime`. */
    private var lanGhiChoNgheDo = 0L

    /**
     * Nho dang nghe toi dau, vai giay mot lan.
     *
     * CHI KHI AURA TU PHAT. Nhac o Zing hay YouTube thi ben do giu cho cua ho,
     * ma AURA cung khong tua duoc cho ho - nho ho mot con so roi khong bao gio
     * dung duoc no la tu lua minh.
     *
     * GHI THUA, KHONG GHI THIEU. Nguoi dung tat app bang cach vuot no ra khoi
     * danh sach gan day, hoac he thong giet tien trinh de lay bo nho - ca hai
     * deu khong bao truoc. Neu doi toi luc "dung phat" moi ghi thi dung nhung
     * lan do lai la nhung lan mat. Nam giay mot lan nghia la mat nhieu nhat nam
     * giay, va cai gia la mot lan ghi tep vai KB moi nam giay trong luc phat.
     */
    private fun nhoChoNgheDo(position: Long) {
        if (localPlayer?.isPlaying != true) return
        val context = appContext ?: return
        val gio = SystemClock.elapsedRealtime()
        if (gio - lanGhiChoNgheDo < GHI_CHO_MOI_MS) return
        lanGhiChoNgheDo = gio
        Playback.ghiChoNgheDo(context, position)
    }

    /**
     * Bai dang duoc bam gio nghe.
     *
     * Giu ca ban tin chu khong chi khoa: luc quyet dinh ghi hay khong, bai da
     * doi roi va `_now` da la bai SAU. Chi giu khoa thi toi luc ghi khong con
     * ten, ca si hay do dai cua chinh bai vua nghe xong.
     */
    private var baiDangNghe: NowPlaying? = null

    /** Dia chi phat cua bai dang bam gio, chot luc no bat dau. */
    private var diaChiDangNghe = ""

    /** Chinh AURA phat bai dang bam gio hay mot app khac. */
    private var tuPhatBaiDangNghe = false

    /** Da phat duoc bao lau cua bai nay, chi cong nhung quang dang phat that. */
    private var daPhatMs = 0L

    /**
     * Luc bat dau quang dang phat hien tai, theo `elapsedRealtime`.
     *
     * 0 nghia la dang khong phat - nen khong co quang nao dang chay de cong.
     */
    private var mocPhat = 0L

    /** Bai nay da vao lich su roi, khoi ghi lai moi lan goi. */
    private var daVaoLichSu = false

    /**
     * Bam gio bai dang nghe, va ghi vao lich su khi da du lau.
     *
     * KHONG DUA VAO NHIP. Nhip chi song khi AURA tu phat, khi khung loi noi bat,
     * hoac khi co widget - ma lich su thi phai ghi ca luc nhac chay o Zing hay
     * YouTube voi ca ba thu do deu tat. Nen ham nay duoc goi tu CHINH DONG
     * `_now`: ban tin media bao moi lan doi bai, bam phat, bam dung. Do la dung
     * nhung luc con so nay can doi.
     *
     * DO BANG QUANG DANG PHAT, khong cong tung nhip. Ban tin media co the im
     * lang suot ba phut giua bai, va ba phut do la ba phut nguoi ta thuc su
     * dang nghe. Con luc bam tam dung thi quang do dong lai - nguoi dung tam
     * dung roi di an com, quay lai bam tiep, va bua com khong duoc tinh la nghe.
     *
     * GHI CA NHAC CUA APP KHAC. Day la cho AURA lam duoc thu ma tung app rieng
     * le khong lam: no dung ngoai va nghe ca hai, nen lich su o day la lich su
     * cua NGUOI DUNG chu khong phai cua mot app.
     */
    private fun nhoLichSu() {
        val now = _now.value
        val gio = SystemClock.elapsedRealtime()

        // Chot quang dang chay TRUOC moi viec khac: du sap doi bai hay sap dung,
        // phan da nghe cua quang do van la da nghe.
        if (mocPhat > 0L) {
            daPhatMs += (gio - mocPhat).coerceAtLeast(0L)
            mocPhat = 0L
        }

        // Xet ghi TRUOC KHI doi sang bai moi. Lan goi lam bai chay het la lan
        // `_now` bao da sang bai sau, nen neu doi bien truoc roi moi xet thi bai
        // vua nghe tron ven lai la bai duy nhat khong bao gio duoc ghi.
        chotLichSu()

        if (now?.key != baiDangNghe?.key) {
            baiDangNghe = now
            tuPhatBaiDangNghe = laLyraPhat()
            diaChiDangNghe =
                if (tuPhatBaiDangNghe) Playback.currentTrack?.uri.orEmpty() else ""
            daPhatMs = 0L
            daVaoLichSu = false
        }

        // Mo lai moc chi khi dang phat. Dang dung thi khong co quang nao chay.
        if (now?.isPlaying == true) mocPhat = gio
    }

    /** Ghi bai dang bam gio vao lich su, neu da nghe du lau va chua ghi. */
    private fun chotLichSu() {
        if (daVaoLichSu) return
        val bai = baiDangNghe ?: return
        val kho = lichSu ?: return
        if (!LichSuNghe.dangGhi(bai.duration, daPhatMs)) return
        daVaoLichSu = true
        kho.ghi(
            LanNghe(
                // Dia chi CHI co khi chinh AURA phat. Nhac o Zing hay YouTube
                // thi AURA khong co duong nao mo lai duoc, va mot dong bam vao
                // khong ra gi con te hon mot dong noi thang la chi de nho.
                diaChi = diaChiDangNghe,
                ten = bai.title,
                caSi = bai.artist,
                app = if (tuPhatBaiDangNghe) "" else bai.packageName,
                luc = System.currentTimeMillis()
            )
        )
    }

    /**
     * Nhip co phai chay tiep vi widget khong.
     *
     * BA dieu kien, thieu mot la sai:
     *
     *   - co widget tren man hinh chinh. Khong co thi khong ai xem.
     *   - dang co nhac phat. Nhac tat roi ma van tick moi giay la dot pin de
     *     ve lai dung mot cau da dung yen.
     *   - man hinh dang sang. Man hinh tat thi widget cung khuat, y het ly do
     *     khung noi ngung ve khi khoa may.
     *
     * Day la CHI PHI THAT cua widget: nhac phat o Zing ma khung noi dang tat
     * thi truoc kia nhip dung han, gio no chay. Bang dung chi phi cua khung
     * noi, va doi lai dung mot thu - cau dang hat tren man hinh chinh.
     */
    private fun widgetCanNhip(): Boolean {
        if (!manHinhSang) return false
        if (_now.value?.isPlaying != true) return false
        val context = appContext ?: return false
        return KhungLoiWidget.dangDung(context)
    }

    /**
     * Dua cau dang hat len widget khung loi.
     *
     * Khac `pushLineToCard` o mot cho quan trong: cai kia chi lam khi CHINH
     * AURA phat, con cai nay chay ca khi nhac o Zing hay YouTube Music. The
     * media la cua ben dang phat nen ta khong ghi vao duoc; widget la cua
     * AURA nen ghi duoc, va do la dung cho AURA co ich nhat.
     */
    private fun pushLineToWidget(position: Long) {
        // Man hinh tat thi widget khuat - khong ve. Va vi ham nay thoat TRUOC
        // khi dung den `widgetBai`/`widgetCau`, hai bien do van dung voi thu
        // dang nam tren widget, nen luc man hinh sang lai, nhip dau tien tinh
        // ra cau moi, thay khac, va day len ngay.
        if (!manHinhSang) return
        val context = appContext ?: return
        if (!KhungLoiWidget.dangDung(context)) return

        val n = _now.value
        if (n == null || !n.isPlaying) {
            widgetBai = null
            widgetCau = null
            KhungLoiWidget.dat(context, null, null, false)
            return
        }

        val loi = lyricsRepoOrNull?.lyrics?.value
        // Moc dang ngo thi khong dua gi len, cung ly do voi the media: tren man
        // hinh chinh nguoi dung khong co cach nao doi chieu xem cau do co dung
        // khong, nen mot cau sai o day dang tin hon han - va vi the tai hai hon.
        val cau = if (loi == null || !loi.synced || loi.timingSuspect) null
        else loi.lines.getOrNull(activeLineIndex(loi.lines, position, loi.offset))
            ?.text?.takeIf { it.isNotBlank() }

        val doiBai = n.title != widgetBai
        val dangTai = lyricsRepoOrNull?.loading?.value == true

        // Bai KHONG CO LOI ma viec tim vua xong. Day la lan day duy nhat tat
        // duoc dong "dang tim loi" tren widget: tu day tro di ten bai khong
        // doi, cau van null, nen khong con dip nao nua.
        //
        // Phai kem dieu kien KHONG CO LOI. Bai co loi ma dung luc tim xong lai
        // roi vao mot dong trong giua hai doan thi cau cung null, va day luc do
        // se ghi "chua co loi cho bai nay" len mot bai co loi han hoi.
        val coLoi = loi != null && loi.lines.isNotEmpty()
        val vuaTimXong = !coLoi && dangTai != widgetDangTai

        // Dong trong giua hai doan thi GIU NGUYEN cau vua hat, khong tra widget
        // ve trang. File .lrc nao cung co nhung dong trong nhu vay, va tra ve
        // roi hien lai cu vai giay mot lan bien man hinh chinh thanh mot cho
        // nhap nhay - trong khi cai nguoi ta muon chi la doc duoc cau vua nghe.
        if (cau == null && !doiBai && !vuaTimXong) return

        // KHONG DAY LAI THU DA NAM SAN TREN WIDGET.
        //
        // Do duoc bang nhat ky khi thu voi NhacCuaTui: dung mot cau bi day len
        // MUOI LAN MOI GIAY suot ca bai. `widgetCau` von duoc ghi o duoi nhung
        // khong cho nao doc, nen cai chan duy nhat la dong `cau == null` ngay
        // tren - ma dong do chi chan luc KHONG co cau.
        //
        // Moi lan day la mot lan dung `RemoteViews` roi goi qua tien trinh cua
        // launcher. Cau chu chi doi vai giay mot lan, nen chin muoi chin phan
        // tram so lan goi ay khong doi mot diem anh nao.
        //
        // Tinh ca `dangTai` o day nua, cho truong hop bai CO loi: luc tim xong,
        // cau da co roi nhung cai co van phai duoc chot lai, khong thi lan sau
        // no doi mot minh se khong ai nhan ra.
        if (!doiBai && cau == widgetCau && dangTai == widgetDangTai) return

        Log.d(TAG, "widget: day '${n.title}' / '$cau'")

        widgetBai = n.title
        widgetCau = cau
        widgetDangTai = dangTai

        // MAU NEN LAY TU ANH BIA, tinh o day chu khong trong `KhungLoiWidget`.
        //
        // `dominantColor` doc tung diem anh, va widget thi duoc ve tu ca nhip
        // 10 lan moi giay lan tu broadcast cua he thong - de viec doc anh o do
        // la doc lai cung mot anh hang tram lan. O day no chi chay khi cau chu
        // that su doi, tuc vai giay mot lan.
        //
        // Uu tien anh AURA tu tai, roi moi toi anh app khac gui kem ban tin
        // media - cung thu tu voi mau nen cua trang Bai, de hai cho khong ra
        // hai mau cho cung mot bai.
        val bia = _artwork.value ?: n.artwork
        val mauNen = if (bia === widgetBiaDaTinh) widgetMau
        else {
            widgetBiaDaTinh = bia
            widgetMau = dominantColor(bia)?.toArgb()
            widgetMau
        }

        // Bo doan lap ten ca si o dau ten bai - xem `tenBaiDeHien`.
        KhungLoiWidget.dat(
            context,
            tenBaiDeHien(n.artist, n.title),
            cau,
            dangTai,
            mauNen
        )
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

                // BAT NHIP KHI APP KHAC BAT DAU PHAT.
                //
                // Truoc day nhip chi duoc danh thuc boi bon thu: bo may phat cua
                // CHINH AURA chay, man hinh vua bat, khung loi noi vua bat, va
                // widget vua duoc tha xuong. Khong co cai nao trong bon do noi
                // "co app khac vua bat nhac".
                //
                // Do duoc tren may: tha widget xuong man hinh chinh, tat khung
                // loi noi, roi mo YouTube - widget nam im o dong chu "Mo mot bai
                // nhac de thay loi o day" suot ca bai. Dung cai viec no sinh ra
                // de lam thi no khong lam duoc.
                //
                // `startTick` tu don lich cu truoc khi dat lich moi, va ban than
                // `tick` tu quyet dinh co chay tiep khong - nen goi thua o day
                // chi ton dung mot vong.
                if (now?.isPlaying == true) startTick()

                // BAM GIO NGHE TU DAY chu khong tu nhip. Nhip chi song khi AURA
                // tu phat, khi khung loi noi bat hoac khi co widget - ma lich su
                // phai ghi ca luc nhac chay o app khac voi ca ba thu do deu tat.
                // Dong nay thi doi bai nao, bam phat nao cung co mot nhip.
                nhoLichSu()

                // Han cua kieu "het bai nay" phai tinh lai moi lan ban tin doi:
                // tua mot cai la con lai bao nhieu doi han.
                soatHenGio()
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
     * dung DA CAP QUYEN DOC THONG BAO. Ai chi nghe nhac trong may - dung AURA
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
        if (suaThe == null) suaThe = SuaThe(context.applicationContext)
        if (banDaChon == null) {
            banDaChon = ManualLyricStore(context.applicationContext, "loi-da-chon")
        }
        if (translationCache == null) translationCache = TranslationCache(context.applicationContext)
        if (translatePrefs == null) translatePrefs = TranslatePrefs(context.applicationContext)
        if (playlistStore == null) {
            playlistStore = PlaylistStore(context.applicationContext).also {
                scope.launch { it.playlists.collect { list -> _playlists.value = list } }
            }
        }
        // Qua `khoLichSu` chu khong tu dung kho o day: man hinh Chinh co the da
        // tao kho truoc khi `chuanBi` chay, va dung hai kho cho mot tep tren dia
        // thi ben nay ghi de mat ban cua ben kia.
        khoLichSu(context)
        khoYeu(context)
    }

    // ---- Tu sao luu ra thu muc nguoi dung chon ----

    private var tuSaoLuu: SaoLuuTuDong? = null

    fun khoTuSaoLuu(context: Context): SaoLuuTuDong =
        tuSaoLuu ?: SaoLuuTuDong(context.applicationContext).also { tuSaoLuu = it }

    /**
     * Chan khong cho ngo dong ho lien tuc. Xem [soatSaoLuuTuDong].
     *
     * `0` = chua soat lan nao trong tien trinh nay.
     */
    private var lanSoatCuoi = 0L

    /**
     * Ngo dong ho mot cai, qua han thi ghi mot ban sao luu.
     *
     * GOI TU NHIEU CHO, khong chi luc mo app - va day la mot lo hong co that
     * trong ban 0.3.34. Truoc do no chi duoc goi tu `MainActivity.onCreate`,
     * nen ai dung AURA dung theo cach no duoc lam ra - bat khung loi noi roi
     * nghe nhac o Zing, KHONG MO APP RA - thi tu luu khong bao gio chay. Ho
     * bat no len, doc thay chu "Dang tu luu", roi hang thang khong co ban nao
     * duoc ghi. Im lang, dung kieu hong ma chinh tinh nang nay sinh ra de
     * tranh.
     *
     * Gio con duoc goi tu `LyraNotificationListener` - cho ay song ca khi
     * khong ai mo app, va no khoi dong lai cung may.
     *
     * CHAN BANG [NGHI_GIUA_HAI_LAN_SOAT_MS] vi cho goi moi la cho chay day
     * dac: mot thong bao nhac moi la mot lan goi. Phep chan la mot phep tru
     * tren mot so nguyen trong bo nho, khong cham dia, nen goi bao nhieu lan
     * cung khong ton gi.
     */
    suspend fun soatSaoLuuTuDong(context: Context): SaoLuuTuDong.KetQua {
        val gio = System.currentTimeMillis()
        // Dong ho may co the bi keo lui - lay ca hai chieu de khong ket vinh
        // vien o nhanh "vua soat xong".
        if (lanSoatCuoi != 0L && kotlin.math.abs(gio - lanSoatCuoi) < NGHI_GIUA_HAI_LAN_SOAT_MS) {
            return SaoLuuTuDong.KetQua.ChuaToiHan
        }
        lanSoatCuoi = gio
        return withContext(Dispatchers.IO) {
            khoTuSaoLuu(context).soat { xuatTatCa(context) }
        }
    }

    suspend fun ghiSaoLuuNgay(context: Context): SaoLuuTuDong.KetQua =
        withContext(Dispatchers.IO) {
            khoTuSaoLuu(context).ghiNgay { xuatTatCa(context) }
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
            // `tenBaiDeHien` bo doan lap ten ca si o dau ten bai. Thieu no thi
            // nhac YouTube ra "Luis Fonsi — Luis Fonsi - Despacito ft. Daddy
            // Yankee" - da do duoc tren khung noi.
            n.artist.isNotEmpty() -> "${n.artist} — ${tenBaiDeHien(n.artist, n.title)}"
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
        // Bat khung trong luc dang o TRONG app thi chua ve ra voi.
        //
        // Ve ra ngay thi no de len chinh trang nguoi dung vua bam nut, va chan
        // luon cu cham tiep theo. Ghi nho la da bat, roi ra khoi app moi hien -
        // trang Chinh noi ro dieu do bang chu, khong de nguoi dung tuong hong.
        if (dangTrongApp) {
            anTamThoi = true
            prefs(context).setEnabled(true)
            _overlayOn.value = true
            return
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

    /**
     * Nguoi dung dang o TRONG app hay da ra ngoai.
     *
     * Khung loi noi sinh ra de nam tren app KHAC. Khi chinh AURA dang mo thi no
     * vua thua - loi da hien to o giua trang Bai roi - vua CHAN THAO TAC: khung
     * chiem mot dai ngang o phia tren, dung cho hang ket qua tim dau tien, va
     * moi cu cham vao do roi vao khung chu khong toi danh sach. Bai khong phat,
     * va khong co dau hieu nao noi tai sao.
     *
     * An TAM THOI, khong dung `hideOverlay`: ham do coi nhu nguoi dung tat han
     * va ghi vao cai dat. O day thi cai dat khong doi, `overlayOn` van bao dang
     * bat, va ra khoi app la khung tro lai ngay.
     */
    fun oTrongApp(context: Context, trong: Boolean) {
        dangTrongApp = trong
        if (trong) {
            if (overlay.isShowing) {
                handler.removeCallbacks(tick)
                overlay.hide()
                anTamThoi = true
            }
        } else if (anTamThoi) {
            anTamThoi = false
            showOverlay(context)
        }
    }

    /** Dang an vi nguoi dung o trong app, chu khong phai vi ho tat di. */
    private var anTamThoi = false

    /** Man hinh cua chinh AURA dang o truoc mat nguoi dung hay khong. */
    private var dangTrongApp = false

    fun hideOverlay() {
        handler.removeCallbacks(tick)
        overlay.hide()
        // Nguoi dung tu tat thi khong con gi de "hien lai" khi ho ra khoi app.
        anTamThoi = false
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
     * này tới câu kia" chứ không kéo hai cái mốc trên một thanh thời gian. AURA
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

        // Nhịp có thể đang ngủ (khung nổi tắt, AURA không phát). Đánh thức nó,
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

    /** AURA có phải là bên đang phát không — tốc độ chỉ đổi được khi đúng. */
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
    /**
     * Cac ban loi khac cho bai dang phat.
     *
     * Tra ve `null` khi hoi khong duoc - mat mang, may chu im. Khac han danh
     * sach RONG, va man hinh phai noi hai chuyen do bang hai cau khac nhau:
     * "khong tim thay ban nao" va "khong hoi duoc" doi hoi nguoi dung lam hai
     * viec khac nhau.
     */
    suspend fun banLoiKhac(): List<Lyrics>? = try {
        lyricsRepo.ungVien()
    } catch (e: Exception) {
        Log.w(TAG, "Khong lay duoc danh sach ban loi", e)
        null
    }

    fun chonBanLoi(ban: Lyrics) = lyricsRepo.chonBan(ban)

    fun boBanLoiDaChon() = lyricsRepo.boBanDaChon()

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
     * CHI cho thu AURA tu phat: bai phat o app khac thi AURA khong biet no doc
     * tep nao, ma cung khong co quyen hoi.
     *
     * CAU TREN DA DUNG TU DAU, MA MA THI KHONG LAM DUNG NHU VAY - va do la mot
     * loi that, do duoc tren may:
     *
     *   phat mot video trong may (co tep .srt nam canh) -> mo YouTube -> dau
     *   trang doi sang "YouTube / Luis Fonsi" DUNG, nhung khung loi van giu
     *   phu de cua video cu, va khong mot lan tim loi nao chay.
     *
     * Vi `Playback.currentTrack` KHONG rong di khi AURA thoi phat - no giu lai
     * bai cuoi cung bo may phat cua AURA nap. Nen ham nay van tra ve tep .lrc
     * cua bai do, `LyricsRepository` thay co loi roi thi dung ngay tai day, va
     * khong bao gio hoi toi lrclib. Nguoi dung nghe Despacito ma doc phu de cua
     * mot video khac.
     *
     * `Playback.currentTrack != null` tra loi cau "AURA CO TUNG phat gi khong".
     * Cau can hoi la "AURA CO DANG phat khong", va cau do do `_now` tra loi.
     */
    private suspend fun loiCanhTep(): Lyrics? {
        val ctx = appContext ?: return null
        if (!laLyraPhat()) return null
        // Hoi bo may phat dang mo tep nao: PHAI o luong chinh. `MediaController`
        // kiem tra luong va nem `IllegalStateException` neu goi tu cho khac.
        val uri = Playback.currentTrack?.uri ?: return null
        // Con doc dia va hoi MediaStore thi dua het xuong luong nen: mot vong
        // goi sang tien trinh khac tren luong ve la du de rot khung hinh.
        return withContext(Dispatchers.IO) { LrcCanhTep.doc(ctx, uri) }
    }

    /**
     * Bai dang phat co phai tep trong may khong.
     *
     * Chi khi do moi co cho ma ghi tep .lrc nam canh - nhac phat tu Zing hay
     * tu app khac thi khong co tep nao tren dia ca. Bay ra mot loi moi khong
     * bam duoc con te hon la khong bay.
     *
     * Hoi CA `_now` chu khong chi `Playback.currentTrack`, cung ly do voi
     * `loiCanhTep` o duoi: bai cuoi cung bo may phat cua AURA nap thi nam lai
     * do mai, ke ca khi nhac da chuyen sang app khac. Thieu ve nay thi dang
     * nghe Zing ma AURA van moi "Ghi loi ra tep .lrc nam canh bai nhac" - ghi
     * canh tep nao thi khong ai biet.
     */
    fun laNhacTrongMay(): Boolean =
        laLyraPhat() && Playback.currentTrack?.uri?.startsWith("lyra://may/") == true

    /**
     * Ghi loi dang hien ra tep .lrc nam canh tep nhac.
     *
     * Ghi dung chuoi ma man hinh soan loi dang cho sua: co moc thi ra ban co
     * moc, chua co thi ra chu tron. Nguoi dung thay gi thi tep mang cai do.
     */
    fun ghiLoiRaTepCanh(context: Context, deLen: Boolean = false): LrcCanhTep.KetQuaGhi {
        val bai = Playback.currentTrack
            ?: return LrcCanhTep.KetQuaGhi.KhongPhaiTepTrongMay
        val now = _now.value
        return LrcCanhTep.ghi(
            context = context,
            uri = bai.uri,
            loi = lyricsRepo.manualDraft(),
            tenBai = now?.title ?: bai.title,
            caSi = now?.artist ?: bai.artist,
            deLen = deLen
        )
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
        // Hoi TRANG THAI NGUOI DUNG DA DAT, khong hoi "cua so co dang ve khong".
        //
        // Tu khi khung tu an luc nguoi dung o trong app, hai thu do khac nhau:
        // dung o trang Chinh thi cua so KHONG BAO GIO dang ve, nen cach hoi cu
        // luon hieu la "dang tat" va bam nut "Tat loi noi" lai di bat len. Nut
        // tro thanh bam bao nhieu lan cung khong tat duoc.
        if (_overlayOn.value) hideOverlay() else showOverlay(context)
        return _overlayOn.value
    }
}
