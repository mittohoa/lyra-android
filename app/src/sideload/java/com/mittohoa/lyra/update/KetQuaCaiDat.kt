package com.mittohoa.lyra.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import com.mittohoa.lyra.service.Lyra

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

            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Da cai xong ban moi")
                Lyra.ketQuaCaiDat(thanhCong = true, vi = null)
            }

            else -> {
                val vi = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w(TAG, "Cai khong xong: $vi")
                // Phai noi ra man hinh, khong chi ghi nhat ky. Nguoi dung vua bam
                // mot nut va cho hang chuc giay; im lang o day la cach chac chan
                // nhat de ho tuong app hong.
                Lyra.ketQuaCaiDat(thanhCong = false, vi = viTiengViet(vi))
            }
        }
    }

    /**
     * Doi loi cua he thong sang cau nguoi dung lam duoc gi do voi no.
     *
     * "INSTALL_FAILED_ABORTED: Self update is blocked by unknown source package"
     * la that - da gap tren may that - nhung noi nguyen van thi khong ai biet
     * phai lam gi tiep. Khong nhan ra thi giu nguyen chu goc con hon bia mot cau
     * chung chung: it ra nguoi dung con tra cuu duoc.
     */
    private fun viTiengViet(goc: String?): String = when {
        goc == null -> "Hệ thống từ chối cài bản mới"
        goc.contains("Self update is blocked") ->
            "Android không cho AURA tự cài đè bản này. Tải APK trên trang phát " +
                "hành rồi cài tay một lần là xong."
        goc.contains("INSTALL_FAILED_VERSION_DOWNGRADE") ->
            "Bản trên máy đã mới hơn bản vừa tải"
        goc.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE") ->
            "Máy hết dung lượng"
        goc.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") ||
            goc.contains("signatures do not match") ->
            "Bản mới ký bằng khoá khác. Gỡ bản cũ rồi cài lại."
        goc.contains("ABORTED") -> "Bạn hoặc hệ thống đã huỷ việc cài"
        else -> goc
    }

    private companion object {
        const val TAG = "AuraCapNhat"
    }
}
