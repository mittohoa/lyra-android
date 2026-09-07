package com.mittohoa.lyra.lyrics

/** Mot dong loi, kem moc thoi gian tinh bang mili-giay. */
data class LyricLine(
    val time: Long,
    val text: String,
    /**
     * Moc KET THUC, mili-giay. 0 nghia la khong biet - va do la truong hop cua
     * moi ban loi bai hat.
     *
     * Chi phu de `.srt` mang moc nay. Loi bai hat thi cau nay noi cau kia nen
     * chi can moc bat dau, con phu de thi giua hai cau thoai co the la ca mot
     * phut im lang: khong biet luc nao cau cu HET thi no nam li tren man hinh
     * suot phut do.
     */
    val ketThuc: Long = 0
)

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
    val timingSuspect: Boolean = false,
    /**
     * Co the la loi cua MOT BAI KHAC, khong phai ban thu khac.
     *
     * Khac han `timingSuspect` o tren, va dung mot cau bao khac: `timingSuspect`
     * la "dung bai, sai ban thu, moc lech"; con co nay la "co khi day khong phai
     * bai cua ban".
     *
     * Bat len khi mot trong hai chuyen xay ra luc di tim:
     *
     *   - app doi chieu CHI BANG TEN BAI vi khong biet ca si la ai
     *   - app co hoi kem ten ca si, nhung nguon tra ve ten ca si khac han
     *
     * VI SAO CAN. Co lan do duoc tren may: tep tag `Sacré Nom! - Hours` nhan
     * lai loi cua `Bhaskar - Hours and Hours`, va dai bao van ghi phang mot cau
     * "Loi tu lrclib" nhu moi lan khop dung. Cua chan duy nhat luc do la do
     * giong nhau cua TEN BAI, con ten ca si thi khong ai so. Voi nhung ten bai
     * chung chung - "Hours", "Home", "Yeu" - chuyen do khong hiem.
     *
     * KHONG loai bo ket qua, chi danh dau. Rat nhieu tep nhac tag sai hoac
     * khong tag ca si, va voi chung thi doan theo ten bai la duong duy nhat
     * con lai. Bo di thi nguoi dung mat loi that; danh dau thi ho tu biet.
     */
    val khacCaSi: Boolean = false
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    /** Toan bo loi dang chu tron, dung khi khong co moc thoi gian. */
    fun plainText(): String = lines.joinToString(System.lineSeparator()) { it.text }

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
        if (lines[i].time <= t) {
            // Cau da het gio thi khong con la cau dang hien nua. Chi phu de moi
            // co moc ket thuc, nen loi bai hat khong doi gi.
            val het = lines[i].ketThuc
            return if (het > 0 && t >= het) -1 else i
        }
    }
    return -1
}
