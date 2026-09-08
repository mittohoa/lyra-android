package com.mittohoa.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.LanNghe
import com.mittohoa.lyra.data.Playlist
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.sources.KieuXep
import com.mittohoa.lyra.sources.LocLoai
import com.mittohoa.lyra.sources.MediaKind
import com.mittohoa.lyra.sources.MusicSource
import com.mittohoa.lyra.sources.NguonNgoai
import com.mittohoa.lyra.sources.Track

/**
 * Trang tim bai.
 *
 * Khong co nut "Tim": go xong bam phim tim tren ban phim, hoac ngung go mot
 * chut la no tu tim. Mot cai nut nua o day chi la mot cu cham nua cho mot viec
 * ma may hoan toan doan duoc.
 *
 * Cham vao mot bai = phat tu bai do tro di, ca danh sach ket qua thanh hang doi.
 * Muon them mot bai vao cuoi hang doi ma khong dung bai dang nghe thi bam dau
 * cong o ben phai.
 */
@Composable
fun SearchPane(
    accent: Color,
    query: String,
    results: List<Track>,
    searching: Boolean,
    playingUri: String?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPlay: (Int) -> Unit,
    onEnqueue: (Track) -> Unit,
    library: List<Track>,
    kieuXep: KieuXep,
    locLoai: LocLoai,
    onDoiKieuXep: (KieuXep) -> Unit,
    onDoiLocLoai: (LocLoai) -> Unit,
    canReadLibrary: Boolean,
    /**
     * Nguoi dung da chi cho AURA thu muc nao chua.
     *
     * Tach hai chuyen deu ra mot thu vien rong: CHUA CHI CHO NO CHO NAO (loi
     * thoat la vao Cai dat chon thu muc) va da chi roi nhung trong do khong co
     * bai nao. Gop lam mot thi cau bao luon sai voi mot nua so nguoi doc.
     */
    daChonThuMuc: Boolean,
    onAskLibrary: () -> Unit,
    onPlayFromLibrary: (Int) -> Unit,
    playlists: List<Playlist>,
    openedPlaylist: Playlist?,
    onOpenPlaylist: (String?) -> Unit,
    onPlayPlaylistAt: (Int) -> Unit,
    onRemoveFromPlaylist: (Int) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    downloads: Map<String, Lyra.Downloading>,
    onDownload: (Track) -> Unit,
    onXemLoi: (Track) -> Unit,
    lichSu: List<LanNghe>,
    /** Bấm vào một dòng lịch sử: phát lại, hoặc đi tìm nếu bài của app khác. */
    onChonLichSu: (LanNghe) -> Unit,
    onXoaMotLanNghe: (String) -> Unit,
    onXoaLichSu: () -> Unit
) {
    // Màn hình lịch sử PHỦ LÊN trang này, cùng lẽ với màn hình danh sách phát:
    // mở ra là một việc ngắn — xem, bấm, rồi đóng.
    var xemLichSu by remember { mutableStateOf(false) }

    val keyboard = LocalSoftwareKeyboardController.current

    // Dang ky o TANG NGOAI CUNG, khong trong nhanh `when` va khong sau lenh
    // `return` cua man hinh danh sach phat: bo nhan ket qua phai duoc dang ky
    // dung mot lan moi lan dung, khong the nam sau mot dieu kien.
    val chonThuMuc = nhoBoChonThuMuc()

    // Dung o TANG NGOAI CUNG, khong trong nhanh `when`: `remember` gan vao vi
    // tri trong cay dung, ma vi tri do doi theo nhanh nao dang chay.
    val dongThuVien = remember(library, kieuXep) {
        dungDongThuVien(library, gomNhom = kieuXep == KieuXep.ALBUM)
    }

    val trangThaiDs = rememberLazyListState()

    // BÀI ĐẦU TIÊN VÀO LỊCH SỬ THÌ PHẢI KÉO DANH SÁCH VỀ ĐẦU.
    //
    // Đo được trên máy, và nguyên nhân không nằm ở chỗ dễ đoán: dữ liệu tới
    // nơi, `SearchPane` dựng lại, mục "Nghe gần đây" có thật trong danh sách —
    // mà màn hình không đổi gì. `LazyColumn` GIỮ NGUYÊN VỊ TRÍ CUỘN THEO KHOÁ
    // của mục đang đứng đầu khung nhìn. Chèn một mục mới lên trước mục ấy thì
    // nó cuộn xuống đúng một mục để giữ mục cũ ở nguyên chỗ — và mục vừa chèn
    // nằm ngay phía trên mép khung, không ai thấy. Vuốt lên một cái là hiện,
    // nên rất dễ tưởng là dữ liệu chưa tới.
    //
    // Cách này đúng chứ không phải mẹo: hành vi neo ấy sinh ra để danh sách
    // đang đọc dở không nhảy lung tung khi có mục mới. Chỉ kéo về đầu khi người
    // dùng ĐANG Ở ĐẦU — đang cuộn giữa thư viện mà bị giật về đầu trang thì đó
    // mới là mất chỗ đang đọc.
    LaunchedEffect(lichSu.isEmpty()) {
        if (lichSu.isNotEmpty() && trangThaiDs.firstVisibleItemIndex <= 1) {
            trangThaiDs.scrollToItem(0)
        }
    }

    // Nhạc trong máy thì bản nào cũng phát được. Nhạc ở Zing/NCT thì tuỳ bản
    // dựng — xem `NguonNgoai`. Bản Play tìm được nhưng không phát, nên chạm
    // vào kết quả là TRA LỜI chứ không phải phát.
    fun phatDuoc(t: Track) = t.source == MusicSource.LOCAL || NguonNgoai.PHAT_DUOC

    if (xemLichSu) {
        LichSuManHinh(
            lichSu = lichSu,
            accent = accent,
            onChon = {
                xemLichSu = false
                onChonLichSu(it)
            },
            onXoaMot = onXoaMotLanNghe,
            onXoaHet = onXoaLichSu,
            onDong = { xemLichSu = false }
        )
        return
    }

    if (openedPlaylist != null) {
        Column(Modifier.fillMaxSize()) {
            PlaylistScreen(
                playlist = openedPlaylist,
                accent = accent,
                playingUri = playingUri,
                onClose = { onOpenPlaylist(null) },
                onPlayAt = onPlayPlaylistAt,
                onRemoveAt = onRemoveFromPlaylist,
                onRename = onRenamePlaylist,
                onDelete = {
                    onDeletePlaylist()
                    onOpenPlaylist(null)
                }
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
                itemsIndexed(openedPlaylist.tracks, key = { _, t -> t.playbackUri }) { i, track ->
                    TrackRow(
                        track = track,
                        accent = accent,
                        playing = track.playbackUri == playingUri,
                        onPlay = { onPlayPlaylistAt(i) },
                        onEnqueue = { onRemoveFromPlaylist(i) },
                        actionLabel = "×",
                        download = downloads[track.playbackUri],
                        onDownload = { onDownload(track) }
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        SearchField(
            accent = accent,
            coOnline = NguonNgoai.CO_ONLINE,
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = {
                keyboard?.hide()
                onSubmit()
            }
        )

        // Ban Play khong co nguon online nao - xem `NguonNgoai`. Moi cau chu o
        // duoi phai doc bien nay chu khong duoc noi chac mot dieu chi dung cho
        // ban tai thang: mot ban dung khong co Zing ma van moi nguoi dung tim
        // Zing thi khong chi sai, no con lam ho tuong app hong.
        val coOnline = NguonNgoai.CO_ONLINE

        when {
            searching && results.isEmpty() -> Center {
                LyraMark(size = 44.dp, busy = true)
                Spacer(Modifier.height(14.dp))
                Text("Đang tìm…", color = mau.chuMo, fontSize = 14.sp)
            }

            // Xin quyen doc nhac phai dung TRUOC ket luan "khong tim thay".
            //
            // Khong co quyen thi kho de tim la RONG, nen ket qua rong khong noi
            // len dieu gi ve bai hat ca - no chi noi rang AURA chua duoc phep
            // nhin. Ket luan "khong tim thay" o day la mot cau tra loi sai, va
            // no che mat dung cai nut sua duoc chuyen do. Nang nhat o ban Play,
            // noi ma nhac trong may la kho DUY NHAT.
            // Chua chi thu muc nao thi day moi la thu chan, khong phai quyen.
            // Phai dung TRUOC nhanh xin quyen: bam "Cho phep" luc nay khong
            // lam thu vien hien ra mot bai nao, va mot nut nhu the con te hon
            // khong co nut.
            results.isEmpty() && !daChonThuMuc && lichSu.isEmpty() -> Center {
                Ask(
                    title = if (query.isBlank()) "Nhạc trong máy" else "Chưa tìm được",
                    body = "AURA không tự quét máy bạn. Chỉ cho nó thư mục bạn để " +
                        "nhạc — nó chỉ đọc đúng trong đó, và đọc thẳng nên thấy " +
                        "được cả những tệp máy bỏ sót.",
                    // Mo THANG bo chon, khong day nguoi dung sang trang Cai dat.
                    // Cung mot viec ma di duong kia la ba nhip: sang Cai dat, tim
                    // dung muc, roi moi bam. Bo chon cua he thong da la mot man
                    // hinh rieng co nut quay lai - khong can muon them mot cai.
                    action = "Chọn thư mục",
                    accent = accent,
                    onAction = chonThuMuc
                )
            }

            results.isEmpty() && !canReadLibrary && lichSu.isEmpty() -> Center {
                Ask(
                    title = if (query.isBlank()) "Nhạc trong máy" else "Chưa tìm được",
                    body = if (coOnline) {
                        "Cho AURA đọc nhạc đã có sẵn trong máy để phát và tìm cùng " +
                            "với hai nguồn online. AURA chỉ xin quyền đọc NHẠC — không " +
                            "đụng tới ảnh, video hay tài liệu của bạn."
                    } else {
                        "Bản này tìm trong nhạc đã có sẵn trong máy bạn, và AURA chưa " +
                            "được phép đọc. AURA chỉ xin quyền đọc NHẠC — không đụng tới " +
                            "ảnh, video hay tài liệu của bạn."
                    },
                    action = "Cho phép",
                    accent = accent,
                    onAction = onAskLibrary
                )
            }

            results.isEmpty() && query.isNotBlank() && !searching -> Center {
                Text(
                    "Không tìm thấy bài nào",
                    color = mau.chuMo,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        coOnline -> "Thử tên bài không dấu, hoặc thêm tên ca sĩ."
                        library.isEmpty() ->
                            "Bản này chỉ tìm trong nhạc có sẵn trong máy, mà AURA " +
                                "chưa thấy bài nào ở đây."
                        else ->
                            "Bản này chỉ tìm trong ${library.size} bài có trong máy. " +
                                "Thử tên bài không dấu."
                    },
                    color = mau.chuRatMo,
                    fontSize = 14.sp
                )
            }

            results.isEmpty() && library.isEmpty() && playlists.isEmpty() && lichSu.isEmpty() -> Center {
                Text(
                    "Nghe gì hôm nay?",
                    color = mau.chuMo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        coOnline && NguonNgoai.PHAT_DUOC ->
                            "Tìm trong Zing MP3 và NhacCuaTui cùng lúc."
                        coOnline ->
                            "Tìm bài trong Zing MP3 và NhacCuaTui để tra lời. Bản này " +
                                "không phát nhạc từ hai nguồn đó — nhạc thì phát từ máy bạn."
                        else ->
                            "Chép nhạc vào máy rồi tìm ở đây — lời bài hát thì AURA tự tra."
                    },
                    color = mau.chuRatMo,
                    fontSize = 14.sp
                )
            }

            results.isEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = trangThaiDs,
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                // NGHE GẦN ĐÂY đứng TRÊN danh sách phát: nó đổi mỗi ngày, còn
                // danh sách phát thì gần như đứng yên. Thứ thay đổi thường
                // xuyên hơn xứng đáng nằm ở chỗ mắt chạm vào trước.
                //
                // ĐIỀU KIỆN Ở NGOÀI `item`, KHÔNG Ở TRONG, và có `key`.
                //
                // `item { }` luôn dựng rồi để hàm bên trong tự thoát sớm khi
                // rỗng thì `LazyColumn` vẫn đếm một mục cao 0 điểm ảnh, và mọi
                // chỗ neo vị trí cuộn đều tính lệch đi một. Chuyện màn hình
                // không hiện hàng này lúc có bài đầu tiên thì do neo cuộn, xem
                // `trangThaiDs` ở trên — nhưng một mục rỗng vẫn là một mục thừa.
                if (lichSu.isNotEmpty()) item(key = "ganday") {
                    HangNgheGanDay(
                        lichSu = lichSu,
                        accent = accent,
                        onChon = onChonLichSu,
                        onXemHet = { xemLichSu = true }
                    )
                }
                if (playlists.isNotEmpty()) item(key = "dsphat") {
                    PlaylistRow(playlists, accent, onOpen = onOpenPlaylist)
                }

                // LỐI VÀO THƯ VIỆN PHẢI CÒN Ở ĐÂY.
                //
                // Ba nhánh rỗng phía trên giờ nhường chỗ khi đã có lịch sử —
                // nếu không thì người chỉ nghe nhạc ở app khác, không chỉ thư
                // mục nào, sẽ không bao giờ thấy được lịch sử của mình. Nhưng
                // nhường chỗ mà không dựng lại lối vào ở đây thì họ mất luôn
                // đường mở thư viện, và đó là đổi một lỗi lấy một lỗi.
                if (!daChonThuMuc || !canReadLibrary) item(key = "moLib") {
                    Ask(
                        title = "Nhạc trong máy",
                        body = if (!daChonThuMuc) {
                            "AURA không tự quét máy bạn. Chỉ cho nó thư mục bạn để " +
                                "nhạc — nó chỉ đọc đúng trong đó."
                        } else {
                            "AURA chưa được phép đọc nhạc trong máy. Chỉ xin quyền đọc " +
                                "NHẠC — không đụng tới ảnh, video hay tài liệu của bạn."
                        },
                        action = if (!daChonThuMuc) "Chọn thư mục" else "Cho phép",
                        accent = accent,
                        onAction = if (!daChonThuMuc) chonThuMuc else onAskLibrary
                    )
                }
                // Khong co bai nao thi khong co gi de dat tieu de
                if (library.isNotEmpty() || locLoai != LocLoai.TAT_CA) item(key = "demXep") {
                    Text(
                        demThuVien(library),
                        color = mau.chuRatMo,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 6.dp)
                    )
                    // HÀNG CHỌN chỉ hiện khi thư viện đủ lớn để cần tới nó.
                    //
                    // Mười bài thì cuộn một cái là hết, và một hàng nút để sắp
                    // xếp mười bài chỉ là một hàng chữ nữa phải đọc qua. Trần
                    // quét là hai nghìn bài, và ở quãng đó thì thứ tự mới là
                    // thứ quyết định có tìm ra bài hay không.
                    if (library.size >= NGUONG_HANG_CHON || locLoai != LocLoai.TAT_CA) {
                        HangChon(kieuXep, locLoai, accent, onDoiKieuXep, onDoiLocLoai)
                    }
                }
                itemsIndexed(
                    dongThuVien,
                    key = { _, d ->
                        when (d) {
                            is DongThuVien.TieuDe -> "nhom:" + d.nhan
                            is DongThuVien.Bai -> library[d.viTri].playbackUri
                        }
                    }
                ) { _, d ->
                    when (d) {
                        is DongThuVien.TieuDe -> Text(
                            if (d.so > 1) "${d.nhan}  ·  ${d.so} bài" else d.nhan,
                            color = mau.chuMo,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                start = 24.dp, end = 24.dp, top = 14.dp, bottom = 4.dp
                            )
                        )

                        is DongThuVien.Bai -> {
                            val track = library[d.viTri]
                            TrackRow(
                                track = track,
                                accent = accent,
                                playing = track.playbackUri == playingUri,
                                // Chi so GOC trong `library`, khong phai chi so
                                // dong dang ve: hang doi duoc xep tu danh sach
                                // that, ma danh sach ve thi co xen tieu de.
                                onPlay = { onPlayFromLibrary(d.viTri) },
                                onEnqueue = { onEnqueue(track) },
                                download = null,
                                onDownload = {}
                            )
                        }
                    }
                }
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                itemsIndexed(results, key = { _, t -> t.playbackUri }) { i, track ->
                    TrackRow(
                        track = track,
                        accent = accent,
                        playing = track.playbackUri == playingUri,
                        onPlay = {
                            if (phatDuoc(track)) onPlay(i) else onXemLoi(track)
                        },
                        chiXemLoi = !phatDuoc(track),
                        onEnqueue = { onEnqueue(track) },
                        download = downloads[track.playbackUri],
                        onDownload = { onDownload(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    accent: Color,
    coOnline: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    // Ô tìm là một DÒNG KẺ, không phải một viên thuốc.
    //
    // Viên thuốc bo tròn là hình dạng ô tìm của mọi app hôm nay. Trên một trang
    // giấy thì chỗ để viết vào là một dòng có kẻ chân — nó vừa hợp với phần còn
    // lại của trang, vừa nói đúng việc phải làm: viết lên đây.
    Column(
        Modifier
            .padding(start = 20.dp, end = 22.dp, top = 14.dp, bottom = 10.dp)
            .fillMaxWidth()
    ) {
    Box(Modifier.fillMaxWidth().padding(bottom = 9.dp)) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = mau.chu, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() })
        )
        if (query.isEmpty()) {
            Text(
                if (coOnline) "Tên bài, hoặc tên ca sĩ"
                else "Tìm trong nhạc của bạn",
                color = mau.chuRatMo,
                fontSize = 15.sp
            )
        }
    }
        // Nét kẻ chân đậm lên khi có chữ: dòng đang được viết thì đậm hơn dòng
        // để trống, đúng như một tờ giấy có người vừa cầm bút.
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (query.isEmpty()) 1.dp else 2.dp)
                .background(if (query.isEmpty()) mau.vien else accent)
        )
    }
}

/**
 * Mot dong ket qua.
 *
 * Ca dong la vung bam de phat; rieng dau cong o ben phai la them vao hang doi.
 * Hai viec khac han nhau nen phai la hai vung bam khac nhau - gop lai thanh mot
 * cu bam giu la kieu tuong tac khong ai doan ra neu khong duoc chi.
 */
@Composable
private fun TrackRow(
    track: Track,
    accent: Color,
    playing: Boolean,
    onPlay: () -> Unit,
    onEnqueue: () -> Unit,
    actionLabel: String = "+",
    download: Lyra.Downloading? = null,
    onDownload: () -> Unit = {},
    /** Bài này chỉ tra lời được, không phát được ở bản dựng này. */
    chiXemLoi: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(start = 24.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (playing) accent else mau.chu,
                fontSize = 15.5.sp,
                fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    track.artist.ifBlank { "Không rõ ca sĩ" },
                    color = mau.chuMo,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    // Nói ngay trên hàng rằng chạm vào sẽ ra lời chứ không ra
                    // nhạc. Để người dùng chạm rồi mới ngạc nhiên là tệ hơn.
                    buildString {
                        append("  ·  ").append(track.source.label)
                        // Noi thang day la video. Mot cai cham vao thu tuong la
                        // bai hat ma ra man hinh phim la mot bat ngo khong ai
                        // muon - nhat la khi dang cam tai nghe cho o dong nguoi.
                        if (track.kind == MediaKind.VIDEO) append("  ·  video")
                        if (chiXemLoi) append("  ·  xem lời")
                    },
                    color = mau.chuRatMo,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        if (track.durationMs > 0) {
            Text(
                clockLabel(track.durationMs),
                color = mau.chuRatMo,
                fontSize = 13.5.sp
            )
            Spacer(Modifier.width(6.dp))
        }

        // Nhac trong may thi khong co gi de tai. Ban Play thi khong co tinh
        // nang nay, va mot nut bam khong an gi con te hon la khong co nut.
        if (track.source != MusicSource.LOCAL && Lyra.downloadsSupported) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = download !is Lyra.Downloading.Working, onClick = onDownload),
                contentAlignment = Alignment.Center
            ) {
                when (download) {
                    // Dang tai: hien phan tram chu khong hien vong xoay. Vong
                    // xoay chi noi "dang ban"; con so noi con bao lau nua.
                    is Lyra.Downloading.Working -> Text(
                        if (download.percent < 0) "…" else "${download.percent}%",
                        color = accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Lyra.Downloading.Done -> Text("✓", color = accent, fontSize = 16.sp)
                    is Lyra.Downloading.Failed -> Text(
                        "!",
                        color = Color(0xFFE0736B),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    null -> Text("↓", color = mau.chuRatMo, fontSize = 18.sp)
                }
            }
        }

        // Khong xep vao hang doi thu khong phat duoc. Hang doi la danh sach
        // SE PHAT; nhet vao do mot bai ban nay khong phat duoc thi den luot no
        // la mot khoang im lang khong ai giai thich duoc.
        if (!chiXemLoi) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onEnqueue),
                contentAlignment = Alignment.Center
            ) {
                Text(actionLabel, color = mau.chuMo, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { content() }
    }
}

/**
 * Cho o tim kiem tu chay sau khi nguoi dung ngung go.
 *
 * Goi mang theo tung phim la vua ton mang vua tra ve ket qua cua mot chuoi go
 * do dang. Cho hon nua giay thi phan lon truong hop chi con dung mot lan goi -
 * va nguoi dung khong kip nhan ra minh da doi.
 */
@Composable
fun DebouncedSearch(query: String, onSearch: (String) -> Unit) {
    var last by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        // O tim trong = xoa ket qua NGAY, khong cho. Cho la de gop nhieu phim go
        // lien tiep thanh mot lan goi mang; o day khong co lan goi nao ca, ma de
        // ket qua cu nam lai duoi mot o trong thi nguoi dung tuong app treo.
        if (query.isBlank()) {
            last = ""
            onSearch("")
            return@LaunchedEffect
        }
        if (query == last) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        last = query
        onSearch(query)
    }
}

/**
 * Dong dem duoi o tim: noi rieng nhac va video.
 *
 * Goi tat ca la "bai" thi mot thu vien co ba bai hat va sau chuc doan phim
 * quay tay hien ra "63 bai" - con so dung ma y nghia thi sai han, va nguoi
 * dung tuong AURA vua doc nham cai gi do.
 */
/**
 * Mot dong trong danh sach thu vien: hoac mot tieu de nhom, hoac mot bai.
 *
 * Bai mang CHI SO GOC trong `library` chu khong mang ca doi tuong: bam vao mot
 * bai la xep CA thu vien lam hang doi tu bai do tro di, nen ben goi can biet
 * bai nam thu may trong danh sach that - ma danh sach ve thi da xen them tieu
 * de vao giua.
 */
private sealed interface DongThuVien {
    data class TieuDe(val nhan: String, val so: Int) : DongThuVien
    data class Bai(val viTri: Int) : DongThuVien
}

/**
 * Xen tieu de nhom vao giua cac bai.
 *
 * ĐOI HOI `library` DA XEP THEO NHOM - xem `ThuVienNgoai.gop`. Ham nay chi cat
 * ra moi lan nhan nhom doi, chu khong tu gom: gom o day nghia la doi thu tu ve
 * so voi thu tu that, va the la chi so tro sai bai.
 */
private fun dungDongThuVien(library: List<Track>, gomNhom: Boolean): List<DongThuVien> {
    // CHI kieu xep theo album moi chen tieu de nhom. Xep theo ten bai roi chen
    // tieu de theo album thi gan nhu moi bai mot tieu de - danh sach dai gap doi
    // ma khong noi them duoc gi.
    if (!gomNhom) return library.indices.map { DongThuVien.Bai(it) }
    if (library.isEmpty()) return emptyList()
    val ra = ArrayList<DongThuVien>(library.size + 8)
    var truoc: String? = null
    library.forEachIndexed { i, bai ->
        // So khong phan biet hoa thuong, y het khoa xep o `ThuVienNgoai`. Hai
        // cho phai dung CUNG mot luat: xep theo mot luat roi cat theo luat khac
        // thi mot album co the bi cat lam nhieu cum roi rac.
        val khoa = bai.nhom.lowercase()
        if (khoa != truoc) {
            // Dem ngay tai day de tieu de noi duoc nhom co bao nhieu bai. Moi
            // phan tu chi bi dem dung mot lan qua tat ca cac nhom.
            var so = 0
            while (i + so < library.size && library[i + so].nhom.lowercase() == khoa) so++
            // Hien chinh chu cua bai DAU nhom - mot cach ghi that trong tep,
            // khong phai ban viet thuong dung de so sanh.
            ra.add(DongThuVien.TieuDe(bai.nhom.ifBlank { "Không rõ album" }, so))
            truoc = khoa
        }
        ra.add(DongThuVien.Bai(i))
    }
    return ra
}

private fun demThuVien(library: List<Track>): String {
    val video = library.count { it.kind == MediaKind.VIDEO }
    val nhac = library.size - video
    return buildString {
        append("Trong máy")
        if (nhac > 0) append(" · ").append(nhac).append(" bài")
        if (video > 0) append(" · ").append(video).append(" video")
    }
}

/**
 * Dưới ngưỡng này thì không bày hàng chọn ra.
 *
 * Mười lăm bài thì cuộn một cái là hết, và một hàng nút để sắp xếp mười lăm bài
 * chỉ là một hàng chữ nữa phải đọc qua. Trần quét là hai nghìn bài — ở quãng đó
 * thứ tự mới là thứ quyết định có tìm ra bài hay không.
 */
private const val NGUONG_HANG_CHON = 15

/**
 * Hàng chọn cách xếp và bộ lọc, ngay trên danh sách trong máy.
 *
 * CUỘN NGANG được, chứ không gói vào một hộp thoại. Bốn cách xếp cộng ba bộ lọc
 * là bảy viên nút; nhét vào một menu thì mỗi lần đổi là hai lần chạm và một lần
 * chờ hộp thoại, mà đổi cách xếp là việc người ta làm rồi đổi lại ngay khi thấy
 * không hợp.
 *
 * Bộ lọc đứng TRƯỚC cách xếp: lọc thu hẹp danh sách, xếp chỉ đổi thứ tự — và
 * người ta gần như luôn nghĩ "mình đang tìm video" trước khi nghĩ "xếp thế nào".
 */
@Composable
private fun HangChon(
    kieuXep: KieuXep,
    locLoai: LocLoai,
    accent: Color,
    onDoiKieuXep: (KieuXep) -> Unit,
    onDoiLocLoai: (LocLoai) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LocLoai.entries.forEach { loai ->
            VienChon(loai.nhan, loai == locLoai, accent) { onDoiLocLoai(loai) }
            Spacer(Modifier.width(7.dp))
        }

        // Vạch ngăn thay cho một khoảng trắng: hai nhóm này trả lời hai câu hỏi
        // khác nhau, và để chúng chạy liền nhau thì cả bảy viên đọc ra như một
        // dãy lựa chọn loại trừ nhau.
        Box(
            Modifier
                .padding(horizontal = 5.dp)
                .width(1.dp)
                .height(18.dp)
                .background(mau.vien)
        )
        Spacer(Modifier.width(7.dp))

        KieuXep.entries.forEach { kieu ->
            VienChon(kieu.nhan, kieu == kieuXep, accent) { onDoiKieuXep(kieu) }
            Spacer(Modifier.width(7.dp))
        }
    }
}

@Composable
private fun VienChon(nhan: String, dangChon: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (dangChon) accent else mau.nenChim)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp)
    ) {
        Text(
            nhan,
            color = if (dangChon) Color.White else mau.chuMo,
            fontSize = 12.5.sp,
            fontWeight = if (dangChon) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
