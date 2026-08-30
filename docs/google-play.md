# Đưa Lyra lên Google Play — những gì phải làm và những gì có thể chặn

> Tra ngày 30/08/2026. Chính sách Play đổi liên tục — mọi mốc thời gian dưới
> đây nên kiểm lại trước khi nộp.

---

## 1. Nói thẳng trước: có hai chỗ có thể chặn hẳn

Trước khi bàn kỹ thuật, hai điều này quyết định app có lên Play được không.

### 1.1 Nguồn lời từ Zing MP3 và NhacCuaTui — rủi ro lớn nhất

Lyra lấy lời từ **API nội bộ** của Zing và NhacCuaTui, không phải API công khai
có giấy phép. Ba vấn đề chồng lên nhau:

- **Vi phạm điều khoản dịch vụ của họ.** Cả hai bên đều không cho phép gọi API
  nội bộ từ ứng dụng bên thứ ba.
- **Bản quyền lời bài hát.** Lời bài hát là tác phẩm có bản quyền riêng, tách
  khỏi bản ghi âm. Hiển thị lời trong một app phát hành công khai mà không có
  giấy phép là chỗ Play có thể gỡ app theo chính sách sở hữu trí tuệ.
- **Khiếu nại gỡ bỏ.** Zing hoặc NCT chỉ cần gửi một khiếu nại là app bị gỡ,
  không cần Play tự phát hiện.

**Khuyến nghị nếu muốn lên Play:** bỏ Zing và NCT, chỉ giữ **LRCLIB**. LRCLIB
là kho cộng đồng, mở, không đòi khoá, và có điều khoản rõ ràng. Đổi lại tỉ lệ
tìm được lời cho nhạc Việt sẽ giảm — đó là cái giá thật, phải cân.

Cài riêng cho mình (sideload) thì không vướng gì cả. Đây thuần tuý là vấn đề
của việc **phát hành công khai**.

### 1.2 Quyền đọc thông báo — bị soi rất kỹ

`BIND_NOTIFICATION_LISTENER_SERVICE` được Google xếp vào nhóm **quyền nhạy cảm,
rủi ro cao**, vì nó hay bị lạm dụng cho lừa đảo tài chính. Hai hệ quả cụ thể:

- **Google Play Protect chặn cài đặt** từ nguồn ngoài Play với các app xin
  quyền này. Nghĩa là **ngay cả bản sideload** người dùng cũng phải bấm qua một
  cảnh báo — và trên nhiều máy, phải tự tắt Play Protect.
- Trên Play, phải chứng minh **chức năng cốt lõi** của app thật sự cần quyền
  đó. Lyra thì đúng là vậy: không đọc được phiên media thì app không có lý do
  tồn tại. Nhưng phải viết rõ điều đó trong phần mô tả và trong khai báo.

Phải nói rõ trong mô tả app **và trong màn hình dẫn nhập**: Lyra không đọc nội
dung thông báo, chỉ đọc phiên media (tên bài, nghệ sĩ, vị trí phát). Hiện màn
hình dẫn nhập đã nói câu đó.

---

## 2. Mốc kỹ thuật bắt buộc

### 2.1 Target API level — **gấp**

| Mốc | Yêu cầu |
|---|---|
| Từ **31/08/2026** | App mới và bản cập nhật phải target **Android 16 (API 36)** |
| Hiện tại | App còn target dưới API 35 sẽ **không hiện ra** với người dùng mới trên máy Android mới hơn |
| Gia hạn | Xin được tới **01/11/2026** qua Play Console, một lần |

**Lyra đang target API 34 → phải nâng lên 36.** Cần tải SDK platform 36 (hiện
máy này mới có 34).

Nâng target không chỉ là đổi một con số — mỗi bậc kéo theo các thay đổi hành vi
phải kiểm lại, riêng với Lyra là:

- **Android 14 (34):** foreground service phải khai báo `type`. Hiện Lyra không
  dùng foreground service nên chưa vướng — nhưng nếu sau này thêm để chống bộ
  diệt nền của hãng thì phải khai `specialUse` kèm biện minh.
- **Android 15 (35):** bắt buộc vẽ tràn viền (edge-to-edge). Đã làm rồi
  (`enableEdgeToEdge`).
- **Android 16 (36):** siết thêm về cửa sổ và foreground service. Phải chạy
  thử lại toàn bộ đường overlay trên máy ảo API 36.

### 2.2 Định dạng và ký

- Nộp **Android App Bundle (.aab)**, không phải APK.
- Dùng **Play App Signing**: Google giữ khoá ký phát hành, mình giữ khoá tải lên.
- Phải bật rút gọn mã (`isMinifyEnabled`) — đã bật cho bản release, nhưng chưa
  chạy thử bản release lần nào. Rất dễ có luật ProGuard thiếu làm hỏng
  `kotlinx.serialization`, phải thử trước khi nộp.

### 2.3 Khai báo an toàn dữ liệu

Bắt buộc, và phải khớp với thứ app làm thật. Lyra:

| Dữ liệu | Có gửi đi không | Nói gì |
|---|---|---|
| Tên bài, nghệ sĩ | **Có** — gửi tới LRCLIB (và Zing/NCT nếu còn giữ) | Phải khai: gửi đi để tra lời, không gắn với danh tính |
| Nội dung thông báo | **Không** | Nói rõ trong mô tả — đây là điều người dùng lo nhất |
| Danh tính, vị trí, danh bạ | **Không** | |
| Lời đã tìm được | Lưu trên máy, không gửi đi | Bộ nhớ đệm trong thư mục riêng của app |

Phải có **chính sách quyền riêng tư** đặt ở một địa chỉ công khai — bắt buộc
với mọi app, không riêng app xin quyền nhạy cảm.

---

## 3. Việc phải làm, theo thứ tự

1. **Quyết định về Zing/NCT** — cái này chặn mọi thứ khác. Bỏ để lên Play, hay
   giữ và chỉ cài riêng?
2. Nâng `compileSdk` và `targetSdk` lên **36**, tải SDK platform 36.
3. Chạy thử bản **release** (có rút gọn mã) trên máy thật — đây là chỗ hay vỡ
   mà bản debug không bao giờ lộ ra.
4. Viết **chính sách quyền riêng tư**, đặt lên một địa chỉ công khai.
5. Điền **khai báo an toàn dữ liệu** trong Play Console.
6. Viết mô tả app nói rõ vì sao cần quyền đọc thông báo, và nói rõ **không** đọc
   nội dung thông báo.
7. Chuẩn bị ảnh chụp màn hình, icon 512×512, ảnh bìa 1024×500.
8. Ký bằng khoá tải lên, dựng `.aab`, nộp.

---

## 4. Ý kiến của tôi

Nếu mục đích là **dùng cho mình và vài người quen**: đừng lên Play. Cài thẳng
file APK là xong, không phải bỏ Zing/NCT — tức là giữ được tỉ lệ tìm ra lời cho
nhạc Việt, vốn là điểm mạnh nhất của app. Chỉ phải bấm qua cảnh báo của Play
Protect một lần.

Nếu mục đích là **phát hành cho người lạ**: phải bỏ Zing và NCT. Lúc đó Lyra
thành một app lời bài hát chạy trên LRCLIB — vẫn tốt cho nhạc quốc tế, nhưng
nhạc Việt thì kém hẳn. Cần cân xem có đáng không.

Có một đường thứ ba: **giữ cả hai**. Bản lên Play chỉ dùng LRCLIB; bản cài
riêng có đủ ba nguồn. Trong Gradle chỉ là hai `productFlavors`, không phải hai
nhánh mã nguồn.

---

## Nguồn

- [Target API level requirements for Google Play apps](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Google Play target API requirements for Android apps (2026)](https://median.co/blog/google-plays-target-api-level-requirement-for-android-apps)
- [Developer Guidance for Google Play Protect Warnings](https://developers.google.com/android/play-protect/warning-dev-guidance)
- [Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/9888170)
