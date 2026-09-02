package com.mittohoa.lyra.lyrics

import android.content.ContentValues
import android.os.Environment
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
 */
class DuongGhiLrcTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun doCacDuongGhiDuoc() {
        val bao = StringBuilder("\n===== ĐO ĐƯỜNG GHI .lrc =====\n")

        // 1. Ghi thẳng vào từng loại thư mục
        bao.append("--- 1. File.writeText thẳng ---\n")
        for (thu in listOf("Music", "Music/Lyra", "Download", "Documents", "Movies")) {
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
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Lyra")
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

        println(bao)
    }
}
