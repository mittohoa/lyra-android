package com.mittohoa.lyra.data

import android.content.Context
import android.util.Log
import com.mittohoa.lyra.sources.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Mot danh sach phat.
 *
 * `id` rieng chu khong lay ten lam khoa: nguoi dung duoc phep dat hai danh sach
 * trung ten, va duoc phep doi ten ma khong lam dut moi lien he voi cai dang mo.
 */
@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tracks: List<Track> = emptyList(),
    /** Mili-giay. Dung de xep danh sach moi len truoc. */
    val createdAt: Long = 0
)

/**
 * Cac danh sach phat cua nguoi dung.
 *
 * Luu ca bo vao MOT file JSON chu khong moi danh sach mot file: toan bo du lieu
 * nay chi vai chuc KB, luon duoc doc het mot lan, va khong bao gio phai tim
 * kiem. Chia nho ra chi de duoc mot thu la nhieu file hon.
 *
 * Ghi la ghi de ca file. Voi co du lieu nay thi mot lan ghi la vai mili-giay,
 * con doi lai la khong bao gio co canh ghi mot nua roi hong.
 *
 * Day la du lieu NGUOI DUNG TU TAO - khac han bo nho dem loi hay ban dich, von
 * luc nao cung tra lai duoc tu mang. Nen no nam trong `filesDir` chu khong phai
 * `cacheDir`: he thong duoc phep don sach `cacheDir` khi may day.
 */
class PlaylistStore(context: Context) {

    private val file = File(context.filesDir, "playlists.json")
    private val json = Json { ignoreUnknownKeys = true }

    private val _playlists = MutableStateFlow(load())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private fun load(): List<Playlist> = try {
        if (!file.exists()) emptyList()
        else json.decodeFromString<List<Playlist>>(file.readText())
    } catch (e: Exception) {
        // File hong thi bat dau lai tu danh sach rong, con hon la khong mo duoc
        // app. Khong xoa file - de do con co co hoi cuu bang tay.
        Log.w(TAG, "Khong doc duoc danh sach phat", e)
        emptyList()
    }

    private fun persist(next: List<Playlist>) {
        _playlists.value = next
        try {
            file.writeText(json.encodeToString(ListSerializer(Playlist.serializer()), next))
        } catch (e: Exception) {
            Log.w(TAG, "Khong ghi duoc danh sach phat", e)
        }
    }

    /** Tao danh sach moi tu cac bai dang co. Tra ve ma cua no. */
    fun create(name: String, tracks: List<Track>): String {
        val playlist = Playlist(
            name = name.trim().ifBlank { "Danh sách không tên" },
            tracks = tracks,
            createdAt = System.currentTimeMillis()
        )
        // Moi nhat len dau: danh sach vua tao gan nhu chac chan la cai nguoi
        // dung sap mo
        persist(listOf(playlist) + _playlists.value)
        return playlist.id
    }

    fun rename(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        persist(_playlists.value.map { if (it.id == id) it.copy(name = trimmed) else it })
    }

    fun delete(id: String) {
        persist(_playlists.value.filterNot { it.id == id })
    }

    /**
     * Them mot bai vao cuoi danh sach.
     *
     * Trung bai thi khong them lai. Mot danh sach phat co hai ban giong het nhau
     * gan nhu luon la mot cu bam nham, khong phai y muon.
     */
    fun add(id: String, track: Track) {
        persist(
            _playlists.value.map { playlist ->
                if (playlist.id != id) playlist
                else if (playlist.tracks.any { it.playbackUri == track.playbackUri }) playlist
                else playlist.copy(tracks = playlist.tracks + track)
            }
        )
    }

    fun removeAt(id: String, index: Int) {
        persist(
            _playlists.value.map { playlist ->
                if (playlist.id != id) playlist
                else playlist.copy(tracks = playlist.tracks.filterIndexed { i, _ -> i != index })
            }
        )
    }

    fun byId(id: String): Playlist? = _playlists.value.firstOrNull { it.id == id }

    private companion object {
        const val TAG = "LyraDanhSach"
    }
}
