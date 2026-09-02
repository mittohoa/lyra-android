package com.mittohoa.lyra.lyrics

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.mittohoa.lyra.BuildConfig
import java.io.File

/**
 * Lời nằm ngay cạnh tệp nhạc.
 *
 * Ai để nhạc trong máy lâu năm thì gần như chắc chắn có sẵn một mớ `.lrc` nằm
 * cạnh từng tệp — đó là cách cả thế giới lưu lời bài hát suốt hai chục năm nay,
 * và mọi trình phát trên máy tính đều đọc kiểu đó. Không đọc thì AURA bắt người
 * ta gõ lại thứ họ đã có sẵn trên máy, hoặc tệ hơn, đi tra mạng để lấy về đúng
 * cái đang nằm trong cùng thư mục.
 *
 * Đây là NGUỒN ĐÁNG TIN gần bằng lời tự nhập: người dùng tự để tệp đó ở đấy.
 * Nên nó đứng trên bộ nhớ đệm và trên mọi nguồn mạng, chỉ dưới lời tự nhập.
 *
 * ĐỌC THẲNG BẰNG `File`, KHÔNG QUA MediaStore. Đo trên Android 17 (Pixel 6 Pro,
 * chỉ có quyền `READ_MEDIA_AUDIO`):
 *
 *     MediaStore biết bao nhiêu tệp .lrc      0
 *     đọc thẳng /storage/emulated/0/Music/    được
 *
 * MediaStore chỉ đánh chỉ mục tệp phương tiện; `.lrc` là tệp chữ nên nó không
 * bao giờ thấy. Không có đường nào khác ngoài đọc thẳng — trừ khi bắt người
 * dùng chỉ thư mục qua bộ chọn, việc mà họ không hiểu tại sao phải làm khi
 * "app đã thấy bài hát rồi mà".
 *
 * Đọc hụt thì im lặng trả `null`: không có tệp `.lrc` là chuyện thường, và
 * chuyện thường thì không được phép làm phiền ai.
 */
object LrcCanhTep {

    /**
     * Ten nguon ghi tren dai bao, va cung la dau de biet loi dang hien VUA doc
     * len tu tep nao do. Mot hang chu khong go tay o tung noi: lech mot chu thi
     * AURA moi nguoi ghi lai dung cai no vua doc ra, ma khong ai thay sai o dau.
     */
    const val NGUON = "tệp cạnh nhạc"

    /** Nguon rieng cho phu de: nguoi dung can biet no la .srt chu khong phai .lrc. */
    const val NGUON_PHU_DE = "phụ đề cạnh video"

    /** Loi hay phu de nay co phai vua doc len tu mot tep nam canh khong. */
    fun laTepCanh(from: String) = from == NGUON || from == NGUON_PHU_DE

    /**
     * Tìm lời cho tệp phương tiện đang phát, theo `uri` của nó.
     *
     * Nhận cả `content://media/...` lẫn `file://`. Trả về chuỗi thô đúng như
     * trong tệp — việc đọc thành dòng là của `parseLrc`, chạy lại mỗi lần.
     */
    fun doc(context: Context, uri: String): Lyrics? {
        val duong = duongDan(context, uri) ?: return null
        return docCanh(duong)
    }

    /**
     * Đường dẫn thật của một tệp phương tiện, hoặc `null` khi hỏi không ra.
     *
     * Hỏi bảng `Files` chứ không hỏi riêng bảng `Audio`: bảng này phủ cả nhạc
     * lẫn video, nên khi AURA phát được video thì chỗ này không phải sửa lại.
     */
    private fun duongDan(context: Context, uri: String): String? = try {
        val u = Uri.parse(uri)
        when {
            u.scheme == "file" -> u.path

            // Hang doi cua AURA mang dia chi rieng `lyra://<nguon>/<ma>` chu
            // khong mang duong that - xem `StreamResolver`. Voi nguon "may"
            // thi <ma> chinh la ma MediaStore.
            u.scheme == "lyra" && u.host == NGUON_TRONG_MAY ->
                u.lastPathSegment?.toLongOrNull()?.let { hoiDuong(context, it) }

            u.scheme == "content" -> hoiDuong(context, ContentUris.parseId(u))

            // Nguon mang: khong co tep nao tren dia de tim canh ca.
            else -> null
        }
    } catch (e: Exception) {
        Log.d(TAG, "khong hoi duoc duong dan", e)
        null
    }

    /**
     * Hoi bang `Files` chu khong hoi rieng bang `Audio`: bang nay phu ca nhac
     * lan video, nen khi AURA phat duoc video thi cho nay khong phai sua lai.
     */
    private fun hoiDuong(context: Context, id: Long): String? =
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.Files.FileColumns.DATA),
            "${MediaStore.Files.FileColumns._ID} = ?",
            arrayOf(id.toString()),
            null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }

    /**
     * Đọc tệp lời nằm cạnh một tệp phương tiện.
     *
     * Thử hai kiểu tên, vì hai kiểu này đều đang tồn tại ngoài đời:
     *
     *     bài hát.mp3  ->  bài hát.lrc       (thay đuôi - phổ biến nhất)
     *     bài hát.mp3  ->  bài hát.mp3.lrc   (nối thêm đuôi)
     *
     * và với mỗi kiểu thì thử cả `.lrc` lẫn `.LRC`: Windows không phân biệt hoa
     * thường nên tệp chép từ máy tính sang rất hay mang đuôi viết hoa, còn
     * Android thì phân biệt.
     */
    fun docCanh(duongNhac: String): Lyrics? {
        val khongDuoi = duongNhac.substringBeforeLast('.')
        // .lrc truoc .srt: mot tep co ca hai thi ban .lrc gan nhu chac chan la
        // loi bai hat co nguoi cham vao, con .srt thuong la phu de tai kem.
        for (ten in listOf(
            "$khongDuoi.lrc", "$khongDuoi.LRC",
            "$duongNhac.lrc", "$duongNhac.LRC",
            "$khongDuoi.srt", "$khongDuoi.SRT",
            "$duongNhac.srt", "$duongNhac.SRT"
        )) {
            val chu = try {
                File(ten).takeIf { it.isFile }?.readText()
            } catch (e: Exception) {
                // Không đọc được là chuyện bình thường trên Android hiện đại:
                // tệp nằm ngoài vùng app được phép đọc. Ghi lại một dòng để còn
                // lần ra khi có người báo "máy tôi không thấy lời", rồi đi tiếp.
                Log.d(TAG, "khong doc duoc $ten", e)
                null
            }
            if (chu.isNullOrBlank()) continue

            val laSrt = ten.endsWith(".srt", ignoreCase = true)
            val doc =
                if (laSrt) parseSrt(chu).copy(from = NGUON_PHU_DE)
                else parseLrc(chu, from = NGUON)
            if (doc.lines.isEmpty()) {
                // Tep co chu ma doc ra khong duoc cau nao: dang tep khac han,
                // hoac hong. Di tiep chu dung nhan mot ban loi rong roi thoi -
                // nhan roi thi khong con di tim nguon nao khac nua.
                Log.i(TAG, "tep khong doc ra cau nao, bo qua: $ten")
                continue
            }
            Log.i(TAG, "doc duoc " + doc.lines.size + " cau tu $ten")
            return doc
        }
        return null
    }


    // ---- Ghi ra ----

    /** Kết quả một lần ghi, để màn hình nói đúng chuyện đã xảy ra. */
    sealed interface KetQuaGhi {
        data class Xong(val duong: String) : KetQuaGhi
        /** Đã có tệp sẵn ở đó — hỏi lại rồi mới đè. */
        data class DaCoTep(val duong: String) : KetQuaGhi
        /** Bài đang phát không phải tệp trong máy, không có chỗ nào để ghi cạnh. */
        data object KhongPhaiTepTrongMay : KetQuaGhi
        data class Hong(val lyDo: String) : KetQuaGhi
    }

    /**
     * Ghi lời ra tệp .lrc nằm cạnh tệp nhạc.
     *
     * Đo trên Pixel 6 Pro / Android 17, app chỉ có `READ_MEDIA_AUDIO`:
     *
     *     tạo tệp .lrc MỚI, ghi thẳng      được, ở mọi thư mục thử
     *     nhờ MediaStore tạo hộ trong Music/  bị từ chối
     *     ghi đè tệp .lrc DO APP KHÁC TẠO  bị chặn, FileNotFoundException
     *
     * Dòng cuối là luật của Android từ bản 11: một app chỉ sửa được tệp do
     * chính nó tạo. Nên tệp AURA tự ghi ra thì lần sau sửa lại được, còn tệp
     * người dùng chép từ máy tính sang thì không — và chỗ đó phải nói thật chứ
     * không được im lặng coi như xong.
     *
     * KHÔNG TỰ ĐÈ tệp có sẵn dù có quyền: một tệp .lrc nằm sẵn ở đó là công của
     * ai đó, có thể là công của chính người dùng gõ trên máy tính. Hỏi lại một
     * câu rẻ hơn nhiều so với làm mất nó.
     */
    fun ghi(
        context: Context,
        uri: String,
        loi: String,
        tenBai: String,
        caSi: String,
        deLen: Boolean = false
    ): KetQuaGhi {
        if (loi.isBlank()) return KetQuaGhi.Hong("chưa có lời nào để ghi")
        val duongNhac = duongDan(context, uri) ?: return KetQuaGhi.KhongPhaiTepTrongMay
        val dich = File(duongNhac.substringBeforeLast('.') + ".lrc")
        if (dich.exists() && !deLen) return KetQuaGhi.DaCoTep(dich.path)

        return try {
            dich.writeText(dungNoiDung(loi, tenBai, caSi))
            Log.i(TAG, "da ghi loi ra " + dich.path)
            KetQuaGhi.Xong(dich.path)
        } catch (e: Exception) {
            Log.i(TAG, "khong ghi duoc " + dich.path, e)
            KetQuaGhi.Hong(
                if (dich.exists())
                    "Android không cho ghi đè tệp do app khác tạo ra"
                else
                    (e.message ?: "lỗi không rõ")
            )
        }
    }

    /**
     * Thân tệp .lrc: mấy thẻ mô tả rồi tới lời.
     *
     * Thẻ `[ti:]` và `[ar:]` là chuẩn .lrc có từ lâu, trình phát nào cũng đọc —
     * và nhờ nó mà tệp tách khỏi bài nhạc vẫn còn biết mình là bài gì. `[re:]`
     * ghi tên thứ đã tạo ra tệp, cũng là chuẩn cũ; đây là chỗ tên AURA nằm lại
     * trong một tệp người dùng đem đi đâu cũng được.
     *
     * Bộ đọc của AURA bỏ qua mọi dòng dạng `[chữ:...]` nên ghi ra rồi đọc lại
     * không sinh thêm câu hát ma nào.
     */
    // `internal` chu khong `private`: day la thu duy nhat trong lop nay kiem
    // duoc bang may ma khong can dien thoai, va no cung la thu de sai nhat -
    // mot the dat sai cho lam ca tep .lrc hong ma ghi ra van "thanh cong".
    internal fun dungNoiDung(loi: String, tenBai: String, caSi: String): String =
        buildString {
            if (tenBai.isNotBlank()) append("[ti:").append(don(tenBai)).append("]\n")
            if (caSi.isNotBlank()) append("[ar:").append(don(caSi)).append("]\n")
            append("[re:AURA by #mittoHOA]\n")
            append("[ve:").append(BuildConfig.VERSION_NAME).append("]\n")
            append(loi.trimEnd('\n')).append('\n')
        }

    /** Dấu `]` trong tên bài sẽ cắt cụt thẻ, nên bỏ đi. */
    private fun don(s: String) = s.replace(']', ')').replace('\n', ' ').trim()

    /** Khop voi `MusicSource.LOCAL.key`. */
    private const val NGUON_TRONG_MAY = "may"

    private const val TAG = "AuraLrcCanhTep"
}
