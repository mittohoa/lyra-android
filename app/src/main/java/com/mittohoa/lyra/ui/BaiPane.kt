package com.mittohoa.lyra.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.sources.MediaKind
import com.mittohoa.lyra.sources.Track
import com.mittohoa.lyra.translate.TranslationState
import com.mittohoa.lyra.translate.languageName
import kotlin.math.absoluteValue

/**
 * Trang Bài: bìa và lời của CÙNG một bài, trong cùng một trang.
 *
 * Trước đây đây là hai trang cạnh nhau trong viên thuốc, và cả hai đều thiếu
 * một nửa. "Đang phát" có bìa và nút bấm nhưng không một chữ lời nào — với một
 * app mà sản phẩm chính là con chữ thì đó là đặt ngược. "Lời" có lời nhưng
 * không bìa, không nút, không biết đang ở giây nào; nó phải mượn dòng "Lời từ
 * lrclib" để nói mình đang ở bài nào. Nửa dưới trang Đang phát thì bỏ trống.
 *
 * Xem Zing, NCT và YouTube Music thì cả ba giải cùng một bài toán theo ba kiểu
 * khác nhau, nhưng **giống nhau ở một điểm**: điều khiển luôn với tới được
 * trong lúc đọc lời. Bố cục ở đây theo đúng điểm đó —
 *
 *   dải ngữ cảnh (luôn có)  ·  một mặt đổi được  ·  điều khiển (luôn có)
 *
 * Khác cả ba ở chỗ **lời là mặt mặc định**. Zing bắt vuốt, YouTube Music bắt
 * bấm một chip, NCT chỉ cho hai câu — cả ba coi lời là mặt phụ phải đi tìm, vì
 * cả ba là app nhạc. Lyra thì lời chính là thứ nó làm ra.
 *
 * Đổi mặt bằng HAI CHIP chứ không bằng vuốt ngang, dù Zing vuốt. Điều hướng
 * của Lyra đã là một pager ngang rồi; lồng thêm một pager ngang nữa thì mặt
 * trong nuốt hết cú vuốt và không ai sang được trang Tìm hay Chỉnh.
 */
@Composable
fun BaiPane(
    now: NowPlaying?,
    lyrics: Lyrics,
    loading: Boolean,
    position: State<Long>,
    accent: Color,
    artwork: android.graphics.Bitmap?,
    queue: List<Track>,
    queueIndex: Int,
    shuffle: Boolean,
    repeat: Int,
    hasNotificationAccess: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSkipInQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onSaveQueue: (String) -> Unit,
    translation: TranslationState,
    onSyncToLine: (Int) -> Unit,
    onSeekToLine: (Int) -> Unit,
    onClearOffset: () -> Unit,
    onEditLyrics: () -> Unit,
    onDownloadModel: () -> Unit,
    effect: LyricEffect
) {
    var naming by remember { mutableStateOf(false) }

    // Hỏi quyền khi VÀ CHỈ KHI trang này thật sự trống. Lyra không cần quyền
    // nào để biết bài của chính nó đang phát, nên chặn cả trang khi thiếu quyền
    // là tự cắt mất bộ phát và phần lời của chính mình.
    if (!hasNotificationAccess && now == null) {
        Box(Modifier.fillMaxSize().padding(horizontal = 26.dp), Alignment.Center) {
            Ask(
                title = "Cho Lyra đọc lời cho app nhạc khác",
                body = "Bật quyền đọc thông báo thì Lyra hiện lời cho nhạc phát ở Spotify, " +
                    "Zing, YouTube Music… Lyra không đọc nội dung thông báo — chỉ đọc tên bài " +
                    "và vị trí phát.\n\nKhông bật cũng được: vuốt sang trái để tìm bài " +
                    "và phát ngay trong Lyra.",
                action = "Mở Cài đặt để bật",
                accent = accent,
                onAction = onOpenNotificationSettings
            )
        }
        return
    }

    if (now == null) {
        Box(Modifier.fillMaxSize().padding(horizontal = 26.dp), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LyraMark(size = 56.dp, busy = false)
                Spacer(Modifier.height(18.dp))
                Text(
                    "Chưa có gì đang phát",
                    color = mau.chu,
                    fontFamily = boChu.loi,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vuốt sang trái để tìm bài, hoặc mở app nhạc khác — Lyra vẫn hiện lời.",
                    color = mau.chuMo,
                    fontSize = 14.5.sp
                )
            }
        }
        return
    }

    // Hỏi lại mỗi lần vẽ: một số app không mở `SEEK_TO`, và nhạc có thể đổi từ
    // app này sang app khác giữa chừng.
    val dieuKhienDuoc = Lyra.dieuKhienDuoc()
    val tuaDuoc = Lyra.tuaDuoc()

    // Hàng đợi CHỈ tồn tại khi Lyra là bên đang phát. Nhạc ở Spotify thì ta bấm
    // được nút của họ nhưng không nhìn thấy hàng đợi của họ.
    val hangDoiRieng = queue.isNotEmpty() && queueIndex >= 0

    var xemBia by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        DaiNguCanh(
            now = now,
            bia = artwork ?: now.artwork,
            accent = accent,
            xemBia = xemBia,
            onDoiMat = { xemBia = it }
        )

        Box(Modifier.weight(1f)) {
            Crossfade(xemBia, label = "mat") { hienBia ->
                if (hienBia) {
                    MatBia(
                        now = now,
                        bia = artwork ?: now.artwork,
                        accent = accent,
                        queue = queue,
                        queueIndex = queueIndex,
                        hangDoiRieng = hangDoiRieng,
                        onSkipInQueue = onSkipInQueue,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onLuuHangDoi = { naming = true }
                    )
                } else {
                    MatLoi(
                        lyrics = lyrics,
                        loading = loading,
                        position = position,
                        accent = accent,
                        translation = translation,
                        tuaDuoc = tuaDuoc,
                        onSyncToLine = onSyncToLine,
                        onSeekToLine = onSeekToLine,
                        onClearOffset = onClearOffset,
                        onEditLyrics = onEditLyrics,
                        onDownloadModel = onDownloadModel,
                        effect = effect
                    )
                }
            }
        }

        // Điều khiển GHIM CỨNG, không bao giờ cuộn đi mất.
        //
        // Đây là chỗ bố cục cũ hỏng: lời và nút bấm ở hai trang khác nhau, nên
        // đang đọc lời mà muốn tua lại một câu thì phải vuốt sang trang khác.
        // Cả Zing, NCT lẫn YouTube Music đều giữ điều khiển trong tầm tay khi
        // đọc lời — ba bố cục khác nhau, cùng một kết luận.
        // Một nét kẻ tách phần đọc khỏi phần bấm. Không có nó thì dòng lời
        // cuối cùng chạy thẳng vào thanh tua, và mắt không biết chữ hết ở đâu.
        Box(
            Modifier
                .padding(horizontal = 26.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(mau.vien)
        )
        Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 10.dp, bottom = 6.dp)) {
            Seek(
                accent = accent,
                position = position,
                duration = now.duration,
                enabled = tuaDuoc,
                onSeek = onSeek
            )
            if (dieuKhienDuoc) {
                Spacer(Modifier.height(2.dp))
                Transport(
                    accent = accent,
                    playing = now.isPlaying,
                    shuffle = shuffle,
                    repeat = repeat,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat
                )
            }
        }
    }

    if (naming) {
        NameDialog(
            title = "Lưu hàng đợi thành danh sách",
            initial = now.artist.ifBlank { now.title },
            accent = accent,
            onCancel = { naming = false },
            onConfirm = {
                onSaveQueue(it)
                naming = false
            }
        )
    }
}

/**
 * Dải ngữ cảnh: bài nào, ai hát, và hai chip đổi mặt.
 *
 * Luôn có mặt, kể cả khi đang đọc lời. Trang Lời cũ không có gì nói nó đang ở
 * bài nào — Zing và YouTube Music đều để đúng một dải như thế này ở đầu trang
 * lời, và đó là thứ đáng lấy.
 */
@Composable
private fun DaiNguCanh(
    now: NowPlaying,
    bia: android.graphics.Bitmap?,
    accent: Color,
    xemBia: Boolean,
    onDoiMat: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(mau.nenChim)
                .border(1.dp, mau.vien, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (bia != null) {
                Image(
                    bitmap = bia.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LyraMark(size = 26.dp, busy = false)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                now.title,
                color = mau.chu,
                fontFamily = boChu.loi,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (now.artist.isNotEmpty()) {
                Text(
                    now.artist.uppercase(),
                    color = mau.chuMo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        ChipMat("Lời", !xemBia, accent) { onDoiMat(false) }
        Spacer(Modifier.width(6.dp))
        ChipMat("Bìa", xemBia, accent) { onDoiMat(true) }
    }
}

@Composable
private fun ChipMat(nhan: String, dangChon: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (dangChon) accent else mau.nenChim)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            nhan,
            color = if (dangChon) Color.White else mau.chuMo,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Mặt bìa: bản in, nguồn đang phát, và hàng đợi phía dưới. */
@Composable
private fun MatBia(
    now: NowPlaying,
    bia: android.graphics.Bitmap?,
    accent: Color,
    queue: List<Track>,
    queueIndex: Int,
    hangDoiRieng: Boolean,
    onSkipInQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onLuuHangDoi: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 24.dp)
    ) {
        item {
            Stage(accent = accent, kind = MediaKind.AUDIO) {
                if (bia != null) {
                    Image(
                        bitmap = bia.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        LyraMark(size = 64.dp, busy = false)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (now.isPlaying) accent else mau.chuRatMo)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${appLabel(now.packageName)} · ${if (now.isPlaying) "đang phát" else "tạm dừng"}",
                    color = mau.chuRatMo,
                    fontSize = 13.5.sp
                )
            }

            if (hangDoiRieng) {
                Spacer(Modifier.height(26.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (queue.size > queueIndex + 1)
                            "Tiếp theo · ${queue.size - queueIndex - 1} bài"
                        else
                            "Hết hàng đợi",
                        color = mau.chuRatMo,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(mau.nenChim)
                            .clickable(onClick = onLuuHangDoi)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "Lưu thành danh sách",
                            color = mau.chu,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (hangDoiRieng) {
            val from = (queueIndex + 1).coerceAtMost(queue.size)
            itemsIndexed(
                queue.subList(from, queue.size),
                key = { _, t -> t.playbackUri }
            ) { i, track ->
                QueueRow(
                    track = track,
                    onSkip = { onSkipInQueue(from + i) },
                    onRemove = { onRemoveFromQueue(from + i) }
                )
            }
        }
    }
}

/** Mặt lời: dải báo ở trên, lời cuộn ở dưới. */
@Composable
private fun MatLoi(
    lyrics: Lyrics,
    loading: Boolean,
    position: State<Long>,
    accent: Color,
    translation: TranslationState,
    tuaDuoc: Boolean,
    onSyncToLine: (Int) -> Unit,
    onSeekToLine: (Int) -> Unit,
    onClearOffset: () -> Unit,
    onEditLyrics: () -> Unit,
    onDownloadModel: () -> Unit,
    effect: LyricEffect
) {
    // Chạm vào câu mà nguồn không cho tua thì phải nói ra. Im lặng ở đây là tệ
    // nhất: người dùng chạm, không có gì xảy ra, và không biết là app hỏng hay
    // mình bấm sai chỗ.
    var baoKhongTua by remember { mutableStateOf(false) }
    LaunchedEffect(baoKhongTua) {
        if (baoKhongTua) {
            kotlinx.coroutines.delay(4000)
            baoKhongTua = false
        }
    }

    // `derivedStateOf`: vị trí phát đổi 5 lần mỗi giây, nhưng dòng đang hát thì
    // vài giây mới đổi một lần. Không bọc thì cả danh sách bị dựng lại liên tục
    // — đây chính là chỗ dễ sinh giật nhất.
    val active by remember(lyrics) {
        derivedStateOf { activeLineIndex(lyrics.lines, position.value, lyrics.offset) }
    }

    // Mốc đang ngờ thì KHÔNG tô sáng và KHÔNG tự cuộn. Tô sáng nhầm một dòng
    // suốt cả bài còn tệ hơn là không tô gì.
    val trustTiming = lyrics.synced && !lyrics.timingSuspect
    val listState = rememberLazyListState()
    val translated = (translation as? TranslationState.Done)?.lines ?: emptyList()

    LaunchedEffect(active, trustTiming) {
        if (trustTiming && active >= 0 && lyrics.lines.isNotEmpty()) {
            listState.animateScrollToItem(active.coerceAtLeast(0), scrollOffset = -260)
        }
    }

    // Câu đang hát sáng dần từ trái sang theo tiến độ TRONG CÂU. Không phải
    // karaoke từng chữ: LRCLIB chỉ cho mốc theo dòng. Chạy bằng một hoạt ảnh
    // tuyến tính đặt một lần mỗi khi đổi dòng, chứ không bám theo `position` —
    // vị trí phát chỉ cập nhật vài lần mỗi giây, quét theo nó thì giật từng nấc.
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
                    color = mau.chuMo,
                    fontSize = 14.5.sp
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
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Một dải báo duy nhất, và nó LUÔN có lối vào chỗ sửa lời.
        Notice(
            accent = accent,
            text = when {
                lyrics.timingSuspect ->
                    "Lời của bản thu khác nên mốc có thể lệch. Nhấn giữ câu đang " +
                        "hát để căn lại."
                lyrics.offset != 0L ->
                    "Đã căn lệch " + offsetLabel(lyrics.offset) + ". Bấm để bỏ."
                lyrics.from == "tự nhập" -> "Lời bạn tự nhập."
                else -> "Lời từ " + lyrics.from + "."
            },
            onClick = if (lyrics.offset != 0L) onClearOffset else null,
            action = if (lyrics.from == "tự nhập") "Sửa lời" else "Tự nhập",
            onAction = onEditLyrics
        )

        if (baoKhongTua) {
            Notice(
                accent = accent,
                text = "App đang phát không cho tua. Nhấn giữ một câu để căn lệch nhịp thay vào đó."
            )
        }

        when (translation) {
            is TranslationState.NeedsModel -> Notice(
                accent = accent,
                text = "Lời đang là tiếng " + languageName(translation.language) +
                    ". Tải gói ngôn ngữ về máy để dịch, một lần dùng mãi.",
                action = "Tải gói",
                onAction = onDownloadModel
            )
            is TranslationState.Failed -> Notice(accent = accent, text = translation.why + ".")
            TranslationState.Working -> Notice(accent = accent, text = "Đang dịch lời…")
            else -> Unit
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 30.dp, bottom = 46.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(lyrics.lines, key = { i, _ -> i }) { i, line ->
                DongLoi(
                    chiSo = i,
                    line = line,
                    dangHat = trustTiming && i == active,
                    xa = if (trustTiming && active >= 0)
                        (i - active).absoluteValue.coerceAtMost(4) else 0,
                    tinMoc = trustTiming,
                    banDich = translated.getOrNull(i).orEmpty(),
                    effect = effect,
                    quet = quet,
                    accent = accent,
                    onCham = { if (tuaDuoc) onSeekToLine(i) else baoKhongTua = true },
                    onNhanGiu = { onSyncToLine(i) }
                )
            }
        }
    }
}
