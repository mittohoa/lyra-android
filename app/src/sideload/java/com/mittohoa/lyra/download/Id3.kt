package com.mittohoa.lyra.download

import java.io.ByteArrayOutputStream

/**
 * Dung the ID3v2.3 de gan vao dau file nhac tai ve.
 *
 * Vi sao nhung vao file chu khong ghi file `.lrc` de canh: tu Android 10, app
 * khong duoc tu ghi mot file bat ky vao thu muc chung. Ta chen duoc file NHAC
 * vao `Music/` qua MediaStore, nhung mot file chu di kem thi bi tu choi - sai
 * loai noi dung cho thu muc do.
 *
 * Nhung vao file lai la cach dung hon: loi di theo bai sang MOI trinh phat,
 * khong phai chi rieng AURA, va khong bao gio lac mat file di kem.
 *
 * Viet tay thay vi keo mot thu vien the nhac: ca app chi can ghi bon khung the,
 * va ID3v2.3 la mot dinh dang dai vai chuc dong. Mot thu vien day du o day la
 * them mot tang phu thuoc de doc ca nhung thu ta khong bao gio doc.
 *
 * Chon v2.3 chu khong phai v2.4: v2.3 la ban duoc doc rong rai nhat, ke ca boi
 * cac trinh phat cu va cac dan xe hoi.
 */
object Id3 {

    /**
     * Dung the day du cho mot bai.
     *
     * `lyrics` la chuoi `.lrc` nguyen ban - giu ca moc thoi gian. Khung USLT
     * theo chuan la loi KHONG moc, nhung dan chu loi tu lau van nhet .lrc vao
     * day, va phan lon trinh phat doc duoc. Nguoc lai, khung SYLT dung chuan cho
     * loi co moc thi rat it noi ho tro - dung dung chuan ma khong ai doc duoc
     * thi khong giup gi ai.
     */
    fun tag(title: String, artist: String, album: String?, lyrics: String?): ByteArray {
        val frames = ByteArrayOutputStream()

        if (title.isNotBlank()) frames.write(textFrame("TIT2", title))
        if (artist.isNotBlank()) frames.write(textFrame("TPE1", artist))
        if (!album.isNullOrBlank()) frames.write(textFrame("TALB", album))
        if (!lyrics.isNullOrBlank()) frames.write(lyricsFrame(lyrics))

        val body = frames.toByteArray()
        val out = ByteArrayOutputStream(body.size + HEADER_SIZE)
        out.write("ID3".toByteArray(Charsets.ISO_8859_1))
        out.write(3) // ban chinh
        out.write(0) // ban phu
        out.write(0) // khong co co nao
        out.write(syncSafe(body.size))
        out.write(body)
        return out.toByteArray()
    }

    /**
     * Do dai phan the o dau mot dong byte, hoac 0 neu khong co the.
     *
     * Dung de BO the cu cua nguon di. Giu lai thi file co hai the ID3 chong len
     * nhau; trinh phat doc cai dau tien va bo cai sau, ma cai dau tien la cua
     * nguon - tuc loi ta vua nhung vao bi lam ngo.
     *
     * Can it nhat 10 byte dau de doc duoc.
     */
    fun tagLength(head: ByteArray): Int {
        if (head.size < HEADER_SIZE) return 0
        if (head[0] != 'I'.code.toByte() ||
            head[1] != 'D'.code.toByte() ||
            head[2] != '3'.code.toByte()
        ) {
            return 0
        }

        var size = 0
        for (i in 6..9) size = (size shl 7) or (head[i].toInt() and 0x7F)

        // Co 0x10 = co them phan duoi the, dai them 10 byte nua
        val footer = if ((head[5].toInt() and 0x10) != 0) HEADER_SIZE else 0
        return HEADER_SIZE + size + footer
    }

    /**
     * Khung chu, ma hoa UTF-16 co dau thu tu byte.
     *
     * Khong dung ISO-8859-1 du no gon hon: bang ma do khong co tieng Viet, va
     * "Nàng Thơ" se thanh mot chuoi dau hoi.
     */
    private fun textFrame(id: String, value: String): ByteArray {
        val body = ByteArrayOutputStream()
        body.write(UTF16)
        body.write(utf16(value))
        return frame(id, body.toByteArray())
    }

    private fun lyricsFrame(lyrics: String): ByteArray {
        val body = ByteArrayOutputStream()
        body.write(UTF16)
        // Ma ngon ngu ba chu. "und" = khong xac dinh, dung khi ta khong biet
        // chac loi la tieng gi - va ta thuc su khong biet
        body.write("und".toByteArray(Charsets.ISO_8859_1))
        // Chu thich rong, van phai co dau ket thuc
        body.write(utf16(""))
        body.write(utf16(lyrics))
        return frame("USLT", body.toByteArray())
    }

    private fun frame(id: String, body: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(body.size + 10)
        out.write(id.toByteArray(Charsets.ISO_8859_1))
        // ID3v2.3 dung so nguyen bon byte binh thuong, KHONG phai syncsafe -
        // khac han phan dau the. Nham cho nay la ca the bi doc lech.
        out.write(byteArrayOf(
            (body.size ushr 24).toByte(),
            (body.size ushr 16).toByte(),
            (body.size ushr 8).toByte(),
            body.size.toByte()
        ))
        out.write(0)
        out.write(0)
        out.write(body)
        return out.toByteArray()
    }

    /** Chuoi UTF-16 kem dau thu tu byte va hai byte ket thuc. */
    private fun utf16(value: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0xFF)
        out.write(0xFE)
        out.write(value.toByteArray(Charsets.UTF_16LE))
        out.write(0)
        out.write(0)
        return out.toByteArray()
    }

    /** Bon byte, moi byte chi mang bay bit - de khong lan voi dau khung nhac. */
    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    private const val HEADER_SIZE = 10

    /** Ma bang ma 1 = UTF-16 co dau thu tu byte. */
    private const val UTF16 = 1
}
