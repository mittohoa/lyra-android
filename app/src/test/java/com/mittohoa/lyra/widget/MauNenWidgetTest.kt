package com.mittohoa.lyra.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Luật chọn chữ đen hay chữ trắng cho nền widget.
 *
 * Sai ở đây thì hỏng theo kiểu tệ nhất với một widget: nó vẫn hiện, vẫn đúng
 * chữ, chỉ là không đọc được. Bìa album có đủ mọi màu nên không thể thử tay
 * từng trường hợp — phải chốt bằng bài kiểm.
 */
class MauNenWidgetTest {

    @Test fun `nen trang va nen giay dung chu den`() {
        assertTrue(KhungLoiWidget.sangHay(0xFFFFFFFF.toInt()))
        assertTrue(KhungLoiWidget.sangHay(0xFFFBF6EC.toInt()))
    }

    @Test fun `nen den va nen tim dam dung chu trang`() {
        assertFalse(KhungLoiWidget.sangHay(0xFF000000.toInt()))
        assertFalse(KhungLoiWidget.sangHay(0xFF6D28D9.toInt()))
    }

    @Test fun `nen vang dung chu den`() {
        // Đây là cái bìa làm hỏng mọi quy tắc "màu thì dùng chữ trắng". Vàng
        // rất sáng với mắt người dù kênh xanh dương bằng không.
        assertTrue(KhungLoiWidget.sangHay(0xFFFFEB3B.toInt()))
    }

    @Test fun `nen xanh duong dung chu trang du sang bang so hoc`() {
        // Xanh dương thuần có cùng "trung bình ba kênh" với một xám nhạt, mà
        // mắt người thấy nó tối hẳn. Lấy trung bình thì chỗ này ra chữ đen.
        assertFalse(KhungLoiWidget.sangHay(0xFF0000FF.toInt()))
    }

    @Test fun `nen xanh la sang thi dung chu den`() {
        // Ngược lại với xanh dương: mắt người nhạy với xanh lá nhất.
        assertTrue(KhungLoiWidget.sangHay(0xFF7CFC00.toInt()))
    }

    @Test fun `xam giua ranh chon chu den`() {
        // Ngưỡng 0,6 chứ không 0,5: chữ đen trên nền hơi tối vẫn đọc được, còn
        // chữ trắng trên nền hơi sáng thì mất hẳn. Xám 0,65 phải ra chữ đen.
        assertTrue(KhungLoiWidget.sangHay(0xFFA6A6A6.toInt()))
        assertFalse(KhungLoiWidget.sangHay(0xFF808080.toInt()))
    }
}
