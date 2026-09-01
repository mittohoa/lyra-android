package com.mittohoa.lyra.service

import android.service.notification.NotificationListenerService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    /**
     * Bo nhan tin man hinh bat/tat.
     *
     * Hai tin nay CHI nhan duoc khi dang ky luc chay - khai trong manifest
     * khong an. Dat o dich vu nay vi no song lau hon moi Activity, va la cho
     * duy nhat con song khi nguoi dung da roi app ma khung noi van bat.
     */
    private val manHinh = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> Lyra.manHinhDoi(true)
                Intent.ACTION_SCREEN_OFF -> Lyra.manHinhDoi(false)
            }
        }
    }
    private var daDangKy = false

    override fun onListenerConnected() {
        Log.i(TAG, "He thong da noi vao - bat dau doc phien media")
        Lyra.watcher.start(this, LyraNotificationListener::class.java)

        // He thong vua giet roi dung lai ta. Neu truoc do nguoi dung dang bat
        // khung noi thi phai tu dung lai - ho dang o trong app nhac, khong co
        // ly do gi de phai quay ve Lyra bat lai bang tay.
        Lyra.restoreOverlay(this)

        if (!daDangKy) {
            registerReceiver(manHinh, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            })
            daDangKy = true
        }
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "He thong ngat ket noi - dung doc")
        Lyra.watcher.stop()
        if (daDangKy) {
            runCatching { unregisterReceiver(manHinh) }
            daDangKy = false
        }
    }

    private companion object {
        const val TAG = "LyraListener"
    }
}
