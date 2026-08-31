package com.mittohoa.lyra.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.mittohoa.lyra.data.Playlist
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.sources.MusicSource
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
    canReadLibrary: Boolean,
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
    onDownload: (Track) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

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
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = {
                keyboard?.hide()
                onSubmit()
            }
        )

        when {
            searching && results.isEmpty() -> Center {
                LyraMark(size = 44.dp, busy = true)
                Spacer(Modifier.height(14.dp))
                Text("Đang tìm…", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            }

            results.isEmpty() && query.isNotBlank() && !searching -> Center {
                Text(
                    "Không tìm thấy bài nào",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Thử tên bài không dấu, hoặc thêm tên ca sĩ.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp
                )
            }

            // O tim con trong: hien nhac cua chinh nguoi dung. Mot man hinh
            // trong voi mot cau moi go la mot man hinh khong lam gi ca, trong
            // khi thu ho hay mo nhat lai dang nam san trong may.
            results.isEmpty() && !canReadLibrary -> Center {
                Ask(
                    title = "Nhạc trong máy",
                    body = "Cho Lyra đọc nhạc đã có sẵn trong máy để phát và tìm cùng " +
                        "với hai nguồn online. Lyra chỉ xin quyền đọc NHẠC — không đụng " +
                        "tới ảnh, video hay tài liệu của bạn.",
                    action = "Cho phép",
                    accent = accent,
                    onAction = onAskLibrary
                )
            }

            results.isEmpty() && library.isEmpty() && playlists.isEmpty() -> Center {
                Text(
                    "Nghe gì hôm nay?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tìm trong Zing MP3 và NhacCuaTui cùng lúc.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp
                )
            }

            results.isEmpty() -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item { PlaylistRow(playlists, accent, onOpen = onOpenPlaylist) }
                // Khong co bai nao thi khong co gi de dat tieu de
                if (library.isNotEmpty()) item {
                    Text(
                        "Trong máy · ${library.size} bài",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 6.dp)
                    )
                }
                itemsIndexed(library, key = { _, t -> t.playbackUri }) { i, track ->
                    TrackRow(
                        track = track,
                        accent = accent,
                        playing = track.playbackUri == playingUri,
                        onPlay = { onPlayFromLibrary(i) },
                        onEnqueue = { onEnqueue(track) },
                        download = null,
                        onDownload = {}
                    )
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
                        onPlay = { onPlay(i) },
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
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        Modifier
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 20.dp, vertical = 15.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() })
        )
        if (query.isEmpty()) {
            Text(
                "Tên bài, hoặc tên ca sĩ",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 15.sp
            )
        }
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
    onDownload: () -> Unit = {}
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
                color = if (playing) accent else Color.White,
                fontSize = 15.5.sp,
                fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    track.artist.ifBlank { "Không rõ ca sĩ" },
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    "  ·  ${track.source.label}",
                    color = Color.White.copy(alpha = 0.32f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        if (track.durationMs > 0) {
            Text(
                clockLabel(track.durationMs),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.5.sp
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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Lyra.Downloading.Done -> Text("✓", color = accent, fontSize = 16.sp)
                    is Lyra.Downloading.Failed -> Text(
                        "!",
                        color = Color(0xFFE0736B),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    null -> Text("↓", color = Color.White.copy(alpha = 0.45f), fontSize = 18.sp)
                }
            }
        }

        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onEnqueue),
            contentAlignment = Alignment.Center
        ) {
            Text(actionLabel, color = Color.White.copy(alpha = 0.6f), fontSize = 22.sp)
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
