package com.mittohoa.lyra.sources

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cac phep bam va ma hoa mà hai nguon nhac Viet Nam doi hoi.
 *
 * Zing ky yeu cau bang HMAC-SHA512 tren mot chuoi co SHA-256 long trong;
 * NhacCuaTui ma hoa file .lrc bang RC4.
 */

private val HEX = "0123456789abcdef".toCharArray()

fun ByteArray.toHex(): String {
    val out = CharArray(size * 2)
    for (i in indices) {
        val v = this[i].toInt() and 0xFF
        out[i * 2] = HEX[v ushr 4]
        out[i * 2 + 1] = HEX[v and 0x0F]
    }
    return String(out)
}

/** Doc chuoi hex thanh byte. Tra ve null neu chuoi khong hop le. */
fun String.hexToBytesOrNull(): ByteArray? {
    if (length % 2 != 0 || isEmpty()) return null
    val out = ByteArray(length / 2)
    for (i in out.indices) {
        val hi = Character.digit(this[i * 2], 16)
        val lo = Character.digit(this[i * 2 + 1], 16)
        if (hi < 0 || lo < 0) return null
        out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
}

fun sha256Hex(input: String): String =
    MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).toHex()

fun hmacSha512Hex(input: String, key: String): String {
    val mac = Mac.getInstance("HmacSHA512")
    mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA512"))
    return mac.doFinal(input.toByteArray()).toHex()
}

/**
 * RC4, viet tay.
 *
 * Khong dung `Cipher.getInstance("ARCFOUR")`: thuat toan nay da bi coi la khong
 * an toan nen mot so ban Android khong con kem no, va luc do lop tren chi nhan
 * duoc `NoSuchAlgorithmException` giua chung. Hai muoi dong nay chay o dau cung
 * duoc.
 *
 * (O day RC4 khong dung de bao mat gi ca - chi de doc lai file .lrc ma
 * NhacCuaTui da xao tron, va khoa thi ho gui kem ngay trong ban tin.)
 */
fun rc4(key: ByteArray, data: ByteArray): ByteArray {
    val s = IntArray(256) { it }

    var j = 0
    for (i in 0 until 256) {
        j = (j + s[i] + (key[i % key.size].toInt() and 0xFF)) and 255
        val t = s[i]; s[i] = s[j]; s[j] = t
    }

    val out = ByteArray(data.size)
    var i = 0
    j = 0
    for (k in data.indices) {
        i = (i + 1) and 255
        j = (j + s[i]) and 255
        val t = s[i]; s[i] = s[j]; s[j] = t
        out[k] = (data[k].toInt() xor s[(s[i] + s[j]) and 255]).toByte()
    }
    return out
}
