package com.mittohoa.lyra.data

import android.content.Context
import com.mittohoa.lyra.lyrics.normalizeForCompare

/**
 * Nho do lech nguoi dung da chinh, theo tung bai.
 *
 * Chinh mot lan roi lan sau nghe lai bai do la dung luon. Khong nho thi moi
 * lan mo lai deu phai chinh lai tu dau - va do la loai phien toai khien nguoi
 * ta bo han tinh nang.
 *
 * Dung `SharedPreferences` chu khong dung file nhu bo nho dem loi: o day moi
 * ban ghi chi la mot so, va can doc duoc ngay lap tuc luc dang dung giao dien.
 */
class OffsetStore(context: Context) {

    private val prefs = context.getSharedPreferences("lyric-offsets", Context.MODE_PRIVATE)

    /** Khoa bo dau, de hai app viet ten khac nhau van tro toi mot cho. */
    private fun keyOf(artist: String, title: String) =
        "${normalizeForCompare(artist)}|${normalizeForCompare(title)}"

    fun get(artist: String, title: String): Long =
        prefs.getLong(keyOf(artist, title), 0L)

    fun put(artist: String, title: String, offsetMs: Long) {
        val key = keyOf(artist, title)
        // Do lech 0 la trang thai mac dinh - xoa han thay vi ghi mot so 0
        if (offsetMs == 0L) prefs.edit().remove(key).apply()
        else prefs.edit().putLong(key, offsetMs).apply()
    }
}
