package com.mittohoa.lyra.lyrics

/** Mot dong loi, kem moc thoi gian tinh bang mili-giay. */
data class LyricLine(val time: Long, val text: String)

/** Loi tim duoc cho mot bai, kem xuat xu de nguoi dung biet no o dau ra. */
data class Lyrics(
    val lines: List<LyricLine> = emptyList(),
    /** Co moc thoi gian hay chi la chu tron. */
    val synced: Boolean = false,
    /** 'lrclib' | 'zing' | 'nct' | 'tu-nhap' | rong khi chua co gi. */
    val from: String = "",
    /** Ten bai va nghe si ma app nhan ra - de doi chieu khi no do nham. */
    val matchedTitle: String = "",
    val matchedArtist: String = "",
    /** Chinh lech thu cong, mili-giay. Duong = loi hien som hon. */
    val offset: Long = 0,
    /** Do dai ban thu ma nguon tra ve, mili-giay; 0 khi nguon khong noi. */
    val sourceDuration: Long = 0,
    /**
     * Moc thoi gian dang ngo.
     *
     * Tim dung TEN bai khong co nghia la dung BAN THU. Loi cua ban thu phong
     * dap len mot ban hat live thi lech tu dau den cuoi - nhip khac, dao dau
     * khac, co khi con noi chuyen truoc khi hat.
     *
     * Bat co nay thi giao dien hien loi dang chu tron: khong to sang, khong tu
     * cuon, va noi ro la moc co the lech. Hien sai mot cach tu tin con te hon
     * hien that tha la khong chac.
     */
    val timingSuspect: Boolean = false
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    companion object {
        val NONE = Lyrics()
    }
}

/**
 * Dong dang hat o thoi diem `position`.
 *
 * Tra ve -1 khi chua toi dong dau tien. Tim tuyen tinh nguoc tu cuoi: loi bai
 * hat hiem khi qua vai tram dong, ma cach nay khong can gia dinh gi ve viec
 * cac moc co duoc sap xep hoan hao khong.
 */
fun activeLineIndex(lines: List<LyricLine>, position: Long, offset: Long = 0): Int {
    if (lines.isEmpty()) return -1
    val t = position + offset
    for (i in lines.indices.reversed()) {
        if (lines[i].time <= t) return i
    }
    return -1
}
