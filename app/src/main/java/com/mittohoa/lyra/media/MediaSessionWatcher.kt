package com.mittohoa.lyra.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bam theo cac phien media dang chay tren may.
 *
 * Day la thu thay cho SMTC cua ban Windows, va tot hon han: he thong TU DAY
 * ban tin toi khi co gi doi, khong phai nuoi mot tien trinh hoi lien tuc.
 *
 * Ba dieu phai lam dung, thieu mot la sai:
 *
 *  1. Chi goi duoc `getActiveSessions` khi nguoi dung DA bat quyen doc thong
 *     bao. Chua bat thi nem `SecurityException`, khong phai tra ve rong.
 *  2. Cac phien den va di lien tuc (mo app khac, dong app cu), nen phai dang ky
 *     `OnActiveSessionsChangedListener` va gan lai callback moi lan doi.
 *  3. Nhieu app cung khai bao phien mot luc (Spotify tam dung, YouTube dang
 *     phat). Phai chon phien DANG PHAT, khong phai phien dau danh sach.
 */
class MediaSessionWatcher {

    private val _now = MutableStateFlow<NowPlaying?>(null)

    /** Bai dang phat, hoac null khi khong co gi. */
    val now: StateFlow<NowPlaying?> = _now.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private var component: ComponentName? = null

    /**
     * Goi cua chinh AURA, de con bo qua phien cua minh.
     *
     * Bat buoc phai bo, va day la mot vong lap that chu khong phai de phong xa:
     * AURA ghi CAU DANG HAT vao phan mo ta phien cua no de hien tren the man
     * hinh khoa. Doc lai chinh phien do thi cau hat tro thanh "ten bai moi", va
     * app di tra loi cho mot cau hat - roi ket qua lai ghi de len, vong tiep.
     *
     * Bai AURA tu phat khong can doc qua day: `AURA` biet chinh xac ten bai,
     * nghe si va vi tri tu bo may phat cua no.
     */
    private var ownPackage: String = ""

    /** Callback dang gan, theo tung controller - de con go ra cho dung. */
    private val attached = mutableMapOf<MediaController, MediaController.Callback>()

    private val onSessionsChanged =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            rebind(controllers.orEmpty())
        }

    fun start(context: Context, listener: Class<*>) {
        if (manager != null) return

        val msm = context.getSystemService(MediaSessionManager::class.java) ?: run {
            Log.w(TAG, "May nay khong co MediaSessionManager")
            return
        }
        val comp = ComponentName(context, listener)
        manager = msm
        component = comp
        ownPackage = context.packageName

        try {
            msm.addOnActiveSessionsChangedListener(onSessionsChanged, comp, handler)
            rebind(msm.getActiveSessions(comp))
        } catch (e: SecurityException) {
            // Chua bat quyen doc thong bao. Khong phai loi cua ta - man hinh
            // dan nhap se dua nguoi dung toi dung cho bat.
            Log.i(TAG, "Chua co quyen doc thong bao nen chua doc duoc phien media")
            stop()
        }
    }

    fun stop() {
        manager?.runCatching { removeOnActiveSessionsChangedListener(onSessionsChanged) }
        attached.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        attached.clear()
        manager = null
        component = null
        _now.value = null
    }

    /** Gan callback cho cac phien moi, go khoi cac phien da bien mat. */
    private fun rebind(controllers: List<MediaController>) {
        val current = controllers.filterNot { it.packageName == ownPackage }.toSet()

        attached.keys.filterNot { it in current }.forEach { gone ->
            attached.remove(gone)?.let { gone.unregisterCallback(it) }
        }

        for (controller in current) {
            if (controller in attached) continue
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onSessionDestroyed() = publish()
            }
            controller.registerCallback(callback, handler)
            attached[controller] = callback
        }

        publish()
    }

    /**
     * Chon phien dang phat roi day ban tin di.
     *
     * Uu tien phien DANG PHAT. Khong co phien nao dang phat thi lay phien dau
     * co metadata - nguoi dung vua tam dung van muon thay loi bai do.
     */
    private fun publish() {
        val controllers = attached.keys.toList()

        val playing = controllers.firstOrNull { it.playbackState.isActuallyPlaying() }
        val chosen = playing ?: controllers.firstOrNull { it.metadata != null }

        _now.value = chosen?.let {
            buildNowPlaying(it.packageName, it.metadata, it.playbackState)
        }
    }

    /**
     * Vi tri phat tinh lai o thoi diem goi.
     *
     * `now.position` la vi tri luc ban tin duoc tao. Khung loi noi ve lai theo
     * tung khung hinh, nen phai hoi lai lien tuc - khong thi loi giat tung nac
     * theo nhip ban tin thay vi chay muot.
     */
    fun livePosition(): Long {
        val controller = attached.keys.firstOrNull { it.playbackState.isActuallyPlaying() }
            ?: return _now.value?.position ?: 0L
        return controller.playbackState?.currentPosition() ?: 0L
    }

    /**
     * Phien dang duoc theo doi, de gui lenh dieu khien.
     *
     * Chon y het `publish`: uu tien phien DANG PHAT, khong co thi lay phien dau
     * con metadata. Phai cung mot phep chon, khong thi nut bam se dieu khien
     * mot bai khac voi bai dang hien loi - va khong ai hieu vi sao.
     */
    private fun chosen(): MediaController? {
        val controllers = attached.keys.toList()
        return controllers.firstOrNull { it.playbackState.isActuallyPlaying() }
            ?: controllers.firstOrNull { it.metadata != null }
    }

    /**
     * Dieu khien bo phat cua app khac.
     *
     * Lam duoc that, va day la duong chinh thuc: quyen doc thong bao cho ta cac
     * `MediaController`, va moi controller mang mot bo `TransportControls`. Do
     * la cach dong ho thong minh va man hinh xe hoi bam nut nhac.
     *
     * Tra `false` khi khong co phien nao - ben goi con biet ma khong ve nut.
     */
    fun dieuKhien(lam: MediaController.TransportControls.() -> Unit): Boolean {
        val c = chosen() ?: return false
        return runCatching { c.transportControls.lam() }.isSuccess
    }

    /** Phien hien tai co cho phep tua khong. */
    fun tuaDuoc(): Boolean {
        val c = chosen() ?: return false
        val actions = c.playbackState?.actions ?: return false
        return actions and PlaybackState.ACTION_SEEK_TO != 0L
    }

    private companion object {
        const val TAG = "AuraWatcher"
    }
}
