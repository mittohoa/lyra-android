package com.mittohoa.lyra.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Công thức mờ dần khi đổi bài.
 *
 * KIỂU HỎNG ĐÁNG SỢ Ở ĐÂY LÀ KẸT Ở MỨC NHỎ. Sót một nhánh là bộ phát giữ
 * nguyên mức gần 0, mọi bài sau đó phát ra tiếng bé tí, và không có gì trên màn
 * hình nói vì sao. Người dùng sẽ kết luận máy hỏng hoặc app hỏng — và họ không
 * sai mấy. Nên mọi nhánh trả về 1 đều có một bài kiểm riêng.
 */
class ChuyenMuotTest {

    private fun am(doDai: Long, viTri: Long, moDan: Int = 1000, coBaiSau: Boolean = true) =
        ChuyenMuot.mucAm(doDai, viTri, moDan, coBaiSau)

    @Test fun `giua bai thi de nguyen muc du`() {
        assertEquals(1f, am(doDai = 200_000, viTri = 10_000), 0f)
    }

    @Test fun `vao doan cuoi thi nho dan theo ti le`() {
        // Còn đúng nửa đoạn mờ dần thì đúng nửa mức.
        assertEquals(0.5f, am(doDai = 200_000, viTri = 199_500), 0.001f)
        assertEquals(0.25f, am(doDai = 200_000, viTri = 199_750), 0.001f)
    }

    @Test fun `dung mep doan mo dan van la muc du`() {
        assertEquals(1f, am(doDai = 200_000, viTri = 199_000), 0f)
    }

    @Test fun `het bai thi ve khong`() {
        assertEquals(0f, am(doDai = 200_000, viTri = 200_000), 0f)
    }

    @Test fun `qua het bai - dong ho tre mot nhip - van la khong, khong am`() {
        assertEquals(0f, am(doDai = 200_000, viTri = 200_500), 0f)
    }

    @Test fun `bai cuoi hang doi khong mo dan`() {
        // Không có bài sau thì mờ dần chỉ là tiếng nhỏ đi ở cuối bài cuối cùng.
        assertEquals(1f, am(doDai = 200_000, viTri = 199_900, coBaiSau = false), 0f)
    }

    @Test fun `chua biet do dai thi khong mo dan`() {
        // Luồng phát trực tiếp, hoặc bài chưa nạp xong. Không biết còn bao lâu
        // thì đoán bừa là nhỏ tiếng giữa bài.
        assertEquals(1f, am(doDai = C.TIME_UNSET, viTri = 5_000), 0f)
        assertEquals(1f, am(doDai = 0, viTri = 5_000), 0f)
        assertEquals(1f, am(doDai = -1, viTri = 5_000), 0f)
    }

    @Test fun `bai ngan hon ca doan mo dan thi nho dan tu dau`() {
        // Nhạc chuông 600ms với đoạn mờ dần 1 giây: không có chỗ nào để phát ở
        // mức đủ cả. Không được phép ra số âm hay số quá 1.
        val m = am(doDai = 600, viTri = 0)
        assertEquals(0.6f, m, 0.001f)
    }

    @Test fun `tat mo dan thi luon la muc du`() {
        // `moDanMs = 0` không bao giờ tới được hàm này trong lúc chạy — vòng
        // lặp không khởi động. Nhưng nếu có ai gọi thẳng thì nó vẫn phải trả
        // mức đủ chứ không chia cho không.
        assertEquals(1f, am(doDai = 200_000, viTri = 199_999, moDan = 0), 0f)
    }
}
