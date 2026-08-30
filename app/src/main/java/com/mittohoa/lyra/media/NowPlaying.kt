package com.mittohoa.lyra.media

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.SystemClock

/**
 * Bai dang phat o mot app KHAC, doc qua MediaSession cua Android.
 *
 * `position` la vi tri TAI THOI DIEM HOI, da bu phan thoi gian troi qua ke tu
 * anh chup cua he thong - xem `currentPosition`.
 */
data class NowPlaying(
    /** Ten goi cua app dang phat, vd. "com.spotify.music". */
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    /** Do dai bai, mili-giay; 0 khi app khong khai bao. */
    val duration: Long,
    /** Vi tri phat, mili-giay. */
    val position: Long,
    val isPlaying: Boolean,
    val artwork: Bitmap? = null
) {
    /** Khoa nhan dang bai, de biet khi nao can di tim loi moi. */
    val key: String get() = "$packageName|$artist|$title"
}

/**
 * Vi tri phat o thoi diem HIEN TAI.
 *
 * `PlaybackState.position` chi la mot ANH CHUP tai `lastPositionUpdateTime`,
 * khong phai vi tri bay gio. Khong bu phan da troi thi loi luon chay cham hon
 * nhac vai giay - dung cai bay da gap o ban Windows voi SMTC.
 *
 * `lastPositionUpdateTime` dung dong ho `elapsedRealtime` (thoi gian tu luc
 * khoi dong may), KHONG phai `currentTimeMillis` (thoi gian thuc). So nham hai
 * thang nay ra sai lech hang chuc nam.
 */
fun PlaybackState.currentPosition(): Long {
    if (state != PlaybackState.STATE_PLAYING) return position
    val drift = SystemClock.elapsedRealtime() - lastPositionUpdateTime
    if (drift <= 0) return position
    return position + (drift * playbackSpeed).toLong()
}

/** Trang thai nay co nghia la dang phat khong. */
fun PlaybackState?.isActuallyPlaying(): Boolean = this?.state == PlaybackState.STATE_PLAYING

/**
 * Gop metadata va trang thai thanh mot ban tin.
 * Tra ve null khi app chua khai bao gi dang ke - khong co ten bai thi khong tra
 * cuu duoc, ma hien mot dong trong con te hon khong hien.
 */
fun buildNowPlaying(
    packageName: String,
    metadata: MediaMetadata?,
    state: PlaybackState?
): NowPlaying? {
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
    if (title.isEmpty()) return null

    return NowPlaying(
        packageName = packageName,
        title = title,
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty(),
        album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim().orEmpty(),
        duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
        position = state?.currentPosition() ?: 0L,
        isPlaying = state.isActuallyPlaying(),
        // Uu tien ART; nhieu app chi dat ALBUM_ART hoac chi dat mot trong hai
        artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
    )
}
