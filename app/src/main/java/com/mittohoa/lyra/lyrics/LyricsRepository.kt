package com.mittohoa.lyra.lyrics

import android.util.Log
import com.mittohoa.lyra.data.LyricCache
import com.mittohoa.lyra.data.ManualLyricStore
import com.mittohoa.lyra.data.OffsetStore
import com.mittohoa.lyra.media.NowPlaying
import com.mittohoa.lyra.sources.LrclibClient
import com.mittohoa.lyra.sources.NguonNgoai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tim loi cho bai dang phat o app khac.
 *
 * Khac han voi tim loi cho bai trong may: o day ta khong biet chac ten bai va
 * nghe si, chi co thu app khai bao. Quy trinh:
 *
 *   1. Xem da nho san chua - nghe lai mot bai la chuyen rat thuong
 *   2. Sinh nhieu phuong an tu thu app khai bao (`candidatesFrom`)
 *   3. Voi tung phuong an, hoi CA BA NGUON CUNG LUC
 *   4. Chi nhan khi TEN TRA VE DU GIONG - hien nham loi bai khac con te hon la
 *      khong hien gi
 *   5. Uu tien ban co moc thoi gian
 */
class LyricsRepository(
    private val scope: CoroutineScope,
    private val cache: LyricCache?,
    private val offsets: OffsetStore?,
    private val manual: ManualLyricStore?,
    /**
     * Doc loi nam canh tep nhac dang phat, hoac null khi khong co.
     *
     * Truyen vao mot ham chu khong phai mot doi tuong: chi ben ngoai moi biet
     * bai dang phat co phai nhac trong may khong, va tep nao. Kho loi khong can
     * biet chuyen do.
     */
    private val lrcCanhTep: (suspend () -> Lyrics?)? = null
) {

    /** Bai dang phat, giu lai de con nho do lech theo dung bai. */
    private var current: NowPlaying? = null

    private val _lyrics = MutableStateFlow(Lyrics.NONE)
    val lyrics: StateFlow<Lyrics> = _lyrics.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Bai da tra gan nhat - tranh tra lai lien tuc theo tung ban tin. */
    private var resolvedKey: String? = null
    private var job: Job? = null

    fun onNowPlaying(now: NowPlaying?) {
        val key = now?.key
        if (key == resolvedKey) return
        resolvedKey = key
        current = now

        job?.cancel()

        if (now == null || now.title.isBlank()) {
            _lyrics.value = Lyrics.NONE
            _loading.value = false
            return
        }

        _lyrics.value = Lyrics.NONE
        _loading.value = true
        Log.i(TAG, "Tim loi: '${now.artist}' - '${now.title}' (${now.duration}ms, ${now.packageName})")

        // BA NGUON GAN DEU DOC DIA, VA KHONG DUOC DOC TREN LUONG VE.
        //
        // Truoc day ca ba nam thang trong ham nay - ma ham nay chay tren
        // `Dispatchers.Main.immediate`, tuc dung luong dang ve man hinh. Voi mot
        // tep loi vai KB thi khong ai thay; nhung tu khi them phan doc tep nam
        // canh, moi lan doi bai con keo theo mot cau hoi MediaStore - la mot
        // vong goi sang tien trinh khac. Do tren may: "Skipped 131 frames", va
        // co lan he thong bao han "Lyra khong phan hoi".
        //
        // Day het xuong luong nen. Cai gia la mot nhip hinh trong truoc khi loi
        // hien ra o nhung bai da nho san - truoc kia hien ngay tuc thi. Mot nhip
        // hinh thi khong ai kip thay, con mot lan treo nam giay thi ai cung thay.
        job = scope.launch {
            // Loi TU NHAP di truoc moi thu, ke ca bo nho dem. Nguoi dung da bo
            // cong go thi khong the de mot lan tra mang ghi de len.
            val tuNhap = withContext(Dispatchers.IO) { manual?.get(now.artist, now.title) }
            if (tuNhap != null) {
                _lyrics.value = dress(parseLrc(tuNhap, from = "tự nhập"), now)
                _loading.value = false
                return@launch
            }

            // Loi nam CANH TEP NHAC dung ngay sau loi tu nhap, tren ca bo nho
            // dem lan moi nguon mang. Nguoi dung TU DE tep do o day, nen no dang
            // tin gan bang loi tu go: no la lua chon cua ho, con ban tra mang
            // chi la phong doan cua may.
            // KHONG boc `withContext(IO)` o day: ham nay phai bat dau tren LUONG
            // CHINH vi no hoi `MediaController` xem dang phat tep nao, ma
            // `MediaController` nem thang neu bi goi tu luong khac. No tu chuyen
            // xuong luong nen cho doan doc dia. Boc o day thi sap ngay - da thu.
            val canh = lrcCanhTep?.invoke()
            if (canh != null) {
                _lyrics.value = dress(canh, now)
                _loading.value = false
                return@launch
            }

            val nho = withContext(Dispatchers.IO) { cache?.get(now.artist, now.title) }
            if (nho != null) {
                _lyrics.value = dress(nho, now)
                _loading.value = false
                return@launch
            }

            val found = resolve(now)
            _lyrics.value = found?.let { dress(it, now) } ?: Lyrics.NONE
            _loading.value = false
            if (found != null) {
                cache?.put(now.artist, now.title, found)
                Log.i(TAG, "Khop: '${found.matchedArtist}' - '${found.matchedTitle}' tu ${found.from}")
            } else {
                Log.i(TAG, "Khong tim ra loi cho '${now.artist}' - '${now.title}'")
            }
        }
    }

    /**
     * Thu tung phuong an, moi phuong an hoi ca ba nguon CUNG LUC.
     *
     * Hoi lan luot thi thoi gian cho la TONG cua ba lan goi, va voi ba phuong
     * an la chin lan - nguoi dung ngoi nhin khung trong hang chuc giay. Hoi
     * song song thi thoi gian cho chi bang lan goi CHAM NHAT trong ba, va
     * thuong dung o phuong an dau.
     *
     * Van duyet theo phuong an o vong ngoai chu khong gop het chin lan goi:
     * phuong an dau (thu app tu khai bao) dung hon han, nen dang no mot ket qua
     * tot con hon mot ket qua tot cua phuong an doan mo.
     */
    /**
     * Tra loi cho mot bai bat ky, KHONG dong toi bai dang phat.
     *
     * Dung khi tai mot bai ve may: bai duoc tai thuong khong phai bai dang
     * nghe, va mot lan tra o day khong duoc phep lam doi loi tren man hinh.
     */
    suspend fun lookup(artist: String, title: String, durationMs: Long): Lyrics? =
        resolve(artist, title, durationMs)

    private suspend fun resolve(now: NowPlaying): Lyrics? =
        resolve(now.artist, now.title, now.duration, now.album)

    private suspend fun resolve(
        artist: String,
        title: String,
        durationMs: Long,
        album: String = ""
    ): Lyrics? {
        val candidates = candidatesFrom(
            RawNowPlaying(
                title = title,
                artist = artist.ifBlank { null },
                album = album.ifBlank { null }
            )
        ).take(MAX_CANDIDATES)

        Log.d(TAG, "${candidates.size} phuong an: ${candidates.joinToString { "'${it.artist}'/'${it.title}'" }}")

        var plainFallback: Lyrics? = null

        for (c in candidates) {
            val results = coroutineScope {
                SOURCES.map { (name, fetch) ->
                    async {
                        try {
                            fetch(c.artist, c.title, durationMs)
                        } catch (e: CancellationException) {
                            // Doi bai giua chung thi lan tra cu bi huy - phai NEM
                            // TIEP, khong duoc nuot. Nuot ngoai le huy la pha vo
                            // co che huy co cau truc cua coroutine: cong viec cu
                            // van chay tiep sau khi da bi huy, va nhat ky thi ghi
                            // nham thanh "nguon khong tra loi duoc".
                            throw e
                        } catch (e: Exception) {
                            Log.d(TAG, "$name khong tra loi duoc", e)
                            null
                        }
                    }
                }.awaitAll()
            }

            for (hit in results.filterNotNull()) {
                // Ten tra ve phai du giong. Nguon nao cung tra ve "gan dung"
                // con hon khong tra gi - ma "gan dung" o day nghia la bai khac.
                val score = titleSimilarity(hit.matchedTitle, c.title)
                if (score < MIN_SIMILARITY) {
                    Log.d(TAG, "Bo qua '${hit.matchedTitle}' - chi giong ${(score * 100).toInt()}%")
                    continue
                }

                if (hit.synced) return hit
                if (plainFallback == null) plainFallback = hit
            }
        }

        return plainFallback
    }

    /**
     * Gan do lech da nho va co "moc dang ngo" vao ket qua.
     *
     * Tach rieng khoi `resolve` vi hai viec khac han: `resolve` di tim NOI DUNG
     * loi, con day la doi chieu no voi bai DANG PHAT. Bo nho dem chi luu noi
     * dung, nen phan nay phai chay lai moi lan - do dai bai va do lech thuoc ve
     * lan nghe nay, khong thuoc ve loi.
     */
    private fun dress(lyrics: Lyrics, now: NowPlaying): Lyrics = lyrics.copy(
        offset = offsets?.get(now.artist, now.title) ?: 0L,
        timingSuspect = lyrics.synced && isSuspect(lyrics.sourceDuration, now.duration)
    )

    /**
     * Do dai lech nhieu nghia la KHAC BAN THU.
     *
     * Tim dung ten bai khong co nghia la dung ban thu. Loi cua ban thu phong dap
     * len mot ban hat live thi lech tu dau den cuoi. Do dai la manh moi re nhat
     * va dang tin nhat de biet dieu do.
     *
     * Nguong 15 giay: hai ban phat hanh cua cung mot bai hiem khi lech qua chung
     * do, con ban live hay ban co dao dau dai thi gan nhu luon vuot.
     */
    private fun isSuspect(sourceMs: Long, playingMs: Long): Boolean {
        if (sourceMs <= 0 || playingMs <= 0) return false
        return kotlin.math.abs(sourceMs - playingMs) > SUSPECT_GAP_MS
    }

    /**
     * Can lai theo dong nguoi dung vua cham.
     *
     * Nguoi dung nghe thay dang hat cau nao thi cham vao cau do - mot cu cham la
     * khop lai ca bai. Hon han kieu bam +/- nua giay mot lan.
     *
     * `activeLineIndex` so `position + offset` voi moc cua tung dong, nen de
     * dong `index` thanh dong dang hat thi do lech chinh la hieu giua moc cua no
     * va vi tri hien tai.
     */
    fun syncToLine(index: Int, positionMs: Long) {
        val lyrics = _lyrics.value
        val line = lyrics.lines.getOrNull(index) ?: return
        val now = current ?: return

        val offset = line.time - positionMs
        _lyrics.value = lyrics.copy(offset = offset, timingSuspect = false)
        offsets?.put(now.artist, now.title, offset)
        Log.i(TAG, "Can lai theo dong $index: lech ${offset}ms")
    }

    /**
     * Luu loi nguoi dung tu nhap va dung no ngay.
     *
     * Nhan ca chuoi tho: co the la loi chu tron, co the la .lrc day du moc
     * thoi gian dan tu noi khac. `parseLrc` lo ca hai - khong co moc thi tra
     * ve dang chu tron, va giao dien tu biet khong to sang dong nao.
     */
    fun saveManual(raw: String) {
        val now = current ?: return
        manual?.put(now.artist, now.title, raw)

        if (raw.isBlank()) {
            // Xoa loi tu nhap thi tra lai dung bai, khong de man hinh trong
            resolvedKey = null
            onNowPlaying(now)
            return
        }
        _lyrics.value = dress(parseLrc(raw, from = "tự nhập"), now)
        _loading.value = false
        Log.i(TAG, "Da luu loi tu nhap cho '${now.title}'")
    }

    /**
     * Tra lai loi cho bai dang phat, bo qua ket qua da tra truoc do.
     *
     * Dung sau khi khoi phuc tu ban sao luu: bai dang mo tren man hinh co the
     * vua duoc them loi tu nhap, ma `onNowPlaying` thi thay khoa khong doi nen
     * khong lam gi ca. Khong co ham nay thi nguoi dung phai doi sang bai khac
     * roi quay lai moi thay - va se tuong la khoi phuc hong.
     */
    fun lamMoi() {
        resolvedKey = null
        onNowPlaying(current)
    }

    /** Chuoi tho de mo ra sua; rong neu chua tung nhap. */
    fun manualDraft(): String {
        val now = current ?: return ""
        manual?.get(now.artist, now.title)?.let { return it }

        // Chua nhap thi mo san bang loi dang co, de nguoi dung SUA thay vi go
        // lai tu dau - phan lon truong hop chi sai vai dong
        val lyrics = _lyrics.value
        if (lyrics.isEmpty) return ""
        return lyrics.lines.joinToString("\n") { line ->
            if (lyrics.synced) "[%02d:%02d.%02d]%s".format(
                line.time / 60_000,
                (line.time / 1000) % 60,
                (line.time % 1000) / 10,
                line.text
            ) else line.text
        }
    }

    /** Bo do lech da chinh, tra ve dung moc goc cua nguon. */
    fun clearOffset() {
        val now = current ?: return
        _lyrics.value = _lyrics.value.copy(offset = 0)
        offsets?.put(now.artist, now.title, 0)
    }

    private companion object {
        const val TAG = "LyraLyrics"

        /** Do dai lech qua nguong nay thi coi la khac ban thu (mili-giay). */
        const val SUSPECT_GAP_MS = 15_000L

        /**
         * Thu tu chi con y nghia khi nhieu nguon cung tra ve ban co moc: luc do
         * ban dau danh sach thang. LRCLIB dat truoc vi no co ca nhac quoc te.
         */
        val SOURCES: List<Pair<String, suspend (String, String, Long) -> Lyrics?>> =
            listOf<Pair<String, suspend (String, String, Long) -> Lyrics?>>(
                "lrclib" to LrclibClient::fetch
            ) + NguonNgoai.NGUON_LOI

        /** Duoi nguong nay coi nhu tra nham bai. */
        const val MIN_SIMILARITY = 0.6

        /**
         * So phuong an dam thu. Moi phuong an la ba lan goi mang chay song song.
         *
         * Phai la 4 chu khong phai 3. Ten video kieu
         *   "Nhà Tôi Có Treo Một Lá Cờ - Noo Phước Thịnh tại Concert ..."
         * co TEN BAI dung truoc, trong khi quy tac cua dau "-" gia dinh nghe si
         * dung truoc. Phuong an dao chieu - cai DUNG - xep hang 4, cat o 3 la
         * mat han. Da gap that tren may.
         */
        const val MAX_CANDIDATES = 4
    }
}
