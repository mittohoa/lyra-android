package com.mittohoa.lyra.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

/**
 * Nhan ket qua tu phien cai dat.
 *
 * He thong tra ve mot trong ba thu: da xong, da hong, hoac CAN NGUOI DUNG XAC
 * NHAN. Cai thu ba moi la cai quan trong - `session.commit` khong tu mo hop
 * thoai, no bao ve day roi ta phai mo. Bo qua no thi phien cai nam im mai mai
 * va khong ai hieu vi sao khong co gi xay ra.
 */
class KetQuaCaiDat : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val hopThoai = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                hopThoai?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(hopThoai) }
            }

            PackageInstaller.STATUS_SUCCESS ->
                Log.i(TAG, "Da cai xong ban moi")

            else -> {
                val vi = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w(TAG, "Cai khong xong: $vi")
            }
        }
    }

    private companion object {
        const val TAG = "LyraCapNhat"
    }
}
