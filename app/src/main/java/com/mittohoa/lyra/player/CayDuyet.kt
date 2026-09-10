package com.mittohoa.lyra.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.sources.MediaKind
import com.mittohoa.lyra.sources.Track

/**
 * Cây duyệt cho Android Auto và mọi bộ duyệt media khác.
 *
 * VÌ SAO CÓ. Manifest KHAI `android.media.browse.MediaBrowserService` từ lâu,
 * mà dịch vụ phát lại kế thừa `MediaSessionService` — thứ không có cây duyệt
 * nào. Android Auto vì vậy thấy AURA trong danh sách, kết nối được, rồi mở ra
 * một thư viện trống. Khai một thứ mà không làm thì tệ hơn không khai: người
 * dùng không kết luận "app này chưa hỗ trợ", họ kết luận "app này hỏng".
 *
 * BỐN NHÁNH, ĐÚNG BỐN THỨ NGƯỜI TA TÌM TRONG XE. Đang lái thì không ai đọc một
 * cây thư mục sâu — mỗi cú chạm là một lần rời mắt khỏi đường. Nên chỉ có
 * những nhánh trả lời được ngay: nghe lại cái vừa nghe, nghe cái mình thích,
 * nghe một danh sách đã xếp sẵn, hoặc lục cả thư viện.
 *
 * CHỈ NHẠC TRONG MÁY. Nhạc ở Zing hay YouTube thì AURA không phát hộ được —
 * bày chúng ra đây là bày những dòng bấm vào không ra gì, mà trong xe thì đó
 * là loại lỗi tệ nhất.
 *
 * KHÔNG BÀY VIDEO. Đầu máy trên xe từ chối phát video khi đang chạy, và một
 * danh sách nửa bấm được nửa không thì tệ hơn một danh sách ngắn hơn.
 */
internal object CayDuyet {

    const val GOC = "aura://goc"
    const val GAN_DAY = "aura://gan-day"
    const val YEU_THICH = "aura://yeu-thich"
    const val THU_VIEN = "aura://thu-vien"
    const val DANH_SACH = "aura://danh-sach"

    /** Tiền tố cho một danh sách phát cụ thể: `aura://danh-sach/<id>`. */
    private const val DANH_SACH_ = "$DANH_SACH/"

    /**
     * Gốc riêng cho câu hỏi "có gì để nghe tiếp không".
     *
     * Android hỏi câu này qua một gốc KHÁC gốc thường: khay media của hệ thống
     * dò từng app nhạc sau mỗi lần khởi động lại máy, để bày ra một nút nghe
     * tiếp trước cả khi người dùng mở app. Trả gốc thường vào đây thì hệ thống
     * nhận được cả cây thư viện cho một chỗ chỉ đủ bày một bài.
     *
     * Đo được trên máy: không trả lời câu này thì nhật ký ghi "Cannot resume
     * with com.mittohoa.lyra_player" sau mỗi lần mở app — và đó là chỗ hụt có
     * từ lâu, không phải mới.
     */
    const val GOC_NGHE_TIEP = "aura://nghe-tiep"

    fun goc(): MediaItem = nhanh(GOC, "AURA")

    fun gocNgheTiep(): MediaItem = nhanh(GOC_NGHE_TIEP, "Nghe tiếp")

    /**
     * Bai de bay ra o nut "nghe tiep".
     *
     * Lay tu lich su nghe chu khong tu hang doi: hang doi song trong bo nho,
     * ma cau hoi nay duoc hoi ngay sau khi khoi dong lai may - luc do khong con
     * hang doi nao ca. Lich su thi nam tren dia.
     */
    fun ngheTiep(): Track? = lichSuThanhBai().firstOrNull()

    /** Các nhánh ở tầng đầu. */
    fun tangDau(): List<MediaItem> = listOf(
        nhanh(GAN_DAY, "Nghe gần đây"),
        nhanh(YEU_THICH, "Yêu thích"),
        nhanh(DANH_SACH, "Danh sách phát"),
        nhanh(THU_VIEN, "Nhạc trong máy")
    )

    /**
     * Con của một nhánh.
     *
     * Trả danh sách rỗng cho một mã lạ chứ không ném ngoại lệ: đầu máy trên xe
     * có thể còn giữ mã của một danh sách phát người dùng đã xoá, và một cú
     * chạm vào đó không được phép làm sập bộ phát nhạc giữa đường.
     */
    fun con(ma: String): List<MediaItem> = when {
        ma == GOC -> tangDau()
        // Dung MOT bai: cho nay la mot nut "nghe tiep", khong phai mot danh
        // sach. Tra nhieu hon thi he thong cung chi lay bai dau.
        ma == GOC_NGHE_TIEP -> listOfNotNull(ngheTiep()?.let { bai(it) })
        ma == GAN_DAY -> tuLichSu()
        ma == YEU_THICH -> Lyra.baiYeuThich().filter { nghevDuoc(it) }.map { bai(it) }
        ma == THU_VIEN -> Lyra.library.value.filter { nghevDuoc(it) }.map { bai(it) }
        ma == DANH_SACH -> Lyra.playlists.value.map { nhanh(DANH_SACH_ + it.id, it.name) }
        ma.startsWith(DANH_SACH_) -> {
            val id = ma.removePrefix(DANH_SACH_)
            Lyra.playlists.value.firstOrNull { it.id == id }
                ?.tracks.orEmpty().filter { nghevDuoc(it) }.map { bai(it) }
        }
        else -> emptyList()
    }

    /** Một mục bất kỳ trong cây, tra theo mã. */
    fun mot(ma: String): MediaItem? {
        if (ma == GOC) return goc()
        tangDau().firstOrNull { it.mediaId == ma }?.let { return it }
        return timBai(ma)?.let { bai(it) }
    }

    /** Bài trong thư viện ứng với một mã — mã chính là địa chỉ phát. */
    fun timBai(ma: String): Track? =
        Lyra.library.value.firstOrNull { it.playbackUri == ma }

    /**
     * Hàng đợi dựng từ một mục được chọn.
     *
     * Chạm một bài trong xe là "phát từ đây trở đi" chứ không phải "phát đúng
     * một bài rồi im" — cùng lẽ với chạm một bài trong app. Nên trả về cả
     * nhánh chứa nó, kèm chỗ bắt đầu.
     */
    fun hangDoiCho(ma: String): Pair<List<Track>, Int>? {
        for (nhanh in listOf(GAN_DAY, YEU_THICH, THU_VIEN)) {
            val ds = danhSachBai(nhanh)
            val i = ds.indexOfFirst { it.playbackUri == ma }
            if (i >= 0) return ds to i
        }
        for (dsp in Lyra.playlists.value) {
            val i = dsp.tracks.indexOfFirst { it.playbackUri == ma }
            if (i >= 0) return dsp.tracks.filter { nghevDuoc(it) } to i
        }
        return null
    }

    private fun danhSachBai(ma: String): List<Track> = when (ma) {
        GAN_DAY -> lichSuThanhBai()
        YEU_THICH -> Lyra.baiYeuThich().filter { nghevDuoc(it) }
        THU_VIEN -> Lyra.library.value.filter { nghevDuoc(it) }
        else -> emptyList()
    }

    private fun tuLichSu(): List<MediaItem> = lichSuThanhBai().map { bai(it) }

    /**
     * Lịch sử nghe, đổi sang bài trong thư viện.
     *
     * Bỏ những dòng không nghe lại được: lịch sử ghi cả nhạc phát ở app khác,
     * mà những dòng ấy trong app thì bấm vào là ĐI TÌM. Trong xe thì không có
     * chỗ nào để đi tìm, nên bày ra chỉ là bày một dòng bấm không ra gì.
     */
    private fun lichSuThanhBai(): List<Track> {
        val thu = Lyra.library.value.associateBy { it.playbackUri }
        return Lyra.lichSuNghe.value.mapNotNull { thu[it.diaChi] }.filter { nghevDuoc(it) }
    }

    private fun nghevDuoc(t: Track) = t.kind != MediaKind.VIDEO

    private fun nhanh(ma: String, ten: String): MediaItem = MediaItem.Builder()
        .setMediaId(ma)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(ten)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()
        )
        .build()

    fun bai(t: Track): MediaItem = MediaItem.Builder()
        .setMediaId(t.playbackUri)
        // PHAI DAT CA URI, khong chi ma.
        //
        // Bo phat khong phat duoc mot muc chi co ma - no can mot dia chi de
        // mo dong byte. Thieu dong nay thi bai tu cay duyet nap vao roi dung
        // ngay, khong bao loi gi: dung cai bay da dam phai khi thu tren may.
        .setUri(t.playbackUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(t.title)
                .setArtist(t.artist.ifBlank { null })
                .setAlbumTitle(t.album.ifBlank { null })
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setArtworkUri(t.artworkUrl?.let { runCatching { Uri.parse(it) }.getOrNull() })
                .build()
        )
        .build()
}
