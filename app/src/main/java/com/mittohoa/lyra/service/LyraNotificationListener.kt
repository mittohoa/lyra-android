package com.mittohoa.lyra.service

import android.service.notification.NotificationListenerService
import android.util.Log

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

        // He thong vua giet roi dung lai ta. Neu truoc do nguoi dung dang bat
        // khung noi thi phai tu dung lai - ho dang o trong app nhac, khong co
        // ly do gi de phai quay ve Lyra bat lai bang tay.
        Lyra.restoreOverlay(this)
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "He thong ngat ket noi - dung doc")
        Lyra.watcher.stop()
    }

    private companion object {
        const val TAG = "LyraListener"
    }
}
