package com.mittohoa.lyra.sources

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.mittohoa.lyra.lyrics.normalizeForCompare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Nhac co san trong may.
 *
 * Doc qua `MediaStore` chu khong tu quet thu muc. Android tu 10 khong cho app
 * di lang thang trong bo nho ngoai nua, va dung ra la vay - `MediaStore` la
 * danh muc he thong tu dung san, luon cap nhat, va doc no thi chi can quyen doc
 * NHAC chu khong phai quyen doc moi thu.
 *
 * Khac han hai nguon online o mot diem quan trong: nhac trong may khong co han
 * su dung, khong can goi mang, va khong bao gio bi go. Nen no cung khong di qua
 * `StreamResolver` - dia chi `content://` cua no dung duoc ngay.
 */
object LocalLibrary {

    /**
     * Loc ra nhac, bang cach BO thu chac chan khong phai nhac.
     *
     * Cach hien nhien la doi `is_music = 1`, va no SAI theo mot kieu rat kho
     * thay: cot do co the la NULL khi may chua kip phan loai xong, ma trong SQL
     * thi `NULL != 0` khong phai la "dung" - no la NULL. Ket qua la nhung file
     * vua chep vao may bien mat khoi thu vien khong dau vet, va nguoi dung nhin
     * mot man hinh trong ma khong hieu vi sao.
     *
     * Nen doi chieu nguoc lai: giu tat ca, tru tieng chuong, tieng bao thuc,
     * tieng thong bao va podcast - nhung thu nam chung bang voi nhac nhung
     * khong ai muon thay trong danh sach bai hat.
     */
    private const val MUSIC_ONLY = """
        (is_music IS NULL OR is_music != 0)
        AND (is_ringtone IS NULL OR is_ringtone = 0)
        AND (is_notification IS NULL OR is_notification = 0)
        AND (is_alarm IS NULL OR is_alarm = 0)
        AND (is_podcast IS NULL OR is_podcast = 0)
    """

    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION
    )

    /**
     * Doc ca thu vien, sap theo ten bai.
     *
     * Doc mot lan roi loc trong bo nho thay vi hoi lai `MediaStore` moi lan go
     * mot chu: mot thu vien vai nghin bai van chi la vai tram KB trong bo nho,
     * con hoi lai co so du lieu he thong theo tung phim thi giat thay ro.
     */
    suspend fun all(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val out = ArrayList<Track>(256)
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                MUSIC_ONLY,
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val artist = cursor.getString(artistCol).orEmpty()
                    out.add(
                        Track(
                            id = id.toString(),
                            source = MusicSource.LOCAL,
                            title = cursor.getString(titleCol).orEmpty(),
                            // MediaStore dien "<unknown>" khi the nhac khong ghi
                            // nghe si. De nguyen thi man hinh day chu do
                            artist = if (artist == MediaStore.UNKNOWN_STRING) "" else artist,
                            artworkUrl = albumArtUri(cursor.getLong(albumCol)),
                            durationMs = cursor.getLong(durationCol),
                            streamUrl = trackUri(id)
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Chua cap quyen doc nhac. Khong phai loi - man hinh se hoi
            Log.i(TAG, "Chua co quyen doc nhac trong may")
        } catch (e: Exception) {
            Log.w(TAG, "Khong doc duoc thu vien nhac", e)
        }
        out
    }

    /**
     * Loc thu vien theo tu khoa.
     *
     * So khong dau va khong phan biet hoa thuong: go "nang tho" phai ra
     * "Nàng Thơ". Day la cach nguoi Viet go tren dien thoai, va bat ho go dau
     * de tim mot bai trong may cua chinh ho la vo ly.
     */
    fun filter(library: List<Track>, query: String, limit: Int): List<Track> {
        val needle = normalizeForCompare(query)
        if (needle.isBlank()) return emptyList()
        return library.asSequence()
            .filter {
                normalizeForCompare(it.title).contains(needle) ||
                    normalizeForCompare(it.artist).contains(needle)
            }
            .take(limit)
            .toList()
    }

    fun trackUri(id: Long): String =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

    /**
     * Anh bia cua album.
     *
     * `content://media/external/audio/albumart/<ma>` la duong cu, khong nam
     * trong tai lieu chinh thuc nhung van chay tren moi ban Android tu 4 toi
     * nay. Khong co bia thi mo ra rong - va `Artwork` xu ly duoc chuyen do.
     */
    private fun albumArtUri(albumId: Long): String? =
        if (albumId <= 0) null else "content://media/external/audio/albumart/$albumId"

    private const val TAG = "LyraThuVien"
}
