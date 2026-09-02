package com.mittohoa.lyra.sources

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Nguon nhac AURA tim va phat duoc. */
@Serializable
enum class MusicSource(val key: String, val label: String) {
    /** Nhac co san trong may. Khong can mang, khong co han, khong bi go. */
    LOCAL("may", "Trong máy"),
    ZING("zing", "Zing MP3"),
    NCT("nct", "NhacCuaTui")
}

/**
 * Loai noi dung.
 *
 * Hom nay chi co nhac. Dat san o day vi ranh gioi nhac/video di xuyen qua ca
 * app - be may phat, san khau tren man hinh, kieu cua so noi - va sua mot
 * ranh gioi da chay khap noi thi dat hon nhieu so voi khai no ra tu dau. Bo
 * giai ma Media3 von phat duoc ca hai; cai thieu chi la mot be mat de ve.
 */
@Serializable
enum class MediaKind { AUDIO, VIDEO }

/**
 * Mot bai tim duoc tu nguon online.
 *
 * `streamUrl` co the co san (NCT tra kem ngay trong ket qua tim) hoac chua
 * (Zing phai hoi rieng). Ben goi khong can biet khac biet do - `Catalog.streamUrl`
 * lo ca hai.
 */
@Serializable
data class Track(
    val id: String,
    val source: MusicSource,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val durationMs: Long = 0,
    /**
     * KHONG luu xuong dia: duong phat cua ca hai nguon online deu co han, va
     * mot danh sach phat mo lai sau mot tuan ma mang theo hai chuc duong da
     * chet thi te hon han la khong mang gi - `Catalog.streamUrl` hoi lai duoc.
     */
    @Transient
    val streamUrl: String? = null,
    val kind: MediaKind = MediaKind.AUDIO,
    /**
     * Ti le khung hinh video: rong chia cao. `null` voi nhac.
     *
     * Doc tu MediaStore luc quet thu vien chu KHONG doi bo giai ma bao. Da thu
     * duong kia: gan `Player.Listener` vao `MediaController` roi cho
     * `onVideoSizeChanged`, va no khong bao gio duoc goi - video van phat ra
     * hinh binh thuong nhung ti le thi khong bao gio toi. MediaStore thi biet
     * san kich thuoc tu luc quet, khong phai cho ai ca.
     */
    val tiLe: Float? = null
) {
    /**
     * Duong dan gia dung trong hang doi.
     *
     * Khong dat duong phat that vao hang doi vi hai le. Mot: duong phat cua ca
     * hai nguon deu CO HAN - xep hai chuc bai vao hang doi thi bai cuoi co the
     * het han truoc khi toi luot. Hai: Zing phai goi mang mot lan cho moi bai,
     * va goi hai chuc lan chi de xep hang doi la bat nguoi dung cho mot viec ho
     * chua chac lam.
     *
     * Duong that duoc hoi ngay truoc khi phat - xem `StreamResolver`.
     */
    val playbackUri: String get() = buildString {
        append("lyra://").append(source.key).append('/').append(id)
        // Mang theo LOAI, vi hang doi chi con dia chi nay chu khong con doi
        // tuong Track. Khong mang thi luc giai ra phai doan, ma doan mac dinh
        // la nhac - nen mot video xep vao hang doi se duoc tra ve dia chi cua
        // bang audio, tro vao cho khong co gi, va bam phat khong len tieng.
        if (kind == MediaKind.VIDEO) append("?loai=video")
    }
}

/**
 * Gop cac nguon nhac lai thanh mot cho tim.
 */
object Catalog {

    /**
     * Nhac trong may, doc mot lan roi giu lai.
     *
     * Gan tu ben ngoai (`AURA`) chu khong tu doc: lop nay khong giu `Context`,
     * va mot doi tuong dung chung ma om `Context` la mot cach ro ket bo nho.
     */
    @Volatile
    var library: List<Track> = emptyList()

    /**
     * Tim o may VA o moi nguon online cung luc, roi tron ket qua.
     *
     * Tron kieu cai rang lua chu khong noi duoi: mot nguon tra ve nhieu ket qua
     * rac ma xep truoc thi day het ket qua tot cua nguon kia xuong duoi man hinh.
     */
    suspend fun search(query: String, limit: Int = 20): List<Track> {
        if (query.isBlank()) return emptyList()

        // Nhac trong may xep TRUOC ket qua online, va khong tinh vao han muc
        // tron: no khong ton mang, phat duoc khi mat song, va nguoi dung da co
        // no roi - khong co ly do gi bat ho cuon qua ket qua online de tim.
        val mine = LocalLibrary.filter(library, query, MAX_LOCAL)

        // Bao nhieu nguon online la chuyen cua bien the dung: ban tai thang co
        // hai, ban Play khong co cai nao. Tron kieu N nguon chu khong viet cung
        // hai, de cho nay khong phai sua lai khi so nguon doi.
        val online = NguonNgoai.tim(query, limit)

        val out = ArrayList<Track>(mine.size + online.sumOf { it.size })
        val seen = HashSet<String>()
        for (track in mine) {
            seen.add("${track.artist.lowercase()}|${track.title.lowercase()}")
            out.add(track)
        }
        val sauNhat = online.maxOfOrNull { it.size } ?: 0
        for (i in 0 until sauNhat) {
            for (nguon in online) {
                val track = nguon.getOrNull(i) ?: continue
                // Cung mot bai o hai nguon thi chi giu ban gap truoc
                val key = "${track.artist.lowercase()}|${track.title.lowercase()}"
                if (seen.add(key)) out.add(track)
            }
        }
        return out.take(limit + mine.size)
    }

    /** Duong phat that cho mot bai, hoac null khi nguon tu choi. */
    suspend fun streamUrl(track: Track): String? = when (track.source) {
        // File trong may: dia chi `content://` dung duoc ngay, khong hoi ai ca
        MusicSource.LOCAL -> track.streamUrl ?: track.id.toLongOrNull()?.let {
            if (track.kind == MediaKind.VIDEO) LocalLibrary.videoUri(it)
            else LocalLibrary.trackUri(it)
        }
        // Ban Play tra null o day. Danh sach phat cu mang theo bai Zing/NCT thi
        // bai do bao loi phat - dung han, va tot hon la giu mot nut bam khong
        // bao gio an gi.
        else -> NguonNgoai.duongPhat(track)
    }

    /**
     * Duong phat cho mot dia chi `lyra://<nguon>/<ma>`.
     *
     * Dung tu bo giai ma, noi chi con lai dia chi chu khong con doi tuong
     * `Track`. Khong nhan ra thi tra null - ben goi bao loi phat, khong doan bua.
     */
    suspend fun streamUrl(uri: Uri): String? {
        val source = MusicSource.entries.firstOrNull { it.key == uri.host } ?: return null
        val id = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: return null
        val loai =
            if (uri.getQueryParameter("loai") == "video") MediaKind.VIDEO else MediaKind.AUDIO
        return streamUrl(
            Track(id = id, source = source, title = "", artist = "", kind = loai)
        )
    }

    /** Toi da bao nhieu bai trong may cho mot lan tim. */
    private const val MAX_LOCAL = 8
}
