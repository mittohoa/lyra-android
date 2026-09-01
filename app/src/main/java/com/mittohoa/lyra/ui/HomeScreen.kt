package com.mittohoa.lyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.data.OverlayLook
import com.mittohoa.lyra.data.TranslateSettings
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.media.NowPlaying
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
private val PANES = listOf("Tìm", "Đang phát", "Lời", "Chỉnh")
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
    onLyricEffectChange: (LyricEffect) -> Unit
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

    val pager = rememberPagerState(initialPage = START_PANE, pageCount = { PANES.size })
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(BACKDROP, accent, 0.30f),
                        lerp(BACKDROP, accent, 0.10f),
                        BACKDROP
                    )
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Dau hieu Lyra thay cho thanh tieu de: no cu dong khi app dang tim
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 22.dp, top = 14.dp, end = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LyraMark(size = 30.dp, busy = loading)
                Spacer(Modifier.width(11.dp))
                Text(
                    if (loading) "đang tìm lời…" else PANES[pager.currentPage],
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp
                )
            }

            // Có bản mới thì báo ở TẦNG APP, không phải bên trong một trang.
            // Trang phát thoát sớm khi chưa có quyền đọc thông báo hoặc chưa có
            // gì đang phát — mà đó đúng là trạng thái của một máy vừa cài xong,
            // tức là người cần biết tin này nhất lại là người không thấy nó.
            if (banMoi != null) {
                Box(Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp)) {
                    Notice(
                        accent = accent,
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
                        accent = accent,
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
                        onDownload = onDownload
                    )
                    1 -> PlayerPane(
                        now = now,
                        accent = accent,
                        artwork = artwork,
                        position = position,
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
                    )
                    2 -> LyricsPane(
                        lyrics, loading, position, accent, translation,
                        onSyncToLine, onClearOffset, onEditLyrics, onDownloadModel,
                        lyricEffect
                    )
                    else -> TunePane(
                        canDrawOverlay = canDrawOverlay,
                        overlayOn = overlayOn,
                        accent = accent,
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
                        onLyricEffectChange = onLyricEffectChange
                    )
                }
            }

            Pill(
                current = pager.currentPage,
                accent = accent,
                onPick = { scope.launch { pager.animateScrollToPage(it) } }
            )
        }
    }
}

/**
 * Vien thuoc noi - toan bo chrome cua app nam o day.
 *
 * Ba cham, cham dang xem thi gian ra thanh ten trang. Doi trang bang cach cham
 * vao cham, hoac vuot ngang tren noi dung.
 */
@Composable
private fun Pill(current: Int, accent: Color, onPick: (Int) -> Unit) {
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PANES.forEachIndexed { i, name ->
                val selected = i == current
                // Bien doi bang graphicsLayer/kich thuoc thay vi doi cay giao dien:
                // khong co gi bi dung lai, chi mot lop bi ve lai
                val weight by animateFloatAsState(
                    if (selected) 1f else 0f,
                    tween(260),
                    label = "pill$i"
                )

                Row(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) accent.copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .clickable { onPick(i) }
                        .padding(
                            horizontal = (12 + 18 * weight).dp,
                            vertical = 9.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selected) {
                        Text(
                            name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.55f))
                        )
                    }
                }
            }
        }
    }
}
/**
 * Trang loi.
 *
 * Day la trai tim cua app, nen no duoc ca man hinh. Dong dang hat sang len va
 * to hon, cac dong khac mo di - dung nhu khung noi, de nhin mot cai la biet
 * ngay minh dang o dau trong bai.
 */
@Composable
private fun LyricsPane(
    lyrics: Lyrics,
    loading: Boolean,
    position: State<Long>,
    accent: Color,
    translation: TranslationState,
    onSyncToLine: (Int) -> Unit,
    onClearOffset: () -> Unit,
    onEditLyrics: () -> Unit,
    onDownloadModel: () -> Unit,
    effect: LyricEffect
) {
    // `derivedStateOf`: vi tri phat doi 5 lan moi giay, nhung dong dang hat
    // thi vai giay moi doi mot lan. Khong boc thi ca danh sach bi dung lai
    // lien tuc - day chinh la cho de sinh giat nhat.
    val active by remember(lyrics) {
        derivedStateOf { activeLineIndex(lyrics.lines, position.value, lyrics.offset) }
    }

    // Moc dang ngo thi KHONG to sang va KHONG tu cuon. To sang nham mot dong
    // suot ca bai con te hon la khong to gi - nguoi dung tin vao no roi phat
    // hien bi lua. Cham mot cai la khop lai, va tu do tro di chay binh thuong.
    val trustTiming = lyrics.synced && !lyrics.timingSuspect
    val listState = rememberLazyListState()

    // Boc mot lan o day thay vi hoi trang thai trong tung dong: danh sach co
    // the vai tram dong, va moi dong tu doc trang thai la moi dong tu dang ky
    // theo doi no.
    val translated = (translation as? TranslationState.Done)?.lines ?: emptyList()

    // Keo dong dang hat ve giua man hinh
    LaunchedEffect(active, trustTiming) {
        if (trustTiming && active >= 0 && lyrics.lines.isNotEmpty()) {
            listState.animateScrollToItem(active.coerceAtLeast(0), scrollOffset = -260)
        }
    }

    /**
     * Cau dang hat sang dan tu trai sang phai theo tien do trong cau.
     *
     * KHONG phai karaoke tung chu. LRCLIB chi cho moc theo DONG, khong co moc
     * theo tu - da kiem: loi tra ve chi co `[00:24.62]` dau dong, khong co
     * `<00:24.62>` chen giua cac tu. Nen day khong gia vo biet dang hat toi chu
     * nao; no cho biet cau da chay duoc bao nhieu, va do la thu biet chac: dong
     * nay bat dau luc nao, dong sau bat dau luc nao.
     *
     * Chay bang mot hoat anh tuyen tinh dat mot lan moi khi doi dong, chu khong
     * bam theo `position`. Vi tri phat chi cap nhat vai lan moi giay, va quet
     * sang theo no thi thanh sang giat tung nac.
     */
    val quet = remember { Animatable(0f) }
    LaunchedEffect(active, trustTiming, lyrics) {
        if (!trustTiming || active < 0) {
            quet.snapTo(0f)
            return@LaunchedEffect
        }
        val batDau = lyrics.lines[active].time + lyrics.offset
        val ketThuc = lyrics.lines.getOrNull(active + 1)?.let { it.time + lyrics.offset }
            ?: (batDau + 4_000L)
        val dai = (ketThuc - batDau).coerceAtLeast(1L)
        val daQua = (position.value - batDau).coerceAtLeast(0L)

        quet.snapTo((daQua.toFloat() / dai).coerceIn(0f, 1f))
        val conLai = (dai - daQua).coerceAtLeast(0L)
        if (conLai > 0) {
            quet.animateTo(1f, tween(conLai.toInt(), easing = LinearEasing))
        }
    }

    if (lyrics.isEmpty) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LyraMark(size = 54.dp, busy = loading)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (loading) "Đang tìm lời…" else "Chưa tìm thấy lời cho bài này",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                if (!loading) {
                    Spacer(Modifier.height(22.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                            .clickable(onClick = onEditLyrics)
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Text(
                            "Tự nhập lời",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Mot dai bao duy nhat, va no LUON co loi vao cho sua loi. Truoc day
        // khi dang co do lech thi dai bao chiem cho va nguoi dung mat han duong
        // toi cho tu nhap - dung luc can nhat, vi loi sai thuong di kem lech.
        Notice(
            accent = accent,
            text = when {
                lyrics.timingSuspect ->
                    "Lời của bản thu khác nên mốc có thể lệch. Chạm câu đang hát để căn lại."
                lyrics.offset != 0L ->
                    "Đã căn lệch " + offsetLabel(lyrics.offset) + ". Bấm để bỏ."
                lyrics.from == "tự nhập" -> "Lời bạn tự nhập."
                else -> "Lời từ " + lyrics.from + "."
            },
            onClick = if (lyrics.offset != 0L) onClearOffset else null,
            action = if (lyrics.from == "tự nhập") "Sửa lời" else "Tự nhập",
            onAction = onEditLyrics
        )

        // Dai bao rieng cho phan dich, va chi hien khi co chuyen de noi. Dich
        // xong thi khong bao gi ca - ban dich da nam duoi tung dong, tu no da
        // la loi thong bao ro nhat.
        when (translation) {
            is TranslationState.NeedsModel -> Notice(
                accent = accent,
                text = "Lời đang là tiếng " + languageName(translation.language) +
                    ". Tải gói ngôn ngữ về máy để dịch, một lần dùng mãi.",
                action = "Tải gói",
                onAction = onDownloadModel
            )
            is TranslationState.Failed -> Notice(
                accent = accent,
                text = translation.why + "."
            )
            TranslationState.Working -> Notice(accent = accent, text = "Đang dịch lời…")
            else -> Unit
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(lyrics.lines, key = { i, _ -> i }) { i, line ->
                val isActive = trustTiming && i == active

                // Xa dong dang hat bao nhieu dong. Dung de do MOT CAI DOC thay
                // vi mot cong tac bat/tat: dong ke ben con doc duoc, dong xa
                // hon lui dan ve nen. Mat nguoi bam duoc cho dang hat ma khong
                // phai doc chu, va van thay truoc cau sap toi.
                //
                // Cat o 4: xa hon nua thi mat da khong phan biet duoc nua, ma
                // moi bac them la mot lop ve nua phai tinh.
                val xa = if (trustTiming && active >= 0) (i - active).absoluteValue.coerceAtMost(4) else 0

                val scale by animateFloatAsState(
                    if (!trustTiming) 1f else 1f - 0.045f * xa,
                    tween(280), label = "s$i"
                )
                // Moc dang ngo thi moi dong deu ro nhu nhau - khong co dong nao
                // duoc quyen sang hon, vi ta khong biet dong nao dung
                val alpha by animateFloatAsState(
                    if (!trustTiming) 0.72f else when (xa) {
                        0 -> 1f
                        1 -> 0.52f
                        2 -> 0.34f
                        3 -> 0.24f
                        else -> 0.18f
                    },
                    tween(280), label = "a$i"
                )
                // Nhoe chi cho nhung dong DA MO SAN - no lam sau them mot lop
                // da co, khong tu minh giau chu nao. Bat dau tu bac 2 de dong
                // ke ben van doc duoc ro.
                //
                // `Modifier.blur` can Android 12; may cu hon thi no lang le
                // khong lam gi, va bo cuc van dung y het. Do la kieu xuong cap
                // dung: mat mot lop trang tri, khong mat mot chuc nang nao.
                val nhoe by animateFloatAsState(
                    if (!trustTiming || xa < 2) 0f else 0.7f * (xa - 1),
                    tween(280), label = "b$i"
                )

                // Hai hieu ung "doi dong thi lam gi do" chay bang mot hoat anh
                // dat lai moi lan dong nay TRO THANH dong dang hat. Cac dong
                // khac khong dung toi, nen khong ton gi.
                val vao = remember(i) { Animatable(1f) }
                LaunchedEffect(isActive, effect) {
                    if (!isActive) { vao.snapTo(1f); return@LaunchedEffect }
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
                        // Cham vao cau dang hat de can lai ca bai - mot cu cham
                        // thay cho hang chuc lan bam +/- nua giay
                        .clickable { onSyncToLine(i) }
                        .then(if (nhoe > 0.05f) Modifier.blur(nhoe.dp) else Modifier)
                        .graphicsLayer {
                            // Doi trong `graphicsLayer` bang lambda: chi cap nhat
                            // mot lop ve, khong dung lai cay giao dien
                            val nay = if (isActive && effect == LyricEffect.NAY) vao.value else 1f
                            scaleX = scale * nay
                            scaleY = scale * nay
                            this.alpha =
                                if (isActive && effect == LyricEffect.TROI_LEN) alpha * vao.value
                                else alpha
                            if (isActive && effect == LyricEffect.TROI_LEN) {
                                translationY = (1f - vao.value) * 34.dp.toPx()
                            }
                            transformOrigin =
                                androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                ) {
                    val chu = line.text.ifEmpty { "♪" }
                    val quetDuoc = isActive &&
                        (effect == LyricEffect.SANG_DAN || effect == LyricEffect.HIEN_CHU)

                    Text(
                        text = chu,
                        color = Color.White,
                        fontSize = if (isActive) 23.sp else 20.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 30.sp,
                        style = when {
                            // Quet sang / hien chu: dung mot dai mau co canh cung
                            // ngay tai vi tri tien do. Khong phai karaoke tung
                            // chu - xem chu thich o `LyricEffect`.
                            quetDuoc -> {
                                val f = quet.value.coerceIn(0f, 1f)
                                val sau = if (effect == LyricEffect.HIEN_CHU)
                                    Color.Transparent else Color.White.copy(alpha = 0.38f)
                                LocalTextStyle.current.copy(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.White,
                                        f to Color.White,
                                        (f + 0.012f).coerceAtMost(1f) to sau,
                                        1f to sau
                                    )
                                )
                            }
                            isActive && effect == LyricEffect.TOA_SANG ->
                                LocalTextStyle.current.copy(
                                    shadow = Shadow(accent, Offset.Zero, 26f)
                                )
                            else -> LocalTextStyle.current
                        }
                    )
                    // O trang Loi thi hien ban dich cho MOI dong, khac han khung
                    // noi chi hien cho dong dang hat: o day nguoi dung dang doc
                    // ca bai chu khong phai liec mat.
                    val meaning = translated.getOrNull(i)?.trim().orEmpty()
                    if (meaning.isNotEmpty() && meaning != line.text.trim()) {
                        Text(
                            text = meaning,
                            color = accent,
                            fontSize = if (isActive) 16.sp else 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
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
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(action, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** "+1,5 giây" hoặc "−0,8 giây" — dấu cho biết lời hiện sớm hay muộn hơn. */
private fun offsetLabel(ms: Long): String {
    val seconds = kotlin.math.abs(ms) / 1000.0
    val sign = if (ms >= 0) "+" else "−"
    return sign + String.format("%.1f", seconds).replace('.', ',') + " giây"
}

@Composable
private fun TunePane(
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
    onLyricEffectChange: (LyricEffect) -> Unit
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

        if (canDrawOverlay) {
        Text(
            if (overlayOn) "Lời đang nổi trên màn hình" else "Lời nổi đang tắt",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (overlayOn) "Kéo khung để dời chỗ. Mở app nhạc rồi xem thử."
            else "Bật lên rồi mở app nhạc — lời sẽ nổi trên màn hình.",
            color = Color.White.copy(alpha = 0.6f),
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
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
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

        } // het phan chi lien quan toi khung noi

        Spacer(Modifier.height(30.dp))
        Text(
            "Dịch lời",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
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
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((code, label) in READING_LANGUAGES) {
                    val chosen = code == translateSettings.readingLanguage
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (chosen) accent else Color.White.copy(alpha = 0.10f)
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
                            color = Color.White,
                            fontSize = 13.sp,
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

        Spacer(Modifier.height(26.dp))
        Toggle(
            label = "Thanh điều khiển dưới khung nổi",
            hint = "Nút phát và thanh sóng làm timeline, nằm dưới cùng nên không đè lời. " +
                "Chạy được cả với nhạc phát ở app khác.",
            checked = look.showControls,
            accent = accent,
            onChange = { onLookChange(look.copy(showControls = it)) }
        )

        Spacer(Modifier.height(30.dp))
        Text(
            "Hiệu ứng chữ ở trang Lời",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
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
                        .background(if (chon) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                        .clickable { onLyricEffectChange(e) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.nhan,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = if (chon) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            if (e.tonPin) e.moTa + " · tốn pin hơn" else e.moTa,
                            color = if (e.tonPin) Color(0xFFF0B24A).copy(alpha = 0.85f)
                                    else Color.White.copy(alpha = 0.55f),
                            fontSize = 12.5.sp
                        )
                    }
                    if (chon) Text("●", color = accent, fontSize = 13.sp)
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
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.5.sp,
            lineHeight = 19.sp
        )

        // Tat nhanh va o Cai dat nhanh deu la cach bat/tat KHUNG NOI,
        // nen chung di theo quyen ve de.
        if (canDrawOverlay) {
        Spacer(Modifier.height(30.dp))
        Text(
            "Tắt nhanh",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Giữ tay vài giây ngay trên khung lời là tắt — máy rung một cái báo " +
                "đã nhận. Đây là đường ngắn nhất: ngón tay đang ở sẵn trên thứ " +
                "cần tắt, không phải rời app nhạc.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
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
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(20.dp))
        }
    }
}

/** Thanh truot mot dong: ten ben trai, gia tri ben phai, thanh ben duoi. */
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
            Text(label, color = Color.White, fontSize = 14.sp)
            if (suggestion == null) {
                Text(display, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            } else {
                Text(
                    "$display → ${suggestion.toInt()}",
                    color = accent,
                    fontSize = 13.sp,
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
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
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
            Text(label, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(hint, color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp, lineHeight = 17.sp)
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

@Composable
internal fun Ask(
    title: String,
    body: String,
    action: String,
    accent: Color,
    onAction: () -> Unit
) {
    Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Text(body, color = Color.White.copy(alpha = 0.62f), fontSize = 14.sp, lineHeight = 21.sp)
    Spacer(Modifier.height(22.dp))
    Big(label = action, accent = accent, filled = true, onClick = onAction)
}

/** Nut lon, bo tron het co - khong dung nut mac dinh de giu net rieng. */
@Composable
private fun Big(label: String, accent: Color, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (filled) accent else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
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
    packageName.contains("spotify") -> "Spotify"
    packageName.contains("youtube.music") -> "YouTube Music"
    packageName.contains("youtube") -> "YouTube"
    packageName.contains("zing") || packageName.contains("mp3.zing") -> "Zing MP3"
    packageName.contains("nhaccuatui") -> "NhacCuaTui"
    packageName.contains("soundcloud") -> "SoundCloud"
    else -> packageName.substringAfterLast('.')
}

/** Nen goc cua ca app; man hinh soan loi dung chung de nhin lien mach. */
internal val BACKDROP = Color(0xFF0E0B14)
private val FALLBACK = Color(0xFF6D28D9)
