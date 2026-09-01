package com.mittohoa.lyra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mittohoa.lyra.data.ChuDe
import com.mittohoa.lyra.data.KieuChu
import com.mittohoa.lyra.data.LyricEffect
import com.mittohoa.lyra.data.OverlayLook
import com.mittohoa.lyra.data.TranslateSettings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Trang Chỉnh có đủ năm mục, và năm mục đó xếp dọc trong cột cuộn.
 *
 * Bộ kiểm này sinh ra từ một lỗi thật. Một dấu `}` thừa đóng `Column` cuộn của
 * `TunePane` ngay sau mục đầu; bốn mục còn lại vì thế phát ra ở thân hàm chứ
 * không phải trong cột. Chỗ gọi `TunePane` là một trang của `HorizontalPager`,
 * tức một `Box` — mà trong `Box` thì mọi con xếp CHỒNG lên nhau. Bốn mục đó
 * vẫn được vẽ, nhưng vẽ dưới cái cột kín cả màn hình rồi bị nó che.
 *
 * Lỗi ấy **biên dịch sạch**: số ngoặc vẫn cân, chỉ cân sai chỗ. Không có lỗi
 * nào để đọc, không có cảnh báo nào, và trên màn hình thì trang Chỉnh trông
 * như một trang chỉ có một mục — hợp lý tới mức không ai nghi ngờ. Nó sống qua
 * HAI bản phát hành: suốt thời gian đó không ai đổi được ngôn ngữ dịch, chọn
 * được hiệu ứng chữ, hay thêm được ô vào Cài đặt nhanh.
 *
 * Chú ý cách kiểm, vì chỗ này dễ viết ra một bài kiểm vô dụng: `assertExists`
 * và cả `assertIsDisplayed` đều VẪN ĐÚNG với bản hỏng. Bốn mục kia có thật
 * trong cây, có kích thước thật, nằm trong khung cửa sổ thật — chúng chỉ nằm
 * dưới một tấm che. Compose không kiểm chuyện bị anh em che khuất.
 *
 * Nên phải kiểm hai thứ khác:
 *
 *   1. `performScrollTo` — hàm này đòi nút phải có tổ tiên cuộn được. Bản hỏng
 *      đặt bốn mục NGOÀI cột cuộn, nên nó ném lỗi. Đây là mũi bắt chính.
 *   2. Toạ độ — bản hỏng dồn cả bốn mục về cùng một chỗ, nên mép trên của
 *      chúng trùng nhau thay vì tăng dần.
 */
@RunWith(AndroidJUnit4::class)
class TrangChinhTest {

    @get:Rule
    val giaoDien = createComposeRule()

    /** Đúng thứ tự chúng phải xuất hiện trên màn hình. */
    private val NAM_MUC = listOf(
        "Khung lời nổi",
        "Dịch lời",
        "Bộ chữ",
        "Mặt giấy",
        "Hiệu ứng chữ",
        "Ô bật nhanh và tắt nhanh"
    )

    private fun dungTrangChinh() {
        giaoDien.setContent {
            TunePane(
                canDrawOverlay = true,
                overlayOn = false,
                accent = Color(0xFF6D28D9),
                look = OverlayLook(),
                suggestedFontSize = 26f,
                onOpenOverlaySettings = {},
                onToggleOverlay = {},
                onLookChange = {},
                translateSettings = TranslateSettings(),
                onTranslateChange = {},
                canAddTile = true,
                onAddTile = {},
                lyricEffect = LyricEffect.SANG_DAN,
                onLyricEffectChange = {},
                chuDe = ChuDe.THEO_MAY,
                onChuDeChange = {},
                kieuChu = KieuChu.SACH,
                onKieuChuChange = {}
            )
        }
    }

    @Test
    fun moiMucDeuNamTrongCotCuon() {
        dungTrangChinh()
        // `performScrollTo` đòi một tổ tiên cuộn được. Mục nào rơi ra ngoài cột
        // thì hàm này ném lỗi ngay, kể cả khi nút đó vẫn tồn tại trong cây.
        for (ten in NAM_MUC) {
            giaoDien.onNodeWithText(ten).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun namMucXepDocKhongDeLenNhau() {
        dungTrangChinh()
        var truoc = Float.NEGATIVE_INFINITY
        var tenTruoc = "(đầu trang)"
        for (ten in NAM_MUC) {
            val o = giaoDien.onNodeWithText(ten).getUnclippedBoundsInRoot()
            val tren = o.top.value
            assertTrue(
                "Mục \"$ten\" có mép trên ${tren}dp, không nằm dưới \"$tenTruoc\" " +
                    "(${truoc}dp). Các mục đang đè lên nhau thay vì xếp dọc.",
                tren > truoc
            )
            truoc = tren
            tenTruoc = ten
        }
    }

    /**
     * Lời xin quyền tự gói mình trong một `Column`.
     *
     * Cùng họ với lỗi ở trên, và cùng ngày. `Ask` từng phát ra năm phần tử rời;
     * ở trang Đang phát nó được đặt trong một `Box`, nên đầu đề, đoạn mô tả và
     * cái nút cùng đè lên một chỗ. Chỗ ĐẶT trông vô hại, cái sai nằm ở chỗ
     * ĐỊNH NGHĨA — nên kiểm ngay tại chỗ định nghĩa, trong một `Box`.
     */
    @Test
    fun loiXinQuyenXepDocDuKhiDatTrongBox() {
        giaoDien.setContent {
            Box {
                Ask(
                    title = "Cần quyền vẽ đè lên app khác",
                    body = "Không có quyền này thì lời không hiện được.",
                    action = "Mở Cài đặt để bật",
                    accent = Color(0xFF6D28D9),
                    onAction = {}
                )
            }
        }
        val dauDe = giaoDien.onNodeWithText("Cần quyền vẽ đè lên app khác").getUnclippedBoundsInRoot()
        val moTa = giaoDien.onNodeWithText("Không có quyền này thì lời không hiện được.").getUnclippedBoundsInRoot()
        val nut = giaoDien.onNodeWithText("Mở Cài đặt để bật").getUnclippedBoundsInRoot()

        assertTrue(
            "Đoạn mô tả (${moTa.top.value}dp) phải nằm dưới đầu đề " +
                "(hết ở ${dauDe.bottom.value}dp), không đè lên.",
            moTa.top.value >= dauDe.bottom.value
        )
        assertTrue(
            "Cái nút (${nut.top.value}dp) phải nằm dưới đoạn mô tả " +
                "(hết ở ${moTa.bottom.value}dp), không đè lên.",
            nut.top.value >= moTa.bottom.value
        )
    }
}
