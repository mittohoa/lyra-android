package com.mittohoa.lyra.translate

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Dich loi bai hat NGAY TREN MAY.
 *
 * Vi sao khong goi mot dich vu dich tren mang: khong khoa API, khong quota,
 * khong tra tien, khong gui loi bai hat cua nguoi ta ra ngoai, va van chay khi
 * mat mang. Doi lai la ban dich tho hon ban tren may chu - voi loi bai hat von
 * nhieu an du thi cang tho. Nhung muc tieu o day khong phai la mot ban dich
 * dep, ma la de nguoi nghe HIEU NGHIA cau dang hat.
 *
 * Ban chat mo hinh: ML Kit dich moi cap ngon ngu bang cach di VONG QUA TIENG
 * ANH. Nen dich Han sang Viet la hai chang, va phai co CA HAI goi tren may.
 * Doi lai la n ngon ngu chi ton n goi chu khong phai n binh phuong.
 *
 * Moi lop `Translator` giu mot mo hinh trong bo nho, va PHAI dong lai - khong
 * dong thi mo hinh nam do het doi tien trinh. Nen o day mo mot cai cho moi lan
 * dich roi dong ngay, thay vi giu san mot cai: mot bai chi dich mot lan roi
 * duoc cat vao bo nho dem, khong co canh dich lien tuc de ma phai toi uu.
 */
object OnDeviceTranslator {

    /**
     * Doan xem loi dang la tieng gi.
     *
     * Tra ve ma ngon ngu ML Kit hieu duoc, hoac null khi khong doan noi. Doan
     * tren MOT MANG loi chu khong tung dong: mot dong ba chu thi tieng nao cung
     * giong tieng nao, ma ca bai gop lai thi gan nhu khong the nham.
     */
    suspend fun detect(lines: List<String>): String? = withContext(Dispatchers.Default) {
        val sample = lines.asSequence()
            .map { it.trim() }
            .filter { it.length > 1 }
            .take(SAMPLE_LINES)
            .joinToString(" ")
        if (sample.length < MIN_SAMPLE_CHARS) return@withContext null

        val client = LanguageIdentification.getClient()
        try {
            val code = client.identifyLanguage(sample).await()
            // ML Kit tra ve chuoi "und" khi no khong dam - khong phai null
            if (code == UNDETERMINED) null else normalize(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Khong nhan ra ngon ngu cua loi", e)
            null
        } finally {
            client.close()
        }
    }

    /** Goi ngon ngu da nam san tren may chua. */
    suspend fun hasModel(language: String): Boolean = withContext(Dispatchers.IO) {
        try {
            RemoteModelManager.getInstance()
                .isModelDownloaded(TranslateRemoteModel.Builder(language).build())
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Khong hoi duoc goi ngon ngu $language", e)
            false
        }
    }

    /**
     * Dich ca bai, tra ve mang DUNG BANG so dong dau vao.
     *
     * Giu dung so dong la dieu bat buoc, khong phai chi cho dep: giao dien ghep
     * dong goc voi dong dich theo chi so, lech mot dong la ca bai lech. Dong nao
     * dich hong thi de RONG chu khong bo di.
     *
     * `allowDownload` = false thi chi dich khi goi da co san tren may. Dat vay
     * de app khong tu tien tai vai chuc me-ga-bai khi nguoi dung dang dung 4G.
     *
     * Tra ve null khi khong dich duoc chut nao - de ben goi phan biet duoc voi
     * mot ban dich rong.
     */
    suspend fun translate(
        lines: List<String>,
        from: String,
        to: String,
        allowDownload: Boolean,
        requireWifi: Boolean = true
    ): List<String>? = withContext(Dispatchers.Default) {
        if (lines.isEmpty()) return@withContext null
        if (from == to) return@withContext null

        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
        )

        try {
            val conditions = DownloadConditions.Builder()
                .apply { if (requireWifi) requireWifi() }
                .build()

            if (allowDownload) {
                translator.downloadModelIfNeeded(conditions).await()
            } else if (!hasModel(from) || !hasModel(to)) {
                return@withContext null
            }

            // Dich TUNG DONG chu khong gop ca bai roi tach lai. Gop thi nhanh
            // hon that, nhung mo hinh se tu quyet dinh xuong dong o dau va so
            // dong tra ve khong con khop - ma khop dong moi la thu ta can.
            // Mot bai vai chuc dong chay het chua toi mot giay tren may that.
            lines.map { line ->
                val text = line.trim()
                if (text.isEmpty() || text == "♪") {
                    text
                } else {
                    try {
                        translator.translate(text).await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.d(TAG, "Mot dong khong dich duoc", e)
                        ""
                    }
                }
            }
        } catch (e: CancellationException) {
            // Doi bai giua chung thi lan dich cu bi huy - phai nem tiep, nuot la
            // pha vo co che huy co cau truc cua coroutine
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Khong dich duoc $from -> $to", e)
            null
        } finally {
            translator.close()
        }
    }

    /**
     * Dua ma ngon ngu ve dung dang ML Kit nhan.
     *
     * Bo nhan dien tra ve the BCP-47 day du kieu "zh-Hant" hay "pt-BR", con ben
     * dich chi nhan ma hai chu cai. Khong cat thi cap ngon ngu bi tu choi mot
     * cach kho hieu.
     */
    private fun normalize(code: String): String? =
        TranslateLanguage.fromLanguageTag(code) ?: TranslateLanguage.fromLanguageTag(
            code.substringBefore('-')
        )

    private const val TAG = "LyraDich"
    private const val UNDETERMINED = "und"

    /** Lay ngan nay dong dau de doan ngon ngu - du de chac, du nhanh. */
    private const val SAMPLE_LINES = 12
    private const val MIN_SAMPLE_CHARS = 20
}
