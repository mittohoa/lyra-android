package com.mittohoa.lyra.update

import android.content.Context

/**
 * Tai ban moi roi mo phien cai dat.
 *
 * Ban PLAY: khong co, va khong can.
 *
 * Google Play tu lo viec cap nhat, nen mot co che cap nhat rieng o day vua thua
 * vua gay nghi ngo. Quyen `REQUEST_INSTALL_PACKAGES` cung chi khai o ban cai
 * tay - Play soi quyen do rat ky va chi cho vai loai app dung.
 */
object ApkInstaller {

    const val SUPPORTED = false

    fun duocPhepCai(context: Context): Boolean = false

    fun moTrangCapQuyen(context: Context) = Unit

    @Suppress("UNUSED_PARAMETER")
    suspend fun taiVaCai(
        context: Context,
        duongTai: String,
        onProgress: (Int) -> Unit = {}
    ): String? = "Bản này cập nhật qua Google Play"
}
