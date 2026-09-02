package com.mittohoa.lyra.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import android.util.Log
import com.mittohoa.lyra.sources.Http
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Tai anh bia va nho lai.
 *
 * Khong dung thu vien anh: ca app chi tai MOT anh mot luc - anh bia cua bai
 * dang phat - va no duoc dung cho dung hai viec, hien len man hinh va lay mau
 * chu dao lam nen. Mot thu vien anh day du la de giai bai toan danh sach hang
 * tram anh cuon nhanh, khong phai bai toan nay.
 *
 * Anh duoc THU NHO ngay luc doc. Bia goc cua Zing/NCT co the toi 1000 diem anh
 * moi chieu; giu nguyen co la giu mot mieng bo nho lon gap hai muoi lan cai ta
 * that su ve ra man hinh, va khien phep do mau cham han.
 */
object Artwork {

    /**
     * Nho vai anh gan nhat. Nho theo dia chi chu khong theo bai: hai bai cung
     * mot album dung chung mot anh, va doi qua lai giua chung la chuyen thuong.
     */
    private val cache = object : LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?) =
            size > MAX_ENTRIES
    }

    /**
     * Tai anh bia tu mot dia chi bat ky.
     *
     * Hai loai dia chi di hai duong khac han: bia cua nhac online la `https`
     * lay qua mang, con bia cua nhac trong may la `content://` lay qua
     * `ContentResolver`. Ben goi khong phai biet khac biet do.
     */
    suspend fun load(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        synchronized(cache) { cache[url] }?.let { return it }

        if (url.startsWith("content://") || url.startsWith("file://")) {
            return loadLocal(context, url)
        }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = Http.client
                    .newCall(Request.Builder().url(url).build())
                    .execute()
                    .use { res ->
                        if (!res.isSuccessful) return@use null
                        val bytes = res.body?.bytes() ?: return@use null
                        decodeScaled(bytes)
                    }
                if (bitmap != null) synchronized(cache) { cache[url] = bitmap }
                bitmap
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Khong tai duoc anh bia", e)
                null
            }
        }
    }

    private suspend fun loadLocal(context: Context, url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(url)
                val bitmap =
                    if (laVideo(url)) khungHinh(context, uri)
                    else context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?.let(::decodeScaled)
                if (bitmap != null) synchronized(cache) { cache[url] = bitmap }
                bitmap
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Album khong co bia la chuyen rat thuong, khong phai loi
                null
            }
        }

    /**
     * Dia chi nay tro toi mot video hay mot tam anh?
     *
     * Nhin duong dan chu khong doan: dia chi video cua MediaStore luon nam duoi
     * `.../video/media/<ma>`. Doan bang cach thu giai ma roi bat truot thi moi
     * bia album hong cung bi coi la video, va moi video se ton mot lan giai ma
     * that bai truoc khi di dung duong.
     */
    private fun laVideo(url: String) = url.contains("/video/")

    /**
     * Mot khung hinh lam anh dai dien cho video.
     *
     * Tu Android 10 co `loadThumbnail`: he thong da dung san anh thu nho khi
     * quet thu vien, lay ra gan nhu tuc thi. Doi may cu hon thi phai tu rut mot
     * khung - cham hon nhieu, nen chi lam khi khong con duong nao.
     *
     * Rut khung o giay thu MOT chu khong phai giay khong: rat nhieu video mo
     * dau bang mot khung den, va mot o den thi khong noi len duoc video nao la
     * video nao.
     */
    private fun khungHinh(context: Context, uri: android.net.Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
        } else {
            // KHONG dung `use`: `MediaMetadataRetriever` chi thanh
            // `AutoCloseable` tu Android 10, ma nhanh nay chay tren may CU HON
            // the. Bien dich thi qua vi bien dich theo SDK moi, con chay tren
            // may that thi no nem `NoSuchMethodError` - dung tren nhung may
            // duy nhat can toi nhanh nay.
            val may = MediaMetadataRetriever()
            try {
                may.setDataSource(context, uri)
                may.getFrameAtTime(1_000_000L)
            } finally {
                may.release()
            }
        }
    } catch (e: Exception) {
        Log.d(TAG, "khong rut duoc khung hinh video", e)
        null
    }

    /**
     * Doc anh o co vua du dung.
     *
     * Doc hai lan: lan dau chi doc kich thuoc (`inJustDecodeBounds`), lan sau
     * moi doc that voi ti le thu nho da tinh. Doc thang roi thu nho sau nghia la
     * co luc anh goc nam tron trong bo nho - va do la luc de het bo nho nhat.
     */
    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        var widest = maxOf(bounds.outWidth, bounds.outHeight)
        while (widest / 2 >= TARGET_PX) {
            widest /= 2
            sample *= 2
        }

        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }

    private const val TAG = "AuraBia"
    private const val MAX_ENTRIES = 8

    /** Be rong dich, tinh bang diem anh. Du de ve to va du nho de do mau nhanh. */
    private const val TARGET_PX = 400
}
