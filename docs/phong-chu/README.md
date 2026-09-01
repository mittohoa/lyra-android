# Phông chữ trong Lyra

Hai bộ, nhúng thẳng vào APK ở `app/src/main/res/font/`. Cả hai đều dùng giấy
phép **SIL Open Font License 1.1** — dùng và phát hành kèm phần mềm thương mại
được, miễn là giấy phép đi kèm. Bản giấy phép để cạnh đây.

| Bộ | File trong app | Dùng cho | Giấy phép |
|---|---|---|---|
| [Be Vietnam Pro](https://fonts.google.com/specimen/Be+Vietnam+Pro) | `be_vietnam_pro_regular.ttf`, `be_vietnam_pro_semibold.ttf` | Giao diện | [OFL](BeVietnamPro-OFL.txt) |
| [Newsreader](https://fonts.google.com/specimen/Newsreader) | `newsreader.ttf` (biến thiên) | Lời bài hát, đầu đề | [OFL](Newsreader-OFL.txt) |

Cả hai đã kiểm đủ **134/134** chữ cái tiếng Việt có dấu (cả hoa lẫn thường),
bằng cách đọc bảng `cmap` trong file chứ không tin vào mô tả trên trang tải.

Be Vietnam Pro do người Việt vẽ, và dấu tiếng Việt được vẽ chứ không ghép máy
móc từ một bộ Latin có sẵn — với một app mà chữ hiển thị hầu hết là tiếng Việt
có dấu thì đó không phải chi tiết trang trí.

Newsreader là bộ **biến thiên**: một file chứa mọi độ đậm, rẻ hơn hẳn so với
nhúng ba bốn file tĩnh. Trục `wght` đặt tay trong `ui/Kieu.kt`.
