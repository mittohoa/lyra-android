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
            if (text.isNotEmpty()) lines += LyricLine(0, text)
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
        }
    }

    if (lines.isEmpty()) return Lyrics.NONE

    val sorted = if (sawStamp) lines.sortedBy { it.time } else lines
    return Lyrics(
        lines = sorted,
        synced = sawStamp,
        from = from,
        offset = offsetTag
    )
}
