package com.mittohoa.lyra.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mittohoa.lyra.service.Lyra
import com.mittohoa.lyra.ui.MainActivity

/**
 * Bo may phat cua chinh AURA.
 *
 * Tu day tro di AURA khong con chi la ke dung xem app khac phat gi. Doi lai duoc
 * mot thu ma che do dong hanh khong bao gio co: **dong ho phat chinh xac**.
 *
 * Nghe qua app khac, AURA chi biet vi tri qua cac ban tin `MediaSession`, va co
 * app de ban tin cu hang tram giay - do la goc cua moi lan loi chay lech. Khi
 * chinh AURA phat, vi tri la mot phep hoi truc tiep vao bo giai ma: dung tung
 * mili-giay, khong can can lech, khong can cham de khop.
 *
 * `MediaSessionService` chu khong phai foreground service tu viet: he thong tu
 * lo cai thong bao, cai the tren man hinh khoa, nut tren tai nghe, va o dieu
 * khien am thanh. Tu lam lai nhung thu do la tu chuoc lay bay loi cua nguoi
 * khac da sua xong.
 */
class LyraPlaybackService : MediaLibraryService() {

    private var session: MediaLibraryService.MediaLibrarySession? = null
    private var chuyenMuot: ChuyenMuot? = null

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

        session = MediaLibrarySession.Builder(this, player, CayCuaAura())
            .setSessionActivity(openApp())
            .build()

        Lyra.attachPlayer(player)
        chuyenMuot = ChuyenMuot(this, player)
    }

    /** Cham vao the media thi mo lai AURA, khong dung lai tu dau. */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /**
     * Cay duyet ma Android Auto doc.
     *
     * Manifest KHAI `MediaBrowserService` tu lau, ma dich vu lai la
     * `MediaSessionService` - thu khong co cay duyet nao. Auto vi vay thay AURA
     * trong danh sach, ket noi duoc, roi mo ra mot thu vien trong. Khai mot thu
     * ma khong lam thi te hon khong khai.
     *
     * Moi ham deu tra ve mot `Future` da xong san: du lieu nam san trong bo nho
     * cua `Lyra`, khong co lan doc dia hay goi mang nao. Day mot viec da xong
     * qua mot luong khac chi them mot nhip cho khong duoc gi.
     */
    private inner class CayCuaAura : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // HAI CAU HOI KHAC NHAU, tra loi khac nhau.
            //
            // `isRecent` la khay media cua he thong do xem co gi de nghe tiep
            // khong - no hoi sau moi lan khoi dong lai may, de bay ra mot nut
            // nghe tiep truoc ca khi nguoi dung mo app. Cho do chi du bay MOT
            // bai; tra ca cay thu vien vao day la tra lam mot cau hoi khac.
            //
            // Chua nghe bai nao thi noi thang la khong co, chu khong tra mot
            // goc rong: mot nut "nghe tiep" bam vao khong ra gi con te hon
            // khong co nut.
            if (params?.isRecent == true) {
                if (CayDuyet.ngheTiep() == null) {
                    return Futures.immediateFuture(
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_NOT_SUPPORTED)
                    )
                }
                return Futures.immediateFuture(
                    LibraryResult.ofItem(CayDuyet.gocNgheTiep(), params)
                )
            }
            return Futures.immediateFuture(LibraryResult.ofItem(CayDuyet.goc(), params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            ma: String,
            trang: Int,
            soMoiTrang: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            // Cat trang DUNG nhu dau may hoi. Tra ca hai nghin bai mot luot thi
            // ban tin qua ranh gioi tien trinh phinh to, va Android cat thang
            // ban tin qua lon - mat sach chu khong phai cham.
            val het = CayDuyet.con(ma)
            val tu = (trang * soMoiTrang).coerceAtMost(het.size)
            val den = (tu + soMoiTrang).coerceAtMost(het.size)
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(het.subList(tu, den)), params)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            ma: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val muc = CayDuyet.mot(ma)
                ?: return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(muc, null))
        }

        /**
         * Dau may gui mot bai de phat.
         *
         * Dung mot bai roi im KHONG phai thu nguoi ta muon: cham mot bai la
         * "phat tu day tro di", giong het trong app. Nen tra ve ca nhanh chua
         * no kem cho bat dau.
         *
         * Bai khong con trong thu vien - tep da xoa, the nho da rut - thi tra
         * lai dung danh sach dau may gui, de bo phat bao mot loi binh thuong
         * thay vi ta tu dung mot hang doi rong roi im lang.
         */
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            cacMuc: List<MediaItem>,
            viTri: Int,
            moc: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // CHI CAN THIEP VAO LENH TU BEN NGOAI.
            //
            // Chinh AURA cung dat hang doi qua duong nay, va hang doi cua no
            // da dung san - co dia chi phat, co cau dang hat gan vao phan mo
            // ta. Dung lai bang cay duyet la vut het nhung thu do di.
            //
            // Dam phai that khi thu tren may: nhac nap vao roi dung ngay,
            // khong bao loi gi, vi muc dung lai khong co dia chi de mo.
            if (controller.packageName == packageName) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(cacMuc, viTri, moc)
                )
            }

            val ma = cacMuc.firstOrNull()?.mediaId
            val doi = ma?.let { CayDuyet.hangDoiCho(it) }
                ?: return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(cacMuc, viTri, moc)
                )
            val (ds, i) = doi
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(ds.map { CayDuyet.bai(it) }, i, 0L)
            )
        }
    }

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
        // Bo nghe TRUOC khi huy bo phat: `thoi` cham vao player, ma sau
        // `release()` thi moi cai cham deu nem loi.
        chuyenMuot?.thoi()
        chuyenMuot = null
        Lyra.detachPlayer()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
