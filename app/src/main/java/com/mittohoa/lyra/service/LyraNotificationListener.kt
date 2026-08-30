package com.mittohoa.lyra.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log
import com.mittohoa.lyra.media.MediaSessionWatcher
import com.mittohoa.lyra.media.NowPlaying
import kotlinx.coroutines.flow.StateFlow

/**
 * Neo cua ca app.
 *
 * Ta khong quan tam toi thong bao - chi can lop nay ton tai thi he thong moi
 * cho goi `getActiveSessions`. Doi lai duoc luon mot cho tru chay nen ma khong
 * phai hien thong bao thuong truc.
 *
 * Day la quyet dinh kien truc nguoc voi truc giac: phan xa thuong la dung
 * foreground service de chay nen. Khong can. `NotificationListenerService` tu
 * no da la mot service do he thong dung va giu song khi nguoi dung da cap
 * quyen doc thong bao, va no song dai hon foreground service cua chinh ta.
 *
 * He thong co the giet va dung lai lop nay bat cu luc nao, nen moi trang thai
 * phai nam o `Lyra` (singleton ben ngoai), khong nam trong chinh no.
 */
class LyraNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        Log.i(TAG, "He thong da noi vao - bat dau doc phien media")
        Lyra.watcher.start(this, LyraNotificationListener::class.java)
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "He thong ngat ket noi - dung doc")
        Lyra.watcher.stop()
    }

    private companion object {
        const val TAG = "LyraListener"
    }
}

/**
 * Trang thai dung chung cua ca app.
 *
 * Phai la singleton ngoai service: `NotificationListenerService` bi he thong
 * dung len roi giet di theo y no, con man hinh Cai dat thi can doc cung mot
 * dong du lieu ay. De trang thai trong service thi moi lan he thong dung lai
 * la mat sach.
 */
object Lyra {
    val watcher = MediaSessionWatcher()

    val now: StateFlow<NowPlaying?> get() = watcher.now

    /**
     * Thu doc phien media ngay, khong doi he thong noi vao service.
     *
     * Dung cho man hinh Cai dat: nguoi dung vua bat quyen xong quay lai app
     * thi thay ket qua luon, khong phai doi.
     */
    fun refresh(context: Context) {
        watcher.start(context.applicationContext, LyraNotificationListener::class.java)
    }
}
