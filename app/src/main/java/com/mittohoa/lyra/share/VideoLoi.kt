package com.mittohoa.lyra.share

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Dựng một đoạn video ngắn từ mấy câu hát.
 *
 * Mỗi câu là một tấm thẻ y hệt thẻ ảnh — dùng lại `TheLoi.ve`, nên video và ảnh
 * tĩnh ra cùng một dáng, không phải nuôi hai bộ vẽ.
 *
 * KHÔNG CÓ TIẾNG. Không phải vì khó ghép, mà vì đây là đoạn đem đăng: mọi nền
 * tảng đều cho người ta tự chọn nhạc nền, và một đoạn câm thì họ ghép được bài
 * họ muốn. Ghép sẵn tiếng vào chỉ tổ làm hỏng việc đó.
 *
 * VÌ SAO PHẢI TỰ MÃ HOÁ: Android không có API nào nhận một dãy ảnh rồi trả về
 * mp4. Chỉ còn `MediaCodec` — và ở đó có hai đường:
 *
 *   Surface + OpenGL   nhanh, nhưng kéo theo vài trăm dòng dựng EGL
 *   Bộ đệm YUV         tốn CPU hơn, nhưng đọc được và lần lỗi được
 *
 * Chọn đường thứ hai. Cái giá của nó nhỏ hơn tưởng nhiều, vì MỖI CÂU chỉ phải
 * đổi màu một lần: các khung trong cùng một câu là ảnh y hệt nhau, nạp lại đúng
 * bộ đệm ấy. Một đoạn tám giây chỉ có chừng bốn lần đổi màu chứ không phải hai
 * trăm.
 *
 * Khung nhỏ hơn thẻ ảnh (720×900 thay vì 1080×1350): phép đổi RGB sang YUV chạy
 * trên CPU nên tốn theo số điểm ảnh, mà video đăng lên mạng xã hội thì bị nén
 * lại nữa — giữ nguyên cỡ thẻ chỉ tốn thời gian dựng chứ không ai thấy đẹp hơn.
 */
object VideoLoi {

    /** 4:5 như thẻ ảnh, nhưng nhỏ hơn. Phải chia hết cho 2 vì H.264 đòi thế. */
    const val RONG = 720
    const val CAO = 900

    private const val KHUNG_MOI_GIAY = 24
    private const val GIAY_MOI_CAU = 2.6f

    /**
     * Dựng video rồi trả về tệp đã ghi xong.
     *
     * Chạy trên luồng nền: dựng vài trăm khung là việc của CPU, làm trên luồng
     * vẽ thì cả app đứng im mấy giây mà không có gì báo.
     *
     * `onTienDo` nhận phần trăm 0..100 để màn hình vẽ được vạch chạy — dựng
     * video mất vài giây, im lặng suốt lúc đó thì người dùng tưởng app treo.
     */
    suspend fun dung(
        context: Context,
        cacCau: List<String>,
        tenBai: String,
        caSi: String,
        mauNhan: Int,
        laGiay: Boolean,
        kieuChu: com.mittohoa.lyra.data.KieuChu,
        mau: MauThe,
        bia: Bitmap?,
        onTienDo: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.Default) {
        require(cacCau.isNotEmpty()) { "Không có câu nào để dựng" }

        val thuMuc = File(context.cacheDir, "the-loi").apply { mkdirs() }
        val dich = File(thuMuc, "lyra-loi.mp4")
        if (dich.exists()) dich.delete()

        val dinhDang = MediaFormat.createVideoFormat(LOAI, RONG, CAO).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, KHUNG_MOI_GIAY)
            // Mỗi giây một khung khoá. Đoạn này ngắn và người ta hay tua tay
            // trong trình xem, mà khung khoá thưa thì tua giật về đầu đoạn.
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val may = MediaCodec.createEncoderByType(LOAI)
        val hop = MediaMuxer(dich.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var rachHop = -1
        var hopDangChay = false

        try {
            may.configure(dinhDang, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            may.start()

            val soKhungMoiCau = (KHUNG_MOI_GIAY * GIAY_MOI_CAU).toInt().coerceAtLeast(1)
            val tongKhung = soKhungMoiCau * cacCau.size
            val thongTin = MediaCodec.BufferInfo()
            var soKhungDaNap = 0

            for ((thuTu, cau) in cacCau.withIndex()) {
                // Vẽ MỘT lần cho cả câu, đổi màu MỘT lần, rồi nạp lại đúng bộ
                // đệm ấy cho mọi khung của câu này.
                val anh = TheLoi.ve(
                    context = context,
                    cauHat = cau,
                    tenBai = tenBai,
                    caSi = caSi,
                    mauNhan = mauNhan,
                    laGiay = laGiay,
                    kieuChu = kieuChu,
                    mau = mau,
                    bia = bia
                ).scale(RONG, CAO, filter = true)

                val yuv = sangI420(anh)
                anh.recycle()

                repeat(soKhungMoiCau) {
                    napMotKhung(may, yuv, soKhungDaNap)
                    soKhungDaNap++
                    rachHop = rutRaHop(may, hop, thongTin, rachHop) { hopDangChay = true }
                    onTienDo(soKhungDaNap * 100 / tongKhung)
                }
                Log.i(TAG, "Xong cau ${thuTu + 1}/${cacCau.size}")
            }

            // Báo hết đầu vào rồi vét nốt phần máy còn giữ trong bụng.
            val chiSo = may.dequeueInputBuffer(CHO_LAU)
            if (chiSo >= 0) {
                may.queueInputBuffer(
                    chiSo, 0, 0, thoiDiem(soKhungDaNap), MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            rachHop = rutRaHop(may, hop, thongTin, rachHop, vetHet = true) { hopDangChay = true }
        } finally {
            runCatching { may.stop() }
            runCatching { may.release() }
            // CHỈ dừng hộp khi nó đã chạy. Gọi `stop` trên hộp chưa `start` là
            // ném IllegalStateException, và cái ném ấy che mất lỗi thật đã làm
            // ta rơi vào đây.
            if (hopDangChay) runCatching { hop.stop() }
            runCatching { hop.release() }
        }

        onTienDo(100)
        Log.i(TAG, "Da dung video: ${dich.length()} byte")
        dich
    }

    private fun thoiDiem(soKhung: Int): Long = soKhung * 1_000_000L / KHUNG_MOI_GIAY

    private fun napMotKhung(may: MediaCodec, yuv: ByteArray, soKhung: Int) {
        val chiSo = may.dequeueInputBuffer(CHO_LAU)
        if (chiSo < 0) return
        val bo = may.getInputBuffer(chiSo) ?: return
        bo.clear()
        bo.put(yuv)
        may.queueInputBuffer(chiSo, 0, yuv.size, thoiDiem(soKhung), 0)
    }

    /**
     * Vét phần đã mã hoá ra và đẩy vào hộp mp4.
     *
     * Rãnh trong hộp chỉ mở được SAU khi máy mã hoá báo định dạng thật của nó —
     * mở sớm bằng định dạng ta tự khai thì tệp ra thiếu phần mô tả và không
     * trình xem nào mở được.
     */
    private inline fun rutRaHop(
        may: MediaCodec,
        hop: MediaMuxer,
        thongTin: MediaCodec.BufferInfo,
        rachHienTai: Int,
        vetHet: Boolean = false,
        danhDauDaChay: () -> Unit
    ): Int {
        var rach = rachHienTai
        while (true) {
            val chiSo = may.dequeueOutputBuffer(thongTin, if (vetHet) CHO_LAU else 0)
            when {
                chiSo == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!vetHet) return rach else continue
                chiSo == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    rach = hop.addTrack(may.outputFormat)
                    hop.start()
                    danhDauDaChay()
                }
                chiSo >= 0 -> {
                    val bo = may.getOutputBuffer(chiSo)
                    // Khung cấu hình (SPS/PPS) đã nằm trong `outputFormat` rồi;
                    // ghi thêm lần nữa là thừa và vài trình xem sẽ vấp.
                    if (thongTin.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        thongTin.size = 0
                    }
                    if (bo != null && thongTin.size > 0 && rach >= 0) {
                        bo.position(thongTin.offset)
                        bo.limit(thongTin.offset + thongTin.size)
                        hop.writeSampleData(rach, bo, thongTin)
                    }
                    may.releaseOutputBuffer(chiSo, false)
                    if (thongTin.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return rach
                }
            }
        }
    }

    /**
     * Đổi ảnh ARGB sang I420 — ba mặt phẳng Y, U, V nối nhau.
     *
     * `COLOR_FormatYUV420Flexible` nhận I420 ở gần như mọi máy. Viết tay chứ
     * không mượn thư viện: phép đổi này chỉ là mấy dòng số học, mà kéo về một
     * gói ngoài chỉ vì nó thì đắt hơn nhiều so với cái nó thay.
     *
     * Lấy mẫu màu theo khối 2×2 — đó là ý nghĩa của "420". Ảnh phải có cạnh
     * chẵn, và `RONG`/`CAO` ở trên đã chẵn sẵn.
     */
    private fun sangI420(anh: Bitmap): ByteArray {
        val w = anh.width
        val h = anh.height
        val diem = IntArray(w * h)
        anh.getPixels(diem, 0, w, 0, 0, w, h)

        val coY = w * h
        val coUV = coY / 4
        val ra = ByteArray(coY + coUV * 2)
        var viTriU = coY
        var viTriV = coY + coUV

        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = diem[y * w + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                ra[y * w + x] = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16)
                    .coerceIn(0, 255).toByte()

                // Chỉ lấy một mẫu màu cho mỗi khối 2×2, đọc ở góc trên trái.
                if (y % 2 == 0 && x % 2 == 0) {
                    ra[viTriU++] = ((((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128))
                        .coerceIn(0, 255).toByte()
                    ra[viTriV++] = ((((112 * r - 94 * g - 18 * b + 128) shr 8) + 128))
                        .coerceIn(0, 255).toByte()
                }
            }
        }
        return ra
    }

    private const val LOAI = "video/avc"
    private const val CHO_LAU = 10_000L
    private const val TAG = "AuraVideoLoi"
}
