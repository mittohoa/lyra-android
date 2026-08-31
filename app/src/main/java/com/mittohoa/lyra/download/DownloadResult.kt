package com.mittohoa.lyra.download

/**
 * Ket qua mot lan tai.
 *
 * Nam o phan chung chu khong o rieng ban sideload: giao dien va `Lyra` deu nhac
 * toi kieu nay, va ca hai deu duoc dung chung cho hai ban dung.
 */
sealed interface DownloadResult {
    data class Done(val uri: String) : DownloadResult
    data class Failed(val why: String) : DownloadResult
}
