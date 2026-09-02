package com.mittohoa.lyra.share

/**
 * Các mẫu thẻ lời.
 *
 * Một mẫu duy nhất thì bài nào chia sẻ ra cũng giống bài nào — mà một câu hát
 * dữ dội và một câu hát buồn không nên trông như nhau. Sáu mẫu ở đây không phải
 * sáu bộ màu: chúng khác nhau về BỐ CỤC, tức là khác nhau ở chỗ mắt nhìn vào
 * đâu trước.
 *
 * Cả sáu đều giữ một thứ: dòng thương hiệu ở góc dưới. Tấm thẻ là của câu hát,
 * không phải của app — nhưng chia sẻ đi mà không ai biết nó từ đâu ra thì cũng
 * chẳng để làm gì.
 */
enum class MauThe(val nhan: String, val moTa: String) {
    GIAY("Trang giấy", "Nền giấy, lề mực màu bìa — dáng mặc định của AURA"),
    BIA_MO("Bìa mờ", "Ảnh bìa nhoè phủ kín, chữ nổi lên trên"),
    BIA_TREN("Bìa trên", "Ảnh bìa vuông ở trên, lời ở dưới như một trang sách"),
    CHU_LON("Chữ lớn", "Bỏ hết trang trí, chỉ còn câu hát thật to"),
    KHOI_MAU("Khối màu", "Nền đặc màu lấy từ bìa, chữ ngà nổi lên"),
    KE_DONG("Kẻ dòng", "Chữ nằm trên dòng kẻ như trang vở");

    /** Mẫu này có cần ảnh bìa không — không có bìa thì đừng bày ra. */
    val canBia: Boolean get() = this == BIA_MO || this == BIA_TREN
}
