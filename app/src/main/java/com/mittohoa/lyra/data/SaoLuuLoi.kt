package com.mittohoa.lyra.data

/**
 * Dinh dang tep sao luu loi tu nhap.
 *
 * Can go tay mot bai loi la viec ton vai phut; can gio tung cau con lau hon.
 * Cong do nam trong bo nho rieng cua app, ma bo nho rieng thi go app la mat,
 * doi may la mat, va nguoi dung khong co cach nao lay ra. Mot tep sao luu la
 * cach duy nhat de cong do song lau hon lan cai dat nay.
 *
 * ĐỊNH DẠNG - chu tron, doc duoc bang mat, mo bang bat ky trinh soan nao:
 *
 *     LYRA-LOI  1
 *     === 24  <khoa>  <nghe si>  <ten bai>
 *     [00:12.30]cau thu nhat
 *     ... dung 24 dong ...
 *     === 18  <khoa>  <nghe si>  <ten bai>
 *     ...
 *
 * Cac cot cach nhau bang tab. Dong `===` ghi SO DONG cua khoi ngay sau no chu
 * khong dung dau phan cach: loi bai hat la chu nguoi dung tu go, ai cam duoc
 * mot cau bat dau bang "==="? Dem dong thi doc bao nhieu lay dung bay nhieu,
 * khong phai canh chung noi dung, va cung khong phai bia ra quy tac thoat ky
 * tu nao - ma quy tac thoat thi lam hong dung cai minh dang co gang giu.
 *
 * KHONG DUNG JSON: tep nay nguoi dung se mo ra xem, se gui cho nhau, va bien
 * mot bai loi thanh mot dong dai co \n thi khong ai doc noi.
 */
object SaoLuuLoi {

    const val NHAN = "LYRA-LOI"
    const val PHIEN_BAN = 1

    /** Ket qua khoi phuc, de man hinh noi that con so thay vi "xong". */
    data class KetQua(val them: Int, val daCo: Int, val hong: Int)

    fun xuat(cac: List<ManualLyricStore.BanLoi>): String = buildString {
        append(NHAN).append('\t').append(PHIEN_BAN).append('\n')
        for (b in cac) {
            val dong = b.loi.trimEnd('\n').split('\n')
            append("===").append('\t').append(dong.size).append('\t')
                .append(b.khoa).append('\t')
                .append(don(b.caSi)).append('\t')
                .append(don(b.tenBai)).append('\n')
            for (d in dong) append(d).append('\n')
        }
    }

    /**
     * Doc mot tep sao luu.
     *
     * Tra ve danh sach rong khi tep khong phai cua AURA. Doc duoc bao nhieu thi
     * lay bay nhieu: mot khoi hong o giua khong duoc phep lam mat nhung khoi
     * con lai - nguoi dung dang o dung luc can nhat.
     */
    fun nhap(raw: String): List<ManualLyricStore.BanLoi> {
        val dong = raw.replace("\r\n", "\n").split('\n')
        if (dong.isEmpty() || !dong[0].startsWith(NHAN)) return emptyList()

        val ra = mutableListOf<ManualLyricStore.BanLoi>()
        var i = 1
        while (i < dong.size) {
            val d = dong[i]
            if (!d.startsWith("===")) { i++; continue }

            val cot = d.split('\t')
            val soDong = cot.getOrNull(1)?.trim()?.toIntOrNull()
            // Chan ca so am lan so to hon ca tep: mot con so hong khong duoc
            // phep nuot not nhung khoi con lai phia sau.
            if (soDong == null || soDong <= 0 || soDong > dong.size) { i++; continue }

            val loi = dong.subList(i + 1, minOf(i + 1 + soDong, dong.size)).joinToString("\n")
            if (loi.isNotBlank()) {
                ra += ManualLyricStore.BanLoi(
                    khoa = cot.getOrNull(2)?.trim().orEmpty(),
                    caSi = cot.getOrNull(3).orEmpty(),
                    tenBai = cot.getOrNull(4).orEmpty(),
                    loi = loi
                )
            }
            i += soDong + 1
        }
        return ra
    }

    private fun don(s: String) = s.replace('\t', ' ').replace('\n', ' ').trim()
}
