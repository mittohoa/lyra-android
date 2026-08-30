package com.mittohoa.lyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mittohoa.lyra.data.OverlayLook
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.media.NowPlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private val PANES = listOf("Đang phát", "Lời", "Chỉnh")

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
    onLookChange: (OverlayLook) -> Unit,
    onEditLyrics: () -> Unit
) {
    // Mau nen lay tu anh bia. Doi bai thi chuyen mau tu tu chu khong nhay cai -
    // nhay mau la thu mat nhat khi nghe nhac.
    //
    // Tinh tren LUONG NEN. Doc diem anh la viec cua CPU, lam trong than
    // composable nghia la lam tren luong chinh giua luc dang dung giao dien -
    // dung mot nhip ngay khi doi bai, va do la luc nguoi dung dang nhin nhat.
    val target by produceState(FALLBACK, now?.key) {
        val art = now?.artwork
        value = withContext(Dispatchers.Default) { dominantColor(art) } ?: FALLBACK
    }
    val accent by animateColorAsState(target, tween(900), label = "accent")

    val pager = rememberPagerState(pageCount = { PANES.size })
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

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                pageSpacing = 0.dp
            ) { page ->
                when (page) {
                    0 -> NowPlayingPane(now, accent, hasNotificationAccess, onOpenNotificationSettings)
                    1 -> LyricsPane(
                        lyrics, loading, position, accent,
                        onSyncToLine, onClearOffset, onEditLyrics
                    )
                    else -> TunePane(
                        canDrawOverlay = canDrawOverlay,
                        overlayOn = overlayOn,
                        accent = accent,
                        look = look,
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onToggleOverlay = onToggleOverlay,
                        onLookChange = onLookChange
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

@Composable
private fun NowPlayingPane(
    now: NowPlaying?,
    accent: Color,
    hasNotificationAccess: Boolean,
    onOpenNotificationSettings: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasNotificationAccess) {
            Ask(
                title = "Cần quyền đọc thông báo",
                body = "Android chỉ cho đọc thông tin bài đang phát ở app khác khi bạn đã bật " +
                    "quyền này. Lyra không đọc nội dung thông báo — chỉ đọc tên bài và vị trí phát.",
                action = "Mở Cài đặt để bật",
                accent = accent,
                onAction = onOpenNotificationSettings
            )
            return@Column
        }

        if (now == null) {
            Text(
                "Chưa thấy app nào đang phát",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Mở Spotify, YouTube Music, Zing hay NhacCuaTui rồi phát một bài.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            return@Column
        }

        Text(
            now.title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )
        if (now.artist.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(now.artist, color = Color.White.copy(alpha = 0.72f), fontSize = 17.sp)
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (now.isPlaying) accent else Color.White.copy(alpha = 0.3f))
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "${appLabel(now.packageName)} · ${if (now.isPlaying) "đang phát" else "tạm dừng"}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
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
    onSyncToLine: (Int) -> Unit,
    onClearOffset: () -> Unit,
    onEditLyrics: () -> Unit
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

    // Keo dong dang hat ve giua man hinh
    LaunchedEffect(active, trustTiming) {
        if (trustTiming && active >= 0 && lyrics.lines.isNotEmpty()) {
            listState.animateScrollToItem(active.coerceAtLeast(0), scrollOffset = -260)
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

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(lyrics.lines, key = { i, _ -> i }) { i, line ->
                val isActive = trustTiming && i == active
                val scale by animateFloatAsState(
                    if (isActive) 1f else 0.92f, tween(280), label = "s$i"
                )
                // Moc dang ngo thi moi dong deu ro nhu nhau - khong co dong nao
                // duoc quyen sang hon, vi ta khong biet dong nao dung
                val alpha by animateFloatAsState(
                    if (isActive) 1f else if (trustTiming) 0.34f else 0.72f,
                    tween(280),
                    label = "a$i"
                )

                Text(
                    text = line.text.ifEmpty { "♪" },
                    color = Color.White,
                    fontSize = if (isActive) 23.sp else 20.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        // Cham vao cau dang hat de can lai ca bai - mot cu cham
                        // thay cho hang chuc lan bam +/- nua giay
                        .clickable { onSyncToLine(i) }
                        .graphicsLayer {
                            // Doi trong `graphicsLayer` bang lambda: chi cap nhat
                            // mot lop ve, khong dung lai cay giao dien
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            transformOrigin =
                                androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                )
            }
        }
    }
}

/** Dai bao nho tren dau trang loi, kem mot nut o ben phai. */
@Composable
private fun Notice(
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
    onOpenOverlaySettings: () -> Unit,
    onToggleOverlay: () -> Unit,
    onLookChange: (OverlayLook) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 30.dp)
    ) {
        if (!canDrawOverlay) {
            Spacer(Modifier.height(120.dp))
            Ask(
                title = "Cần quyền vẽ đè lên app khác",
                body = "Không có quyền này thì lời không hiện được khi bạn đang ở trong " +
                    "Spotify hay YouTube.",
                action = "Mở Cài đặt để bật",
                accent = accent,
                onAction = onOpenOverlaySettings
            )
            return@Column
        }

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
            onChange = { onLookChange(look.copy(fontSizeSp = it)) }
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

        Spacer(Modifier.height(26.dp))
        Text(
            "Mẹo: kéo ô \"Lời nổi\" vào bảng Cài đặt nhanh (vuốt thanh thông báo " +
                "xuống, sửa các ô) để bật tắt ngay khi đang nghe nhạc.",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(20.dp))
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
    steps: Int = 0
) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text(display, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
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
private fun Ask(
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
private fun appLabel(packageName: String): String = when {
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
