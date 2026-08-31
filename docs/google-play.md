# Hồ sơ Google Play — điền gì vào đâu

Mọi thứ dưới đây viết sẵn để chép thẳng vào Play Console. Chỗ nào cần bạn quyết
thì đánh dấu **[quyết định]**.

Bản nộp: `app/build/outputs/bundle/playRelease/app-play-release.aab` —
`versionCode 5`, `versionName 0.1.5`, `targetSdk 36`, 35,8 MB.

---

## 0. Ba chỗ chốt trước, sau khó sửa

### Khoá ký app **[quyết định]**

Play bắt buộc dùng Play App Signing với app mới. Lúc tạo app, Console hỏi lấy
khoá ký ở đâu:

| Chọn | Hệ quả |
|---|---|
| Google tự tạo khoá (mặc định) | Bản GitHub và bản Play ký khác nhau → **không cập nhật chéo được**. Ai đang dùng bản GitHub phải gỡ sạch rồi cài lại, mất hết cài đặt và danh sách phát |
| **Tải khoá của mình lên** | Cùng một khoá cả hai kênh → bản Play về như một bản cập nhật bình thường |

Khuyến nghị chọn cái thứ hai. Bỏ lỡ là phải tạo app mới với `applicationId`
khác, tức mất luôn tên gói đang dùng.

Vân tay chứng chỉ khi Console hỏi:

```
0B:64:C6:0A:B8:45:86:A2:1E:64:7E:56:8F:12:22:62:CD:74:28:E9:70:2A:59:50:7A:68:98:0F:56:B7:A7:B6
```

Dạng liền, cho ô không nhận dấu hai chấm:

```
0b64c60ab84586a21e647e568f122262cd7428e9702a59507a68980f56b7a7b6
```

Chủ thể: `CN=Lyra, OU=Lyra, O=mittohoa, L=Ho Chi Minh, C=VN`

### Kênh phát hành

Đi **Thử nghiệm nội bộ** trước, không đi thẳng bản chính thức. Không qua duyệt
đầy đủ, thường có link trong vài giờ, tối đa 100 người test. Người test cài qua
Play nên Play Protect không đụng tới — đó là lý do phải đi đường này thay vì
phát APK.

### `applicationId`

`com.mittohoa.lyra_player`. Không đổi được sau khi tạo app.

---

## 1. Thông tin trên cửa hàng

### Tên app (tối đa 30 ký tự)

```
Lyra — Lời bài hát nổi
```

### Mô tả ngắn (tối đa 80 ký tự)

```
Lời bài hát nổi trên màn hình cho nhạc phát ở mọi app. Kèm trình phát nhạc.
```

### Mô tả đầy đủ (tối đa 4000 ký tự)

```
Lyra hiện lời bài hát nổi lên trên màn hình, chạy theo đúng nhịp bài đang phát —
dù bạn đang nghe ở Spotify, YouTube Music, hay bất kỳ app nhạc nào khác. Và khi
bạn muốn, chính Lyra cũng là một trình nghe nhạc đầy đủ.

HAI VAI, MỘT APP

Đồng hành — Nhạc phát ở app khác, Lyra đọc xem đang phát bài gì, tự đi tìm lời,
rồi vẽ một khung lời nổi đè lên màn hình. Bạn vừa lướt vừa đọc lời được.

Trình phát — Lyra phát nhạc trong máy bạn, có hàng đợi, trộn bài, lặp, danh sách
phát. Lúc đó lời khớp tuyệt đối, vì Lyra nắm chính đồng hồ phát.

LỜI KHỚP TỪNG DÒNG

Lời lấy từ LRCLIB, kho lời mở và miễn phí. Lệch nhịp thì chạm vào đúng câu đang
hát là Lyra tự căn lại cho cả bài — không phải kéo thanh trượt mò mẫm.

DỊCH NGAY TRÊN MÁY

Lời tiếng Anh, Nhật, Hàn, Trung dịch được sang tiếng Việt, chạy hoàn toàn trên
máy bạn. Không cần mạng sau lần tải bộ máy dịch đầu tiên, và câu chữ không gửi
đi đâu cả.

LỜI TRÊN MÀN HÌNH KHOÁ

Câu đang hát hiện luôn trên thẻ điều khiển nhạc ở màn hình khoá, nên không cần
mở app cũng đọc được.

Ô BẬT NHANH

Thêm ô Lyra vào bảng Cài đặt nhanh để bật tắt khung lời nổi bằng một chạm.

MIỄN PHÍ, KHÔNG TÀI KHOẢN, KHÔNG QUẢNG CÁO

Không đăng ký, không đăng nhập, không quảng cáo, không theo dõi hành vi. Mã
nguồn mở tại github.com/mittohoa/lyra-android.

VỀ QUYỀN ĐỌC THÔNG BÁO

Android chỉ cho biết app nào đang phát bài gì qua đúng một đường: quyền đọc
thông báo. Lyra chỉ lấy tên bài, tên ca sĩ và vị trí phát, rồi bỏ hết phần còn
lại — không đọc nội dung tin nhắn, không gửi gì đi. Không bật quyền này cũng
dùng được: Lyra vẫn phát nhạc trong máy và hiện lời bình thường, chỉ thiếu phần
lời cho nhạc ở app khác.

Lyra không lưu trữ và không phân phối nhạc. Lời bài hát thuộc về tác giả và đơn
vị nắm bản quyền.
```

### Phân loại và liên hệ

- Danh mục ứng dụng: **Âm nhạc và âm thanh**
- Thẻ: lời bài hát, trình phát nhạc, khung nổi
- Email liên hệ **[quyết định]** — Play hiển thị công khai địa chỉ này, nên lập
  một hộp thư riêng cho app chứ đừng dùng hộp thư cá nhân hay của trường
- Chính sách quyền riêng tư:
  `https://mittohoa.github.io/lyra-player/quyen-rieng-tu.html`
- Trang web: `https://mittohoa.github.io/lyra-player/`

---

## 2. An toàn dữ liệu

Đây là chỗ dễ bị trả hồ sơ nhất, vì Google đối chiếu lời khai với hành vi thật
của app. Khai theo đúng số đo: soi mã máy bản phát hành `play` chỉ thấy ba nhóm
địa chỉ mạng — `lrclib.net`, `dl.google.com/translate/offline`, và
`firebaseinstallations` + `firebaseremoteconfig` (do ML Kit kéo theo).

### App có thu thập hoặc chia sẻ dữ liệu người dùng không?

**Có.** Đừng khai "không" — ML Kit gửi một mã định danh đi, và khai thiếu tệ hơn
khai thừa.

### Khai một mục duy nhất

| Trường | Điền |
|---|---|
| Loại dữ liệu | **Mã nhận dạng** (Device or other IDs) |
| Thu thập | Có |
| Chia sẻ | Có — với Google, qua thư viện ML Kit |
| Bắt buộc hay tuỳ chọn | **Tuỳ chọn** — chỉ xảy ra khi người dùng bật dịch |
| Mục đích | **Chức năng của ứng dụng** |
| Mã hoá khi truyền | Có |
| Người dùng yêu cầu xoá được không | Không áp dụng — Lyra không giữ dữ liệu ở đâu |

Giải thích khi Console hỏi thêm:

```
Lyra dùng ML Kit Translation của Google để dịch lời ngay trên máy. Lần đầu người
dùng bật dịch cho một ngôn ngữ, ML Kit tải bộ máy dịch từ máy chủ Google; trong
quá trình đó thư viện tạo và gửi đi một Firebase Installation ID. Đây là hành vi
của thư viện Google, không phải của Lyra. Người dùng không bật tính năng dịch
thì không có bước này.
```

### Những gì KHÔNG khai, và vì sao

- **Tên bài và tên ca sĩ gửi tới LRCLIB** — không thuộc loại dữ liệu nào trong
  danh sách của Google, không kèm định danh, không gắn với người dùng. Muốn chắc
  thì khai thêm *Hoạt động trong ứng dụng → Nội dung khác*, nhưng không bắt buộc.
- **Nội dung thông báo** — Lyra đọc nhưng không gửi đi và không lưu lại. Google
  hỏi về dữ liệu *thu thập*, tức có rời khỏi máy; cái này không rời.
- **File nhạc trong máy** — không rời khỏi máy.
- Không tài khoản, không vị trí, không danh bạ, không ảnh, không quảng cáo,
  không đo đạc hành vi. Kho phụ thuộc không có Crashlytics, Analytics, Sentry
  hay bất kỳ thư viện đo đạc nào — kiểm được bằng `app/build.gradle.kts`.

---

## 3. Giải trình quyền đọc thông báo

Chuẩn bị sẵn, vì đây là phần hay bị hỏi nhất với app dùng
`BIND_NOTIFICATION_LISTENER_SERVICE`.

```
Chức năng chính của Lyra là hiện lời bài hát đồng bộ cho nhạc đang phát, kể cả
nhạc phát ở ứng dụng khác. Để biết ứng dụng nào đang phát bài gì và ở vị trí
nào, Android chỉ cung cấp đúng một đường: MediaSessionManager.getActiveSessions,
và API này chỉ cho phép gọi khi ứng dụng là một NotificationListenerService đã
được người dùng bật. Không có API thay thế cho ứng dụng thường.

Lyra chỉ đọc siêu dữ liệu của phiên phát nhạc: tên bài, tên nghệ sĩ, độ dài và
vị trí phát. Toàn bộ phần còn lại của thông báo bị bỏ qua ngay tại chỗ, không
được lưu và không được truyền đi. Không có nội dung thông báo nào rời khỏi thiết
bị.

Tính năng này là tuỳ chọn. Người dùng từ chối quyền vẫn dùng được ứng dụng như
một trình phát nhạc đầy đủ có lời đồng bộ và dịch; chỉ mất phần hiện lời cho
nhạc phát ở ứng dụng khác. Ứng dụng nói rõ điều đó ngay tại màn hình xin quyền.

Mã nguồn công khai: github.com/mittohoa/lyra-android
Nơi dùng API: media/MediaSessionWatcher.kt, service/LyraNotificationListener.kt
```

---

## 4. Xếp hạng nội dung

Bảng câu hỏi, trả lời **Không** cho tất cả:

- Bạo lực, máu me, tình dục, khoả thân: Không
- Ma tuý, rượu bia, thuốc lá: Không
- Cờ bạc, mua trong ứng dụng: Không
- Chia sẻ vị trí, chia sẻ thông tin cá nhân giữa người dùng: Không
- Nội dung do người dùng tạo: Không
- Nhắn tin, mạng xã hội: Không

Kết quả dự kiến: **Mọi lứa tuổi**.

## 5. Khai báo khác

| Mục | Trả lời |
|---|---|
| Quảng cáo | Không có quảng cáo |
| Đối tượng mục tiêu | 13 tuổi trở lên, không nhắm trẻ em |
| App tin tức | Không |
| COVID-19 / chính phủ / tài chính | Không |
| `MANAGE_EXTERNAL_STORAGE` | Không dùng |
| SMS / nhật ký cuộc gọi | Không dùng |
| Dịch vụ trợ năng | Không dùng |
| `QUERY_ALL_PACKAGES` | Không dùng |
| `REQUEST_INSTALL_PACKAGES` | **Không có trong bản Play** — chỉ ở bản tải thẳng |

---

## 6. Ảnh chụp màn hình

Tối thiểu **2 ảnh điện thoại**, nên có 4–8. Cỡ 1080×2340 chụp từ máy thật là đạt.

Bộ nên chụp, xếp theo thứ tự kể một câu chuyện:

1. Khung lời nổi đang đè lên một app nhạc khác — ảnh quan trọng nhất, nó nói
   ngay app này làm gì
2. Trang Lời, đang chạy khớp nhịp
3. Trang Đang phát với bìa album và nút điều khiển
4. Lời kèm bản dịch tiếng Việt bên dưới
5. Thẻ điều khiển ở màn hình khoá có câu đang hát
6. Trang Tìm với kết quả nhạc trong máy

Chụp bằng `adb exec-out screencap -p > ten.png`.

Cần thêm: **biểu tượng 512×512** (PNG, không viền bo) và **ảnh nổi bật
1024×500**.

---

## 7. Thứ tự làm

1. Tạo app trong Console — **nhớ chọn tải khoá của mình lên** ở bước ký
2. Thông tin trên cửa hàng (mục 1)
3. An toàn dữ liệu (mục 2)
4. Bảng xếp hạng nội dung (mục 4)
5. Các khai báo khác (mục 5)
6. Ảnh chụp màn hình, biểu tượng, ảnh nổi bật (mục 6)
7. Tạo bản phát hành trên kênh **Thử nghiệm nội bộ**, tải `app-play-release.aab`
8. Thêm email người test, lấy link mời
9. Chờ vài giờ, tự cài thử qua link đó trước khi mời người khác

## 8. Cái đã biết chắc và cái chưa

**Đã kiểm bằng mã máy trên bản dựng `play`:** không mang một dòng nào gọi Zing
MP3 hay NhacCuaTui, không mang `REQUEST_INSTALL_PACKAGES`, không hỏi GitHub tìm
bản mới, không thư viện đo đạc nào.

**Chưa biết:** Google có chấp nhận `BIND_NOTIFICATION_LISTENER_SERVICE` cho app
này không. Chính sách hiện hành *không* liệt kê nó vào nhóm quyền phải khai báo
riêng, và lập luận "chức năng chính" ở mục 3 là lập luận thật — app bỏ quyền đó
vẫn chạy được, đã đo trên máy. Nhưng đây vẫn là chỗ duy nhất có thể bị từ chối.

**Nếu bị từ chối:** vẫn phát hành được với vai trình phát nhạc làm chức năng
chính, để vai đồng hành lại cho bản tải thẳng. Không phải ngõ cụt.
