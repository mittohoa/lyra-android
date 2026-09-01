# Hồ sơ Google Play — điền gì vào đâu

Mọi thứ dưới đây viết sẵn để chép thẳng vào Play Console. Chỗ nào cần bạn quyết
thì đánh dấu **[quyết định]**.

Bản nộp: `app/build/outputs/bundle/playRelease/app-play-release.aab` —
`versionCode 7`, `versionName 0.1.7`, `targetSdk 36`, 34,1 MB.

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

SÁU HIỆU ỨNG CHỮ

Chọn kiểu lời chạy: sáng dần, hiện chữ, nảy, toả sáng, trôi lên — hoặc tắt hẳn.
Áp cho cả trang lời lẫn khung nổi. Hai kiểu quét tốn pin hơn, và app nói rõ điều
đó ngay tại chỗ chọn.

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

### Ghi chú phát hành — tiếng Việt (tối đa 500 ký tự)

Play hỏi mục này ở **mỗi** bản phát hành, kể cả bản thử nghiệm nội bộ. Bỏ trống
được nhưng không nên: người test đọc đúng chỗ này để biết cần thử cái gì.

```
Bản 0.2.0 — Lyra đổi mặt.

• Nền giấy ngà chữ mực, lật được sang nền mực chữ ngà (Chỉnh › Mặt giấy)
• Hai bộ chữ mới, hoặc giữ phông máy (Chỉnh › Bộ chữ)
• Khung lời nổi chọn được màu chữ và màu nền

Sửa lỗi:
• Bốn mục cài đặt bị che mất — dịch lời, hiệu ứng chữ, ô bật nhanh giờ mới bấm được
• Ô tìm mời tìm nguồn nhạc bản này không có, thay vì mời cấp quyền đọc nhạc trong máy
• Bìa album sai khi bài phát ở app khác

Nhờ thử: mở Chỉnh, cuộn hết trang — phải đủ năm mục.
```

### Ghi chú phát hành — tiếng Anh (tối đa 500 ký tự)

```
Version 0.2.0 — Lyra gets a face of its own.

• Ivory paper with ink text, or flip to ink (Tune › Surface)
• Two new typefaces, or keep your system font (Tune › Typeface)
• Pick text and background colour for the floating window

Fixes:
• Four hidden settings sections are back: translation, effects, quick tile
• Search offered sources this build does not ship, not library access
• Wrong cover art for music playing in another app

Please check: open Tune, scroll down — five sections.
```

### Bản tiếng Anh của thông tin cửa hàng

Play lấy **một ngôn ngữ mặc định**. Nếu chọn tiếng Việt làm mặc định thì phần
này là ngôn ngữ thêm; nếu chọn tiếng Anh thì ngược lại. Có cả hai thì app hiện
đúng tiếng ở mọi thị trường.

Tên app (≤30):

```
Lyra — Floating Lyrics
```

Mô tả ngắn (≤80):

```
Floating lyrics for music in any app. Plus a full music player.
```

Mô tả đầy đủ (≤4000):

```
Lyra shows song lyrics floating on your screen, in time with whatever is playing —
Spotify, YouTube Music, or any other music app. And when you want it to, Lyra is a
full music player itself.

TWO ROLES, ONE APP

Companion — Music plays in another app; Lyra reads what is playing, finds the
lyrics, and draws them over your screen. Read along while you scroll.

Player — Lyra plays the music on your phone, with a queue, shuffle, repeat and
playlists. Here the timing is exact, because Lyra owns the playback clock.

LINE-BY-LINE SYNC

Lyrics come from LRCLIB, an open and free lyrics library. If the timing drifts,
tap the line you actually hear and Lyra realigns the whole song — no dragging a
slider around.

SIX TEXT EFFECTS

Pick how the lyrics move: sweep, reveal, bounce, glow, rise — or none at all.
Applies to both the lyrics page and the floating window. Two of them redraw
continuously and cost more battery; the app says so where you pick them.

ON-DEVICE TRANSLATION

English, Japanese, Korean and Chinese lyrics translate to Vietnamese entirely on
your phone. No network needed after the first model download, and the text never
leaves your device.

LYRICS ON THE LOCK SCREEN

The current line appears on the media control card, so you can read without
opening the app.

QUICK SETTINGS TILE

Add a Lyra tile to Quick Settings to toggle the floating lyrics with one tap.

FREE, NO ACCOUNT, NO ADS

No sign-up, no login, no ads, no behavioural tracking. Open source at
github.com/mittohoa/lyra-android.

ABOUT NOTIFICATION ACCESS

Android exposes what is playing in other apps through exactly one door:
notification access. Lyra reads only the song title, artist and playback position,
and discards everything else — it does not read message contents and sends nothing
anywhere. You can decline: Lyra still plays your own music and shows lyrics for it,
you only lose lyrics for other apps.

Lyra does not host or distribute music. Lyrics belong to their authors and rights
holders.
```

### Hướng dẫn cho người thử nghiệm nội bộ

Console cho dán một đoạn hướng dẫn kèm lời mời. Người test cài qua Play nên
không vướng Play Protect, nhưng vẫn cần biết phải bật quyền gì.

```
Cảm ơn bạn thử Lyra.

Cài xong, mở app rồi làm hai bước:

1. Bật quyền đọc thông báo khi app hỏi — không có nó thì Lyra không biết app khác
   đang phát bài gì. Lyra chỉ đọc tên bài và vị trí phát, không đọc nội dung
   thông báo nào.
2. Bật "hiển thị trên ứng dụng khác" để dựng khung lời nổi.

Rồi mở Spotify hay YouTube Music, phát một bài, và xem lời có hiện lên không.

Đáng thử nhất:
• Lời lệch nhịp thì chạm vào đúng câu đang nghe — cả bài phải căn lại theo
• Kéo khung lời nổi sang chỗ khác trên màn hình
• Bật dịch trong trang Chỉnh với một bài tiếng Anh
• Khoá máy và xem câu đang hát trên thẻ điều khiển

Báo lỗi hoặc góp ý: mittohoa@gmail.com
```

### Phân loại và liên hệ

- Danh mục ứng dụng: **Âm nhạc và âm thanh**
- Thẻ: lời bài hát, trình phát nhạc, khung nổi
- Email liên hệ: `mittohoa@gmail.com` — Play hiển thị công khai địa chỉ này.
  Nó trùng tên với tài khoản GitHub đang phát hành nên nhất quán; đổi lại là
  hộp thư này sẽ nhận thư từ người lạ và không tách được khỏi thư cá nhân
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

### Quyền truy cập ứng dụng (App access)

Console hỏi có phần nào của app cần đăng nhập mới xem được không.

> **Toàn bộ chức năng đều dùng được mà không cần thông tin đăng nhập đặc biệt.**

Đúng: Lyra không có tài khoản, không có màn hình đăng nhập, không khoá tính năng
nào sau đăng ký. Người duyệt mở app là dùng được ngay.

### Đối tượng mục tiêu và nội dung

| Câu hỏi | Trả lời |
|---|---|
| Nhóm tuổi nhắm tới | **13–15, 16–17, 18 tuổi trở lên**. Không chọn nhóm dưới 13 |
| App có thu hút trẻ em không | Không |
| Có quảng cáo hiển thị cho trẻ em không | Không có quảng cáo |

Không chọn nhóm dưới 13 là có chủ đích: chọn vào là app rơi vào chương trình
*Designed for Families*, kéo theo một bộ yêu cầu riêng nặng hơn hẳn, mà Lyra
không nhắm tới trẻ em.

### Ngôn ngữ mặc định

Chọn **tiếng Việt** làm ngôn ngữ mặc định rồi thêm tiếng Anh, chứ không ngược
lại: người dùng đang nhắm tới là người Việt, và mô tả tiếng Việt là bản viết
kỹ hơn. Bản tiếng Anh ở mục 1 dùng cho phần ngôn ngữ thêm.

---

## 6. Đồ hoạ — đã dựng xong

Nằm ở thư mục hồ sơ (`scratchpad/play/`), không nằm trong kho mã.

| File | Cỡ | Dùng cho |
|---|---|---|
| `bieu-tuong-512.png` | 512×512 | Biểu tượng app |
| `anh-noi-bat-1024x500.png` | 1024×500 | Ảnh nổi bật |
| `anh-chup/1-khung-loi-noi.png` | 1080×2340 | Khung lời nổi đè lên YouTube Music |
| `anh-chup/7-lyra-tu-phat.png` | 1080×2340 | Lyra tự phát: đủ nút bấm, hàng đợi, màu lấy từ bìa |
| `anh-chup/2-trang-loi.png` | 1080×2340 | Trang Lời cho nhạc ở app khác |
| `anh-chup/8-loi-lyra-phat.png` | 1080×2340 | Trang Lời khi Lyra tự phát |
| `anh-chup/5-chinh.png` | 1080×2340 | Trang Chỉnh |
| `anh-chup/4-tim-bai.png` | 1080×2340 | Trang Tìm — **nên bỏ**, thư viện quá nghèo |

Play cần tối thiểu 2 ảnh điện thoại. Xếp theo thứ tự trên — ảnh khung lời nổi
đứng đầu vì nó nói ngay app làm gì, và đó là thứ duy nhất phân biệt Lyra với
một trình phát nhạc thường.

Biểu tượng cố ý **vuông đầy khung**, không bo góc sẵn: Play tự bo góc và đổ
bóng, nộp ảnh đã bo thì bốn góc trong suốt hiện ra thành viền răng cưa. Bốn góc
lấp bằng chuyển sắc lấy từ chính hai đầu đường chéo của biểu tượng, nên không
thấy đường nối.

**Đã bỏ:** ảnh thẻ điều khiển ở màn hình khoá. Chụp ra thì dính ảnh nền cá nhân
của chủ máy. Tính năng vẫn chạy — `dumpsys media_session` cho thấy Lyra đẩy đúng
câu đang hát lên thẻ (`description=Rền vang non sông sáng ngời`) — chỉ là không
chụp được một ảnh dùng cho cửa hàng. Muốn có thì phải đổi hình nền máy trước.

---

## 6b. Cảnh báo "chưa tải biểu tượng gỡ lỗi" — không gỡ được

Sau khi tải bản gộp lên, Play sẽ nhắc:

> App Bundle này chứa mã gốc và bạn chưa tải biểu tượng gỡ lỗi lên.

Đây là **cảnh báo, không phải lỗi** — nộp và phát hành bình thường. Nhưng nó
không im được, và đây là lý do:

Lyra không viết dòng mã máy nào. Bốn thư viện `.so` trong bản nộp đều là nhị
phân dựng sẵn của ML Kit và AndroidX:

```
libtranslate_jni.so              liblanguage_id_l2c_jni.so
libdatastore_shared_counter.so   libandroidx.graphics.path.so
```

Đã đọc bảng section của ELF trong cả bốn: **không file nào còn `.symtab` hay
`.debug_*`** — chúng đã lột sạch. Không có ký hiệu thì không có gì để đóng gói,
và Google không phát hành ký hiệu gỡ lỗi cho những thư viện này.

`build.gradle.kts` vẫn đặt `debugSymbolLevel = "SYMBOL_TABLE"`. Hôm nay dòng đó
không sinh ra gì — kiểm được bằng cách mở bản gộp, trong `BUNDLE-METADATA/`
không có mục `nativesymbols`. Giữ lại để ngày nào Lyra tự viết mã máy thì ký
hiệu tự được gói mà không ai phải nhớ ra.

**Hệ quả thật:** app sập bên trong mã của ML Kit thì báo cáo sự cố chỉ có địa
chỉ số, không đọc ra tên hàm. Sập trong mã Kotlin của Lyra thì vẫn đọc được
bình thường, nhờ `proguard.map` mà Play tự nhận từ bản gộp.

---

## 7. Thứ tự làm

1. Tạo app trong Console — **nhớ chọn tải khoá của mình lên** ở bước ký
2. Thông tin trên cửa hàng (mục 1)
3. An toàn dữ liệu (mục 2)
4. Bảng xếp hạng nội dung (mục 4)
5. Các khai báo khác (mục 5)
6. Ảnh chụp màn hình, biểu tượng, ảnh nổi bật (mục 6)
7. Tạo bản phát hành trên kênh **Thử nghiệm nội bộ**, tải `app-play-release.aab`,
   dán **ghi chú phát hành** (mục 1)
8. Thêm email người test, dán **hướng dẫn cho người thử nghiệm** (mục 1), lấy
   link mời
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
