package com.mittohoa.lyra.lyrics

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.mittohoa.lyra.BuildConfig
import com.mittohoa.lyra.sources.ThuVienNgoai
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

    /**
     * Loi nay co phai vua doc len tu mot tep NAM CANH khong.
     *
     * Dung de an loi moi ghi ra .lrc: ghi lai dung cai vua doc len tu chinh tep
     * do la mot nut bam xong khong doi gi.
     *
     * KHONG tinh loi doc tu THE TRONG TEP vao day. Nhin thi giong - deu la "loi
     * da co san trong may" - nhung ghi ra .lrc luc do KHONG thua: the nam trong
     * tep nhac thi chi app nao chiu doc the moi thay, con tep .lrc nam canh thi
     * moi trinh phat deu doc duoc. Do la mot lan doi dinh dang that su.
     */
    fun laTepCanh(from: String) = from == NGUON || from == NGUON_PHU_DE

    /**
     * Tìm lời cho tệp phương tiện đang phát, theo `uri` của nó.
     *
     * Nhận cả `content://media/...` lẫn `file://`. Trả về chuỗi thô đúng như
     * trong tệp — việc đọc thành dòng là của `parseLrc`, chạy lại mỗi lần.
     */
    fun doc(context: Context, uri: String): Lyrics? {
        // Bai den tu thu muc nguoi dung tu tro vao khong co duong dan tren dia
        // nao ca - chi co dia chi tai lieu. Phai tach ra mot duong rieng, vi
        // `File` khong mo duoc dia chi do.
        ThuVienNgoai.diaChiTuMa(uri)?.let { return docCanhTaiLieu(context, it) }

        val duong = duongDan(context, uri) ?: return null
        // Tep nam CANH di truoc the nam TRONG: nguoi dung tu dat tep .lrc do o
        // day, con the trong tep la thu di kem san tu luc tai ve. Cai nguoi ta
        // tu tay lam thi dang tin hon.
        return docCanh(duong) ?: LoiTrongTep.doc(duong)
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
        for (ten in tenLoiUngVien(duongNhac)) {
            val chu = try {
                File(ten).takeIf { it.isFile }?.readText()
            } catch (e: Exception) {
                // Không đọc được là chuyện bình thường trên Android hiện đại:
                // tệp nằm ngoài vùng app được phép đọc. Ghi lại một dòng để còn
                // lần ra khi có người báo "máy tôi không thấy lời", rồi đi tiếp.
                Log.d(TAG, "khong doc duoc $ten", e)
                null
            }
            val doc = dungLoi(chu, ten) ?: continue
            Log.i(TAG, "doc duoc " + doc.lines.size + " cau tu $ten")
            return doc
        }
        return null
    }

    /**
     * Cac ten tep loi co the nam canh mot tep nhac, theo thu tu uu tien.
     *
     * Hai kieu dat ten, vi ca hai deu dang ton tai ngoai doi:
     *
     *     bai hat.mp3  ->  bai hat.lrc       (thay duoi - pho bien nhat)
     *     bai hat.mp3  ->  bai hat.mp3.lrc   (noi them duoi)
     *
     * Moi kieu thu ca chu thuong lan chu hoa: Windows khong phan biet nen tep
     * chep tu may tinh sang rat hay mang duoi viet hoa, con Android thi phan
     * biet.
     *
     * `.lrc` truoc `.srt`: mot tep co ca hai thi ban `.lrc` gan nhu chac chan
     * la loi bai hat co nguoi cham vao, con `.srt` thuong la phu de tai kem.
     *
     * Nhan CA duong dan day du lan ten tep tran - hai duong tim loi (tep trong
     * may va tai lieu trong thu muc tu tro) dung chung dung mot danh sach nay,
     * nen them mot duoi moi la ca hai duong cung co.
     */
    internal fun tenLoiUngVien(ten: String): List<String> {
        val khongDuoi = ten.substringBeforeLast('.')
        return listOf(
            "$khongDuoi.lrc", "$khongDuoi.LRC",
            "$ten.lrc", "$ten.LRC",
            "$khongDuoi.srt", "$khongDuoi.SRT",
            "$ten.srt", "$ten.SRT"
        )
    }

    /**
     * Doc chu thanh loi, hoac null khi khong ra cau nao.
     *
     * Tep co chu ma doc ra khong duoc cau nao nghia la dang tep khac han, hoac
     * hong. Tra null de ben goi di tiep, chu dung nhan mot ban loi rong roi
     * thoi - nhan roi thi khong con di tim nguon nao khac nua.
     */
    private fun dungLoi(chu: String?, ten: String): Lyrics? {
        if (chu.isNullOrBlank()) return null
        val doc =
            if (ten.endsWith(".srt", ignoreCase = true)) parseSrt(chu).copy(from = NGUON_PHU_DE)
            else parseLrc(chu, from = NGUON)
        if (doc.lines.isEmpty()) {
            Log.i(TAG, "tep khong doc ra cau nao, bo qua: $ten")
            return null
        }
        return doc
    }

    /**
     * Loi nam canh mot TAI LIEU - bai den tu thu muc nguoi dung tu tro vao.
     *
     * Duong nay ton tai vi bai kieu do khong co duong dan tren dia: ta chi cam
     * mot dia chi tai lieu, va `File` khong mo duoc no. Khong co doan nay thi
     * nguoi dung tro AURA vao thu muc nhac cua ho, thay bai hien ra day du, roi
     * ngac nhien vi may tep `.lrc` nam ngay canh do lai khong duoc doc.
     */
    private fun docCanhTaiLieu(context: Context, tep: Uri): Lyrics? {
        val tenNhac = ThuVienNgoai.tenTaiLieu(tep) ?: return null

        for (ten in tenLoiUngVien(tenNhac)) {
            val anhEm = ThuVienNgoai.anhEm(tep) { _ -> ten } ?: continue
            val chu = try {
                context.contentResolver.openInputStream(anhEm)
                    ?.use { it.readBytes().decodeToString() }
            } catch (e: Exception) {
                // Khong co tep do la chuyen thuong - `openInputStream` nem
                // `FileNotFoundException` chu khong tra null.
                null
            }
            val doc = dungLoi(chu, ten) ?: continue
            Log.i(TAG, "doc duoc " + doc.lines.size + " cau tu tai lieu $ten")
            return doc
        }

        // Khong co tep nam canh thi xuong the nam TRONG tep nhac - dung thu tu
        // nhu duong tep trong may: cai nguoi dung tu tay dat o day dang tin hon
        // cai di kem san tu luc tai ve.
        return LoiTrongTep.doc(tenNhac) { context.contentResolver.openInputStream(tep) }
    }


    // ---- Ghi ra ----

    /** Kết quả một lần ghi, để màn hình nói đúng chuyện đã xảy ra. */
    sealed interface KetQuaGhi {
        data class Xong(val duong: String) : KetQuaGhi
        /** Đã có tệp sẵn ở đó — hỏi lại rồi mới đè. */
        data class DaCoTep(val duong: String) : KetQuaGhi
        /** Bài đang phát không phải tệp trong máy, không có chỗ nào để ghi cạnh. */
        data object KhongPhaiTepTrongMay : KetQuaGhi

        /**
         * Android không cho ghi vào chỗ đó. Kèm theo TÊN GỢI Ý và NỘI DUNG, để
         * màn hình còn mời người dùng chọn chỗ khác mà lưu — chứ không bỏ họ
         * đứng một mình trước một dòng báo lỗi.
         */
        data class BiChan(val tenGoiY: String, val noiDung: String) : KetQuaGhi

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

        // Bai trong thu muc nguoi dung tu tro vao di duong rieng - va day la
        // duong CHAC CHAN ghi duoc: nguoi dung da tu trao quyen ghi cho dung
        // thu muc do, nen khong vap phai luat "chi bon thu muc" cua he thong.
        ThuVienNgoai.diaChiTuMa(uri)?.let {
            return ghiTaiLieu(context, it, dungNoiDung(loi, tenBai, caSi), deLen)
        }

        val duongNhac = duongDan(context, uri) ?: return KetQuaGhi.KhongPhaiTepTrongMay
        val dich = File(duongNhac.substringBeforeLast('.') + ".lrc")
        if (dich.exists() && !deLen) return KetQuaGhi.DaCoTep(dich.path)

        val than = dungNoiDung(loi, tenBai, caSi)
        return try {
            dich.writeText(than)
            Log.i(TAG, "da ghi loi ra " + dich.path)
            KetQuaGhi.Xong(dich.path)
        } catch (e: Exception) {
            // KHÔNG đoán nguyên nhân qua `dich.exists()`: khi bị chặn thì chính
            // `exists()` cũng trả về false dù tệp có thật, và màn hình sẽ nói
            // sai. Cứ báo là bị chặn rồi mời một chỗ khác — đúng hay sai nguyên
            // nhân thì lối thoát vẫn thế.
            Log.i(TAG, "khong ghi duoc " + dich.path, e)
            KetQuaGhi.BiChan(dich.name, than)
        }
    }

    /**
     * Ghi `.lrc` nam canh mot TAI LIEU trong thu muc nguoi dung tu tro vao.
     *
     * Khac han duong tep trong may o mot diem dang ke: o day khong co luat "chi
     * bon thu muc nhan tep .lrc". Nguoi dung da tu trao quyen ghi cho dung thu
     * muc nay qua bo chon, nen he thong khong con gi de chan - `BiChan` o duoi
     * gan nhu khong bao gio toi, va giu lai chi de phong trinh cung cap tep cua
     * ben thu ba tra ve chi doc.
     *
     * DE LEN thi ghi vao chinh tai lieu da co chu KHONG tao lai: `createDocument`
     * gap ten trung se de ra "bai (1).lrc" chu khong bao loi, va nguoi dung bam
     * "Ghi de" ma may de them mot tep thu hai la sai han y ho.
     */
    private fun ghiTaiLieu(
        context: Context,
        tep: Uri,
        than: String,
        deLen: Boolean
    ): KetQuaGhi {
        val tenNhac = ThuVienNgoai.tenTaiLieu(tep) ?: return KetQuaGhi.KhongPhaiTepTrongMay
        val tenLrc = tenNhac.substringBeforeLast('.') + ".lrc"

        val san = ThuVienNgoai.anhEm(tep) { _ -> tenLrc }
        val daCo = san != null && ThuVienNgoai.coThat(context, san)
        if (daCo && !deLen) return KetQuaGhi.DaCoTep(tenLrc)

        return try {
            val dich = if (daCo) san!! else {
                val cha = ThuVienNgoai.thuMucCha(tep)
                    ?: return KetQuaGhi.Hong("không tìm được thư mục chứa bài này")
                DocumentsContract.createDocument(
                    context.contentResolver,
                    cha,
                    // KHONG dung "text/plain": bo cung cap tep se doi duoi theo
                    // kieu MIME va de ra "bai.lrc.txt". Kieu nhi phan thi khong
                    // co duoi nao de doi, nen ten giu nguyen - cung ly do man
                    // hinh "Chon cho luu" dang dung kieu nay.
                    "application/octet-stream",
                    tenLrc
                ) ?: return KetQuaGhi.Hong("không tạo được tệp trong thư mục đó")
            }

            // "wt" - cat ngan truoc khi ghi. Thieu chu `t` thi ghi de mot ban
            // loi ngan hon ban cu se de lai duoi thua cua ban cu.
            context.contentResolver.openOutputStream(dich, "wt")?.use {
                it.write(than.toByteArray())
            } ?: return KetQuaGhi.Hong("không mở được tệp để ghi")

            Log.i(TAG, "da ghi loi ra tai lieu $tenLrc")
            KetQuaGhi.Xong(tenLrc)
        } catch (e: Exception) {
            Log.i(TAG, "khong ghi duoc tai lieu $tenLrc", e)
            KetQuaGhi.BiChan(tenLrc, than)
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
