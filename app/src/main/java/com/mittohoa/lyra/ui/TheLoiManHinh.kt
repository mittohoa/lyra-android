package com.mittohoa.lyra.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.mittohoa.lyra.share.MauThe
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittohoa.lyra.data.KieuChu
import com.mittohoa.lyra.lyrics.LyricLine
import com.mittohoa.lyra.share.TheLoi
import com.mittohoa.lyra.share.VideoLoi
import com.mittohoa.lyra.share.guiTheLoi
import com.mittohoa.lyra.share.guiVideoLoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Nhiều nhất bao nhiêu dòng lấy được một lần.
 *
 * Trần này là cho VIDEO, nơi lời được cắt thành nhiều cảnh nên dài bao nhiêu
 * cũng đọc được. Thẻ ảnh thì chật hơn hẳn — xem `DONG_MOI_CANH`.
 */
private const val TRAN_DONG = 8

/**
 * Mỗi cảnh video, và mỗi tấm thẻ ảnh, chứa nhiều nhất bấy nhiêu dòng.
 *
 * Thẻ có kích thước cố định 1080×1350 và chữ tự co lại cho vừa. Nhồi quá thì
 * chữ nhỏ tới mức chính thứ đem khoe lại là thứ khó đọc nhất trên ảnh.
 *
 * Video dùng CÙNG con số này: mỗi cảnh vẽ bằng đúng bộ vẽ thẻ, nên cảnh nào
 * cũng phải đọc được như một tấm thẻ. Chọn tám dòng thì ra bốn cảnh chứ không
 * phải một cảnh nhồi tám dòng.
 */
private const val DONG_MOI_CANH = 2

/**
 * Xem trước tấm thẻ lời rồi gửi đi.
 *
 * Mở ra ở câu đang hát vì đó là câu người ta vừa nghe thấy và muốn giữ lại,
 * nhưng đổi được sang câu khác bằng hai nút ‹ ›: câu đáng chia sẻ thường là câu
 * vừa trôi qua, không phải câu đang trôi.
 *
 * Lấy được NHIỀU DÒNG vào một thẻ, không chỉ một. Một câu tách khỏi đoạn của nó
 * thường mất nghĩa, và người nhận không có gì để bám.
 */
@Composable
fun TheLoiManHinh(
    cacDong: List<LyricLine>,
    dongDau: Int,
    tenBai: String,
    caSi: String,
    accent: Color,
    kieuChu: KieuChu,
    bia: android.graphics.Bitmap?,
    onDong: () -> Unit
) {
    val context = LocalContext.current
    val bangMau = mau

    // Bỏ qua các dòng trống và dòng chỉ có dấu nhạc: không ai chia sẻ một tấm
    // thẻ in mỗi chữ "♪".
    val dungDuoc = remember(cacDong) {
        cacDong.indices.filter { cacDong[it].text.isNotBlank() && cacDong[it].text.trim() != "♪" }
    }
    if (dungDuoc.isEmpty()) { onDong(); return }

    // Mẫu nào cần ảnh bìa mà bài này không có bìa thì đừng bày ra - một mục
    // chọn xong không đổi gì là một mục hỏng.
    val cacMau = remember(bia) { MauThe.entries.filter { bia != null || !it.canBia } }
    var mauThe by remember { mutableStateOf(MauThe.GIAY) }

    var viTri by remember {
        val gan = dungDuoc.indexOfFirst { it >= dongDau }
        mutableIntStateOf(if (gan >= 0) gan else dungDuoc.lastIndex)
    }
    // Bao nhiêu dòng vào thẻ, tính từ dòng đang chọn.
    //
    // Bản đầu chỉ cho đúng MỘT câu, và đó là chỗ hụt thật: người ta chia sẻ một
    // đoạn chứ ít khi một câu. Một câu tách khỏi đoạn của nó thường mất nghĩa,
    // và người nhận không có gì để bám.
    //
    // Trần bốn dòng, không mở hơn: thẻ có kích thước cố định 1080×1350, chữ tự
    // co lại cho vừa. Nhồi thêm nữa thì chữ nhỏ tới mức chính thứ đem khoe lại
    // là thứ khó đọc nhất trên tấm ảnh.
    var soDong by remember { mutableIntStateOf(1) }

    // Trạng thái của việc dựng video. `baoVideo` chỉ có chữ khi hỏng — dựng
    // xong thì bảng chia sẻ tự hiện ra, không cần báo thêm câu nào.
    var dangDungVideo by remember { mutableStateOf(false) }
    var tienDo by remember { mutableIntStateOf(0) }
    var baoVideo by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val conLai = dungDuoc.size - viTri
    val soDongThat = soDong.coerceAtMost(minOf(TRAN_DONG, conLai))
    val cauHat = remember(viTri, soDongThat, dungDuoc, cacDong) {
        dungDuoc.drop(viTri).take(soDongThat).joinToString("\n") { cacDong[it].text }
    }

    // Vẽ trên luồng nền: một tấm 1080×1350 kèm bố cục chữ là việc của CPU, làm
    // trên luồng chính thì mỗi lần bấm ‹ › là một cú khựng.
    val anh by produceState<Bitmap?>(null, cauHat, bangMau.laGiay, kieuChu, accent, mauThe) {
        value = withContext(Dispatchers.Default) {
            TheLoi.ve(
                context = context,
                cauHat = cauHat,
                tenBai = tenBai,
                caSi = caSi,
                mauNhan = accent.toArgb(),
                laGiay = bangMau.laGiay,
                kieuChu = kieuChu,
                mau = mauThe,
                bia = bia
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .chanChamXuyen()
            .background(mau.nen)
            // Man hinh nay ve tran ra sat vien nhu ca app, nen phai tu chua le
            // cho hai dai he thong. Khong chua thi dong "The loi" chui len duoi
            // dong ho va vach song.
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Thẻ lời", color = mau.chu, fontFamily = boChu.loi,
                fontSize = 21.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDong)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Đóng", color = mau.chuMo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val a = anh
            if (a != null) {
                Image(
                    bitmap = a.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(TheLoi.RONG.toFloat() / TheLoi.CAO)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, mau.vien, RoundedCornerShape(6.dp))
                )
            } else {
                LyraMark(size = 44.dp, busy = true)
            }
        }

        // Hàng mẫu. Cuộn ngang chứ không xuống dòng: sáu mục xếp thành hai hàng
        // thì phần xem trước bị đẩy lên và mất chỗ.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (m in cacMau) {
                val chon = m == mauThe
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (chon) accent else mau.nenChim)
                        .clickable { mauThe = m }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        m.nhan,
                        color = if (chon) Color.White else mau.chuMo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NutDoiCau("‹", viTri > 0) { viTri-- }
            Text(
                if (soDongThat > 1) "Câu ${viTri + 1}–${viTri + soDongThat}/${dungDuoc.size}"
                else "Câu ${viTri + 1}/${dungDuoc.size}",
                color = mau.chuMo,
                fontSize = 13.5.sp,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            NutDoiCau("›", viTri < dungDuoc.lastIndex) { viTri++ }
        }

        // Lấy thêm mấy dòng nữa vào cùng một thẻ.
        //
        // Hàng riêng chứ không nhét chung với ‹ ›: hai hàng trả lời hai câu
        // khác nhau — bắt đầu từ đâu, và lấy bao nhiêu. Gộp vào một hàng bốn
        // nút thì không ai đoán được nút nào làm gì.
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NutDoiCau("−", soDongThat > 1) { soDong = soDongThat - 1 }
            Text(
                "$soDongThat dòng",
                color = mau.chuMo,
                fontSize = 13.5.sp,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            NutDoiCau("+", soDongThat < minOf(TRAN_DONG, conLai)) { soDong = soDongThat + 1 }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 22.dp)
                .clip(RoundedCornerShape(50))
                .background(if (anh != null) accent else mau.nenChim)
                .clickable(enabled = anh != null) {
                    anh?.let { guiTheLoi(context, it, tenBai) }
                }
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Chia sẻ ảnh",
                color = if (anh != null) Color.White else mau.chuRatMo,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Dựng video mất vài giây, nên nút phải NÓI nó đang làm gì.
        //
        // Im lặng trong lúc dựng là kiểu hỏng tệ nhất ở đây: người dùng bấm,
        // không thấy gì, bấm lại — và lần bấm thứ hai chồng lên lần đầu.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 22.dp)
                .clip(RoundedCornerShape(50))
                .background(mau.nenChim)
                .clickable(enabled = !dangDungVideo) {
                    dangDungVideo = true
                    tienDo = 0
                    scope.launch {
                        runCatching {
                            VideoLoi.dung(
                                context = context,
                                // Cắt thành CẢNH chứ không dồn hết vào một
                                // màn: mỗi cảnh vẽ bằng đúng bộ vẽ thẻ, nên
                                // cảnh nào cũng phải đọc được như một tấm thẻ.
                                cacCau = dungDuoc.drop(viTri).take(soDongThat)
                                    .map { cacDong[it].text }
                                    .chunked(DONG_MOI_CANH)
                                    .map { it.joinToString("\n") },
                                tenBai = tenBai,
                                caSi = caSi,
                                mauNhan = accent.toArgb(),
                                laGiay = bangMau.laGiay,
                                kieuChu = kieuChu,
                                mau = mauThe,
                                bia = bia,
                                onTienDo = { tienDo = it }
                            )
                        }.onSuccess {
                            guiVideoLoi(context, it, tenBai)
                        }.onFailure {
                            baoVideo = "Không dựng được video: " +
                                (it.message ?: "lỗi không rõ")
                        }
                        dangDungVideo = false
                    }
                }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (dangDungVideo) "Đang dựng video… $tienDo%"
                // Nói luôn đoạn sẽ dài bao nhiêu — dựng mất vài giây, người ta
                // cần biết mình đang chờ để lấy cái gì.
                else {
                    val soCanh = (soDongThat + DONG_MOI_CANH - 1) / DONG_MOI_CANH
                    "Chia sẻ video · khoảng ${(soCanh * 2.6f).toInt()} giây"
                },
                color = mau.chu,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        baoVideo?.let {
            Text(
                it,
                color = mau.chu,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun NutDoiCau(nhan: String, bamDuoc: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(50))
            .background(mau.nenChim)
            .clickable(enabled = bamDuoc, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            nhan,
            color = if (bamDuoc) mau.chu else mau.chuRatMo,
            fontSize = 22.sp
        )
    }
}
