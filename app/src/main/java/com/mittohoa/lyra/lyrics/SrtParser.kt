package com.mittohoa.lyra.lyrics

/**
 * Đọc phụ đề `.srt`.
 *
 * Video trong máy hay đi kèm một tệp `.srt` cùng tên — đó là cách phụ đề được
 * lưu từ hai chục năm nay, và mọi trình xem phim đều đọc nó. Với Lyra thì phụ
 * đề đi thẳng vào đúng bộ máy đang chạy lời bài hát: tô sáng câu đang nói, chạm
 * để nhảy tới, khung nổi, lặp đoạn A–B đều dùng lại được không phải viết mới.
 *
 * KHÁC .lrc Ở MỘT CHỖ QUAN TRỌNG: mỗi câu có cả mốc bắt đầu và mốc KẾT THÚC.
 * Lời bài hát thì câu này nối câu kia nên chỉ cần mốc bắt đầu; phụ đề thì giữa
 * hai câu thoại có thể là một phút im lặng, mà bỏ qua mốc kết thúc thì câu cũ
 * nằm lì trên màn hình suốt phút đó.
 *
 * Dạng tệp:
 *
 *     1
 *     00:00:20,000 --> 00:00:24,400
 *     Dòng thứ nhất
 *     Dòng thứ hai
 *
 *     2
 *     ...
 */

/** `00:01:23,456` hoặc `00:01:23.456`; giờ có thể một hoặc hai chữ số. */
private val MOC = Regex("""(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})""")

/**
 * The dinh dang: `<i>`, `</b>`, `<font color="#fff">` va kieu `{\\an8}` cua
 * SubStation. Khong hien duoc thi bo di, chu de nguyen thi nguoi dung doc phai
 * may dau ngoac giua cau thoai.
 *
 * DAU } PHAI THOAT. Bo may bieu thuc chinh quy cua Android (ICU) chat hon cua
 * may tinh (OpenJDK): mot dau } dung mot minh, may tinh coi la ky tu thuong con
 * Android goi la loi cu phap. Bai kiem chay tren may tinh nen XANH HET, roi cai
 * len dien thoai la sap ngay lan doc dau tien. Co mot bai kiem chay tren may
 * that o androidTest de canh dung cho nay.
 */
private val THE = Regex("""<[^>]*>|\{[^}]*\}""")

fun parseSrt(content: String): Lyrics {
    val dong = content.removePrefix("﻿").replace("\r\n", "\n").split('\n')
    val cac = mutableListOf<LyricLine>()

    var i = 0
    while (i < dong.size) {
        val d = dong[i]
        if (!d.contains("-->")) { i++; continue }

        val moc = MOC.findAll(d).toList()
        if (moc.size < 2) { i++; continue }
        val batDau = mili(moc[0])
        val ketThuc = mili(moc[1])

        // Gom các dòng chữ tới khi gặp dòng trống. Một câu thoại hai dòng được
        // nối lại bằng khoảng trắng: xuống dòng trong .srt là để vừa bề ngang
        // màn hình chứ không mang nghĩa gì, mà chỗ hiện của Lyra thì tự xuống
        // dòng theo bề ngang của nó.
        val chu = StringBuilder()
        i++
        while (i < dong.size && dong[i].isNotBlank()) {
            if (chu.isNotEmpty()) chu.append(' ')
            chu.append(dong[i].trim())
            i++
        }

        val sach = THE.replace(chu, "").trim()
        if (sach.isNotEmpty()) {
            cac += LyricLine(time = batDau, text = sach, ketThuc = ketThuc)
        }
    }

    return Lyrics(lines = cac.sortedBy { it.time }, synced = cac.isNotEmpty())
}

private fun mili(m: MatchResult): Long {
    val (gio, phut, giay, le) = m.destructured
    // "1" trong "00:00:01,1" là một phần mười giây, không phải một mili-giây.
    val phanLe = le.padEnd(3, '0').take(3).toLong()
    return gio.toLong() * 3_600_000 + phut.toLong() * 60_000 + giay.toLong() * 1_000 + phanLe
}
