package com.mittohoa.lyra.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.scale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lay mau chu dao cua anh bia, de ca man hinh doi mau theo bai dang phat.
 *
 * Chuyen tu `src/main/artwork.ts` cua ban Windows: gom cac diem anh vao o mau
 * tho, roi cham diem uu tien mau TUOI va khong qua toi/qua sang. Lay trung binh
 * ca anh thi luon ra mau bun.
 *
 * Thu nho ve 24x24 truoc khi doc diem anh. Anh bia goc co the 1000x1000 - doc
 * het la mot trieu diem anh cho moi lan doi bai, du 576 diem la thua de biet
 * mau chu dao. Day la khac biet giua "muot" va "khung hinh roi moi lan doi bai".
 *
 * GOI TREN LUONG NEN. Ham nay khong nang, nhung no khong bao gio duoc phep
 * chen vao luong ve hinh.
 */
fun dominantColor(bitmap: Bitmap?): Color? {
    if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return null

    val small = try {
        bitmap.scale(SAMPLE, SAMPLE, filter = true)
    } catch (e: Exception) {
        return null
    }

    val pixels = IntArray(SAMPLE * SAMPLE)
    try {
        small.getPixels(pixels, 0, SAMPLE, 0, 0, SAMPLE, SAMPLE)
    } catch (e: Exception) {
        return null
    } finally {
        if (small !== bitmap) small.recycle()
    }

    // O mau tho 32 muc moi kenh: gom cac sac do gan nhau lam mot
    val sums = HashMap<Int, IntArray>(64)

    for (pixel in pixels) {
        val a = (pixel ushr 24) and 0xFF
        if (a < 128) continue
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF

        val (_, s, l) = toHsl(r, g, b)
        if (l < 0.12f || l > 0.92f) continue  // gan den hoac gan trang
        if (s < 0.12f) continue               // gan nhu xam

        val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
        val acc = sums.getOrPut(key) { IntArray(4) }
        acc[0] += r; acc[1] += g; acc[2] += b; acc[3]++
    }

    if (sums.isEmpty()) return null

    var bestScore = -1f
    var best = Color(0xFF6B6B78)

    for (acc in sums.values) {
        val n = acc[3]
        val r = acc[0] / n
        val g = acc[1] / n
        val b = acc[2] / n
        val (_, s, l) = toHsl(r, g, b)

        // Nhieu diem anh thi diem cao, nhung mau tuoi duoc uu ai; phat mau qua
        // toi hoac qua sang vi con phai nhin duoc chu tren nen do
        val vividness = s * (1f - abs(l - 0.5f) * 1.4f)
        val score = sqrt(n.toFloat()) * (0.35f + vividness)
        if (score > bestScore) {
            bestScore = score
            best = Color(r / 255f, g / 255f, b / 255f)
        }
    }

    return best
}

/** RGB -> HSL, de danh gia do tuoi va do sang. */
private fun toHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rn = r / 255f
    val gn = g / 255f
    val bn = b / 255f
    val max = maxOf(rn, gn, bn)
    val min = minOf(rn, gn, bn)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)

    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        rn -> ((gn - bn) / d + (if (gn < bn) 6f else 0f)) / 6f
        gn -> ((bn - rn) / d + 2f) / 6f
        else -> ((rn - gn) / d + 4f) / 6f
    }
    return Triple(h * 360f, s, l)
}

private const val SAMPLE = 24
