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

    @Suppress("DEPRECATION")   // `DATA` la duong duy nhat lay ten thu muc tu Android 8
    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA
    )

    /**
     * Ten thu muc chua tep, cat ra tu duong dan day du.
     *
     * `/storage/emulated/0/Music/Off Land - Microcosm/01.mp3` -> `Off Land - Microcosm`
     *
     * Chi lay DOAN CUOI chu khong lay ca duong: man hinh can mot cai nhan doc
     * luot duoc, ma mot duong dan day du thi vua dai vua toan phan khong ai can.
     */
    internal fun tenThuMuc(duong: String?): String {
        if (duong.isNullOrBlank()) return ""
        return duong.substringBeforeLast('/', "").substringAfterLast('/')
    }

    /**
     * Doc ca thu vien, sap theo ten bai.
     *
     * Doc mot lan roi loc trong bo nho thay vi hoi lai `MediaStore` moi lan go
     * mot chu: mot thu vien vai nghin bai van chi la vai tram KB trong bo nho,
     * con hoi lai co so du lieu he thong theo tung phim thi giat thay ro.
     */
    suspend fun all(
        context: Context,
        /**
         * Chi lay tep nam trong may thu muc nay. Rong = khong gioi han.
         *
         * Day la PHAM VI QUET, do nguoi dung dat trong Cai dat. Ap ngay trong
         * cau truy van chu khong loc sau khi doc ve: mot thu vien vai nghin bai
         * ma chi lay mot thu muc thi doc het roi vut di la tra gia cho tat ca
         * nhung bai khong ai hoi toi.
         */
        chiTrong: List<String> = emptyList()
    ): List<Track> = withContext(Dispatchers.IO) {
        // Nhac truoc, video sau. Hai quyen rieng nhau tu Android 13, nen nguoi
        // dung hoan toan co the dong y mot ben va tu choi ben kia - moi ben tu
        // nuot ngoai le cua minh de ben con lai van ra duoc danh sach.
        docNhac(context, chiTrong) + docVideo(context, chiTrong)
    }

    /**
     * Menh de loc theo thu muc, hoac `null` khi khong gioi han.
     *
     * Loc bang `_data` chu khong bang `RELATIVE_PATH`: cot kia chi co tu
     * Android 10, ma AURA chay tu Android 8. Mot cot dung duoc o moi doi thi
     * khong phai viet hai nhanh roi chi kiem duoc mot.
     */
    internal fun locThuMuc(cot: String, duong: List<String>): Pair<String, Array<String>>? {
        if (duong.isEmpty()) return null
        val ve = duong.joinToString(" OR ") { "$cot LIKE ? ESCAPE '\\'" }
        return "($ve)" to duong.map { thoatLike(it) + "/%" }.toTypedArray()
    }

    /**
     * Thoat ba ky tu mang nghia rieng trong `LIKE`.
     *
     * Khong thoat thi mot thu muc ten `My_Nhac` cung khop `MyXNhac`, vi `_`
     * trong LIKE la "mot ky tu bat ky". Chuyen nho, nhung no bien mot pham vi
     * NGUOI DUNG DAT RA thanh mot pham vi rong hon ho tuong - dung cai ma man
     * hinh nay hua la se khong lam.
     */
    internal fun thoatLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /**
     * Video trong may.
     *
     * AURA la trinh phat, khong phai trinh xem phim - nhung ranh gioi nhac va
     * video di ngay qua giua thu nguoi ta nghe: mot MV tai ve, mot ban thu buoi
     * dien, mot bai hat quay tay. Bo video ra ngoai thi dung nhung thu do bi
     * mac ket ngoai app.
     *
     * KHONG loc theo do dai hay theo thu muc. Loc thi luon co mot ai do co dung
     * ba phut nhac trong mot tep hai muoi phut, hoac de MV o cho khong ai ngo.
     */
    @Suppress("DEPRECATION")   // `DATA` la cot duy nhat loc duoc tu Android 8
    private fun docVideo(context: Context, chiTrong: List<String>): List<Track> {
        val ra = ArrayList<Track>(64)
        val loc = locThuMuc(MediaStore.Video.Media.DATA, chiTrong)
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.ARTIST,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.ORIENTATION,
                    MediaStore.Video.Media.DATA
                ),
                loc?.first,
                loc?.second,
                "${MediaStore.Video.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val wCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val hCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val xoayCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.ORIENTATION)
                val duongCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    // Tep video hiem khi co the ghi nghe si. De rong con hon dien
                    // "<unknown>" - man hinh da biet cach an mot dong rong.
                    val artist = c.getString(artistCol).orEmpty()
                    ra.add(
                        Track(
                            id = id.toString(),
                            source = MusicSource.LOCAL,
                            title = c.getString(titleCol).orEmpty(),
                            artist = if (artist == MediaStore.UNKNOWN_STRING) "" else artist,
                            // Anh dai dien la MOT KHUNG HINH lay tu chinh video -
                            // `Artwork` nhan ra dia chi video va rut khung ho.
                            artworkUrl = videoUri(id),
                            // Video khong co the album; thu muc la thu duy nhat
                            // noi duoc no den tu dau.
                            thuMuc = tenThuMuc(c.getString(duongCol)),
                            durationMs = c.getLong(durationCol),
                            streamUrl = videoUri(id),
                            kind = MediaKind.VIDEO,
                            tiLe = tiLeKhungHinh(
                                c.getInt(wCol), c.getInt(hCol), c.getInt(xoayCol)
                            )
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.i(TAG, "Chua co quyen doc video trong may")
        } catch (e: Exception) {
            Log.w(TAG, "Khong doc duoc video trong may", e)
        }
        return ra
    }

    @Suppress("DEPRECATION")   // `DATA` la cot duy nhat loc duoc tu Android 8
    private fun docNhac(context: Context, chiTrong: List<String>): List<Track> {
        val out = ArrayList<Track>(256)
        val loc = locThuMuc(MediaStore.Audio.Media.DATA, chiTrong)
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                if (loc == null) MUSIC_ONLY else "$MUSIC_ONLY AND ${loc.first}",
                loc?.second,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val tenAlbumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val duongCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val artist = cursor.getString(artistCol).orEmpty()
                    val tenAlbum = cursor.getString(tenAlbumCol).orEmpty()
                    out.add(
                        Track(
                            id = id.toString(),
                            source = MusicSource.LOCAL,
                            title = cursor.getString(titleCol).orEmpty(),
                            // MediaStore dien "<unknown>" khi the nhac khong ghi
                            // nghe si. De nguyen thi man hinh day chu do
                            artist = if (artist == MediaStore.UNKNOWN_STRING) "" else artist,
                            artworkUrl = albumArtUri(cursor.getLong(albumCol)),
                            // Cung mot cho dien "<unknown>" nhu nghe si
                            album = if (tenAlbum == MediaStore.UNKNOWN_STRING) "" else tenAlbum,
                            thuMuc = tenThuMuc(cursor.getString(duongCol)),
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
        return out
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

    /**
     * Ti le khung hinh sau khi da tinh goc xoay.
     *
     * Dien thoai quay doc van GHI khung 1920x1080 roi kem mot co xoay 90 do -
     * bo qua co do thi moi video quay bang dien thoai deu bi hieu la nam ngang.
     */
    // `internal` de kiem duoc bang may: goc xoay la cho de sai nhat o day, va
    // sai thi moi video quay bang dien thoai deu nam ngang ma khong ai bao.
    internal fun tiLeKhungHinh(rong: Int, cao: Int, xoay: Int): Float? {
        if (rong <= 0 || cao <= 0) return null
        val dung = xoay == 90 || xoay == 270
        return if (dung) cao.toFloat() / rong else rong.toFloat() / cao
    }

    /** Dia chi `content://` cua mot video trong may. */
    fun videoUri(id: Long): String =
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

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

    private const val TAG = "AuraThuVien"
}
