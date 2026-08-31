package com.mittohoa.lyra.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.ui.MainActivity

/**
 * Bo may phat cua chinh Lyra.
 *
 * Tu day tro di Lyra khong con chi la ke dung xem app khac phat gi. Doi lai duoc
 * mot thu ma che do dong hanh khong bao gio co: **dong ho phat chinh xac**.
 *
 * Nghe qua app khac, Lyra chi biet vi tri qua cac ban tin `MediaSession`, va co
 * app de ban tin cu hang tram giay - do la goc cua moi lan loi chay lech. Khi
 * chinh Lyra phat, vi tri la mot phep hoi truc tiep vao bo giai ma: dung tung
 * mili-giay, khong can can lech, khong can cham de khop.
 *
 * `MediaSessionService` chu khong phai foreground service tu viet: he thong tu
 * lo cai thong bao, cai the tren man hinh khoa, nut tren tai nghe, va o dieu
 * khien am thanh. Tu lam lai nhung thu do la tu chuoc lay bay loi cua nguoi
 * khac da sua xong.
 */
class LyraPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Bo qua the ID3 trong file nhac.
        //
        // ExoPlayer doc the ID3 roi GHI DE len phan mo ta ta da dat cho bai -
        // ke ca khi ta dat sau. Ta thi dat cau dang hat vao do de no hien tren
        // the man hinh khoa, nen de nguyen la cu vai giay lai bi keo nguoc ve
        // ten bai.
        //
        // Bo di khong mat gi: ten bai va nghe si ta lay tu chinh nguon nhac,
        // dung hon the ID3 nhieu. The cua NCT con nhet ca "NhacCuaTui.com" vao
        // o mo ta.
        val extractors = DefaultExtractorsFactory()
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_DISABLE_ID3_METADATA)

        // Hang doi chua dia chi gia `lyra://<nguon>/<ma>`; duong phat that duoc
        // hoi ngay truoc khi mo dong byte dau tien - xem `StreamResolver`.
        val dataSource = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(this),
            StreamResolver()
        )

        val player = ExoPlayer.Builder(
            this,
            DefaultMediaSourceFactory(dataSource, extractors)
        )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                // true = xin quyen uu tien am thanh, va tu ha nho khi co thong
                // bao chen ngang. Khong xin thi hai app cung phat mot luc.
                true
            )
            // Cam tai nghe ra thi dung, khong phat oang oang ra loa ngoai
            .setHandleAudioBecomingNoisy(true)
            .build()

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp())
            .build()

        Lyra.attachPlayer(player)
    }

    /** Cham vao the media thi mo lai Lyra, khong dung lai tu dau. */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /**
     * Nguoi dung vuot bo app khoi danh sach gan day.
     *
     * Dang phat thi GIU NGUYEN - vuot app khoi danh sach khong co nghia la
     * "dung nhac", va dung han la cach nhanh nhat de nguoi dung buc minh. Dang
     * tam dung thi khong con ly do gi de o lai, dong luon cho nhe may.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Lyra.detachPlayer()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
