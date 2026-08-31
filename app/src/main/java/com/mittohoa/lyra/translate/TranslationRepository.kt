package com.mittohoa.lyra.translate

import android.util.Log
import com.mittohoa.lyra.data.TranslatePrefs
import com.mittohoa.lyra.data.TranslateSettings
import com.mittohoa.lyra.data.TranslationCache
import com.mittohoa.lyra.lyrics.Lyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Trang thai ban dich cua bai dang phat.
 *
 * Tach hen khoi `Lyrics`: loi la thu tim duoc mot lan roi thoi, con ban dich di
 * qua nhieu chang - doan ngon ngu, tim trong bo nho dem, co khi phai tai goi ve
 * may - va giao dien can biet dang o chang nao de noi cho dung.
 */
sealed interface TranslationState {

    /** Khong co gi de noi: dang tat, chua co loi, hoac loi da dung tieng roi. */
    data object Idle : TranslationState

    /** Dang doan ngon ngu hoac dang dich. */
    data object Working : TranslationState

    /** Dich xong. Do dai luon bang so dong cua loi. */
    data class Done(val lines: List<String>, val fromLanguage: String) : TranslationState

    /**
     * Dich duoc, nhung phai tai goi ngon ngu ve may truoc.
     *
     * Khong tu tai: goi nang vai chuc me-ga-bai, ma nguoi dung co the dang dung
     * 4G va dang nghe mot bai ho khong quan tam nghia. Hoi mot cau roi de ho
     * quyet dinh.
     */
    data class NeedsModel(val language: String) : TranslationState

    /** Da thu va khong xong. */
    data class Failed(val why: String) : TranslationState
}

/**
 * Dich loi khi - va chi khi - can.
 *
 * Thu tu cac chang duoc xep theo gia phai tra, re truoc dat sau:
 *
 *   1. Tat, hoac khong co loi -> khong lam gi
 *   2. Doan ngon ngu (tren may, vai mili-giay, khong tai gi)
 *   3. Loi da la tieng nguoi dung doc -> khong lam gi. Day la truong hop THUONG
 *      GAP NHAT voi nguoi Viet nghe nhac Viet, va no khong ton mot byte nao
 *   4. Da dich lan truoc -> lay trong bo nho dem
 *   5. Goi ngon ngu co san tren may -> dich ngay
 *   6. Chua co goi -> hoi nguoi dung roi moi tai
 */
class TranslationRepository(
    private val scope: CoroutineScope,
    private val cache: TranslationCache?,
    private val prefs: TranslatePrefs?
) {

    private val _state = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val state: StateFlow<TranslationState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(prefs?.read() ?: TranslateSettings())
    val settings: StateFlow<TranslateSettings> = _settings.asStateFlow()

    private var job: Job? = null

    /** Loi dang xu ly, giu de con dich lai khi nguoi dung dong y tai goi. */
    private var current: List<String> = emptyList()

    fun onLyrics(lyrics: Lyrics) {
        job?.cancel()
        val texts = lyrics.lines.map { it.text }

        if (texts == current && _state.value is TranslationState.Done) return
        current = texts

        if (lyrics.isEmpty || !_settings.value.enabled) {
            _state.value = TranslationState.Idle
            return
        }

        job = scope.launch { run(texts, allowDownload = false) }
    }

    /** Nguoi dung dong y tai goi ngon ngu ve may. */
    fun downloadAndTranslate() {
        val texts = current
        if (texts.isEmpty()) return
        job?.cancel()
        job = scope.launch { run(texts, allowDownload = true) }
    }

    fun update(settings: TranslateSettings) {
        val changed = settings != _settings.value
        _settings.value = settings
        prefs?.write(settings)
        if (!changed) return

        // Tat thi xoa ngay ban dang hien - de lai la noi doi. Bat lai, hoac doi
        // ngon ngu doc, thi lam lai tu dau voi chinh bai dang phat.
        job?.cancel()
        if (!settings.enabled || current.isEmpty()) {
            _state.value = TranslationState.Idle
        } else {
            job = scope.launch { run(current, allowDownload = false) }
        }
    }

    private suspend fun run(texts: List<String>, allowDownload: Boolean) {
        val settings = _settings.value
        val target = settings.readingLanguage

        _state.value = TranslationState.Working
        try {
            val from = OnDeviceTranslator.detect(texts)
            if (from == null) {
                // Khong doan noi thi im lang. Dich bua tu mot ngon ngu doan sai
                // ra thu vo nghia con te hon la khong dich.
                _state.value = TranslationState.Idle
                return
            }
            if (from == target) {
                _state.value = TranslationState.Idle
                return
            }

            cache?.get(texts, target)?.let {
                _state.value = TranslationState.Done(it, from)
                return
            }

            if (!allowDownload && !(OnDeviceTranslator.hasModel(from) &&
                    OnDeviceTranslator.hasModel(target))
            ) {
                _state.value = TranslationState.NeedsModel(from)
                return
            }

            val translated = OnDeviceTranslator.translate(
                lines = texts,
                from = from,
                to = target,
                allowDownload = allowDownload,
                requireWifi = settings.wifiOnly
            )

            if (translated == null) {
                _state.value = TranslationState.Failed(
                    if (allowDownload) "Không tải được gói tiếng ${languageName(from)}"
                    else "Không dịch được lời bài này"
                )
                return
            }

            cache?.put(texts, target, translated)
            _state.value = TranslationState.Done(translated, from)
            Log.i(TAG, "Dich xong ${translated.size} dong: $from -> $target")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Duong dich hong", e)
            _state.value = TranslationState.Failed("Không dịch được lời bài này")
        }
    }

    private companion object {
        const val TAG = "LyraDich"
    }
}
