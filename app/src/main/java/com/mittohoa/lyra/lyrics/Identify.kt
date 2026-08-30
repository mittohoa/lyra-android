package com.mittohoa.lyra.lyrics

import java.text.Normalizer

/**
 * Doan ten bai va nghe si that tu mot chuoi tho.
 *
 * Tren Android phan lon app deu khai bao nghe si va ten bai o hai truong rieng,
 * sach hon han Windows. Nhung app YouTube thi van dua nguyen TEN VIDEO:
 *
 *   "NƠI NÀY CÓ ANH | OFFICIAL MUSIC VIDEO | SƠN TÙNG M-TP"
 *   "【MV】Bo Hen - Vu Cat Tuong「Lyrics Video」4K"
 *
 * Dem nguyen chuoi do di tra LRCLIB thi truot gan nhu chac chan. Module nay boc
 * rac ra, roi sinh NHIEU phuong an (nghe si, ten bai) de ben goi thu lan luot -
 * vi khong the biet chac ve nao la nghe si, ve nao la ten bai.
 *
 * Chuyen tu `src/main/lyrics/identify.ts` cua ban Windows. Bo kiem tra cung
 * duoc chuyen sang, xem `IdentifyTest.kt`.
 */

/** Cum thuong gap trong ten video, khong phai mot phan ten bai. */
private val NOISE = setOf(
    "official music video", "official video", "official mv", "official audio",
    "official lyric video", "official lyrics video", "official visualizer", "official",
    "music video", "lyric video", "lyrics video", "lyric", "lyrics",
    "audio", "visualizer", "mv", "m/v", "hd", "hq", "4k", "8k",
    "full", "full hd", "video", "vietsub", "engsub", "sub",
    "karaoke", "beat", "instrumental", "reaction", "cover by",
    "live performance", "performance video", "dance practice",
    "teaser", "trailer", "clip"
)

/** Dau ngoac cac loai, ke ca ngoac tieng Nhat/Trung hay gap trong ten video. */
private val BRACKETS = listOf(
    '(' to ')', '[' to ']', '{' to '}',
    '【' to '】', '「' to '」', '『' to '』', '〈' to '〉', '《' to '》'
)

/**
 * Cum tach nghe si va ten bai, kem chieu thuong gap cua chinh dau do.
 *
 * Hai kieu dat ten pho bien va NGUOC nhau:
 *   "Sơn Tùng M-TP - Nơi Này Có Anh"        gach ngang: nghe si dung truoc
 *   "NƠI NÀY CÓ ANH | ... | SƠN TÙNG M-TP"  gach dung:  ten bai dung truoc
 * Ap mot chieu cho ca hai thi mot nua so truong hop se nhan nham.
 */
private data class Separator(val sep: String, val artistFirst: Boolean)

private val SEPARATORS = listOf(
    Separator(" - ", true),
    Separator(" – ", true),
    Separator(" — ", true),
    Separator(" | ", false),
    Separator(" ~ ", true),
    Separator(" / ", true),
    Separator("「", true),
    Separator("『", true)
)

private val DIACRITICS = Regex("\\p{Mn}+")
private val NON_ALNUM = Regex("[^a-z0-9\\s]")
private val SPACES = Regex("\\s+")
private val APP_SUFFIX = Regex(
    "\\s*[-–—|]\\s*(YouTube|YouTube Music|Spotify|SoundCloud|Vimeo|Zing MP3|NhacCuaTui)\\s*$",
    RegexOption.IGNORE_CASE
)
private val CHANNEL_SUFFIX = Regex("\\s*-?\\s*(Official|Topic|VEVO)\\s*$", RegexOption.IGNORE_CASE)
private val TRIM_HEAD = Regex("^[\\s\\-–—|·•,.:]+")
private val TRIM_TAIL = Regex("[\\s\\-–—|·•,.:]+$")
private val SPLIT_PARTS = Regex("\\s+[-–—|~/]\\s+")

private fun stripDiacritics(text: String): String =
    DIACRITICS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
        .replace('đ', 'd')
        .replace('Đ', 'D')

/** Chuoi de so sanh: bo dau, bo ky tu khong phai chu/so, ve chu thuong. */
fun normalizeForCompare(text: String): String =
    SPACES.replace(NON_ALNUM.replace(stripDiacritics(text).lowercase(), " "), " ").trim()

/** Doan nay co phai chi toan tu rac khong. */
private fun isNoise(segment: String): Boolean {
    val n = normalizeForCompare(segment)
    if (n.isEmpty()) return true
    if (n in NOISE) return true
    // "official music video 4k" - toan tu rac ghep lai
    val words = n.split(' ')
    return words.size <= 5 && words.all { it in NOISE }
}

/** Bo cac cum trong ngoac neu cum do chi la nhan quang cao. */
private fun stripBrackets(text: String): String {
    var out = text
    for ((open, close) in BRACKETS) {
        var guard = 0
        while (true) {
            val start = out.indexOf(open)
            if (start == -1) break
            val end = out.indexOf(close, start + 1)
            if (end == -1) break
            val inside = out.substring(start + 1, end)
            if (!isNoise(inside)) break
            out = out.substring(0, start) + " " + out.substring(end + 1)
            if (++guard > 12) break
        }
    }
    return SPACES.replace(out, " ").trim()
}

/** Bo duoi " - YouTube", " - Spotify"... ma trinh duyet them vao tieu de. */
private fun stripAppSuffix(text: String): String = APP_SUFFIX.replace(text, "").trim()

/** Bo cac cum rac dung roi le, va dau cau thua o hai dau. */
private fun tidy(text: String): String {
    var out = text
    for (sep in listOf('|', '·', '•')) {
        out = out.split(sep).filterNot { isNoise(it) }.joinToString(" | ")
    }
    return SPACES.replace(TRIM_TAIL.replace(TRIM_HEAD.replace(out, ""), ""), " ").trim()
}

/**
 * Bo ten nghe si ra khoi ten bai neu no bi lap lai o do.
 * So sanh sau khi bo dau nen "SƠN TÙNG M-TP" van khop voi "Sơn Tùng M-TP".
 */
private fun removeArtistFromTitle(title: String, artist: String): String {
    val needle = normalizeForCompare(artist)
    if (needle.length < 3) return title

    val kept = SPLIT_PARTS.split(title)
        .filter { normalizeForCompare(it) != needle }
        .joinToString(" - ")
    return tidy(kept)
}

/** Mot phuong an (nghe si, ten bai) de dem di tra. */
data class Candidate(
    val artist: String,
    val title: String,
    /** Cang cao cang dang thu truoc. */
    val weight: Int
)

/** Thu app khac dang phat, o dang tho nhat ma he thong dua ra. */
data class RawNowPlaying(
    val title: String,
    val artist: String? = null,
    val album: String? = null
)

/**
 * Sinh danh sach phuong an (nghe si, ten bai) de thu lan luot.
 *
 * Khong the biet chac ve nao la nghe si: "Sơn Tùng M-TP - Nơi Này Có Anh" va
 * "Nơi Này Có Anh - Sơn Tùng M-TP" deu gap ngoai doi. Nen sinh ca hai chieu,
 * de ben goi tra thu tu phuong an nang nhat tro xuong.
 */
fun candidatesFrom(raw: RawNowPlaying): List<Candidate> {
    val cleanedTitle = tidy(stripBrackets(stripAppSuffix(raw.title)))
    val rawArtist = tidy(stripBrackets(raw.artist ?: ""))

    // Nhieu app dat nghe si = ten kenh: "Sơn Tùng M-TP Official"
    val artist = CHANNEL_SUFFIX.replace(rawArtist, "").trim()

    val out = mutableListOf<Candidate>()
    val seen = mutableSetOf<String>()

    fun add(a: String, t: String, weight: Int) {
        if (t.isBlank()) return
        val key = "${normalizeForCompare(a)}|${normalizeForCompare(t)}"
        if (!seen.add(key)) return
        out += Candidate(a.trim(), t.trim(), weight)
    }

    // 1. App khai bao nghe si rieng - dang tin nhat
    if (artist.isNotEmpty()) {
        // Ten video thuong lap lai ten nghe si trong chinh no:
        //   "NƠI NÀY CÓ ANH | SƠN TÙNG M-TP"  voi nghe si "Sơn Tùng M-TP"
        // Bo phan trung di thi ten bai gon va tra cung trung hon.
        val withoutArtist = removeArtistFromTitle(cleanedTitle, artist)
        if (withoutArtist.isNotEmpty() && withoutArtist != cleanedTitle) {
            add(artist, withoutArtist, 110)
        }
        add(artist, cleanedTitle, 100)
    }

    // 2. Tach theo dau ngan cach. Van sinh ca hai chieu de con duong lui,
    //    nhung chieu thuong gap cua chinh dau do duoc thu truoc.
    for ((sep, artistFirst) in SEPARATORS) {
        if (!cleanedTitle.contains(sep)) continue

        // Ten nhieu doan ("A | B | C"): ghep doan DAU voi doan CUOI, vi phan
        // giua thuong la nhan quang cao (da bi loc bot o buoc tren)
        val parts = cleanedTitle.split(sep).map(::tidy).filter { it.isNotEmpty() }
        if (parts.size < 2) continue
        val head = parts.first()
        val tail = parts.last()

        if (artistFirst) {
            add(head, tail, 80)
            add(tail, head, 60)
        } else {
            add(tail, head, 80)
            add(head, tail, 60)
        }
        break
    }

    // 3. Khong tach duoc thi tra bang chinh ten da lam sach, khong co nghe si
    add("", cleanedTitle, 40)

    return out.sortedByDescending { it.weight }
}

/**
 * Do giong nhau giua hai ten (0..1), dua tren ti le tu chung.
 * Dung de cham diem ket qua tra ve co dung bai khong.
 */
fun titleSimilarity(a: String, b: String): Double {
    val wa = normalizeForCompare(a).split(' ').filter { it.isNotEmpty() }.toSet()
    val wb = normalizeForCompare(b).split(' ').filter { it.isNotEmpty() }.toSet()
    if (wa.isEmpty() || wb.isEmpty()) return 0.0

    val shared = wa.count { it in wb }

    // Chia cho ben NHO hon: "Nơi Này Có Anh" khop tot voi
    // "Nơi Này Có Anh (Remix)" du ben kia dai hon
    return shared.toDouble() / minOf(wa.size, wb.size)
}
