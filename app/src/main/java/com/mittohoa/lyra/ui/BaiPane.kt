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
import androidx.compose.ui.platform.LocalContext
import com.mittohoa.lyra.lyrics.LrcCanhTep
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.player.Playback
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
    effect: LyricEffect,
    /** Mở màn hình thẻ lời ở câu này. -1 nghĩa là chưa có câu nào đang hát. */
    onChiaSeCau: (Int) -> Unit,
    onCanGio: () -> Unit,
    /** Mở video ra toàn màn hình. Chỉ có nghĩa khi bài đang phát là video. */
    onToanManHinh: () -> Unit,
    /**
     * Video đang xem toàn màn hình hay chưa.
     *
     * Ô hình nhỏ trong trang phải BIẾN MẤT trong lúc đó, không phải vì nó bị
     * che — mà vì bộ giải mã chỉ vẽ vào MỘT bề mặt. Để cả hai cùng sống thì ô
     * mở sau giành mất bề mặt, và lúc thoát ra ô nhỏ nằm lại một màu đen mà
     * không lỗi nào báo. Bỏ hẳn ô nhỏ đi thì lúc quay về nó dựng lại từ đầu và
     * tự xin bề mặt.
     */
    toanManHinh: Boolean
) {
    val ngucanh = androidx.compose.ui.platform.LocalContext.current
    var naming by remember { mutableStateOf(false) }

    // Chế độ luyện tập: lặp một đoạn và đổi tốc độ.
    //
    // Là một CHẾ ĐỘ có bật tắt chứ không phải thêm một cử chỉ nữa lên dòng lời.
    // Chạm và nhấn giữ đều đã có việc; nhét việc thứ ba vào thì lại đúng cái
    // lỗi vừa sửa hôm nay — một cử chỉ gánh hai việc, và không ai đoán được.
    // Bật chế độ lên thì có một dải nói rõ đang chờ chọn gì.
    var luyenTap by remember { mutableStateOf(false) }
    var dongA by remember { mutableStateOf<Int?>(null) }

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

    // Bài chỉ TRA LỜI, không phát — bản Play tìm được nhạc ở Zing/NCT nhưng
    // không phát được. Lúc đó thanh tua và hàng nút là hai thứ chết: giấu đi
    // chứ không bày ra rồi để chúng không ăn gì.
    val chiXem = Lyra.laDangXem()

    // Hàng đợi CHỈ tồn tại khi Lyra là bên đang phát. Nhạc ở Spotify thì ta bấm
    // được nút của họ nhưng không nhìn thấy hàng đợi của họ.
    val hangDoiRieng = queue.isNotEmpty() && queueIndex >= 0

    var xemBia by remember { mutableStateOf(false) }
    val doanLap by Lyra.doanLap.collectAsStateWithLifecycle()
    val nguonHangDoi by Lyra.nguonHangDoi.collectAsStateWithLifecycle()
    val trangThaiGop by Lyra.gop.collectAsStateWithLifecycle()
    val tocDo by Lyra.tocDo.collectAsStateWithLifecycle()

    // Dòng đang hát tính MỘT lần ở đây rồi truyền xuống: mặt lời cần nó để tô
    // sáng, thẻ lời cần nó để mở đúng câu vừa nghe.
    val dongDangHat by remember(lyrics) {
        derivedStateOf { activeLineIndex(lyrics.lines, position.value, lyrics.offset) }
    }

    Column(Modifier.fillMaxSize()) {
        DaiNguCanh(
            now = now,
            bia = artwork ?: now.artwork,
            accent = accent,
            xemBia = xemBia,
            onDoiMat = { xemBia = it },
            chiaSeDuoc = lyrics.lines.isNotEmpty(),
            onChiaSe = { onChiaSeCau(dongDangHat) },
            luyenTapDuoc = lyrics.lines.isNotEmpty() && tuaDuoc,
            dangLuyenTap = luyenTap || doanLap != null,
            onLuyenTap = {
                if (luyenTap || doanLap != null) {
                    luyenTap = false
                    dongA = null
                    Lyra.boDoanLap()
                } else {
                    luyenTap = true
                    dongA = null
                }
            }
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
                        chiXem = chiXem,
                        nguon = nguonHangDoi,
                        onSkipInQueue = onSkipInQueue,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onDoiCho = { tu, den -> Lyra.doiChoTrongHangDoi(ngucanh, tu, den) },
                        onLuuHangDoi = { naming = true },
                        onToanManHinh = onToanManHinh,
                        toanManHinh = toanManHinh
                    )
                } else {
                    MatLoi(
                        lyrics = lyrics,
                        active = dongDangHat,
                        loading = loading,
                        position = position,
                        accent = accent,
                        translation = translation,
                        baoKhongTua = baoKhongTua,
                        onChamDong = { i ->
                            when {
                                luyenTap && dongA == null -> dongA = i
                                luyenTap -> {
                                    Lyra.datDoanLap(ngucanh, dongA!!, i)
                                    dongA = null
                                    luyenTap = false
                                }
                                tuaDuoc -> onSeekToLine(i)
                                else -> baoKhongTua = true
                            }
                        },
                        onSyncToLine = onSyncToLine,
                        onClearOffset = onClearOffset,
                        onEditLyrics = onEditLyrics,
                        onDownloadModel = onDownloadModel,
                        effect = effect,
                        gop = trangThaiGop,
                        onGop = { Lyra.gopLoiChoLrclib() },
                        onThoiGop = { Lyra.thoiGopLoi() },
                        onCanGio = onCanGio
                    )
                }
            }
        }

        if (luyenTap || doanLap != null) {
            DaiLuyenTap(
                accent = accent,
                dongA = dongA,
                doan = doanLap,
                tocDo = tocDo,
                doiTocDoDuoc = Lyra.laLyraPhat(),
                onTocDo = { Lyra.datTocDo(ngucanh, it) },
                onBo = {
                    luyenTap = false
                    dongA = null
                    Lyra.boDoanLap()
                }
            )
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
        if (!chiXem) Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 10.dp, bottom = 6.dp)) {
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
    onDoiMat: (Boolean) -> Unit,
    chiaSeDuoc: Boolean,
    onChiaSe: () -> Unit,
    luyenTapDuoc: Boolean,
    dangLuyenTap: Boolean,
    onLuyenTap: () -> Unit
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
        Spacer(Modifier.width(8.dp))
        if (luyenTapDuoc) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (dangLuyenTap) accent else mau.nenChim)
                    .clickable(onClick = onLuyenTap),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "AB",
                    color = if (dangLuyenTap) Color.White else mau.chuMo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (chiaSeDuoc) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(mau.nenChim)
                    .clickable(onClick = onChiaSe),
                contentAlignment = Alignment.Center
            ) {
                Text("↗", color = mau.chuMo, fontSize = 17.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
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

/**
 * Dải luyện tập: lặp một đoạn, và đổi tốc độ.
 *
 * Chọn đoạn theo CÂU chứ không kéo hai mốc trên thanh thời gian — đó là chỗ
 * Lyra làm được mà một bộ lặp A–B thường không: nó biết câu nào bắt đầu lúc nào.
 *
 * Tốc độ chỉ hiện khi Lyra là bên đang phát. `MediaController` của hệ thống
 * không có đường đổi tốc độ cho nhạc ở app khác, và bày một nút không làm gì
 * thì tệ hơn là không bày.
 */
@Composable
private fun DaiLuyenTap(
    accent: Color,
    dongA: Int?,
    doan: Lyra.DoanLap?,
    tocDo: Float,
    doiTocDoDuoc: Boolean,
    onTocDo: (Float) -> Unit,
    onBo: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    doan != null -> "Lặp câu ${doan.tuDong + 1}–${doan.denDong + 1}"
                    dongA != null -> "Câu ${dongA + 1} → chạm câu cuối đoạn"
                    else -> "Chạm câu đầu của đoạn muốn lặp"
                },
                color = if (doan != null) accent else mau.chuMo,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBo)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("✕", color = mau.chuMo, fontSize = 15.sp)
            }
        }
        if (doiTocDoDuoc) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (v in listOf(0.5f, 0.75f, 1f, 1.25f)) {
                    val chon = kotlin.math.abs(tocDo - v) < 0.01f
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (chon) accent else mau.nenChim)
                            .clickable { onTocDo(v) }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (v == 1f) "1×" else v.toString().trimEnd('0').replace('.', ',') + "×",
                            color = if (chon) Color.White else mau.chuMo,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
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
    chiXem: Boolean,
    nguon: String?,
    onSkipInQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onDoiCho: (Int, Int) -> Unit,
    onLuuHangDoi: () -> Unit,
    onToanManHinh: () -> Unit,
    toanManHinh: Boolean
) {
    // Kéo thả sắp lại hàng đợi.
    //
    // Chỉ dời VỀ HÌNH ẢNH trong lúc kéo, tới lúc thả mới sửa hàng đợi thật.
    // Sửa ngay từng bước thì danh sách dựng lại giữa chừng, cái ô đang kéo
    // nhảy sang chỉ số khác, bộ nhận cử chỉ bị dựng lại theo — và cú kéo đứt
    // ngang giữa ngón tay.
    var keoTu by remember { mutableIntStateOf(-1) }
    var lech by remember { mutableFloatStateOf(0f) }
    var caoMuc by remember { mutableIntStateOf(0) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 24.dp)
    ) {
        item {
            // Bai dang phat la video thi chinh o bia tro thanh man hinh. Mot
            // trang rieng cho video se cat doi app lam hai nua ma khong duoc gi:
            // cho de anh bia von da la mot o hinh vuong dat giua trang.
            val baiNay = queue.getOrNull(queueIndex)
            val laVideo = baiNay?.kind == MediaKind.VIDEO && !toanManHinh

            Stage(
                accent = accent,
                kind = if (laVideo) MediaKind.VIDEO else MediaKind.AUDIO,
                tiLe = if (laVideo) baiNay?.tiLe else null
            ) {
                if (laVideo) {
                    ManHinhVideo(dangPhat = now.isPlaying)
                    // Nút mở toàn màn hình, góc dưới phải của chính ô hình.
                    // Không đặt ở dải nút chung bên dưới: nó chỉ có nghĩa khi
                    // đang có hình, mà ô hình thì là chỗ mắt đang nhìn.
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(onClick = onToanManHinh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⛶", color = Color.White, fontSize = 17.sp)
                    }
                } else if (bia != null) {
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
                    if (chiXem) "Đang tra lời — bản này không phát nhạc từ nguồn đó"
                    else "${appLabel(now.packageName)} · ${if (now.isPlaying) "đang phát" else "tạm dừng"}",
                    color = mau.chuRatMo,
                    fontSize = 13.5.sp
                )
            }

            if (nguon != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "PHÁT TỪ · " + nguon.uppercase(),
                    color = mau.chuRatMo,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
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
            val conLai = queue.subList(from, queue.size)
            itemsIndexed(conLai, key = { _, t -> t.playbackUri }) { i, track ->
                val dangKeo = keoTu == i
                // Chỗ sẽ thả xuống, tính theo số ô đã trượt qua.
                val dich = if (keoTu < 0 || caoMuc == 0) -1
                    else (keoTu + Math.round(lech / caoMuc)).coerceIn(0, conLai.lastIndex)
                // Các ô nằm giữa chỗ nhấc lên và chỗ sắp thả phải nhường chỗ,
                // không thì người kéo không thấy mình đang chen vào đâu.
                val nhuong = when {
                    keoTu < 0 || dangKeo || dich < 0 -> 0
                    keoTu < dich && i in (keoTu + 1)..dich -> -caoMuc
                    keoTu > dich && i in dich until keoTu -> caoMuc
                    else -> 0
                }
                val nhuongMuot by animateFloatAsState(nhuong.toFloat(), tween(120), label = "nhuong$i")

                QueueRow(
                    track = track,
                    modifier = Modifier
                        .onSizeChanged { if (caoMuc == 0) caoMuc = it.height }
                        .zIndex(if (dangKeo) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dangKeo) lech else nhuongMuot
                            scaleX = if (dangKeo) 1.02f else 1f
                            scaleY = if (dangKeo) 1.02f else 1f
                            alpha = if (dangKeo) 0.92f else 1f
                        }
                        .pointerInput(conLai.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { keoTu = i; lech = 0f },
                                onDrag = { doi, keo ->
                                    doi.consume()
                                    lech += keo.y
                                },
                                onDragEnd = {
                                    val den = if (caoMuc == 0) i
                                        else (i + Math.round(lech / caoMuc)).coerceIn(0, conLai.lastIndex)
                                    if (den != i) onDoiCho(from + i, from + den)
                                    keoTu = -1
                                    lech = 0f
                                },
                                onDragCancel = { keoTu = -1; lech = 0f }
                            )
                        },
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
    active: Int,
    loading: Boolean,
    position: State<Long>,
    accent: Color,
    translation: TranslationState,
    baoKhongTua: Boolean,
    onChamDong: (Int) -> Unit,
    onSyncToLine: (Int) -> Unit,
    onClearOffset: () -> Unit,
    onEditLyrics: () -> Unit,
    onDownloadModel: () -> Unit,
    effect: LyricEffect,
    gop: Lyra.TrangThaiGop?,
    onGop: () -> Unit,
    onThoiGop: () -> Unit,
    onCanGio: () -> Unit
) {
    // Mốc đang ngờ thì KHÔNG tô sáng và KHÔNG tự cuộn. Tô sáng nhầm một dòng
    // suốt cả bài còn tệ hơn là không tô gì.
    val trustTiming = lyrics.synced && !lyrics.timingSuspect

    // Trang thai cua dai "ghi ra tep": cau bao gan nhat, va co dang cho xac
    // nhan ghi de hay khong. Doi bai thi quen het - mot cau bao ve bai truoc
    // nam lai o bai sau la sai.
    var baoGhi by remember(lyrics.from, lyrics.lines.size) { mutableStateOf<String?>(null) }
    var choDeLen by remember(lyrics.from, lyrics.lines.size) { mutableStateOf(false) }
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

        // Lời chữ trơn thì mời căn giờ.
        //
        // Chưa căn thì Lyra chỉ hiện được một khối chữ: không tô sáng câu đang
        // hát, không khung lời nổi chạy theo, không lặp A–B, không thẻ lời. Gần
        // hết những gì app làm đều đứng trên chỗ có mốc thời gian.
        if (!lyrics.synced && lyrics.lines.isNotEmpty()) {
            Notice(
                accent = accent,
                text = "Lời này chưa có mốc thời gian nên không chạy theo nhạc được.",
                action = "Căn giờ",
                onAction = onCanGio
            )
        }

        // Góp lời ngược lại cho LRCLIB.
        //
        // Chỉ mời khi lời là do người dùng TỰ NHẬP: lời tải về từ LRCLIB thì
        // gửi lại chính nó là vô nghĩa, còn lời từ Zing/NCT thì không phải của
        // mình mà đem cho.
        //
        // Và chỉ MỜI, không tự làm. Đây là đăng lên một kho công cộng ai cũng
        // đọc được và không rút lại được.
        if (lyrics.from == "tự nhập" || gop != null) {
            Notice(
                accent = accent,
                text = when (gop) {
                    null -> "Góp bản lời này cho LRCLIB để ai cũng dùng được."
                    is Lyra.TrangThaiGop.DangGiai ->
                        "Đang giải thử thách chống spam… " +
                            "${gop.daThu / 1000} nghìn lần bằm"
                    Lyra.TrangThaiGop.DangGui -> "Đang gửi…"
                    Lyra.TrangThaiGop.Xong -> "Đã góp cho LRCLIB. Cảm ơn bạn."
                    is Lyra.TrangThaiGop.Hong -> gop.vi
                },
                action = when (gop) {
                    null -> "Góp"
                    is Lyra.TrangThaiGop.DangGiai -> "Huỷ"
                    is Lyra.TrangThaiGop.Hong -> "Thử lại"
                    else -> null
                },
                onAction = if (gop is Lyra.TrangThaiGop.DangGiai) onThoiGop else onGop
            )
        }

        // Ghi lời ra tệp .lrc nằm cạnh tệp nhạc.
        //
        // Chỉ mời khi đang phát nhạc TRONG MÁY: nhạc từ Zing hay từ app khác
        // thì không có tệp nào trên đĩa để mà ghi cạnh.
        //
        // Và chỉ mời khi lời KHÔNG PHẢI vừa đọc lên từ chính tệp đó — ghi lại
        // đúng cái mình vừa đọc ra là một nút bấm xong không đổi gì.
        if (Lyra.laNhacTrongMay() && lyrics.lines.isNotEmpty() &&
            !LrcCanhTep.laTepCanh(lyrics.from)
        ) {
            val nhac = LocalContext.current
            Notice(
                accent = accent,
                text = baoGhi ?: "Ghi lời này ra tệp .lrc nằm cạnh bài nhạc — " +
                    "trình phát khác cũng đọc được, và gỡ app đi vẫn còn.",
                action = if (choDeLen) "Ghi đè" else "Ghi ra tệp",
                onAction = {
                    when (val kq = Lyra.ghiLoiRaTepCanh(nhac, deLen = choDeLen)) {
                        is LrcCanhTep.KetQuaGhi.Xong -> {
                            choDeLen = false
                            baoGhi = "Đã ghi ra " + tenTep(kq.duong) + "."
                        }
                        is LrcCanhTep.KetQuaGhi.DaCoTep -> {
                            // Không tự đè: tệp nằm sẵn ở đó là công của ai đó,
                            // có thể là công của chính người dùng gõ trên máy
                            // tính. Hỏi một câu rẻ hơn làm mất nó nhiều.
                            choDeLen = true
                            baoGhi = "Đã có sẵn " + tenTep(kq.duong) + ". Ghi đè lên?"
                        }
                        is LrcCanhTep.KetQuaGhi.Hong -> {
                            choDeLen = false
                            baoGhi = "Không ghi được: " + kq.lyDo + "."
                        }
                        LrcCanhTep.KetQuaGhi.KhongPhaiTepTrongMay -> {
                            choDeLen = false
                            baoGhi = "Bài này không phải tệp trong máy nên không có chỗ để ghi cạnh."
                        }
                    }
                }
            )
        }

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
                    onCham = { onChamDong(i) },
                    onNhanGiu = { onSyncToLine(i) }
                )
            }
        }
    }
}

/** Chi lay ten tep de cau bao khong dai loang ngoang ca duong dan. */
private fun tenTep(duong: String) = duong.substringAfterLast('/')
