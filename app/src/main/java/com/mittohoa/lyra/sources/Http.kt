package com.mittohoa.lyra.sources

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

/**
 * Mot client dung chung cho ca ba nguon.
 *
 * Dung chung mot the hien la co y: OkHttp gop san ket noi va luong nen tao
 * nhieu client la lang phi that su, khong phai chuyen gon gang.
 */
object Http {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Goi mot dia chi, tra ve than tin dang chuoi.
     *
     * Tra ve null cho MOI kieu that bai - mat mang, may chu tu choi, than tin
     * rong. Ben goi khong phan biet duoc cac truong hop do, va cung khong can:
     * ket qua deu la "nguon nay khong co loi, thu nguon khac".
     */
    fun text(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null
    ): String? = try {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.header(k, v) }
        if (body != null) builder.post(body)

        client.newCall(builder.build()).execute().use { res ->
            if (res.isSuccessful) res.body?.string() else null
        }
    } catch (e: Exception) {
        null
    }

    /** Lay cac cookie may chu dat, dang chuoi ghep san de gui lai. */
    fun collectCookies(url: String): String = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { res ->
            res.headers("Set-Cookie")
                .mapNotNull { it.substringBefore(';').takeIf(String::isNotBlank) }
                .joinToString("; ")
        }
    } catch (e: Exception) {
        ""
    }
}
