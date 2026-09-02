package com.mittohoa.lyra.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Cua nhan lenh phat thu qua adb. **Chi co trong ban go loi.**
 *
 * Nam o `src/debug` nen no khong ton tai trong ban phat hanh - khong phai nho
 * tat, khong phai nho go, va khong the lo lot ra ngoai. Mot cai cong nhan lenh
 * phat bat ky duong dan nao ma con song trong ban phat hanh thi la mot lo hong,
 * khong phai mot tien ich.
 *
 * Dung de nghiem thu bo may phat truoc khi co giao dien tim bai:
 *
 *   adb shell am broadcast -n <goi>/com.mittohoa.lyra.player.DebugPlayReceiver \
 *     -a com.mittohoa.lyra.DEBUG_PLAY --es url "<duong dan>" \
 *     --es title "<ten bai>" --es artist "<nghe si>"
 */
class DebugPlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("url")
        if (url.isNullOrBlank()) {
            Log.w(TAG, "Thieu tham so url")
            return
        }

        val track = Playback.Playable(
            id = intent.getStringExtra("id") ?: url.hashCode().toString(),
            title = intent.getStringExtra("title") ?: "Không rõ tên",
            artist = intent.getStringExtra("artist").orEmpty(),
            uri = url,
            artworkUri = intent.getStringExtra("art")
        )

        Log.i(TAG, "Phat thu: '${track.artist}' - '${track.title}'")
        Playback.play(context, track)
    }

    private companion object {
        const val TAG = "AuraPhatThu"
    }
}
