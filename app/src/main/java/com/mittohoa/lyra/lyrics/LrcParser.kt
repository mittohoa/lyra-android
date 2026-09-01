package com.mittohoa.lyra.lyrics

/**
 * Doc dinh dang .lrc.
 *
 * Chuyen tu `src/main/lyrics/lrc.ts` cua ban Windows, giu nguyen cac truong
 * hop da xu ly: mot dong co the mang NHIEU moc thoi gian (doan diep khuc lap
 * lai), mili-giay co the la 2 hoac 3 chu so, va cac tag metadata ([ar:], [ti:])
 * phai bi bo chu khong duoc coi la loi.
 */

/** [mm:ss.xx] hoac [mm:ss.xxx] hoac [mm:ss] */
private val STAMP = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{2,3}))?]""")

/** [ar:...], [ti:...], [offset:...] - tag mo ta, khong phai loi */
private val TAG = Regex("""^\[[a-zA-Z#]+:.*]$""")

/**
 * Doc noi dung .lrc thanh danh sach dong.
 *
 * Khong tim thay moc thoi gian nao thi coi la loi chu tron: van tra ve tung
 * dong, chi khac la `synced = false` va moc deu bang 0.
 */
fun parseLrc(content: String, from: String = ""): Lyrics {
    val lines = mutableListOf<LyricLine>()
    // Song song voi danh sach tren: dong nay co mang moc that hay khong.
    //
    // Khong doan qua "time > 0" duoc: mot dong co moc [00:00.00] cung mang
    // time bang 0, va doan kieu do se vut mat dung dong dau bai cua nhung ban
    // loi bat dau ngay giay thu khong.
    val tuMoc = mutableListOf<Boolean>()
    var sawStamp = false
    var offsetTag = 0L

    for (raw in content.lines()) {
        val line = raw.trim()
        if (line.isEmpty()) continue

        // [offset:+200] - vai nguon dat san do lech o day
        if (line.startsWith("[offset:", ignoreCase = true)) {
            offsetTag = line.removePrefix("[offset:").removeSuffix("]")
                .trim().toLongOrNull() ?: 0L
            continue
        }
        if (TAG.matches(line)) continue

        val stamps = STAMP.findAll(line).toList()
        val text = STAMP.replace(line, "").trim()

        if (stamps.isEmpty()) {
            // Khong co moc - giu lai lam loi chu tron
            if (text.isNotEmpty()) {
                lines += LyricLine(0, text)
                tuMoc += false
            }
            continue
        }

        sawStamp = true
        // Mot dong co the mang nhieu moc: doan diep khuc duoc dung lai nhieu lan
        for (stamp in stamps) {
            val (m, s, frac) = stamp.destructured
            val millis = when (frac.length) {
                3 -> frac.toLong()
                2 -> frac.toLong() * 10
                else -> 0L
            }
            lines += LyricLine(m.toLong() * 60_000 + s.toLong() * 1_000 + millis, text)
            tuMoc += true
        }
    }

    if (lines.isEmpty()) return Lyrics.NONE

    // File TRON - co dong mang moc, co dong khong - thi bo cac dong khong moc.
    //
    // Chung dang mang thoi gian 0, va sap xep theo thoi gian se day het chung
    // len dau bai. Mot doan loi dao lon nhu vay te hon han la thieu vai dong:
    // nguoi doc tin vao thu tu, va thu tu sai thi ho khong nhan ra la sai.
    //
    // Hay gap o file that: vai nguon de mot dong tieu de hoac ten nguoi dich
    // khong mang moc lan giua cac dong co moc. Va cong cu can gio trong app
    // cung tao ra dung loai file nay neu can do dang.
    val locSach = if (sawStamp) lines.filterIndexed { i, _ -> tuMoc[i] } else lines

    val sorted = if (sawStamp) locSach.sortedBy { it.time } else locSach
    return Lyrics(
        lines = sorted,
        synced = sawStamp,
        from = from,
        offset = offsetTag
    )
}
