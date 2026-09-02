package com.mittohoa.lyra.lyrics

import android.content.ContentUris
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.io.File

/**
 * Đo xem Android CHO đọc tệp .lrc nằm cạnh tệp nhạc bằng đường nào.
 *
 * Không phải bài kiểm đúng/sai — là bài đo. Quyền `READ_MEDIA_AUDIO` phủ tệp
 * NHẠC, còn .lrc là tệp chữ nên không nằm trong vùng đó. Câu hỏi phải trả lời
 * bằng số liệu trước khi viết một dòng nào: đọc thẳng có được không, MediaStore
 * có thấy nó không, hay bắt buộc phải nhờ người dùng chỉ thư mục.
 */
class DuongDocLrcTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun doCacDuongDocDuoc() {
        val bao = StringBuilder("\n===== ĐO ĐƯỜNG ĐỌC .lrc =====\n")

        // Tìm tệp nhạc đầu tiên và đường dẫn thật của nó.
        var duongNhac: String? = null
        var idNhac = 0L
        ctx.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA),
            null, null, null
        )?.use {
            if (it.moveToFirst()) { idNhac = it.getLong(0); duongNhac = it.getString(1) }
        }
        bao.append("nhạc:            ").append(duongNhac ?: "KHÔNG THẤY").append('\n')
        val nhac = duongNhac ?: run { println(bao); return }

        val duongLrc = nhac.substringBeforeLast('.') + ".lrc"
        bao.append("lrc cần đọc:     ").append(duongLrc).append('\n')

        // 1. Đọc thẳng bằng java.io.File
        val f = File(duongLrc)
        bao.append("1) File.exists:  ").append(runCatching { f.exists() }
            .fold({ it.toString() }, { "NÉM: ${it.javaClass.simpleName}" })).append('\n')
        bao.append("1) File.readText:").append(runCatching { f.readText().take(30) }
            .fold({ "ĐỌC ĐƯỢC: $it" }, { "NÉM: ${it.javaClass.simpleName}: ${it.message}" }))
            .append('\n')

        // 2. Hỏi MediaStore.Files xem nó có đánh chỉ mục tệp .lrc không
        var thayTrongMediaStore = 0
        runCatching {
            ctx.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA),
                "${MediaStore.Files.FileColumns.DATA} LIKE ?", arrayOf("%.lrc"), null
            )?.use { c ->
                thayTrongMediaStore = c.count
                if (c.moveToFirst()) {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"), c.getLong(0))
                    bao.append("2) MediaStore thấy: ").append(c.getString(1)).append('\n')
                    bao.append("2) đọc qua uri:  ").append(
                        runCatching {
                            ctx.contentResolver.openInputStream(uri)!!
                                .use { it.readBytes().decodeToString().take(30) }
                        }.fold({ "ĐỌC ĐƯỢC: $it" },
                               { "NÉM: ${it.javaClass.simpleName}: ${it.message}" })).append('\n')
                }
            }
        }.onFailure { bao.append("2) truy vấn NÉM: ").append(it.javaClass.simpleName).append('\n') }
        bao.append("2) số .lrc MediaStore biết: ").append(thayTrongMediaStore).append('\n')

        // 3. Thư mục chứa nhạc có liệt kê được không
        bao.append("3) listFiles thư mục: ").append(
            runCatching { File(nhac).parentFile?.list()?.size }
                .fold({ if (it == null) "null (không đọc được)" else "$it mục" },
                      { "NÉM: ${it.javaClass.simpleName}" })).append('\n')

        // 4. Còn đọc được chính tệp NHẠC không - để biết quyền đang có tác dụng
        bao.append("4) mở được tệp nhạc qua MediaStore: ").append(
            runCatching {
                ctx.contentResolver.openInputStream(
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, idNhac))!!
                    .use { it.read(); "được" }
            }.fold({ it }, { "NÉM: ${it.javaClass.simpleName}" })).append('\n')

        // Phạm vi đọc thẳng tới đâu? Quyết định cả thiết kế: nếu chỉ đọc được
        // trong Music/ thì người để nhạc ở thư mục riêng vẫn phải nhờ bộ chọn.
        bao.append("--- đọc thẳng ở từng loại thư mục ---\n")
        for (thu in listOf("Music", "Download", "Movies", "Documents", "ThuMucRieng")) {
            val t = File("/storage/emulated/0/" + thu + "/lyra-thu.lrc")
            bao.append(thu.padEnd(14)).append(
                runCatching { t.readText().trim() }
                    .fold({ "ĐỌC ĐƯỢC" }, { "chặn: " + it.javaClass.simpleName })
            ).append("   liệt kê thư mục: ").append(
                runCatching { File("/storage/emulated/0/" + thu).list()?.size }
                    .fold({ if (it == null) "không được" else "được ($it mục)" },
                          { "chặn" })
            ).append('\n')
        }

        println(bao)
    }
}
