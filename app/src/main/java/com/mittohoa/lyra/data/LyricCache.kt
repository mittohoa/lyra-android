package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import com.mittohoa.lyra.lyrics.Lyrics
import com.mittohoa.lyra.lyrics.LyricLine
import com.mittohoa.lyra.lyrics.normalizeForCompare
import java.io.File
import java.security.MessageDigest

/**
 * Nho lai loi da tim duoc.
 *
 * Nghe lai mot bai la chuyen rat thuong. Khong nho thi lan nao cung phai goi
 * mang lai tu dau, va nguoi dung ngoi nhin mot khung trong vai giay du may da
 * biet cau tra loi tu lan truoc.
 *
 * Luu thanh file thay vi dung co so du lieu: moi ban ghi vai KB, doc mot lan
 * roi thoi, khong bao gio phai tim kiem hay noi bang. Room o day chi la them
 * mot tang phu thuoc de lam dung viec `File.readText` da lam xong.
 *
 * Bo nho RAM dat truoc file: doi qua lai giua hai bai thi khong cham vao dia
 * lan nao ca.
 */
class LyricCache(context: Context) {

    private val dir = File(context.cacheDir, "lyrics").apply { mkdirs() }
    private val memory = object : LinkedHashMap<String, Lyrics>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Lyrics>?) =
            size > MEMORY_ENTRIES
    }

    /**
     * Khoa theo ten da bo dau va bo ky tu la.
     *
     * Nho vay "Sơn Tùng M-TP" va "SON TUNG M-TP" cung tro toi mot cho - hai app
     * khac nhau viet ten mot kieu, ma ta thi muon nho mot lan dung cho ca hai.
     */
    private fun keyOf(artist: String, title: String): String {
        val raw = "${normalizeForCompare(artist)}|${normalizeForCompare(title)}"
        val hash = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun get(artist: String, title: String): Lyrics? {
        val key = keyOf(artist, title)
        memory[key]?.let { return it }

        val file = File(dir, key)
        if (!file.exists()) return null

        return try {
            val lyrics = decode(file.readText())
            if (lyrics != null) memory[key] = lyrics
            lyrics
        } catch (e: Exception) {
            Log.d(TAG, "Ban ghi hong, bo di", e)
            file.delete()
            null
        }
    }

    @Synchronized
    fun put(artist: String, title: String, lyrics: Lyrics) {
        if (lyrics.isEmpty) return
        val key = keyOf(artist, title)
        memory[key] = lyrics
        try {
            File(dir, key).writeText(encode(lyrics))
            trimIfNeeded()
        } catch (e: Exception) {
            // Het cho, khong co quyen ghi - mat bo nho dem thi cham hon chu
            // khong hong gi, nen nuot loi o day la dung
            Log.d(TAG, "Khong ghi duoc bo nho dem", e)
        }
    }

    /**
     * Dinh dang tu dat, khong dung JSON.
     *
     * Dong dau la phan mo ta, cac dong sau moi dong mot cau loi kem moc. Chinh
     * la .lrc hoi khac di - doc bang `split` la xong, khong can bo phan tich
     * nao, va mo file ra bang tay van doc duoc.
     */
    private fun encode(lyrics: Lyrics): String = buildString {
        append(lyrics.from).append('\t')
        append(if (lyrics.synced) 1 else 0).append('\t')
        append(lyrics.offset).append('\t')
        append(lyrics.matchedArtist).append('\t')
        append(lyrics.matchedTitle).append('\n')
        for (line in lyrics.lines) {
            append(line.time).append('\t').append(line.text.replace('\n', ' ')).append('\n')
        }
    }

    private fun decode(raw: String): Lyrics? {
        val rows = raw.split('\n')
        if (rows.isEmpty()) return null

        val head = rows[0].split('\t')
        if (head.size < 5) return null

        val lines = rows.drop(1).mapNotNull { row ->
            if (row.isEmpty()) return@mapNotNull null
            val tab = row.indexOf('\t')
            if (tab < 0) return@mapNotNull null
            val time = row.substring(0, tab).toLongOrNull() ?: return@mapNotNull null
            LyricLine(time, row.substring(tab + 1))
        }
        if (lines.isEmpty()) return null

        return Lyrics(
            lines = lines,
            synced = head[1] == "1",
            from = head[0],
            matchedArtist = head[3],
            matchedTitle = head[4],
            offset = head[2].toLongOrNull() ?: 0
        )
    }

    /** Xoa bot ban ghi cu nhat khi qua nhieu file. */
    private fun trimIfNeeded() {
        val files = dir.listFiles() ?: return
        if (files.size <= DISK_ENTRIES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - DISK_ENTRIES)
            .forEach { it.delete() }
    }

    private companion object {
        const val TAG = "AuraCache"
        const val MEMORY_ENTRIES = 24
        const val DISK_ENTRIES = 400
    }
}
