package com.mittohoa.lyra.player

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.mittohoa.lyra.sources.Catalog
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * Doi dia chi gia trong hang doi thanh duong phat that, NGAY TRUOC KHI PHAT.
 *
 * Hang doi chua `lyra://zing/<ma>` chu khong chua duong phat that, vi hai le:
 *
 *   - Duong phat cua ca Zing lan NCT deu co han. Xep hai chuc bai vao hang doi
 *     roi nghe den bai cuoi sau mot tieng thi duong ay da chet.
 *   - Zing phai goi mang mot lan cho MOI bai. Goi hai chuc lan chi de xep hang
 *     doi la bat nguoi dung tra gia cho nhung bai ho chua chac nghe toi.
 *
 * `ResolvingDataSource` la cho dung de lam viec nay: no chay tren luong nap du
 * lieu, ngay truoc khi mo dong byte dau tien, va chi cho bai that su sap phat.
 *
 * `runBlocking` o day KHONG phai cau tha: ham nay duoc goi tren luong nap cua
 * bo giai ma, no von duoc phep chan, va toan bo giao dien cua ExoPlayer o cho
 * nay la dong bo. Chuyen sang bat dong bo nghia la tu viet lai mot tang chuyen
 * doi khong ai can.
 */
class StreamResolver : ResolvingDataSource.Resolver {

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != SCHEME) return dataSpec

        val real = runBlocking { Catalog.streamUrl(dataSpec.uri) }
            ?: throw IOException(
                "Không lấy được đường phát cho bài này. Nguồn có thể đã gỡ bài, " +
                    "hoặc bài chỉ dành cho tài khoản trả phí."
            )

        return dataSpec.withUri(Uri.parse(real))
    }

    private companion object {
        const val SCHEME = "lyra"
    }
}
