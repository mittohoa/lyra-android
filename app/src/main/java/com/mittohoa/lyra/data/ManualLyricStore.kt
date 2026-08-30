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
            val file = File(dir, keyOf(artist, title))
            if (raw.isBlank()) file.delete() else file.writeText(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Khong ghi duoc loi tu nhap", e)
        }
    }

    fun remove(artist: String, title: String) {
        runCatching { File(dir, keyOf(artist, title)).delete() }
    }

    private companion object {
        const val TAG = "LyraManual"
    }
}
