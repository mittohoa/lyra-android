package com.mittohoa.lyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mittohoa.lyra.data.ChuDe
import com.mittohoa.lyra.data.KieuChu
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.data.OverlayLook
import com.mittohoa.lyra.data.TranslateSettings
import com.mittohoa.lyra.lyrics.LyricLine
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.sources.MediaKind
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.sources.Track
import com.mittohoa.lyra.translate.READING_LANGUAGES
import com.mittohoa.lyra.translate.TranslationState
import com.mittohoa.lyra.translate.languageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Man hinh chinh.
 *
 * Khong co thanh tieu de, khong co thanh dieu huong, khong co ngan keo. Toan bo
 * chrome cua app la MOT VIEN THUOC noi o day man hinh - cham de doi trang, va
 * chinh no cho biet dang o dau.
 *
 * Ly do khong phai de la: ba man hinh nay deu la mot dong noi dung duy nhat
 * chay theo bai hat. Dat mot thanh tieu de co dinh len tren la cat doi cai dong
 * do, va an di dung phan dang xem - anh bia va loi. Nen nen anh trai het man
 * hinh, con dieu khien thi noi len tren no.
 */

/**
 * Bon trang, xep theo dung thu tu nguoi dung di qua chung: tim bai, nghe, doc
 * loi, chinh. Trang mo dau la "Dang phat" chu khong phai "Tim" - phan lon lan
 * mo app la de xem dang phat gi, khong phai de tim bai moi.
 */
private val PANES = listOf("Tìm", "Bài", "Chỉnh")
private const val START_PANE = 1

@Composable
fun HomeScreen(
    now: NowPlaying?,
    lyrics: Lyrics,
    loading: Boolean,
    position: State<Long>,
    hasNotificationAccess: Boolean,
    canDrawOverlay: Boolean,
    overlayOn: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onToggleOverlay: () -> Unit,
    onSyncToLine: (Int) -> Unit,
    onSeekToLine: (Int) -> Unit,
    onClearOffset: () -> Unit,
    look: OverlayLook,
    suggestedFontSize: Float,
    onLookChange: (OverlayLook) -> Unit,
    onEditLyrics: () -> Unit,
    translation: TranslationState,
    translateSettings: TranslateSettings,
    onDownloadModel: () -> Unit,
    onTranslateChange: (TranslateSettings) -> Unit,
    canAddTile: Boolean,
    onAddTile: () -> Unit,
    searchQuery: String,
    results: List<Track>,
    searching: Boolean,
    queue: List<Track>,
    queueIndex: Int,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onPlayResult: (Int) -> Unit,
    onEnqueue: (Track) -> Unit,
    onSkipInQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    artwork: android.graphics.Bitmap?,
    shuffle: Boolean,
    repeat: Int,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    library: List<Track>,
    canReadLibrary: Boolean,
    onAskLibrary: () -> Unit,
    onPlayFromLibrary: (Int) -> Unit,
    playlists: List<com.mittohoa.lyra.data.Playlist>,
    openedPlaylist: com.mittohoa.lyra.data.Playlist?,
    onOpenPlaylist: (String?) -> Unit,
    onPlayPlaylistAt: (Int) -> Unit,
    onRemoveFromPlaylist: (Int) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onSaveQueue: (String) -> Unit,
    downloads: Map<String, com.mittohoa.lyra.service.Lyra.Downloading>,
    onDownload: (Track) -> Unit,
    banMoi: String?,
    capNhat: Lyra.TrangThaiCapNhat?,
    tuCaiDuoc: Boolean,
    onCapNhat: () -> Unit,
    lyricEffect: LyricEffect,
    onLyricEffectChange: (LyricEffect) -> Unit,
    chuDe: ChuDe,
    onChuDeChange: (ChuDe) -> Unit,
    kieuChu: KieuChu,
    onKieuChuChange: (KieuChu) -> Unit,
    onXemLoi: (com.mittohoa.lyra.sources.Track) -> Unit,
    onLuuLoiDaCan: (String) -> Unit
) {
    // Mau nen lay tu anh bia. Doi bai thi chuyen mau tu tu chu khong nhay cai -
    // nhay mau la thu mat nhat khi nghe nhac.
    //
    // Tinh tren LUONG NEN. Doc diem anh la viec cua CPU, lam trong than
    // composable nghia la lam tren luong chinh giua luc dang dung giao dien -
    // dung mot nhip ngay khi doi bai, va do la luc nguoi dung dang nhin nhat.
    val target by produceState(FALLBACK, now?.key, artwork) {
        // `artwork` la anh Lyra tu tai ve cho bai chinh no phat; `now.artwork` la
        // anh app khac gui kem ban tin media. Uu tien cai dau vi no ro net hon -
        // ban tin media thuong chi kem mot anh nho.
        val art = artwork ?: now?.artwork
        value = withContext(Dispatchers.Default) { dominantColor(art) } ?: FALLBACK
    }
    val accent by animateColorAsState(target, tween(900), label = "accent")

    // Câu đang mở thẻ lời; -1 là đang đóng.
    //
    // Giữ ở TẦNG MÀN HÌNH chứ không trong `BaiPane`. Bản đầu để trong đó và
    // dựng tấm thẻ làm con thứ hai của một trang `HorizontalPager` — nút bấm
    // chạy, trạng thái đổi, mà tấm thẻ không bao giờ hiện ra: chỗ đặt nội dung
    // một trang pager không hứa hẹn gì về việc xếp chồng nhiều con. Ở đây thì
    // nó là con của một `Box` thật, và `Box` thì xếp chồng theo đúng thứ tự.
    var cauChiaSe by remember { mutableIntStateOf(-1) }

    // Video toan man hinh. Giu o day chu khong trong BaiPane: no phai phu len
    // CA man hinh, ma o trong trang thi no chi phu duoc mot trang cua bo vuot.
    var videoToanManHinh by remember { mutableStateOf(false) }
    var moCanGio by remember { mutableStateOf(false) }

    val pager = rememberPagerState(initialPage = START_PANE, pageCount = { PANES.size })
    val scope = rememberCoroutineScope()

    // Mau lay tu anh bia, chinh MOT LAN o day roi truyen xuong duoi ten
    // `accent`. Cac trang ben duoi khong phai biet hom nay dang la mat giay hay
    // mat muc - chung chi nhan mot mau da dung san. Chinh o moi noi dung thi
    // som muon co mot cho quen chinh, va cho do se chinh mot mau khong doc duoc
    // tren nen giay.
    val mucMau = mau.mucMau(accent)

    Box(Modifier.fillMaxSize().background(mau.nen)) {

        // LE MUC.
        //
        // Mot dai mau chay doc suot mep trai, co mat o CA BON trang. Day la dau
        // hieu rieng cua Lyra, va no khong phai trang tri: mau lay tu anh bia
        // bai dang phat, nen ca app doi mau theo bai - mot dau hieu ngoai le
        // cua trang giay noi rang ben trong dang co nhac.
        //
        // Nhat dan xuong duoi nhu muc in tham vao giay, de no la mot canh giay
        // chu khong phai mot thanh giao dien.
        Box(
            Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            mucMau.copy(alpha = 0.92f),
                            mucMau.copy(alpha = 0.30f)
                        )
                    )
                )
        )

        // KHONG co thanh tieu de.
        //
        // Truoc day o day co mot dau trang: dau hieu Lyra, chu "LYRA", va ten
        // trang dang mo. No doc dep nhung no khong noi gi moi - vien thuoc duoi
        // day da noi dang o trang nao, va trang Bai da co dai ngu canh noi dang
        // nghe bai gi. Ba dong cung mot tin.
        //
        // Doi lai la gan 90dp chieu cao cho phan doc, tren mot man hinh ma thu
        // dang doc la loi bai hat. Dau hieu cua app khong mat: le muc chay doc
        // mep trai va bo chu co chan lam viec do o moi trang.
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(start = 6.dp, top = 8.dp)) {

            // Có bản mới thì báo ở TẦNG APP, không phải bên trong một trang.
            // Trang phát thoát sớm khi chưa có quyền đọc thông báo hoặc chưa có
            // gì đang phát — mà đó đúng là trạng thái của một máy vừa cài xong,
            // tức là người cần biết tin này nhất lại là người không thấy nó.
            if (banMoi != null) {
                Box(Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp)) {
                    Notice(
                        accent = mucMau,
                        text = when (capNhat) {
                            null -> "Có bản $banMoi"
                            is Lyra.TrangThaiCapNhat.DangTai ->
                                if (capNhat.phanTram < 0) "Đang tải bản mới…"
                                else "Đang tải bản mới… ${capNhat.phanTram}%"
                            // Đoạn này lâu hàng chục giây vì Play Protect gửi cả
                            // file lên Google quét. Không nói ra thì người dùng
                            // nhìn thanh 100% đứng im và kết luận là treo.
                            Lyra.TrangThaiCapNhat.ChoHeThong ->
                                "Đang chờ hệ thống kiểm tra và cài…"
                            is Lyra.TrangThaiCapNhat.Hong -> capNhat.vi
                        },
                        action = when (capNhat) {
                            null -> if (tuCaiDuoc) "Cập nhật" else "Xem"
                            is Lyra.TrangThaiCapNhat.Hong -> "Mở trang tải"
                            else -> null
                        },
                        onAction = onCapNhat
                    )
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                pageSpacing = 0.dp
            ) { page ->
                when (page) {
                    0 -> SearchPane(
                        accent = mucMau,
                        query = searchQuery,
                        results = results,
                        searching = searching,
                        playingUri = queue.getOrNull(queueIndex)?.playbackUri,
                        onQueryChange = onSearchQueryChange,
                        onSubmit = onSearchSubmit,
                        onPlay = onPlayResult,
                        onEnqueue = onEnqueue,
                        library = library,
                        canReadLibrary = canReadLibrary,
                        onAskLibrary = onAskLibrary,
                        onPlayFromLibrary = onPlayFromLibrary,
                        playlists = playlists,
                        openedPlaylist = openedPlaylist,
                        onOpenPlaylist = onOpenPlaylist,
                        onPlayPlaylistAt = onPlayPlaylistAt,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                        onRenamePlaylist = onRenamePlaylist,
                        onDeletePlaylist = onDeletePlaylist,
                        downloads = downloads,
                        onDownload = onDownload,
                        onXemLoi = {
                            onXemLoi(it)
                            scope.launch { pager.animateScrollToPage(1) }
                        }
                    )
                    1 -> BaiPane(
                        now = now,
                        lyrics = lyrics,
                        loading = loading,
                        position = position,
                        accent = mucMau,
                        artwork = artwork,
                        queue = queue,
                        queueIndex = queueIndex,
                        shuffle = shuffle,
                        repeat = repeat,
                        hasNotificationAccess = hasNotificationAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onSkipInQueue = onSkipInQueue,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onSaveQueue = onSaveQueue,
                        translation = translation,
                        onSyncToLine = onSyncToLine,
                        onSeekToLine = onSeekToLine,
                        onClearOffset = onClearOffset,
                        onEditLyrics = onEditLyrics,
                        onDownloadModel = onDownloadModel,
                        effect = lyricEffect,
                        onChiaSeCau = { cauChiaSe = it.coerceAtLeast(0) },
                        onToanManHinh = { videoToanManHinh = true },
                        toanManHinh = videoToanManHinh,
                        onCanGio = { moCanGio = true }
                    )
                    else -> TunePane(
                        canDrawOverlay = canDrawOverlay,
                        overlayOn = overlayOn,
                        accent = mucMau,
                        look = look,
                        suggestedFontSize = suggestedFontSize,
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onToggleOverlay = onToggleOverlay,
                        onLookChange = onLookChange,
                        translateSettings = translateSettings,
                        onTranslateChange = onTranslateChange,
                        canAddTile = canAddTile,
                        onAddTile = onAddTile,
                        lyricEffect = lyricEffect,
                        onLyricEffectChange = onLyricEffectChange,
                        chuDe = chuDe,
                        onChuDeChange = onChuDeChange,
                        kieuChu = kieuChu,
                        onKieuChuChange = onKieuChuChange
                    )
                }
            }

            Pill(
                current = pager.currentPage,
                accent = mucMau,
                onPick = { scope.launch { pager.animateScrollToPage(it) } }
            )
        }

        if (moCanGio && now != null) {
            CanGioManHinh(
                cacDong = lyrics.lines,
                tenBai = now.title,
                caSi = now.artist,
                accent = mucMau,
                dangPhat = now.isPlaying,
                onLuu = {
                    onLuuLoiDaCan(it)
                    moCanGio = false
                },
                onDong = { moCanGio = false }
            )
        }

        // Video toan man hinh. Chi mo khi bai dang phat DUNG la video: bam nut
        // roi doi bai sang mot ban nhac thi day tu dong lai, khong de mot man
        // hinh den phu len ca app.
        val baiDangPhat = queue.getOrNull(queueIndex)

        // Video het roi sang mot bai nhac thi dong lop phu lai.
        //
        // Lam bang `LaunchedEffect` chu khong bang mot phep gan ngay trong luc
        // dung giao dien: ghi vao trang thai giua chung mot lan dung la thu
        // Compose khong hua se chay - thu lan dau viet the, va lop phu nam lai
        // tren mot bai nhac khong co hinh.
        LaunchedEffect(baiDangPhat?.kind) {
            if (videoToanManHinh && baiDangPhat?.kind != MediaKind.VIDEO) {
                videoToanManHinh = false
            }
        }

        if (videoToanManHinh) {
            if (baiDangPhat?.kind == MediaKind.VIDEO) {
                ToanManHinh(
                    tiLe = baiDangPhat.tiLe,
                    accent = mucMau,
                    dangPhat = now?.isPlaying == true,
                    viTri = position,
                    doDai = now?.duration ?: 0L,
                    onPhatDung = onPlayPause,
                    onTruoc = onPrevious,
                    onSau = onNext,
                    onTua = onSeek,
                    onDong = { videoToanManHinh = false }
                )
            }
        }

        if (cauChiaSe >= 0 && now != null) {
            TheLoiManHinh(
                cacDong = lyrics.lines,
                dongDau = cauChiaSe,
                tenBai = now.title,
                caSi = now.artist,
                accent = mucMau,
                kieuChu = kieuChu,
                bia = artwork ?: now.artwork,
                onDong = { cauChiaSe = -1 }
            )
        }
    }
}

/**
 * Ba vạch ở đáy màn hình: đang ở trang nào, và chạm để sang trang khác.
 *
 * Trước đây đây là một viên thuốc có nền, có chữ tên trang. Nó nói đúng nhưng
 * nói to quá: trên trang Bài nó nằm ngay dưới hàng nút phát và thành ra hai
 * hàng nút chồng lên nhau, còn cái vỏ nền thì cắt ngang đáy màn hình.
 *
 * Xem Zing, NCT, YouTube Music và cả Spotify thì trình phát của cả bốn đều
 * CHE LUÔN thanh điều hướng — trong màn hình đang phát không app nào để tab
 * bar. Cảm giác "liền mạch" của chúng đến từ chỗ đó.
 *
 * Bỏ hẳn thì không được: vuốt là cử chỉ vô hình, và người mở app lần đầu sẽ
 * không biết còn hai trang nữa. Ba vạch là mức tối thiểu vẫn nói được điều đó —
 * cùng lượng tin, mất cái vỏ nút, và trả lại khoảng 60dp chiều cao.
 */
@Composable
private fun Pill(current: Int, accent: Color, onPick: (Int) -> Unit) {
    Row(
        Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PANES.forEachIndexed { i, _ ->
            val chon = i == current
            // Vạch của trang đang mở dài ra chứ không đổi màu suông: chiều dài
            // đọc được bằng đuôi mắt, còn màu thì không khi đang nhìn chỗ khác.
            val dai by animateFloatAsState(if (chon) 22f else 6f, tween(240), label = "vach$i")
            Box(
                Modifier
                    .padding(horizontal = 5.dp)
                    // Vùng chạm rộng hơn nét vẽ: một vạch 6dp thì không ai bấm
                    // trúng, mà 44dp là mức tối thiểu cho một chỗ bấm được.
                    .clip(RoundedCornerShape(50))
                    .clickable { onPick(i) }
                    .padding(vertical = 14.dp, horizontal = 6.dp)
            ) {
                Box(
                    Modifier
                        .height(6.dp)
                        .width(dai.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (chon) accent else mau.vien)
                )
            }
        }
    }
}
/**
 * Một dòng lời trên trang Bài.
 *
 * Tách ra khỏi chỗ gọi vì trang Bài dựng lời BÊN TRONG cùng một cột cuộn với
 * bìa và nút bấm — không còn một trang riêng nào để chứa nó nữa. Mọi thứ dòng
 * này từng đọc lén từ phạm vi bao ngoài giờ đi vào bằng tham số.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun DongLoi(
    chiSo: Int,
    line: LyricLine,
    /** Đây có phải câu đang hát không. */
    dangHat: Boolean,
    /** Cách câu đang hát mấy dòng, đã cắt ở 4. */
    xa: Int,
    /** Mốc thời gian có đáng tin không; không tin thì mọi dòng rõ như nhau. */
    tinMoc: Boolean,
    /** Bản dịch của đúng dòng này, rỗng nếu không có. */
    banDich: String,
    effect: LyricEffect,
    quet: Animatable<Float, AnimationVector1D>,
    accent: Color,
    onCham: () -> Unit,
    onNhanGiu: () -> Unit
) {
    val scale by animateFloatAsState(
        if (!tinMoc) 1f else 1f - 0.045f * xa,
        tween(280), label = "s$chiSo"
    )
    // Moc dang ngo thi moi dong deu ro nhu nhau - khong co dong nao
    // duoc quyen sang hon, vi ta khong biet dong nao dung
    val alpha by animateFloatAsState(
        if (!tinMoc) 0.72f else when (xa) {
            0 -> 1f
            1 -> 0.52f
            2 -> 0.34f
            3 -> 0.24f
            else -> 0.18f
        },
        tween(280), label = "a$chiSo"
    )
    // Nhoe chi cho nhung dong DA MO SAN - no lam sau them mot lop
    // da co, khong tu minh giau chu nao. Bat dau tu bac 2 de dong
    // ke ben van doc duoc ro.
    //
    // `Modifier.blur` can Android 12; may cu hon thi no lang le
    // khong lam gi, va bo cuc van dung y het. Do la kieu xuong cap
    // dung: mat mot lop trang tri, khong mat mot chuc nang nao.
    val nhoe by animateFloatAsState(
        if (!tinMoc || xa < 2) 0f else 0.7f * (xa - 1),
        tween(280), label = "b$chiSo"
    )

    // Hai hieu ung "doi dong thi lam gi do" chay bang mot hoat anh
    // dat lai moi lan dong nay TRO THANH dong dang hat. Cac dong
    // khac khong dung toi, nen khong ton gi.
    val vao = remember(chiSo) { Animatable(1f) }
    LaunchedEffect(dangHat, effect) {
        if (!dangHat) { vao.snapTo(1f); return@LaunchedEffect }
        when (effect) {
            LyricEffect.NAY -> {
                vao.snapTo(0.86f)
                vao.animateTo(1f, spring(dampingRatio = 0.34f, stiffness = 620f))
            }
            LyricEffect.TROI_LEN -> {
                vao.snapTo(0f)
                vao.animateTo(1f, tween(340))
            }
            else -> vao.snapTo(1f)
        }
    }

    Column(
        Modifier
            // MOT cu chi, MOT viec.
            //
            // Cham vao mot cau thi nhay toi cau do - do la thu nguoi
            // ta doan ra truoc khi doc bat ky huong dan nao, va la
            // thu moi app loi bai hat khac deu lam.
            //
            // Can lech nhip lui ve nhan giu. No hiem hon han, va no
            // tung an ngay tren cu cham: ai cham mot cau de nhay toi
            // thi lai vo tinh doi moc thoi gian ca bai, roi khong
            // hieu vi sao loi bong chay sai.
            .combinedClickable(onClick = onCham, onLongClick = onNhanGiu)
            .then(if (nhoe > 0.05f) Modifier.blur(nhoe.dp) else Modifier)
            .graphicsLayer {
                // Doi trong `graphicsLayer` bang lambda: chi cap nhat
                // mot lop ve, khong dung lai cay giao dien
                val nay = if (dangHat && effect == LyricEffect.NAY) vao.value else 1f
                scaleX = scale * nay
                scaleY = scale * nay
                this.alpha =
                    if (dangHat && effect == LyricEffect.TROI_LEN) alpha * vao.value
                    else alpha
                if (dangHat && effect == LyricEffect.TROI_LEN) {
                    translationY = (1f - vao.value) * 34.dp.toPx()
                }
                transformOrigin =
                    androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            }
    ) {
        val chu = line.text.ifEmpty { "♪" }
        val quetDuoc = dangHat &&
            (effect == LyricEffect.SANG_DAN || effect == LyricEffect.HIEN_CHU)

        // Lời bài hát đặt bằng bộ chữ CÓ CHÂN, khác hẳn phần giao
        // diện. Đây là thứ duy nhất trên màn hình để ĐỌC chứ không
        // phải để bấm, và tách nó ra bằng dáng chữ nói điều đó rõ
        // hơn bất kỳ đường viền nào.
        //
        // Chữ có chân cần khoảng cách dòng rộng hơn chữ không chân
        // cùng cỡ, nên `lineHeight` nới ra theo.
        val coChu = if (dangHat) 26.sp else 22.sp
        val damNhat = if (dangHat) FontWeight.SemiBold else FontWeight.Normal
        val caoDong = if (dangHat) 36.sp else 32.sp
        // `accent` tới đây đã chỉnh sẵn cho mặt giấy đang dùng.
        val mucMau = accent

        if (quetDuoc) {
            // Quet phai chay THEO TUNG DONG, khong theo be ngang cua
            // ca khoi chu.
            //
            // `Brush.horizontalGradient` ap cho ca `Text` thi cau bi
            // rot xuong hai dong se sang DONG THOI ca hai, moi dong
            // tu trai sang - trong nhu hai cau rieng cung chay, hoan
            // toan khong ra thu tu doc. Cau ngan mot dong thi khong
            // lo ra, nen loi nay song duoc mot ban phat hanh.
            //
            // Nen ve hai lop: lop duoi la chu mo, lop tren la chu
            // sang bi CAT theo dung phan da qua - het dong mot moi
            // sang dong hai. `TextLayoutResult` cho biet tung dong
            // nam o dau.
            var bocCuc by remember(chu, coChu) {
                mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null)
            }
            val mo = if (effect == LyricEffect.HIEN_CHU) Color.Transparent
            else mau.chuRatMo

            Box {
                Text(
                    text = chu, color = mo, fontFamily = boChu.loi,
                    fontSize = coChu, fontWeight = damNhat, lineHeight = caoDong,
                    onTextLayout = { bocCuc = it }
                )
                Text(
                    text = chu, color = mau.chu, fontFamily = boChu.loi,
                    fontSize = coChu, fontWeight = damNhat, lineHeight = caoDong,
                    modifier = Modifier.drawWithContent {
                        val bc = bocCuc
                        if (bc == null) { drawContent(); return@drawWithContent }

                        // Chia tien do cho cac dong theo BE NGANG
                        // that cua tung dong: dong ngan phai qua
                        // nhanh hon dong dai, khong thi nhip quet
                        // giat khuc o cho xuong dong.
                        val rong = (0 until bc.lineCount).map {
                            bc.getLineRight(it) - bc.getLineLeft(it)
                        }
                        val tong = rong.sum()
                        if (tong <= 0f) { drawContent(); return@drawWithContent }

                        var conLai = quet.value.coerceIn(0f, 1f) * tong
                        val duong = androidx.compose.ui.graphics.Path()
                        for (i in 0 until bc.lineCount) {
                            if (conLai <= 0f) break
                            val phan = minOf(conLai, rong[i])
                            duong.addRect(
                                androidx.compose.ui.geometry.Rect(
                                    bc.getLineLeft(i), bc.getLineTop(i),
                                    bc.getLineLeft(i) + phan, bc.getLineBottom(i)
                                )
                            )
                            conLai -= phan
                        }
                        clipPath(duong) { this@drawWithContent.drawContent() }
                    }
                )
            }
        } else Text(
            text = chu,
            color = mau.chu,
            fontFamily = boChu.loi,
            fontSize = coChu,
            fontWeight = damNhat,
            lineHeight = caoDong,
            style = when {
                dangHat && effect == LyricEffect.TOA_SANG ->
                    LocalTextStyle.current.copy(
                        shadow = Shadow(mucMau, Offset.Zero, 26f)
                    )
                else -> LocalTextStyle.current
            }
        )
        // O trang Loi thi hien ban dich cho MOI dong, khac han khung
        // noi chi hien cho dong dang hat: o day nguoi dung dang doc
        // ca bai chu khong phai liec mat.
        val meaning = banDich.trim()
        if (meaning.isNotEmpty() && meaning != line.text.trim()) {
            // Bản dịch đặt bằng chữ KHÔNG chân, cỡ nhỏ hơn - nó là
            // chú thích cho câu ở trên chứ không phải lời bài hát,
            // và trên một trang in thì chú thích trông khác chính văn.
            Text(
                text = meaning,
                color = mucMau,
                fontSize = if (dangHat) 15.sp else 13.5.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Dai bao nho tren dau trang loi, kem mot nut o ben phai. */
@Composable
fun Notice(
    accent: Color,
    text: String,
    onClick: (() -> Unit)? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.22f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = mau.chu,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(mau.vien)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(action, color = mau.chu, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** "+1,5 giây" hoặc "−0,8 giây" — dấu cho biết lời hiện sớm hay muộn hơn. */
internal fun offsetLabel(ms: Long): String {
    val seconds = kotlin.math.abs(ms) / 1000.0
    val sign = if (ms >= 0) "+" else "−"
    return sign + String.format("%.1f", seconds).replace('.', ',') + " giây"
}

@Composable
internal fun TunePane(
    canDrawOverlay: Boolean,
    overlayOn: Boolean,
    accent: Color,
    look: OverlayLook,
    suggestedFontSize: Float,
    onOpenOverlaySettings: () -> Unit,
    onToggleOverlay: () -> Unit,
    onLookChange: (OverlayLook) -> Unit,
    translateSettings: TranslateSettings,
    onTranslateChange: (TranslateSettings) -> Unit,
    canAddTile: Boolean,
    onAddTile: () -> Unit,
    lyricEffect: LyricEffect,
    onLyricEffectChange: (LyricEffect) -> Unit,
    chuDe: ChuDe,
    onChuDeChange: (ChuDe) -> Unit,
    kieuChu: KieuChu,
    onKieuChuChange: (KieuChu) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 30.dp)
    ) {
        // Thieu quyen ve de thi CHI phan khung noi mat, khong phai ca trang.
        //
        // Truoc day cho nay `return@Column` - va keo theo ca phan dich lan phan
        // hieu ung chu xuong ho, du hai thu do khong lien quan gi toi viec ve
        // de len app khac. Nguoi tu choi mot quyen thi mat dung thu gan voi
        // quyen do, khong mat nhung thu khac.
        if (!canDrawOverlay) {
            Ask(
                title = "Cần quyền vẽ đè lên app khác",
                body = "Không có quyền này thì lời không hiện được khi bạn đang ở trong " +
                    "Spotify hay YouTube. Các phần khác bên dưới vẫn dùng được.",
                action = "Mở Cài đặt để bật",
                accent = accent,
                onAction = onOpenOverlaySettings
            )
        }

        Muc(
            tieuDe = "Khung lời nổi",
            accent = accent,
            moSan = true,
            tomTat = if (overlayOn) "Đang bật" else "Đang tắt"
        ) {
        if (canDrawOverlay) {
        Head(if (overlayOn) "Lời đang nổi trên màn hình" else "Lời nổi đang tắt")
        Spacer(Modifier.height(8.dp))
        Text(
            if (overlayOn) "Kéo khung để dời chỗ. Mở app nhạc rồi xem thử."
            else "Bật lên rồi mở app nhạc — lời sẽ nổi trên màn hình.",
            color = mau.chuMo,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))
        Big(
            label = if (overlayOn) "Tắt lời nổi" else "Bật lời nổi",
            accent = accent,
            filled = !overlayOn,
            onClick = onToggleOverlay
        )

        Spacer(Modifier.height(30.dp))
        Text(
            "Hình thức khung nổi",
            color = mau.chuMo,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))

        // Chinh cai gi cung thay ngay tren khung dang noi - khong co nut "Luu",
        // vi ban chi biet co vua mat khong bang cach nhin no
        Slider(
            label = "Cỡ chữ",
            value = look.fontSizeSp,
            range = 14f..48f,
            display = look.fontSizeSp.toInt().toString(),
            accent = accent,
            onChange = { onLookChange(look.copy(fontSizeSp = it)) },
            // Chi moi khi dang khac co do man hinh nay. Bay ra dung cho con so
            // vua dung, nen no doc nhu chinh con so ay moc them duong quay ve -
            // khong phai mot nut nua chen vao hang.
            suggestion = suggestedFontSize.takeIf { it.toInt() != look.fontSizeSp.toInt() },
            onSuggestion = { onLookChange(look.copy(fontSizeSp = suggestedFontSize)) }
        )
        Spacer(Modifier.height(22.dp))
        MauChon(
            nhan = "Màu chữ",
            dangChon = look.textColor,
            bang = MAU_CHU,
            accent = accent,
            onChon = { onLookChange(look.copy(textColor = it)) }
        )
        Spacer(Modifier.height(18.dp))
        MauChon(
            nhan = "Màu nền khung",
            dangChon = look.backgroundColor,
            bang = MAU_NEN,
            accent = accent,
            onChon = { onLookChange(look.copy(backgroundColor = it)) }
        )
        Spacer(Modifier.height(4.dp))

        Slider(
            label = "Nền mờ",
            value = look.backgroundOpacity,
            range = 0f..1f,
            display = "${(look.backgroundOpacity * 100).toInt()}%",
            accent = accent,
            onChange = { onLookChange(look.copy(backgroundOpacity = it)) }
        )
        Slider(
            label = "Viền chữ",
            value = look.strokeWidthDp,
            range = 0f..5f,
            display = if (look.strokeWidthDp < 0.2f) "không" else
                String.format("%.1f", look.strokeWidthDp).replace('.', ','),
            accent = accent,
            onChange = { onLookChange(look.copy(strokeWidthDp = it)) }
        )
        Slider(
            label = "Số dòng phụ",
            value = look.contextLines.toFloat(),
            range = 0f..3f,
            steps = 2,
            display = if (look.contextLines == 0) "chỉ dòng đang hát"
                else "${look.contextLines} dòng mỗi bên",
            accent = accent,
            onChange = { onLookChange(look.copy(contextLines = it.roundToInt())) }
        )

        Spacer(Modifier.height(6.dp))
        Toggle(
            label = "Chạm xuyên qua khung",
            hint = "Ngón tay đi thẳng xuống app bên dưới — khung chỉ còn để nhìn, " +
                "không kéo được nữa",
            checked = look.clickThrough,
            accent = accent,
            onChange = { onLookChange(look.copy(clickThrough = it)) }
        )

        Spacer(Modifier.height(26.dp))
        Slider(
            label = "Làm mờ nền để tập trung",
            value = look.dimBackground,
            range = 0f..0.85f,
            display = if (look.dimBackground < 0.01f) "Tắt"
                      else "${(look.dimBackground * 100).roundToInt()}%",
            accent = accent,
            onChange = { onLookChange(look.copy(dimBackground = it)) }
        )
        Text(
            "Phủ một lớp tối lên cả màn hình, dưới khung lời. Chạm vẫn xuyên qua " +
                "bình thường nên app bên dưới dùng được như thường.\n\n" +
                "Vài app ngân hàng từ chối hoạt động khi có lớp phủ màn hình — đó là " +
                "cách họ tự bảo vệ, không sửa được từ phía Lyra. Gặp thì kéo về Tắt.",
            color = mau.chuRatMo,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(26.dp))
        Toggle(
            label = "Thanh điều khiển dưới khung nổi",
            hint = "Nút phát và thanh sóng làm timeline, nằm dưới cùng nên không đè lời. " +
                "Chạy được cả với nhạc phát ở app khác.",
            checked = look.showControls,
            accent = accent,
            onChange = { onLookChange(look.copy(showControls = it)) }
        )
        } // het phan chi lien quan toi khung noi
        }

        Muc(
            tieuDe = "Dịch lời",
            accent = accent,
            tomTat = if (translateSettings.enabled) "Bật · đọc tiếng " +
                languageName(translateSettings.readingLanguage) else "Tắt"
        ) {
        Spacer(Modifier.height(30.dp))
        Text(
            "Dịch lời",
            color = mau.chuMo,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))

        Toggle(
            label = "Dịch lời tiếng nước ngoài",
            hint = "Dịch ngay trên máy, không gửi lời đi đâu và không tốn tiền. " +
                "Lời đã đúng thứ tiếng bạn đọc thì app không đụng tới.",
            checked = translateSettings.enabled,
            accent = accent,
            onChange = { onTranslateChange(translateSettings.copy(enabled = it)) }
        )

        if (translateSettings.enabled) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Bạn đọc được tiếng",
                color = mau.chuMo,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((code, label) in READING_LANGUAGES) {
                    val chosen = code == translateSettings.readingLanguage
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (chosen) accent else mau.nenChim
                            )
                            .clickable {
                                onTranslateChange(
                                    translateSettings.copy(readingLanguage = code)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            color = mau.chu,
                            fontSize = 14.sp,
                            fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Toggle(
                label = "Chỉ tải gói ngôn ngữ qua Wi-Fi",
                hint = "Mỗi thứ tiếng là một gói vài chục MB, tải một lần rồi " +
                    "dùng mãi kể cả khi mất mạng",
                checked = translateSettings.wifiOnly,
                accent = accent,
                onChange = { onTranslateChange(translateSettings.copy(wifiOnly = it)) }
            )
        }


        }
        Muc(
            tieuDe = "Bộ chữ",
            accent = accent,
            tomTat = kieuChu.nhan
        ) {
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (kc in KieuChu.entries) {
                    val chon = kc == kieuChu
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (chon) accent.copy(alpha = 0.22f) else mau.nenChim)
                            .clickable { onKieuChuChange(kc) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                kc.nhan,
                                color = mau.chu,
                                fontSize = 14.5.sp,
                                fontWeight = if (chon) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(kc.moTa, color = mau.chuMo, fontSize = 13.5.sp)
                        }
                        if (chon) Text("●", color = accent, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Áp cho cả app lẫn khung lời nổi. Hai bộ chữ của Lyra nằm sẵn " +
                    "trong app nên không cần mạng; chọn “Phông máy” thì Lyra dùng " +
                    "đúng bộ chữ hệ thống, như mọi app khác.",
                color = mau.chuRatMo,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
        }
        Muc(
            tieuDe = "Mặt giấy",
            accent = accent,
            tomTat = chuDe.nhan
        ) {
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (cd in ChuDe.entries) {
                    val chon = cd == chuDe
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (chon) accent.copy(alpha = 0.22f) else mau.nenChim)
                            .clickable { onChuDeChange(cd) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                cd.nhan,
                                color = mau.chu,
                                fontSize = 14.5.sp,
                                fontWeight = if (chon) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(cd.moTa, color = mau.chuMo, fontSize = 13.5.sp)
                        }
                        if (chon) Text("●", color = accent, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Chỉ đổi màn hình trong app. Khung lời nổi có bộ màu riêng ở " +
                    "mục trên, vì nó nằm đè lên app khác chứ không nằm trên giấy " +
                    "của Lyra.",
                color = mau.chuRatMo,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
        }
        Muc(
            tieuDe = "Hiệu ứng chữ",
            accent = accent,
            tomTat = lyricEffect.nhan + " · cho cả trang Lời lẫn khung nổi"
        ) {
        Spacer(Modifier.height(30.dp))
        Text(
            "Hiệu ứng chữ ở trang Lời",
            color = mau.chuMo,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        // Chi doi cach TRINH BAY o trang Loi. Khung noi khong dung cai nay: no
        // nam de len app khac va co y ve lai cang it cang tot.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (e in LyricEffect.entries) {
                val chon = e == lyricEffect
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (chon) accent.copy(alpha = 0.22f) else mau.nenChim)
                        .clickable { onLyricEffectChange(e) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.nhan,
                            color = mau.chu,
                            fontSize = 14.5.sp,
                            fontWeight = if (chon) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            if (e.tonPin) e.moTa + " · tốn pin hơn" else e.moTa,
                            color = if (e.tonPin) Color(0xFFF0B24A).copy(alpha = 0.85f)
                                    else mau.chuMo,
                            fontSize = 13.5.sp
                        )
                    }
                    if (chon) Text("●", color = accent, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Áp cho cả trang Lời lẫn khung lời nổi.\n\n" +
                "Lời lấy từ LRCLIB chỉ có mốc theo từng dòng, không có mốc theo từng " +
                "chữ — nên không hiệu ứng nào ở đây tô sáng theo tiếng hát tới đâu. " +
                "“Sáng dần” và “Hiện chữ” chạy theo tiến độ trong câu, thứ biết chắc " +
                "từ mốc dòng này tới dòng sau.\n\n" +
                "Hai hiệu ứng đó bắt khung nổi vẽ lại liên tục thay vì mỗi câu một " +
                "lần, nên tốn pin hơn rõ. Bốn hiệu ứng còn lại không đổi gì về nhịp vẽ.",
            color = mau.chuRatMo,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )

        }
        Muc(
            tieuDe = "Lời tự nhập",
            accent = accent,
            tomTat = "Sao lưu và khôi phục lời bạn tự gõ hoặc tự căn giờ"
        ) {
            SaoLuuLoiMuc(accent)
        }

        Muc(
            tieuDe = "Ô bật nhanh và tắt nhanh",
            accent = accent,
            tomTat = "Cách bật tắt khung nổi mà không mở Lyra"
        ) {
        // Tat nhanh va o Cai dat nhanh deu la cach bat/tat KHUNG NOI,
        // nen chung di theo quyen ve de.
        if (canDrawOverlay) {
        Spacer(Modifier.height(30.dp))
        Text(
            "Tắt nhanh",
            color = mau.chuMo,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Giữ tay vài giây ngay trên khung lời là tắt — máy rung một cái báo " +
                "đã nhận. Đây là đường ngắn nhất: ngón tay đang ở sẵn trên thứ " +
                "cần tắt, không phải rời app nhạc.",
            color = mau.chuMo,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(14.dp))
        if (canAddTile) {
            // Android 13 tro len cho app tu xin them o - nguoi dung chi phai
            // dong y mot cai. Ban cu hon khong co duong nao ngoai viec chi cho
            // ho tu keo, nen cau huong dan van con o duoi.
            Big(
                label = "Thêm ô Lời nổi vào Cài đặt nhanh",
                accent = accent,
                filled = false,
                onClick = onAddTile
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            if (canAddTile)
                "Ô đó bật tắt được ngay trong lúc đang nghe nhạc, không cần mở Lyra."
            else
                "Hoặc kéo ô Lời nổi vào bảng Cài đặt nhanh (vuốt thanh thông báo " +
                    "xuống, sửa các ô) để bật tắt ngay khi đang nghe nhạc.",
            color = mau.chuRatMo,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(20.dp))
        }
    }
    }
}

/**
 * Bang mau cho khung loi noi.
 *
 * Vai o chon san chu khong phai mot vong mau day du. Day la khung de LIEC khi
 * dang xem thu khac, nen cai quyet dinh chi la "chu co doc duoc tren nen kia
 * khong" - mot vong mau bat nguoi dung chinh ba con so de tra loi mot cau ho
 * tra loi duoc bang mot cu cham.
 *
 * Mau chu thi sang, mau nen thi toi: hai bang khac nhau vi chung lam hai viec
 * khac nhau, va tron chung lai thi de chon ra mot cap khong doc duoc.
 */
private val MAU_CHU = listOf(
    "Trắng" to 0xFFFFFFFF,
    "Ngà" to 0xFFF6F1E6,
    "Vàng" to 0xFFFFD54A,
    "Đào" to 0xFFFFAB91,
    "Bạc hà" to 0xFF9FE8C8,
    "Đen" to 0xFF101010
)

private val MAU_NEN = listOf(
    "Đen" to 0xFF000000,
    "Than" to 0xFF1A1A1A,
    "Đêm" to 0xFF0E1730,
    "Nâu" to 0xFF241C14,
    "Rêu" to 0xFF12211A,
    "Trắng" to 0xFFFFFFFF
)

/** Mot hang o mau tron, o dang chon co vong sang quanh no. */
@Composable
private fun MauChon(
    nhan: String,
    dangChon: Int,
    bang: List<Pair<String, Long>>,
    accent: Color,
    onChon: (Int) -> Unit
) {
    Column {
        Text(nhan, color = mau.chu, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((ten, gia) in bang) {
                val ma = gia.toInt()
                val chon = ma == dangChon
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        // Vong ngoai danh dau o dang chon. Khong dung dau tick ve
                        // len tren o: o mau nao cung co the trung mau voi dau tick.
                        .background(if (chon) accent else Color.Transparent)
                        .padding(if (chon) 4.dp else 0.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(gia))
                        // Vien mo de o mau sang khong biet mat tren nen giay
                        .border(1.dp, mau.vien, RoundedCornerShape(50))
                        .clickable { onChon(ma) }
                )
            }
        }
    }
}

/** Thanh truot mot dong: ten ben trai, gia tri ben phai, thanh ben duoi. */
/**
 * Mot muc gap duoc o trang Chinh.
 *
 * Trang nay dai dan theo tung tinh nang - toi luc co ca hinh thuc khung noi,
 * lam mo nen, thanh dieu khien, dich, hieu ung chu va o Cai dat nhanh thi mot
 * cuon phang bat nguoi dung luot qua het moi thu de toi cai ho can.
 *
 * Gap lai thi ca trang thanh vai dong tieu de - nhin mot cai la biet co nhung
 * gi, va mo dung cai minh can. `moSan` cho muc dau vi do la thu nguoi ta vao
 * day de chinh nhieu nhat.
 */
@Composable
private fun Muc(
    tieuDe: String,
    accent: Color,
    moSan: Boolean = false,
    /** Mot dong ngan ta trang thai hien tai, doc duoc khi dang gap. */
    tomTat: String? = null,
    noiDung: @Composable () -> Unit
) {
    var mo by remember { mutableStateOf(moSan) }
    val xoay by animateFloatAsState(if (mo) 90f else 0f, tween(200), label = "xoay")

    Column(Modifier.padding(bottom = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { mo = !mo }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "▸",
                color = accent,
                fontSize = 15.sp,
                modifier = Modifier.graphicsLayer { rotationZ = xoay }
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tieuDe,
                    color = mau.chu,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!mo && tomTat != null) {
                    Text(
                        tomTat,
                        color = mau.chuRatMo,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
        if (mo) {
            Column(Modifier.padding(start = 4.dp, bottom = 10.dp)) { noiDung() }
        }
    }
}

@Composable
private fun Slider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    accent: Color,
    onChange: (Float) -> Unit,
    steps: Int = 0,
    /** Gia tri hop voi may nay, neu no dang khac gia tri hien tai. */
    suggestion: Float? = null,
    onSuggestion: () -> Unit = {}
) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = mau.chu, fontSize = 14.sp)
            if (suggestion == null) {
                Text(display, color = mau.chuMo, fontSize = 14.sp)
            } else {
                Text(
                    "$display → ${suggestion.toInt()}",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSuggestion)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = mau.vien
            )
        )
    }
}

/** Cong tac mot dong, kem mot cau giai thich khi can. */
@Composable
private fun Toggle(
    label: String,
    hint: String,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(label, color = mau.chu, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(hint, color = mau.chuRatMo, fontSize = 13.sp, lineHeight = 18.sp)
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = Color.White
            )
        )
    }
}

/**
 * Lời xin quyền. Ba chỗ trong app đều dùng đúng cái này.
 *
 * Tự gói mình trong `Column`, không phát năm phần tử rời ra ngoài.
 *
 * Bản trước phát rời, và ở trang Đang phát nó được đặt trong một `Box` — mà
 * trong `Box` thì mọi con đều xếp CHỒNG lên nhau, nên tiêu đề, đoạn mô tả và
 * cái nút cùng đè lên một chỗ. Chỗ đặt thì trông vô hại, và cái sai lại nằm ở
 * đây; gói sẵn thì đặt vào đâu cũng đúng.
 */
/**
 * Đầu đề một màn hình.
 *
 * Có một chỗ duy nhất định nghĩa nó vì trước đây mỗi màn hình tự đặt lấy: chỗ
 * thì 24sp đậm, chỗ thì 27sp vừa, chỗ chữ có chân chỗ không — và người dùng
 * thấy ngay rằng mấy màn hình này không phải một app.
 */
@Composable
internal fun Head(text: String) {
    Text(
        text,
        color = mau.chu,
        fontFamily = boChu.loi,
        fontSize = 27.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 33.sp
    )
}

@Composable
internal fun Ask(
    title: String,
    body: String,
    action: String,
    accent: Color,
    onAction: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Head(title)
        Spacer(Modifier.height(10.dp))
        Text(body, color = mau.chuMo, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(22.dp))
        Big(label = action, accent = accent, filled = true, onClick = onAction)
    }
}

/** Nut lon, bo tron het co - khong dung nut mac dinh de giu net rieng. */
@Composable
private fun Big(label: String, accent: Color, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (filled) accent else mau.nenChim)
            .clickable(onClick = onClick)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (filled) Color.White else mau.chu,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/** Ten goi -> ten app doc duoc. Khong biet thi tra ve chinh ten goi. */
internal fun appLabel(packageName: String): String = when {
    // Bai do chinh Lyra phat mang mot ten goi gia, xem `Lyra.OWN`
    packageName == "lyra" -> "Lyra"
    // Mot ban Lyra KHAC tren cung may - ban cai tu Play va ban dung tay chay
    // canh nhau. Khong bat thi nhanh cuoi cat ten goi ra thanh "lyra_player",
    // va mot cai ten may moc nhu vay lo ngay ra rang cho nay chi doan bua.
    packageName.startsWith("com.mittohoa.lyra") -> "Lyra"
    packageName.contains("spotify") -> "Spotify"
    packageName.contains("youtube.music") -> "YouTube Music"
    packageName.contains("youtube") -> "YouTube"
    packageName.contains("zing") || packageName.contains("mp3.zing") -> "Zing MP3"
    // Goi cua NhacCuaTui la "ht.nct", khong mang chu "nhaccuatui" nao ca.
    // Khong bat thi man hinh ghi nguon la "nct" - mot cai ten cat tu ten goi ra,
    // lo ngay rang cho nay chi doan bua.
    packageName.contains("nhaccuatui") || packageName == "ht.nct" -> "NhacCuaTui"
    packageName.contains("soundcloud") -> "SoundCloud"
    else -> packageName.substringAfterLast('.')
}

/** Nen goc cua ca app; man hinh soan loi dung chung de nhin lien mach. */
/** Mau dung khi anh bia khong cho ra mau nao - hoac chua co bai nao. */
private val FALLBACK = Color(0xFF6D28D9)
