package com.mittohoa.lyra.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.sources.MediaKind
import com.mittohoa.lyra.sources.Track
import androidx.compose.foundation.Image
import kotlin.math.roundToLong

/**
 * Trang phat.
 *
 * Hai quyet dinh hinh thuc, va ca hai deu co ly do chuc nang:
 *
 * **San khau la mot CHO CAM, khong phai mot khung anh.** Hom nay no dung anh
 * bia vuong; mai co video thi cung cho ay doi sang 16:9 va nhan mot be mat ve.
 * Moi thu quanh no - thanh tua, nut bam, hang doi - khong biet ben trong la gi.
 * Nho vay them video khong phai viet lai trang nay.
 *
 * **Thanh tua la duong vien duoi cua san khau.** Khong phai mot thanh truot roi
 * dat o dau do, ma la chinh canh duoi cua thu dang phat sang dan len. No vua la
 * mot hinh anh de nho, vua chinh xac hon vong tron hay cung tron - keo ngang
 * mot duong thang la thao tac dung nhat ma ngon tay lam duoc.
 */
@Composable
fun PlayerPane(
    now: NowPlaying?,
    accent: Color,
    artwork: Bitmap?,
    position: State<Long>,
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
    onSaveQueue: (String) -> Unit
) {
    var naming by remember { mutableStateOf(false) }
    // Hỏi quyền khi VÀ CHỈ KHI màn hình này thật sự trống. Lyra không cần quyền
    // nào để biết bài của chính nó đang phát, nên chặn cả trang khi thiếu quyền
    // là tự cắt mất bộ phát, phần tìm bài và phần lời của chính mình — đúng thứ
    // vẫn chạy được. Người từ chối quyền vẫn còn nguyên một app dùng được.
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
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vuốt sang trái để tìm bài, hoặc mở app nhạc khác — Lyra vẫn hiện lời.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.5.sp
                )
            }
        }
        return
    }

    // Lyra tu phat thi co nut bam; nhac o app khac thi khong. Khong app nao
    // dieu khien duoc bo phat cua app khac, va bay nut ra roi bam khong an gi
    // con te hon la khong co nut.
    val mine = queue.isNotEmpty() && queueIndex >= 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 120.dp)
    ) {
        item {
            // Bài Lyra tự phát thì bìa do Lyra tải về (`artwork`); bài ở app khác
            // thì bìa đi kèm bản tin media (`now.artwork`). Chỉ dùng cái đầu là
            // bỏ trống ô ảnh với mọi bài phát ở app khác — đúng cảnh hay gặp
            // nhất, vì đó là vai chính của app.
            val bia = artwork ?: now.artwork

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

            Seek(
                accent = accent,
                position = position,
                duration = now.duration,
                enabled = mine,
                onSeek = onSeek
            )

            Spacer(Modifier.height(20.dp))
            Text(
                now.title,
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 31.sp,
                maxLines = 2
            )
            if (now.artist.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(now.artist, color = Color.White.copy(alpha = 0.66f), fontSize = 15.sp, maxLines = 1)
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (now.isPlaying) accent else Color.White.copy(alpha = 0.3f))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${appLabel(now.packageName)} · ${if (now.isPlaying) "đang phát" else "tạm dừng"}",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.5.sp
                )
            }

            if (mine) {
                Spacer(Modifier.height(22.dp))
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

            if (mine) {
                Spacer(Modifier.height(30.dp))
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
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Hang doi la cho lam viec; danh sach phat la thu giu lai.
                    // Mot nut noi hai cai do la duong ngan nhat tu "vua xep duoc
                    // mot chuoi hay" toi "muon nghe lai chuoi nay".
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { naming = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "Lưu thành danh sách",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (mine) {
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
 * San khau: cho de VE thu dang phat.
 *
 * Hom nay la anh bia vuong. Video chi can doi ti le sang 16:9 va truyen vao mot
 * be mat thay cho anh - phan quang sang phia sau, bo goc, va moi thu ben ngoai
 * deu khong doi.
 *
 * Quang sang la anh chinh no bi lam mo va phong to nam phia sau. Do khong phai
 * trang tri: no keo mau cua bia tran ra ngoai khung, nen ca man hinh doi mau
 * theo bai dang nghe ma khong can them mot lop mau gia nao.
 */
@Composable
private fun Stage(
    accent: Color,
    kind: MediaKind,
    content: @Composable BoxScope.() -> Unit
) {
    val ratio = if (kind == MediaKind.VIDEO) 16f / 9f else 1f

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(0.94f)
                .blur(38.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.55f))
        )
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Thanh tua nam sat duoi san khau.
 *
 * Keo thi hien vi tri DANG KEO chu khong phai vi tri dang phat: cho toi luc tha
 * tay moi nhay toi thi ngon tay va vach sang di lech nhau suot ca thao tac, va
 * cam giac la may khong nghe loi.
 */
@Composable
private fun Seek(
    accent: Color,
    position: State<Long>,
    duration: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(1f) }

    val playedFraction = if (duration > 0) {
        (position.value.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction = if (dragging) dragFraction else playedFraction
    val shown = if (dragging) (dragFraction * duration).roundToLong() else position.value

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .then(
                    if (!enabled) Modifier else Modifier.pointerInput(duration) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                dragging = true
                                dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                dragging = false
                                if (duration > 0) onSeek((dragFraction * duration).roundToLong())
                            },
                            onDragCancel = { dragging = false }
                        ) { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    }
                )
                .then(
                    if (!enabled) Modifier else Modifier.pointerInput(duration) {
                        detectTapGestures { offset ->
                            if (duration > 0) {
                                onSeek(((offset.x / size.width).coerceIn(0f, 1f) * duration).roundToLong())
                            }
                        }
                    }
                )
        ) {
            Canvas(Modifier.fillMaxSize()) {
                width = size.width
                val y = size.height / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                if (fraction > 0f) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.75f), accent)
                        ),
                        start = Offset(0f, y),
                        end = Offset(size.width * fraction, y),
                        strokeWidth = if (dragging) 9f else 6f,
                        cap = StrokeCap.Round
                    )
                }
                if (enabled) {
                    drawCircle(
                        color = accent,
                        radius = if (dragging) 17f else 11f,
                        center = Offset(size.width * fraction, y)
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(clockLabel(shown), color = Color.White.copy(alpha = 0.45f), fontSize = 11.5.sp)
            Text(
                if (duration > 0) clockLabel(duration) else "--:--",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.5.sp
            )
        }
    }
}

/**
 * Nam nut, xep theo tam quan trong: nut phat to nhat va sang nhat o giua, hai
 * nut chuyen bai hai ben, tron va lap nho hon o ngoai cung.
 *
 * Tron va lap sang len khi dang bat - khong doi mau nen, chi doi mau chu. Chung
 * la trang thai chu khong phai hanh dong, va trang thai thi khong nen keu to.
 */
@Composable
private fun Transport(
    accent: Color,
    playing: Boolean,
    shuffle: Boolean,
    repeat: Int,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Ghost("⤨", active = shuffle, accent = accent, onClick = onToggleShuffle)
        Ghost("◀◀", active = false, accent = accent, onClick = onPrevious)
        PlayButton(accent = accent, playing = playing, onClick = onPlayPause)
        Ghost("▶▶", active = false, accent = accent, onClick = onNext)
        Ghost(
            if (repeat == REPEAT_ONE) "↻¹" else "↻",
            active = repeat != REPEAT_OFF,
            accent = accent,
            onClick = onCycleRepeat
        )
    }
}

@Composable
private fun PlayButton(accent: Color, playing: Boolean, onClick: () -> Unit) {
    // Nut phong nhe khi bam - phan hoi duy nhat cho mot thao tac ma ket qua cua
    // no (tieng nhac) den sau vai tram mili-giay
    val scale by animateFloatAsState(if (playing) 1f else 0.94f, tween(220), label = "play")

    Box(
        Modifier
            .size(72.dp)
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.78f))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(if (playing) "❚❚" else "▶", color = Color.White, fontSize = 23.sp)
    }
}

@Composable
private fun Ghost(label: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) accent else Color.White.copy(alpha = 0.55f),
            fontSize = 17.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun QueueRow(track: Track, onSkip: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSkip)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                track.artist.ifBlank { track.source.label },
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Text("×", color = Color.White.copy(alpha = 0.45f), fontSize = 19.sp)
        }
    }
}

private const val REPEAT_OFF = 0
private const val REPEAT_ONE = 2

/** Do dai bai dang "3:45"; qua mot tieng thi them phan gio. */
internal fun clockLabel(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, (total % 3600) / 60, total % 60)
    else "%d:%02d".format(total / 60, total % 60)
}
