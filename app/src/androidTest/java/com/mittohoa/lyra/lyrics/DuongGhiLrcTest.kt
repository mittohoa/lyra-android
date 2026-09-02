package com.mittohoa.lyra.lyrics

import android.content.ContentValues
import android.os.Environment
import android.util.Log
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.io.File

/**
 * Đo xem Android CHO ghi tệp .lrc cạnh tệp nhạc bằng đường nào.
 *
 * Đọc đã đo rồi và đọc thẳng được. Ghi thì Android chặt hơn hẳn: từ Android 11
 * một app chỉ được sửa tệp DO CHÍNH NÓ tạo ra, còn tệp của người khác thì phải
 * xin từng cái một. Nên phải đo lại từ đầu, đừng suy từ kết quả đọc sang.
 *
 * Ba đường có thể đi, đo cả ba:
 *   1. `File.writeText` thẳng - đơn giản nhất nếu được
 *   2. Tạo qua MediaStore rồi ghi qua `openOutputStream`
 *   3. (không đo được bằng máy) bộ chọn thư mục SAF - cần người dùng bấm
 *
 * ĐO ĐƯỢC (Pixel 6 Pro, Android 17, có READ_MEDIA_AUDIO + READ_MEDIA_VIDEO):
 *
 *   Music  Download  Documents  Movies  Movies/Zalo     ghi được
 *   DCIM   DCIM/Camera  Pictures                        EPERM
 *
 * Hai thư mục ảnh CHỈ NHẬN ẢNH VÀ VIDEO, không nhận loại tệp nào khác - kể cả
 * khi app có đủ quyền đọc. Đó là nguyên nhân thật của "Permission denied" mà
 * người dùng gặp: video quay bằng máy ảnh nằm ở DCIM/Camera.
 *
 * ĐỪNG BỎ DCIM VÀ Pictures RA KHỎI DANH SÁCH ĐO. Lần trước chỉ đo mấy thư mục
 * "chắc là được" nên không thấy gì, rồi đi đổ cho thẻ nhớ ngoài - trong khi
 * máy đo còn chẳng có khe thẻ.
 */
class DuongGhiLrcTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun doCacDuongGhiDuoc() {
        val bao = StringBuilder("\n===== ĐO ĐƯỜNG GHI .lrc =====\n")

        // 1. Ghi thẳng vào từng loại thư mục
        bao.append("--- 1. File.writeText thẳng ---\n")
        // DCIM và Movies nằm trong danh sách vì đó là chỗ tệp của người dùng
        // thật nằm — máy đo được có 63 video ở DCIM/Camera, Movies/Zalo và
        // Movies, không có tệp nhạc nào. Đoán thư mục nào "chắc là được" rồi
        // chỉ đo mấy thư mục đó là cách bỏ sót đúng trường hợp đang hỏng.
        for (thu in listOf(
            "Music", "Music/Lyra", "Download", "Documents",
            "Movies", "Movies/Zalo", "DCIM", "DCIM/Camera", "Pictures",
        )) {
            val f = File("/storage/emulated/0/$thu/lyra-ghi-thu.lrc")
            bao.append(thu.padEnd(14)).append(
                runCatching {
                    f.writeText("[00:01.00]thử ghi")
                    val doc = f.readText()
                    f.delete()
                    if (doc == "[00:01.00]thử ghi") "GHI ĐƯỢC" else "ghi ra rỗng"
                }.getOrElse { "chặn: ${it.javaClass.simpleName}: ${it.message?.take(60)}" }
            ).append('\n')
        }

        // 2. Nhờ MediaStore tạo hộ rồi ghi vào
        bao.append("--- 2. qua MediaStore ---\n")
        val gt = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "lyra-ghi-thu.lrc")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AURA")
        }
        bao.append("tạo trong Music/Lyra: ").append(
            runCatching {
                val uri = ctx.contentResolver.insert(
                    MediaStore.Files.getContentUri("external"), gt)
                    ?: error("insert trả null")
                ctx.contentResolver.openOutputStream(uri)!!.use {
                    it.write("[00:01.00]thử ghi".toByteArray())
                }
                val doc = ctx.contentResolver.openInputStream(uri)!!
                    .use { it.readBytes().decodeToString() }
                ctx.contentResolver.delete(uri, null, null)
                "TẠO VÀ GHI ĐƯỢC ($doc)"
            }.getOrElse { "chặn: ${it.javaClass.simpleName}: ${it.message?.take(80)}" }
        ).append('\n')

        // 3. Ghi ĐÈ lên một tệp đã có sẵn do người khác tạo - trường hợp khó
        //    nhất, và cũng là trường hợp thật: sửa lại lời đã có.
        bao.append("--- 3. ghi đè tệp .lrc có sẵn ---\n")
        val coSan = File("/storage/emulated/0/Music/Lyra/Hoàng Dũng - Nàng Thơ.lrc")
        bao.append("tệp có sẵn tồn tại: ").append(coSan.exists()).append('\n')
        if (coSan.exists()) {
            val goc = runCatching { coSan.readText() }.getOrNull()
            bao.append("ghi đè: ").append(
                runCatching {
                    coSan.appendText("")           // mở ghi mà không đổi nội dung
                    "GHI ĐƯỢC"
                }.getOrElse { "chặn: ${it.javaClass.simpleName}" }
            ).append('\n')
            // Trả lại nguyên trạng dù có chuyện gì xảy ra
            if (goc != null) runCatching { coSan.writeText(goc) }
        }

        // 4. Đúng cách mã nguồn thật làm: lấy đường dẫn của MỘT TỆP CÓ THẬT
        //    trong máy rồi đổi đuôi thành .lrc. Khác hẳn phép đo ở trên — ở
        //    trên là tạo tệp tên do mình đặt trong thư mục, còn đây là tạo tệp
        //    NẰM CẠNH tệp của người khác. Người dùng báo lỗi ở đúng bước này.
        bao.append("--- 4. ghi .lrc cạnh một tệp có thật ---\n")
        val cot = arrayOf(MediaStore.MediaColumns.DATA)
        for (kho in listOf(
            "video" to MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            "nhạc" to MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        )) {
            val daThu = mutableSetOf<String>()
            ctx.contentResolver.query(kho.second, cot, null, null, null)?.use { con ->
                while (con.moveToNext()) {
                    val duong = con.getString(0) ?: continue
                    val thuMuc = duong.substringBeforeLast('/')
                    // Mỗi thư mục đo một lần là đủ; đo 63 lần chỉ ra 63 dòng giống nhau.
                    if (!daThu.add(thuMuc)) continue
                    val dich = File(duong.substringBeforeLast('.') + ".aura-thu.lrc")
                    bao.append(kho.first).append(' ').append(thuMuc.padEnd(34)).append(
                        runCatching {
                            dich.writeText("[00:01.00]thử ghi")
                            dich.delete()
                            "GHI ĐƯỢC"
                        }.getOrElse { "chặn: " + it.javaClass.simpleName + ": " + it.message?.take(50) }
                    ).append('\n')
                }
            }
        }

        // In ra bằng println thì Gradle không giữ lại trong tệp kết quả — đã
        // mất một lượt đo vì chuyện đó. Ghi vào nhật ký thì đọc lại được.
        for (dong in bao.lines()) Log.i("AuraDoGhi", dong)
        println(bao)
    }
}
