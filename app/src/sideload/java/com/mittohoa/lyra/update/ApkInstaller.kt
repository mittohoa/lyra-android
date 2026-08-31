package com.mittohoa.lyra.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.mittohoa.lyra.sources.Http
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Tải bản mới về rồi mở phiên cài đặt.
 *
 * Bản SIDELOAD: có thật.
 *
 * Android không cho app tự cài đè chính nó **hoàn toàn im lặng** — người dùng
 * luôn phải bấm xác nhận một lần ở hộp thoại của hệ thống. Nhưng nó cho app tải
 * file về rồi mở sẵn hộp thoại đó, và khác biệt so với việc quăng cho người ta
 * một đường dẫn là rất lớn: không phải rời app, không phải tìm file trong thư
 * mục Tải về, không phải đoán file nào đúng kiến trúc máy mình.
 *
 * Dùng `PackageInstaller` chứ không dùng `Intent.ACTION_VIEW` với file APK: cách
 * cũ cần một `FileProvider` và quyền đọc tạm, còn cách này ghi thẳng byte vào
 * một phiên do hệ thống giữ. Ít mảnh ghép hơn, và không để lại file APK nằm chờ
 * trong máy người dùng.
 *
 * Quyền `REQUEST_INSTALL_PACKAGES` chỉ khai ở bản này. Google Play soi quyền đó
 * rất kỹ và chỉ cho vài loại app dùng — mà bản trên Play thì không cần: Play tự
 * lo việc cập nhật.
 */
object ApkInstaller {

    const val SUPPORTED = true

    /**
     * Người dùng đã cho phép app này cài đặt ứng dụng chưa.
     *
     * Từ Android 8, quyền cài đặt được cấp **theo từng app** chứ không còn là
     * một công tắc chung của máy. Chưa cấp thì phải đưa họ tới đúng trang cài
     * đặt của app này — xem `moTrangCapQuyen`.
     */
    fun duocPhepCai(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun moTrangCapQuyen(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            android.net.Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /**
     * Tải APK rồi giao cho hệ thống cài.
     *
     * `onProgress` nhận phần trăm 0..100, hoặc -1 khi máy chủ không nói trước độ
     * dài. Trả về câu báo lỗi, hoặc `null` khi đã mở được hộp thoại cài đặt.
     *
     * Ghi **thẳng vào phiên cài** chứ không tải xuống file rồi mới nạp: bản APK
     * gần 20 MB, và làm hai bước là chiếm gấp đôi chỗ trống cùng lúc trên máy
     * vốn đã sắp đầy của ai đó.
     */
    suspend fun taiVaCai(
        context: Context,
        duongTai: String,
        onProgress: (Int) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        if (!duocPhepCai(context)) return@withContext "Chưa được cấp quyền cài đặt ứng dụng"

        val installer = context.packageManager.packageInstaller
        var sessionId = -1

        try {
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            sessionId = installer.createSession(params)

            installer.openSession(sessionId).use { session ->
                Http.client.newCall(Request.Builder().url(duongTai).build()).execute().use { res ->
                    if (!res.isSuccessful) throw IllegalStateException("Máy chủ trả về ${res.code}")
                    val body = res.body ?: throw IllegalStateException("Máy chủ trả về rỗng")
                    val total = body.contentLength()

                    session.openWrite("lyra", 0, total).use { out ->
                        val input = body.byteStream()
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        var lastPercent = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            written += read
                            if (total > 0) {
                                val percent = ((written * 100) / total).toInt().coerceIn(0, 100)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            } else {
                                onProgress(-1)
                            }
                        }
                        session.fsync(out)
                    }
                }

                // Hệ thống mở hộp thoại xác nhận từ đây. Không có cách nào bỏ qua
                // bước này với một app thường, và cũng không nên có.
                val intent = Intent(context, KetQuaCaiDat::class.java)
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                session.commit(pending.intentSender)
            }

            Log.i(TAG, "Da giao ban moi cho he thong cai")
            null
        } catch (e: CancellationException) {
            if (sessionId >= 0) runCatching { installer.abandonSession(sessionId) }
            throw e
        } catch (e: Exception) {
            if (sessionId >= 0) runCatching { installer.abandonSession(sessionId) }
            Log.w(TAG, "Khong cai duoc ban moi", e)
            when {
                e.message?.contains("ENOSPC") == true -> "Máy hết dung lượng"
                e is java.net.UnknownHostException -> "Không có mạng"
                else -> "Không tải được bản mới"
            }
        }
    }

    private const val TAG = "LyraCapNhat"
}
