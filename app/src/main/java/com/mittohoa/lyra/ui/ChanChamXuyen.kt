package com.mittohoa.lyra.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Chặn mọi cú chạm và vuốt rơi xuống thứ nằm dưới lớp phủ.
 *
 * VÌ SAO CẦN. Các màn hình phủ toàn màn của AURA — thẻ lời, căn giờ, video
 * toàn màn hình, ô soạn lời — không phải cửa sổ riêng. Chúng là **anh em trong
 * cùng một `Box`** với nội dung chính, chỉ khác chỗ đứng sau nên được vẽ đè
 * lên. Mà trong Compose, một `Column` có `fillMaxSize()` và nền đặc thì **vẽ
 * đè chứ không nuốt chạm**: hệ thống vẫn thả sự kiện xuống anh em nằm dưới.
 *
 * Hậu quả đúng như báo cáo từ máy thật: mở thẻ lời rồi chạm vào chỗ trống là
 * bấm trúng dòng lời của trang bên dưới — không thấy gì mà vẫn ăn.
 *
 * NUỐT Ở LƯỢT `Main`, KHÔNG PHẢI `Initial`. Lượt `Initial` đi từ ngoài vào
 * trong, nên cướp ở đó là cướp trước cả con: mọi nút bên trong chính lớp phủ sẽ
 * chết theo. Lượt `Main` đi từ trong ra ngoài — tới được đây nghĩa là không đứa
 * con nào nhận, tức đúng những cú chạm rơi vào chỗ trống.
 *
 * Nuốt CẢ chuỗi sự kiện chứ không riêng cú nhấn, nên vuốt ngang cũng không lật
 * được trang của bộ vuốt bên dưới.
 */
fun Modifier.chanChamXuyen(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val su = awaitPointerEvent(PointerEventPass.Main)
            // Chỉ đụng tới cái chưa ai nhận. Nhận lại thứ con đã nhận thì vô
            // hại, nhưng viết đúng ý định vẫn hơn: chỗ này chỉ dọn phần thừa.
            su.changes.forEach { if (!it.isConsumed) it.consume() }
        }
    }
}
