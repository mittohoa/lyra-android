package com.mittohoa.lyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.compose.foundation.Image
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.activeLineIndex
import com.mittohoa.lyra.lyrics.tenBaiDeHien
import com.mittohoa.lyra.player.Playback
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.sources.MediaKind
import com.mittohoa.lyra.sources.Track
import com.mittohoa.lyra.translate.TranslationState
import com.mittohoa.lyra.translate.languageName
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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
 *   dải ngữ cảnh (luôn có)  ·  bìa rồi lời, cuộn liền  ·  điều khiển (luôn có)
 *
 * Khác cả ba ở chỗ **không phải đi tìm lời**. Zing bắt vuốt, YouTube Music bắt
 * bấm một chip, NCT chỉ cho hai câu — cả ba coi lời là mặt phụ, vì cả ba là app
 * nhạc. AURA thì lời chính là thứ nó làm ra.
 *
 * BÌA VÀ LỜI KHÔNG CÒN LÀ HAI MẶT. Trước đây có cặp chip "Lời / Bìa" để đổi qua
 * lại, và cặp chip ấy bắt người dùng chọn giữa hai thứ họ muốn thấy cùng lúc:
 * bìa để biết đang bài nào, lời để đọc. Giờ bìa là MỤC ĐẦU của chính danh sách
 * lời — mở lên thấy bìa, cuộn xuống thì bìa trôi lên nhường chỗ cho chữ, và nó
 * tự quay lại khi lời cuộn về đầu bài mới. Không có nút nào phải bấm.
 *
 * Còn HÀNG ĐỢI thì đúng là một trang khác thật, nên nó là một tấm đè lên, mở
 * bằng nút ☰ trên dải ngữ cảnh. Không nhét được nó vào cuối danh sách lời: nó
 * cần cử chỉ nhấn-giữ-rồi-kéo, mà hai danh sách cuộn dọc lồng nhau thì giành
 * nhau cùng một cú vuốt.
 *
 * Vẫn KHÔNG dùng vuốt ngang cho bất cứ việc gì ở đây, dù Zing vuốt. Điều hướng
 * của AURA đã là một pager ngang rồi; lồng thêm một pager ngang nữa thì mặt
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
    /** Mở màn hình chọn bản lời khác. */
    onChonBanLoi: () -> Unit,
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

    // Hỏi quyền khi VÀ CHỈ KHI trang này thật sự trống. AURA không cần quyền
    // nào để biết bài của chính nó đang phát, nên chặn cả trang khi thiếu quyền
    // là tự cắt mất bộ phát và phần lời của chính mình.
    if (!hasNotificationAccess && now == null) {
        Box(Modifier.fillMaxSize().padding(horizontal = 26.dp), Alignment.Center) {
            Ask(
                title = "Cho AURA đọc lời cho app nhạc khác",
                body = "Bật quyền đọc thông báo thì AURA hiện lời cho nhạc phát ở Spotify, " +
                    "Zing, YouTube Music… AURA không đọc nội dung thông báo — chỉ đọc tên bài " +
                    "và vị trí phát.\n\nKhông bật cũng được: vuốt sang trái để tìm bài " +
                    "và phát ngay trong AURA.",
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
                    "Vuốt sang trái để tìm bài, hoặc mở app nhạc khác — AURA vẫn hiện lời.",
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

    // Hàng đợi CHỈ tồn tại khi AURA là bên đang phát. Nhạc ở Spotify thì ta bấm
    // được nút của họ nhưng không nhìn thấy hàng đợi của họ.
    val hangDoiRieng = queue.isNotEmpty() && queueIndex >= 0

    // Hàng đợi là một TẤM ĐÈ LÊN, mở ra rồi đóng lại — không phải một trong hai
    // mặt ngang hàng nhau như trước. Đổi bài thì tự đóng: người ta mở nó ra để
    // chọn bài kế, chọn xong rồi mà tấm vẫn che thì phải bấm thêm một cái nữa
    // mới nhìn được bài mình vừa chọn.
    var xemHangDoi by remember(queueIndex) { mutableStateOf(false) }
    val doanLap by Lyra.doanLap.collectAsStateWithLifecycle()
    val nguonHangDoi by Lyra.nguonHangDoi.collectAsStateWithLifecycle()
    val tenAppNguon = tenApp(now.packageName)
    val trangThaiGop by Lyra.gop.collectAsStateWithLifecycle()
    val tocDo by Lyra.tocDo.collectAsStateWithLifecycle()

    // Dòng đang hát tính MỘT lần ở đây rồi truyền xuống: mặt lời cần nó để tô
    // sáng, thẻ lời cần nó để mở đúng câu vừa nghe.
    val dongDangHat by remember(lyrics) {
        derivedStateOf { activeLineIndex(lyrics.lines, position.value, lyrics.offset) }
    }

    Box(Modifier.fillMaxSize()) {
        // MỘT nền liền mạch phía sau cả trang.
        //
        // Trước đây ba khối — dải ngữ cảnh, mặt lời, hàng điều khiển — đều nằm
        // trên nền phẳng, nên mắt đọc ra ba vùng rời chứ không phải một trang.
        // Zing và NCT đều giải bằng cùng một cách: lấy chính bìa album làm nền
        // phủ kín, rồi phủ màu lên gần hết. Cái còn lại không phải một tấm ảnh
        // mà là một vệt màu, và vệt ấy chạy suốt từ tiêu đề xuống tới nút bấm —
        // không còn cạnh nào để mắt bám vào mà chia trang ra.
        NenBia(artwork ?: now.artwork, Modifier.fillMaxSize())

    Column(Modifier.fillMaxSize()) {
        DaiNguCanh(
            now = now,
            bia = artwork ?: now.artwork,
            // CHỈ hiện nguồn khi nhạc đến từ app KHÁC. AURA tự phát thì người
            // dùng vừa tự bấm bài đó trong chính app này — nói lại "Trong máy"
            // là kể một điều họ vừa làm.
            //
            // Gọi `tenApp` vô điều kiện ở trên rồi mới chọn ở đây: nó có
            // `remember` bên trong, mà nhớ trong một nhánh `if` thì đổi nhánh
            // là mất chỗ nhớ.
            nguon = if (Lyra.laLyraPhat()) null else tenAppNguon,
            accent = accent,
            hangDoiDuoc = hangDoiRieng,
            dangXemHangDoi = xemHangDoi,
            onHangDoi = { xemHangDoi = !xemHangDoi },
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
            MatLoi(
                // Bìa là MỤC ĐẦU của chính danh sách lời, không phải
                // một mặt riêng phải bấm mới sang. Hai mặt Bìa và Lời
                // giờ là một: bìa ở trên, lời chảy xuống dưới, cuộn
                // xuống đọc thì bìa trôi lên vì nó đã làm xong việc.
                khoiBia = {
                    KhoiBia(
                        now = now,
                        bia = artwork ?: now.artwork,
                        accent = accent,
                        chiXem = chiXem,
                        nguon = nguonHangDoi,
                        queue = queue,
                        queueIndex = queueIndex,
                        onToanManHinh = onToanManHinh,
                        toanManHinh = toanManHinh
                    )
                },
                khoaBai = now.title + " " + now.artist,
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
                onCanGio = onCanGio,
                onChonBanLoi = onChonBanLoi
            )

            // Chữ MỜ DẦN vào nền trước khi tới hàng nút, thay cho một nét kẻ.
            //
            // Vẫn giải đúng bài toán mà nét kẻ sinh ra để giải: không có gì
            // ngăn thì dòng lời cuối chạy thẳng vào thanh tua và mắt không biết
            // chữ hết ở đâu. Nhưng một nét kẻ cắt màn hình thành hai mảnh, còn
            // vệt mờ thì nói cùng một điều mà không dựng thêm cạnh nào — cả
            // trang đọc liền một mạch từ tên bài xuống tới nút bấm.
            //
            // Chỉ có nền nên KHÔNG nuốt chạm: chạm vào dòng lời nằm dưới vệt
            // này vẫn tới nơi. Xem `chanChamXuyen` để biết vì sao.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, mau.nen)))
            )

            // Hàng đợi đè lên vùng lời, KHÔNG đè lên thanh tua và hàng nút:
            // đang xem danh sách bài kế mà vẫn tạm dừng hay tua được là đúng.
            if (xemHangDoi && hangDoiRieng) {
                TamHangDoi(
                    queue = queue,
                    queueIndex = queueIndex,
                    onSkipInQueue = onSkipInQueue,
                    onRemoveFromQueue = onRemoveFromQueue,
                    onDoiCho = { tu, den -> Lyra.doiChoTrongHangDoi(ngucanh, tu, den) },
                    onLuuHangDoi = { naming = true },
                    onDong = { xemHangDoi = false }
                )
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
        //
        // Chỗ này TỪNG có một nét kẻ tách phần đọc khỏi phần bấm. Việc ấy giờ
        // do vệt mờ ở đáy vùng lời lo — cùng công dụng, không dựng thêm cạnh.
        // Danh tính nằm NGOÀI khối điều khiển, không nằm trong.
        //
        // Bản Play tìm được nhạc ở Zing/NCT nhưng không phát, nên khối điều
        // khiển bị giấu đi (`chiXem`). Nhét tên bài vào trong đó thì đúng bản
        // ấy lại mất luôn tên bài — mà đó là bản mà một dòng chữ còn quan trọng
        // hơn, vì nó chẳng còn gì khác để nói đang xem lời của bài nào.
        //
        // CHỈ hiện nguồn khi nhạc đến từ app KHÁC. AURA tự phát thì người dùng
        // vừa tự bấm bài đó trong chính app này — nói lại "Trong máy" là kể một
        // điều họ vừa làm.
        KhoiDanhTinh(now)

        if (!chiXem) Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 4.dp, bottom = 6.dp)) {
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
 * Độ đục của nền các nút tròn trên dải ngữ cảnh khi nút CHƯA bật.
 *
 * Nền chìm đặc biến mỗi nút thành một viên rời nổi trên nền — đúng thứ vừa bỏ
 * công xoá đi ở chỗ khác. Để nó trong bớt thì vệt màu phía sau ăn xuyên qua, và
 * hàng nút đọc ra như một phần của dải chứ không phải mấy vật thể đặt lên.
 *
 * Nút ĐANG bật thì vẫn tô đặc màu nhấn: đó là chỗ duy nhất trên dải cần hét
 * lên, và làm nó mờ đi thì không còn gì phân biệt bật với tắt.
 */
private const val DUC_CHIP = 0.40f

/**
 * Nền chung của cả trang Bài: chính bìa album, làm nhoè và phủ màu lên.
 *
 * LÀM NHOÈ BẰNG CÁCH THU ẢNH VỀ 40×40 RỒI KÉO GIÃN, không dùng `Modifier.blur`:
 * hàm kia chỉ có từ Android 12, mà AURA chạy từ Android 8. Đây cũng đúng cách
 * thẻ lời đang làm cho mẫu "Bìa mờ", nên hai chỗ nhìn ra cùng một chất.
 *
 * Phủ màu lên tới 86–100%: thứ cần ở đây là một VỆT MÀU liền mạch phía sau, chứ
 * không phải một tấm ảnh đòi người ta nhìn. Để ảnh rõ hơn thì chữ khó đọc, mà
 * chữ mới là thứ trang này bày ra.
 *
 * Đậm dần xuống đáy để hàng nút luôn nằm trên nền sạch, kể cả khi bìa sáng.
 *
 * Không có bìa thì chỉ còn nền phẳng — y như trước, không hỏng gì.
 */
@Composable
private fun NenBia(bia: android.graphics.Bitmap?, modifier: Modifier = Modifier) {
    val nen = mau.nen
    val nhoe = remember(bia) {
        bia?.let { runCatching { it.scale(40, 40, filter = true) }.getOrNull() }
    }

    Box(modifier.background(nen)) {
        if (nhoe != null) {
            Image(
                bitmap = nhoe.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                nen.copy(alpha = 0.86f),
                                nen.copy(alpha = 0.93f),
                                nen
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Tên hiển thị của app đang phát, hoặc `null` khi hỏi không ra.
 *
 * Từ Android 11, hỏi tên của một gói khác sẽ ném lỗi nếu app không khai báo
 * nhìn thấy gói đó. Manifest có một khối `<queries>` mở đúng nhóm app có dịch
 * vụ duyệt nhạc — đủ cho Spotify, YouTube Music, Zing, NhacCuaTui.
 *
 * Hỏi không ra thì trả `null` chứ KHÔNG trả tên gói: dải ngữ cảnh bỏ hàng nguồn
 * đi còn hơn bày ra `com.google.android.apps.youtube.music`.
 */
@Composable
private fun tenApp(goi: String): String? {
    if (goi.isBlank()) return null
    val ngucanh = LocalContext.current
    return remember(goi) {
        runCatching {
            val pm = ngucanh.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(goi, 0)).toString()
        }.getOrNull()
    }
}

/**
 * Tên bài, nằm NGAY TRÊN thanh tua.
 *
 * Chỉ mỗi tên bài. Nguồn và ca sĩ ở lại dải trên — đưa cả ba xuống đây thì khối
 * đáy phình lên ba dòng chữ chồng thanh tua và hàng nút, còn dải trên trống một
 * khoảng chỉ để chứa mấy cái nút: chiếm chỗ mà không bày ra được gì.
 *
 * Tên bài thì đáng ở đây, vì nó và thanh tua nói về cùng một thứ và mắt đọc
 * xuôi một mạch từ tên xuống vị trí đang phát rồi tới nút bấm.
 *
 * Đây là chỗ NCT, Zing và Spotify đều đặt, và lần này lý do của họ áp được cho
 * AURA: ô bìa nhỏ vẫn nằm ở góc trái trên, nên trang Lời không mất chỗ nhận
 * biết đang ở bài nào — thứ mà dải ngữ cảnh cũ sinh ra để lo.
 */
@Composable
private fun KhoiDanhTinh(now: NowPlaying) {
    ChuChay(
        // Bỏ đoạn lặp tên ca sĩ ở đầu tên bài — xem `tenBaiDeHien`. Dải ngữ
        // cảnh ngay trên đã có tên ca sĩ rồi, nên để nguyên là in nó hai lần
        // trong hai dòng liền nhau.
        chu = tenBaiDeHien(now.artist, now.title),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 26.dp, end = 26.dp, bottom = 4.dp)
    )
}

/**
 * Chữ trôi ngang LIÊN TỤC, chạy cả khi chữ ngắn hơn khung.
 *
 * `basicMarquee` của Compose không làm được việc này: nó chỉ chạy khi chữ tràn
 * khung, còn tên bài vừa chỗ thì đứng im. Đúng về mặt thiết kế — Zing, NCT,
 * Spotify đều thế — nhưng không phải thứ được yêu cầu ở đây.
 *
 * Cách chạy: vẽ chữ HAI LẦN cách nhau một khoảng hở, rồi dịch cả hàng sang
 * trái đúng bằng "một bản + khoảng hở" và lặp lại. Đúng lúc bản đầu trôi khuất
 * thì bản sau đã tới đúng vị trí bản đầu vừa rời — nên vòng lặp không có mối
 * nối, mắt không thấy chỗ nhảy.
 *
 * Thời lượng tính theo QUÃNG ĐƯỜNG chứ không đặt cứng: tên dài và tên ngắn
 * phải trôi cùng một tốc độ, không thì tên ngắn vụt qua còn tên dài thì lết.
 */
@Composable
private fun ChuChay(chu: String, modifier: Modifier = Modifier) {
    val doDay = LocalDensity.current
    var rongChu by remember(chu) { mutableIntStateOf(0) }
    var rongKhung by remember { mutableIntStateOf(0) }
    val lech = remember(chu) { Animatable(0f) }

    // KHOẢNG HỞ BẰNG ĐÚNG BỀ NGANG KHUNG, không phải một con số cố định.
    //
    // Đây là chỗ bản đầu sai. Hở cố định 64dp thì với tên bài NGẮN, cả hai bản
    // chữ cùng lọt vào khung một lúc — người dùng thấy đuôi bản một, khoảng hở,
    // rồi chữ đầu của bản hai, và đọc ra như một chữ bị văng xa khỏi phần còn
    // lại. Tên dài thì không lộ, vì bản hai luôn nằm ngoài màn hình.
    //
    // Hở bằng bề ngang khung thì bản sau chỉ bắt đầu vào khung đúng lúc bản
    // trước vừa ra hẳn. Không bao giờ thấy hai bản cùng lúc, dù chữ ngắn tới
    // đâu — đổi lại có một quãng trống trôi qua, đúng như bảng chạy chữ thật.
    val hoPx = maxOf(rongKhung, with(doDay) { HO_TOI_THIEU.roundToPx() })
    val hoDp = with(doDay) { hoPx.toDp() }

    LaunchedEffect(chu, rongChu, hoPx) {
        if (rongChu <= 0) return@LaunchedEffect
        val quang = (rongChu + hoPx).toFloat()
        lech.snapTo(0f)
        lech.animateTo(
            targetValue = -quang,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (quang / TOC_DO_CHU_CHAY * 1000).toInt().coerceAtLeast(1),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { rongKhung = it.width }
    ) {
        Row(
            Modifier
                // CHO HÀNG RỘNG VƯỢT KHUNG. Không có dòng này thì `Row` bị bó
                // theo bề ngang của `Box`, và mọi thứ không lọt sẽ bị nén còn 0
                // điểm ảnh — bản chữ thứ hai biến mất, nên chạy hết một vòng là
                // màn hình trống trơn.
                //
                // Đây cũng chính là gốc của lỗi "chữ đầu văng xa" trước đó: hồi
                // hở còn 64dp thì bản hai chưa mất hẳn, chỉ bị cắt còn vài chữ,
                // và mấy chữ sót ấy trông như một mẩu rơi ra khỏi câu.
                //
                // `Box` bọc ngoài đã `clipToBounds`, nên phần thò ra vẫn bị cắt
                // gọn ở rìa chứ không tràn sang chỗ khác.
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .offset { IntOffset(lech.value.roundToInt(), 0) }
        ) {
            MotBanChu(chu) { rongChu = it }
            Spacer(Modifier.width(hoDp))
            // Bản thứ hai chỉ để vá chỗ trống lúc bản đầu trôi ra ngoài.
            MotBanChu(chu) {}
        }
    }
}

@Composable
private fun MotBanChu(chu: String, onRong: (Int) -> Unit) {
    Text(
        chu,
        color = mau.chu,
        fontFamily = boChu.loi,
        fontSize = 21.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { onRong(it.size.width) }
    )
}

/** Sàn cho khoảng hở, dùng khi chưa đo được bề ngang khung. */
private val HO_TOI_THIEU = 64.dp

/** Điểm ảnh mỗi giây. Chữ để ĐỌC, không phải bảng chạy chữ ngoài đường. */
private const val TOC_DO_CHU_CHAY = 45f
/**
 * Dải đầu trang: ô bìa nhỏ · nguồn và ca sĩ · các nút chức năng.
 *
 * TỪNG mang cả tên bài nữa, và ba dòng chữ là lý do nó nặng. Tên bài giờ xuống
 * cạnh thanh tua; ở đây còn lại hai dòng ngắn — bài này ở đâu ra, ai hát.
 *
 * Không đưa nốt hai dòng ấy xuống theo: làm thế thì khối đáy phình lên còn dải
 * này trống một khoảng chỉ để chứa mấy cái nút, tức chiếm chỗ mà không bày ra
 * được gì. Chia đôi thì mỗi bên vừa một việc.
 *
 * GIỮ Ô BÌA NHỎ. Nó là thứ rẻ nhất để mắt liếc một cái biết đang bài nào mà
 * không tốn dòng chữ nào — nhờ nó mà việc đưa tên bài xuống đáy không làm trang
 * Lời mất chỗ nhận biết.
 */
@Composable
private fun DaiNguCanh(
    now: NowPlaying,
    bia: android.graphics.Bitmap?,
    /** Bài này đến từ đâu. `null` khi AURA tự phát — xem chỗ gọi. */
    nguon: String?,
    accent: Color,
    /** Có hàng đợi của riêng AURA để mở ra xem hay không. */
    hangDoiDuoc: Boolean,
    dangXemHangDoi: Boolean,
    onHangDoi: () -> Unit,
    chiaSeDuoc: Boolean,
    onChiaSe: () -> Unit,
    luyenTapDuoc: Boolean,
    dangLuyenTap: Boolean,
    onLuyenTap: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            // Thấp hơn bản cũ: hàng này giờ chỉ còn nút, không còn ba dòng chữ
            // phải chừa chỗ. Đệm dôi ra trả lại cho vùng lời.
            .padding(start = 20.dp, end = 22.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                // Bo tròn hơn và BỎ VIỀN: ô bìa nhỏ trước đây là một hình vuông
                // có nét bao, tức thêm một cạnh nữa trên màn hình vốn đang cố
                // liền mạch. Không viền thì ảnh bìa tự nó là khối, còn bài chưa
                // có bìa thì nền chìm đã đủ nói đây là một ô.
                .clip(RoundedCornerShape(8.dp))
                .background(mau.nenChim),
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
        // Nguồn và ca sĩ ở LẠI trên này, chỉ tên bài xuống dưới.
        //
        // Cả ba cùng xuống thì khối đáy phình lên ba dòng chữ chồng thanh tua
        // và hàng nút, mà dải trên lại trống một khoảng chỉ để chứa mấy cái
        // nút — chiếm chỗ mà không bày ra được gì. Chia đôi thì mỗi bên vừa
        // một việc: trên nói bài này ở đâu ra và ai hát, dưới nói nó tên gì
        // ngay cạnh thanh tua đang chạy.
        Column(Modifier.weight(1f)) {
            if (!nguon.isNullOrBlank()) {
                Text(
                    nguon,
                    color = mau.chuRatMo,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (now.artist.isNotEmpty()) {
                Text(
                    now.artist,
                    color = mau.chuMo,
                    fontSize = 13.sp,
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
                    .background(if (dangLuyenTap) accent else mau.nenChim.copy(alpha = DUC_CHIP))
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
                    .background(mau.nenChim.copy(alpha = DUC_CHIP))
                    .clickable(onClick = onChiaSe),
                contentAlignment = Alignment.Center
            ) {
                Text("↗", color = mau.chuMo, fontSize = 17.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        // Cặp chip "Lời / Bìa" đã BỎ. Bìa không còn là một mặt phải bấm mới
        // sang — nó nằm ngay đầu danh sách lời, cuộn lên là thấy. Chỗ đó giờ
        // để cho hàng đợi, thứ thật sự là một trang khác.
        if (hangDoiDuoc) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (dangXemHangDoi) accent else mau.nenChim.copy(alpha = DUC_CHIP))
                    .clickable(onClick = onHangDoi),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "☰",
                    color = if (dangXemHangDoi) Color.White else mau.chuMo,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Dải luyện tập: lặp một đoạn, và đổi tốc độ.
 *
 * Chọn đoạn theo CÂU chứ không kéo hai mốc trên thanh thời gian — đó là chỗ
 * AURA làm được mà một bộ lặp A–B thường không: nó biết câu nào bắt đầu lúc nào.
 *
 * Tốc độ chỉ hiện khi AURA là bên đang phát. `MediaController` của hệ thống
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

/**
 * Khối bìa: ảnh bìa (hoặc khung video), dòng trạng thái, và nguồn đang phát.
 *
 * Đây là MỤC ĐẦU của danh sách lời, không phải một mặt riêng. Cuộn xuống đọc
 * thì nó trôi lên vì nó đã làm xong việc của mình; muốn nhìn lại bìa thì cuộn
 * ngược lên, không phải bấm để đổi trang.
 */
@Composable
private fun KhoiBia(
    now: NowPlaying,
    bia: android.graphics.Bitmap?,
    accent: Color,
    chiXem: Boolean,
    nguon: String?,
    queue: List<Track>,
    queueIndex: Int,
    onToanManHinh: () -> Unit,
    toanManHinh: Boolean
) {
    // Bai dang phat la video thi chinh o bia tro thanh man hinh. Mot trang rieng
    // cho video se cat doi app lam hai nua ma khong duoc gi: cho de anh bia von
    // da la mot o hinh vuong dat giua trang.
    val baiNay = queue.getOrNull(queueIndex)
    val laVideo = baiNay?.kind == MediaKind.VIDEO && !toanManHinh

    Stage(
        accent = accent,
        kind = if (laVideo) MediaKind.VIDEO else MediaKind.AUDIO,
        tiLe = if (laVideo) baiNay?.tiLe else null
    ) {
        if (laVideo) {
            ManHinhVideo(dangPhat = now.isPlaying)
            // Nút mở toàn màn hình, góc dưới phải của chính ô hình. Không đặt ở
            // dải nút chung bên dưới: nó chỉ có nghĩa khi đang có hình, mà ô
            // hình thì là chỗ mắt đang nhìn.
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
}

/**
 * Tấm hàng đợi: những bài SẼ phát tiếp, kéo thả sắp lại được.
 *
 * KHÔNG còn vẽ ảnh bìa. Từ lúc bìa nằm ở đầu mặt Lời, vẽ lại nó ở đây là bày
 * cùng một thứ hai lần, và nó đẩy hàng đợi — thứ người ta mở tấm này ra để
 * xem — xuống dưới mép màn hình.
 *
 * Vẫn là một tấm ĐÈ LÊN chứ không nhét vào cuối danh sách lời: hàng đợi cần cử
 * chỉ nhấn-giữ-rồi-kéo của riêng nó, mà lồng một danh sách cuộn dọc vào trong
 * một danh sách cuộn dọc khác thì hai bên giành nhau cùng một cú vuốt.
 */
@Composable
private fun TamHangDoi(
    queue: List<Track>,
    queueIndex: Int,
    onSkipInQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onDoiCho: (Int, Int) -> Unit,
    onLuuHangDoi: () -> Unit,
    onDong: () -> Unit
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

    // Nền ĐẶC, và nuốt mọi cú chạm rơi ra ngoài các ô bài. Tấm này che mặt Lời
    // đang nằm dưới; để chạm lọt xuống thì người dùng bấm trúng một câu lời họ
    // không nhìn thấy. Xem `chanChamXuyen` để biết vì sao Compose không tự chặn.
    Box(
        Modifier
            .fillMaxSize()
            .chanChamXuyen()
            .background(mau.nen)
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 16.dp, bottom = 24.dp)
        ) {
            item {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(mau.nenChim)
                                .clickable(onClick = onDong),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", color = mau.chuMo, fontSize = 19.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

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
    // Bìa vẽ như MỤC ĐẦU của danh sách lời chứ không phải một mặt riêng.
    // Truyền vào chứ không dựng tại chỗ: `MatLoi` không cần biết gì về ảnh
    // bìa, hàng đợi hay toàn màn hình, và nó vẫn không biết.
    khoiBia: @Composable () -> Unit,
    /**
     * Bài nào — dùng làm mốc để QUÊN việc người dùng đã thu gọn dải lời nhắc.
     *
     * Không lấy `lyrics` làm mốc: cùng một bài, `lyrics` còn đổi vài lần nữa
     * (dịch xong, sửa lời, căn lệch), và mỗi lần đổi lại bung dải ra thì thu
     * gọn thành một nút không bấm được lâu.
     */
    khoaBai: String,
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
    onCanGio: () -> Unit,
    onChonBanLoi: () -> Unit
) {
    // Mốc đang ngờ thì KHÔNG tô sáng và KHÔNG tự cuộn. Tô sáng nhầm một dòng
    // suốt cả bài còn tệ hơn là không tô gì.
    val trustTiming = lyrics.synced && !lyrics.timingSuspect

    // Trang thai cua dai "ghi ra tep": cau bao gan nhat, va co dang cho xac
    // nhan ghi de hay khong. Doi bai thi quen het - mot cau bao ve bai truoc
    // nam lai o bai sau la sai.
    var baoGhi by remember(lyrics.from, lyrics.lines.size) { mutableStateOf<String?>(null) }
    var choDeLen by remember(lyrics.from, lyrics.lines.size) { mutableStateOf(false) }

    // Khi Android chặn không cho ghi cạnh tệp nhạc, giữ lại nội dung ở đây để
    // còn lưu sang chỗ khác qua bộ chọn tệp. Rỗng nghĩa là chưa bị chặn.
    var choLuuKhac by remember(lyrics.from, lyrics.lines.size) {
        mutableStateOf<Pair<String, String>?>(null)
    }
    val nguCanhLuu = LocalContext.current
    val luuChoKhac = rememberLauncherForActivityResult(
        // KHÔNG khai "text/plain": bộ chọn tệp sẽ tự thêm đuôi .txt vào tên, ra
        // một tệp "bài hát.lrc.txt" mà không trình phát nào nhận là lời bài hát.
        // Đo trên máy thật một lần rồi mới thấy.
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val than = choLuuKhac?.second
        if (uri == null || than == null) return@rememberLauncherForActivityResult
        baoGhi = try {
            nguCanhLuu.contentResolver.openOutputStream(uri)?.use { it.write(than.toByteArray()) }
                ?: error("không mở được tệp")
            choLuuKhac = null
            "Đã lưu lời ra tệp bạn chọn."
        } catch (e: Exception) {
            "Không lưu được: " + (e.message ?: "lỗi không rõ") + "."
        }
    }
    // Dải lời nhắc BUNG RA MỖI BÀI, rồi thu gọn được.
    //
    // Mấy lời nhắc này đều đáng nói MỘT LẦN cho mỗi bài — "lời của bản thu
    // khác", "ghi ra tệp", "tải gói dịch" — nhưng chúng nằm lì trên đầu trang
    // suốt cả bài và ăn mất chỗ của chính thứ người ta mở app ra để đọc.
    //
    // Nên: hiện lúc mới sang bài (`remember(khoaBai)` trả về `true` lại), thu
    // gọn được bằng một nút, và KHÔNG nhớ việc thu gọn sang bài sau. Nhớ luôn
    // thì bài sau có tệp .lrc chưa ghi hay có gói dịch cần tải cũng không ai
    // biết — lời nhắc bị tắt vĩnh viễn là lời nhắc vô dụng.
    var hienNhac by remember(khoaBai) { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val translated = (translation as? TranslationState.Done)?.lines ?: emptyList()

    LaunchedEffect(active, trustTiming) {
        if (trustTiming && active >= 0 && lyrics.lines.isNotEmpty()) {
            // CỘNG MỘT vì mục 0 của danh sách là khối bìa, không phải câu lời.
            // Dòng thứ `active` nằm ở mục `active + 1`. Quên số này thì lời tự
            // cuộn lệch đúng một câu suốt cả bài — sai lặng lẽ, không báo gì.
            listState.animateScrollToItem(active.coerceAtLeast(0) + 1, scrollOffset = -260)
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
        // Không có lời thì VẪN CÓ BÌA. Từ lúc bìa thôi là một mặt riêng, nhánh
        // này là chỗ duy nhất còn lại có thể vô tình nuốt mất nó — và một bài
        // không tìm ra lời mà cũng mất luôn ảnh bìa thì trang bài trắng trơn.
        Column(Modifier.fillMaxSize()) {
            // Đệm phải khai LẠI ở đây. Nhánh kia lấy nó từ `contentPadding` của
            // `LazyColumn`, mà nhánh này không có `LazyColumn` nào.
            Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 30.dp)) { khoiBia() }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
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
        }
        return
    }

    // ĐIỀU KIỆN của từng lời nhắc, viết MỘT LẦN ở đây.
    //
    // Trước đây mỗi lời nhắc tự mang cái `if` của nó ngay tại chỗ vẽ. Giờ cần
    // đếm xem có bao nhiêu cái để nút thu gọn nói được "3 lời nhắc", mà chép
    // lại chùm điều kiện lần thứ hai để đếm thì sớm muộn hai bản sẽ lệch nhau
    // và con số nói dối.
    val nhacChuaCanGio = !lyrics.synced && lyrics.lines.isNotEmpty()
    val nhacGopLrclib = lyrics.from == "tự nhập" || gop != null
    // Mời chọn bản khác khi lời đến TỪ MẠNG, và cả khi họ ĐÃ chọn một bản.
    //
    // Vế thứ hai không phải cho đẹp: chọn xong thì lối vào biến mất, và lúc đó
    // không còn đường nào để đổi bản khác hay bỏ chọn — người dùng bị khoá vào
    // đúng cái họ vừa chọn, kể cả khi chọn nhầm.
    //
    // Lời tự nhập và lời nằm cạnh tệp nhạc thì KHÔNG mời: cả hai đều là thứ
    // người dùng tự đặt bằng tay ở chỗ khác, và mời họ thay bằng một bản tải
    // về là đề nghị vứt công của chính họ.
    val nhacChonBan = lyrics.lines.isNotEmpty() &&
        lyrics.from != "tự nhập" && !LrcCanhTep.laTepCanh(lyrics.from)
    // Chỉ mời ghi tệp khi đang phát nhạc TRONG MÁY: nhạc từ Zing hay app khác
    // thì không có tệp nào trên đĩa để mà ghi cạnh. Và chỉ mời khi lời KHÔNG
    // PHẢI vừa đọc lên từ chính tệp đó — ghi lại đúng cái mình vừa đọc ra là
    // một nút bấm xong không đổi gì.
    val nhacGhiTep = Lyra.laNhacTrongMay() && lyrics.lines.isNotEmpty() &&
        !LrcCanhTep.laTepCanh(lyrics.from)
    val nhacDich = translation is TranslationState.NeedsModel ||
        translation is TranslationState.Failed ||
        translation == TranslationState.Working
    val soNhac = 1 + listOf(
        nhacChuaCanGio, nhacChonBan, nhacGopLrclib, nhacGhiTep, baoKhongTua, nhacDich
    )
        .count { it }

    Column(Modifier.fillMaxSize()) {
        if (hienNhac) {
            // Một dải báo duy nhất, và nó LUÔN có lối vào chỗ sửa lời.
            Notice(
                accent = accent,
                text = when {
                    // ĐẶT TRÊN `timingSuspect`: hai cờ có thể cùng bật, mà "có
                    // khi đây không phải bài của bạn" là tin nặng hơn "mốc có
                    // thể lệch". Nói cái nhẹ rồi nuốt cái nặng là nói giảm đi.
                    lyrics.khacCaSi ->
                        "Lời này khớp theo tên bài chứ không khớp tên ca sĩ — có " +
                            "thể là của bài khác. Bấm “Chọn bản” bên dưới để đổi."
                    lyrics.timingSuspect ->
                        "Lời của bản thu khác nên mốc có thể lệch. Nhấn giữ câu đang " +
                            "hát để căn lại."
                    lyrics.offset != 0L ->
                        "Đã căn lệch " + offsetLabel(lyrics.offset) + ". Bấm để bỏ."
                    lyrics.from == "tự nhập" -> "Lời bạn tự nhập."
                    // Nhánh riêng chứ không để rơi xuống "Lời từ ${from}." bên
                    // dưới: câu đó ghép ra "Lời từ bạn chọn." — đọc lên như thể
                    // "bạn chọn" là tên một cái kho lời nào đó.
                    lyrics.from == "bạn chọn" -> "Bản lời bạn đã chọn cho bài này."
                    else -> "Lời từ " + lyrics.from + "."
                },
                onClick = if (lyrics.offset != 0L) onClearOffset else null,
                action = if (lyrics.from == "tự nhập") "Sửa lời" else "Tự nhập",
                onAction = onEditLyrics
            )

            // Lời chữ trơn thì mời căn giờ.
            //
            // Chưa căn thì AURA chỉ hiện được một khối chữ: không tô sáng câu đang
            // hát, không khung lời nổi chạy theo, không lặp A–B, không thẻ lời. Gần
            // hết những gì app làm đều đứng trên chỗ có mốc thời gian.
            // Đường SỬA khi app đưa nhầm lời. Bản 0.3.25 mới chỉ nói ra là có
            // thể nhầm rồi để đấy — người dùng chỉ còn cách tự gõ lại cả bài.
            // Dữ liệu để sửa đã nằm sẵn trong tay: kho lời trả về cả danh sách
            // trong một lần gọi, app chọn một bản rồi vứt phần còn lại.
            if (nhacChonBan) {
                Notice(
                    accent = accent,
                    text = when {
                        lyrics.from == "bạn chọn" ->
                            "Chọn nhầm? Mở lại danh sách để đổi bản khác hoặc bỏ chọn."
                        lyrics.khacCaSi ->
                            "Không đúng bài? Xem các bản lời khác rồi tự chọn."
                        else ->
                            "Lời không khớp bản thu bạn đang nghe? Chọn bản khác."
                    },
                    action = if (lyrics.from == "bạn chọn") "Đổi bản" else "Chọn bản",
                    onAction = onChonBanLoi
                )
            }

            if (nhacChuaCanGio) {
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
            if (nhacGopLrclib) {
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

            // Ghi lời ra tệp .lrc nằm cạnh tệp nhạc. Xem `nhacGhiTep` ở trên để
            // biết vì sao lời nhắc này không phải lúc nào cũng có nghĩa.
            if (nhacGhiTep) {
                val nhac = LocalContext.current
                Notice(
                    accent = accent,
                    text = baoGhi ?: "Ghi lời này ra tệp .lrc nằm cạnh bài nhạc — " +
                        "trình phát khác cũng đọc được, và gỡ app đi vẫn còn.",
                    action = when {
                        choLuuKhac != null -> "Chọn chỗ lưu"
                        choDeLen -> "Ghi đè"
                        else -> "Ghi ra tệp"
                    },
                    onAction = {
                        val cho = choLuuKhac
                        if (cho != null) {
                            luuChoKhac.launch(cho.first)
                            return@Notice
                        }
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
                            is LrcCanhTep.KetQuaGhi.BiChan -> {
                                // Không bỏ người dùng cụt đường. Đây là luật của hệ
                                // thống chứ không phải thiếu quyền nào xin được, nên
                                // mời họ chọn chỗ khác — việc đó thì luôn làm được.
                                //
                                // ĐO ĐƯỢC trên Pixel 6 Pro / Android 17 — chỉ BỐN
                                // thư mục nhận tệp .lrc:
                                //
                                //   được : Music  Movies  Download  Documents
                                //   chặn : DCIM  Pictures  Recordings  Audiobooks
                                //          Podcasts  Notifications  Alarms
                                //
                                // Nên câu báo kể ra CHỖ ĐƯỢC PHÉP chứ không đoán
                                // xem chỗ đang hỏng là loại gì. Đã sai hai lần vì
                                // đoán: lần đầu đổ cho thẻ nhớ (máy không có khe
                                // thẻ), lần sau đổ cho thư mục ảnh (trong khi
                                // Recordings cũng chặn mà chẳng dính gì đến ảnh).
                                choDeLen = false
                                choLuuKhac = kq.tenGoiY to kq.noiDung
                                baoGhi = "Android chỉ cho ghi tệp .lrc vào Music, " +
                                    "Movies, Download và Documents — thư mục của bài " +
                                    "này không nằm trong số đó. Chọn chỗ khác để lưu?"
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
        }

        ThanhThuGonNhac(
            soNhac = soNhac,
            dangHien = hienNhac,
            accent = accent,
            onDoi = { hienNhac = !hienNhac }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 30.dp, bottom = 46.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "bia") {
                Column {
                    khoiBia()
                    // Nới rộng hơn khoảng cách giữa hai câu lời: chỗ này là
                    // chuyển từ hình sang chữ, không phải câu này sang câu kế.
                    Spacer(Modifier.height(18.dp))
                }
            }

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

/**
 * Nút thu gọn / bung lại dải lời nhắc.
 *
 * THU GỌN CHỨ KHÔNG TẮT. Thu gọn thì còn lại đúng một dòng chữ nhỏ nói còn mấy
 * lời nhắc đang gấp lại, nên người dùng biết mình vừa giấu cái gì và bấm một
 * cái là thấy lại. Một nút "×" đóng hẳn thì lời nhắc biến mất không dấu vết, và
 * lời nhắc quan trọng nhất — "bài này chưa có tệp .lrc" — chính là cái người ta
 * hay đóng nhầm nhất vì nó dài nhất.
 *
 * Đặt DƯỚI dải chứ không trên: lúc bung, mắt đọc lời nhắc từ trên xuống rồi mới
 * gặp nút; lúc gọn, nút nằm ngay sát chỗ lời bài hát bắt đầu nên nó đọc ra như
 * một cái nắp đậy chứ không phải một mục lạ chen vào đầu trang.
 */
@Composable
private fun ThanhThuGonNhac(
    soNhac: Int,
    dangHien: Boolean,
    accent: Color,
    onDoi: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = if (dangHien) 2.dp else 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    // Lúc gọn thì nó là thứ DUY NHẤT còn lại của cả dải, nên
                    // phải nhìn ra được là một nút. Lúc bung thì nó chỉ là cái
                    // nắp của mấy ô ngay trên, tô đậm nữa là thành ô thứ tư.
                    if (dangHien) Color.Transparent else accent.copy(alpha = 0.22f)
                )
                .clickable(onClick = onDoi)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (dangHien) "Thu gọn" else "$soNhac lời nhắc",
                color = if (dangHien) mau.chuRatMo else mau.chu,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(5.dp))
            Text(
                if (dangHien) "▴" else "▾",
                color = if (dangHien) mau.chuRatMo else mau.chu,
                fontSize = 11.sp
            )
        }
    }
}

/** Chi lay ten tep de cau bao khong dai loang ngoang ca duong dan. */
private fun tenTep(duong: String) = duong.substringAfterLast('/')
