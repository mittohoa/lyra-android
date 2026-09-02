package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import com.mittohoa.lyra.lyrics.normalizeForCompare
import java.io.File
import java.security.MessageDigest

/**
 * Loi nguoi dung tu nhap.
 *
 * Day la duong cuu khi ca ba nguon deu khong co, hoac co ma sai. Khong co no
 * thi voi nhung bai it nguoi nghe, app chi biet noi "chua tim thay" va het -
 * ma nguoi dung thi hoan toan co the tu dan loi vao.
 *
 * Loi tu nhap duoc UU TIEN HON MOI NGUON, va khong bao gio bi xoa tu dong.
 * Nguoi dung da bo cong go thi khong the de mot lan tra mang ghi de len.
 *
 * Luu THO chu khong luu da doc: nguoi dung dan gi thi giu nguyen the ay, de lan
 * sau mo ra sua con thay dung cai minh da dan. Doc thanh dong la viec cua
 * `parseLrc`, va no chay lai moi lan.
 */
class ManualLyricStore(context: Context) {

    private val dir = File(context.filesDir, "manual-lyrics").apply { mkdirs() }

    /**
     * Bang ten: khoa bam -> nghe si + ten bai.
     *
     * Ten tep la bam SHA-1 cua "nghe si|ten bai", ma bam thi khong lan nguoc
     * duoc. Khong co bang nay thi ban sao luu ra chi la mot dong loi khong ten:
     * nguoi dung mo ra khong biet bai nao la bai nao, va Lyra cung khong the
     * tinh lai khoa neu cach bam doi o ban sau.
     *
     * De rieng mot tep chu khong nhet vao dau tung tep loi: noi dung tep loi la
     * thu nguoi dung tu go, mo ra sua phai thay dung cai minh da go chu khong
     * phai mot dong dau do app tu them vao.
     */
    private val bangTen = File(dir, TEN_BANG)

    private fun keyOf(artist: String, title: String): String {
        val raw = "${normalizeForCompare(artist)}|${normalizeForCompare(title)}"
        return MessageDigest.getInstance("SHA-1")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /** Chuoi tho nguoi dung da nhap, hoac null neu chua co. */
    fun get(artist: String, title: String): String? = try {
        File(dir, keyOf(artist, title)).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.d(TAG, "Khong doc duoc loi tu nhap", e)
        null
    }

    fun put(artist: String, title: String, raw: String) {
        try {
            val key = keyOf(artist, title)
            val file = File(dir, key)
            if (raw.isBlank()) {
                file.delete()
                xoaTen(key)
            } else {
                file.writeText(raw)
                ghiTen(key, artist, title)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Khong ghi duoc loi tu nhap", e)
        }
    }

    fun remove(artist: String, title: String) {
        val key = keyOf(artist, title)
        runCatching { File(dir, key).delete() }
        runCatching { xoaTen(key) }
    }

    // ---- Sao luu ----

    /** Mot bai co loi tu nhap. `caSi`/`tenBai` rong voi ban ghi cu chua co bang ten. */
    data class BanLoi(val khoa: String, val caSi: String, val tenBai: String, val loi: String)

    /**
     * Tat ca loi tu nhap dang giu.
     *
     * Ban ghi luu tu truoc khi co bang ten thi khong biet ten bai - van tra ve,
     * chi la khong ten. Bo chung di thi ban sao luu im lang lam mat dung nhung
     * ban ghi cu nhat, tuc nhung ban ghi da song lau nhat.
     */
    fun tatCa(): List<BanLoi> {
        val ten = docBangTen()
        return (dir.listFiles() ?: return emptyList())
            .filter { it.isFile && it.name != TEN_BANG }
            .mapNotNull { f ->
                val loi = runCatching { f.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val (caSi, tenBai) = ten[f.name] ?: ("" to "")
                BanLoi(f.name, caSi, tenBai, loi)
            }
            .sortedWith(compareBy({ it.tenBai.isEmpty() }, { it.tenBai.lowercase() }))
    }

    /** So bai dang giu - de trang Chinh noi duoc con so ma khong doc het noi dung. */
    fun demBai(): Int {
        val cac = dir.listFiles() ?: return 0
        return cac.count { it.isFile && it.name != TEN_BANG }
    }

    /**
     * Dat lai mot ban ghi khi khoi phuc.
     *
     * Co ten thi TINH LAI khoa tu ten chu khong dung khoa trong tep: cach bam
     * co the doi giua hai ban Lyra, va tinh lai thi ban sao luu cu van dung cho
     * ban moi. Khong co ten thi danh dung lai khoa cu - van hon la vut di.
     *
     * Tra ve `false` khi bai do da co san: KHONG ghi de. Nguoi khoi phuc len mot
     * may dang co loi tu go thi cai dang nam tren may la cai moi hon.
     */
    fun dat(ban: BanLoi): Boolean {
        val khoa = if (ban.tenBai.isNotBlank()) keyOf(ban.caSi, ban.tenBai) else ban.khoa
        // Khong ten ma cung khong khoa thi khong biet dat vao dau. Bo qua chu
        // dung tu bia mot khoa: bia ra thi ban ghi nam do vinh vien va khong
        // bao gio khop voi bai nao ca.
        if (khoa.isBlank() || khoa.contains('/') || khoa == TEN_BANG) return false
        val file = File(dir, khoa)
        if (file.exists()) return false
        return try {
            file.writeText(ban.loi)
            if (ban.tenBai.isNotBlank()) ghiTen(khoa, ban.caSi, ban.tenBai)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Khong khoi phuc duoc mot ban ghi", e)
            false
        }
    }

    // ---- bang ten ----

    private fun docBangTen(): Map<String, Pair<String, String>> = try {
        if (!bangTen.exists()) emptyMap()
        else bangTen.readLines().mapNotNull { dong ->
            val o = dong.split('\t')
            if (o.size < 3) null else o[0] to (o[1] to o[2])
        }.toMap()
    } catch (e: Exception) {
        Log.d(TAG, "Khong doc duoc bang ten", e)
        emptyMap()
    }

    @Synchronized
    private fun ghiTen(khoa: String, caSi: String, tenBai: String) {
        val ban = docBangTen().toMutableMap()
        ban[khoa] = don(caSi) to don(tenBai)
        luuBangTen(ban)
    }

    @Synchronized
    private fun xoaTen(khoa: String) {
        val ban = docBangTen().toMutableMap()
        if (ban.remove(khoa) != null) luuBangTen(ban)
    }

    private fun luuBangTen(ban: Map<String, Pair<String, String>>) {
        runCatching {
            bangTen.writeText(ban.entries.joinToString("\n") { (k, v) ->
                "$k\t${v.first}\t${v.second}"
            })
        }
    }

    /**
     * Ten bai co the mang tab hoac xuong dong khi app nhac kia dat ten la.
     * Thay bang khoang trang: bang nay chia cot bang tab, mot tab lac cho lam
     * hong ca dong, con mot khoang trang thua thi khong ai thay.
     */
    private fun don(s: String) = s.replace('\t', ' ').replace('\n', ' ').trim()

    private companion object {
        const val TAG = "LyraManual"

        /** Bang ten, khong phai loi bai nao - moi cho duyet thu muc phai bo qua. */
        const val TEN_BANG = "ten.tsv"
    }
}
