# Lyra cho Android

Lời bài hát hiện **nổi trên màn hình** cho nhạc đang phát ở app khác — Spotify,
YouTube, Zing MP3, NhacCuaTui. Và khi bạn muốn, chính Lyra cũng là một trình
nghe nhạc đầy đủ.

- Bản Windows: <https://github.com/mittohoa/lyra-player>
- Tải về: <https://mittohoa.github.io/lyra-player/>
- Kiến trúc đầy đủ: [bản đọc trên web](https://mittohoa.github.io/lyra-player/kien-truc-android.html)

---

## Hai vai của cùng một app

**Đồng hành** — nhạc phát ở app khác, Lyra chỉ đọc xem đang phát gì qua
`MediaSessionManager`, tự đi dò lời, rồi vẽ lên một khung nổi đè trên app bạn
đang dùng. Đây là vai không ai làm thay được.

**Trình phát** — Lyra tự tìm ở Zing MP3 và NhacCuaTui, đọc nhạc có sẵn trong
máy, có hàng đợi, trộn bài, lặp, danh sách phát. Lúc đó nó **sở hữu đồng hồ
phát**, nên lời khớp tuyệt đối và câu đang hát hiện được lên thẻ điều khiển ở
màn hình khoá.

Hai vai gặp nhau ở đúng một chỗ trong mã. Mọi thứ phía sau — tìm lời, dịch,
khung nổi — không biết nhạc đang đến từ đâu.

---

## Đang có gì

| | |
|---|---|
| Đọc nhạc đang phát ở app khác | ✅ |
| Khung lời nổi, kéo thả, chỉnh cỡ chữ | ✅ |
| Ba nguồn lời: LRCLIB, Zing MP3, NhacCuaTui | ✅ |
| Chạm một cái để căn lại lời lệch, nhớ theo bài | ✅ |
| Tự nhập lời khi ba nguồn đều không có | ✅ |
| Dịch lời ngay trên máy (ML Kit) | ✅ |
| Tự phát: tìm online, nhạc trong máy, hàng đợi | ✅ |
| Danh sách phát | ✅ |
| Lời trên thẻ màn hình khoá | ✅ |
| Ô bật/tắt nhanh trong Cài đặt nhanh | ✅ |
| Tải nhạc kèm lời nhúng trong file | ✅ chỉ bản `sideload` |

---

## Hai biến thể phát hành

| | `sideload` | `play` |
|---|---|---|
| Tải nhạc | có | **không có** |
| Tự tải và cài bản mới | có | không (Play tự lo) |
| Mọi thứ khác | đầy đủ | đầy đủ |

Chính sách Google Play cấm app cho tải nội dung từ dịch vụ phát trực tuyến, nên
bản lên Play không mang phần đó. Tách bằng **bộ mã nguồn** (`src/sideload/`,
`src/play/`) chứ không bằng một cờ bật/tắt lúc chạy: một cái cờ vẫn để lại toàn
bộ mã trong file cài đặt, và người duyệt Play mở file ra xem thì thấy.

```bash
./gradlew assembleSideloadDebug     # cài tay, có tải nhạc
./gradlew assemblePlayRelease       # lên Play
./gradlew bundlePlayRelease         # bản gộp, Play giao đúng một kiến trúc CPU
./gradlew :app:testDebugUnitTest    # bộ kiểm tra logic thuần
```

Yêu cầu: **JDK 17** và Android SDK. `compileSdk`/`targetSdk` là **36**
(Android 16), tối thiểu là API 26 (Android 8) — mốc đầu tiên có kiểu cửa sổ nổi
mà app này cần.

`local.properties` trỏ tới SDK và **không vào git** vì mỗi máy một khác. Nhớ
dùng dấu `/` chứ không dùng `\`: trong file `.properties`, `\` là ký tự thoát
nên `C:\Users` bị đọc thành `C:Users`.

Bản phát hành cần một khoá ký. `keystore.properties` (cũng không vào git) trỏ
tới nó:

```properties
storeFile=/duong/dan/toi/khoa.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Không có file đó thì bản phát hành vẫn dựng được, chỉ là không được ký — để ai
clone về cũng build được, và họ tự tạo khoá của mình. Khoá ký là danh tính của
người phát hành, không phải của mã nguồn.

---

## Cập nhật

App không nằm trong cửa hàng nào thì phải tự lo việc báo có bản mới. Không lo
thì một người cài tay bản 0.1.0 dùng nó mãi mãi, kể cả sau khi lỗi họ gặp đã
được sửa — và họ không có cách nào biết.

Mỗi lần mở app, Lyra hỏi trang phát hành một lần rồi hiện một dải báo ở đầu màn
hình. Bản `sideload` tải APK **thẳng vào một phiên `PackageInstaller`** rồi để
hệ thống mở hộp xác nhận — không rẽ qua file trong thư mục Tải về, không cần
`FileProvider`, và không để lại một file APK nằm chờ trong máy. Android vẫn bắt
người dùng bấm xác nhận một lần; không có cách nào bỏ qua bước đó với một app
thường, và cũng không nên có.

Quyền `REQUEST_INSTALL_PACKAGES` chỉ khai ở `src/sideload/AndroidManifest.xml`.
Bản `play` mang một `ApkInstaller` rỗng với `SUPPORTED = false` và chỉ mở trang
phát hành.

Ba chỗ dễ sai, đều đã tránh:

- **Đọc tên file APK, không đọc tên thẻ phát hành.** Một bản phát hành mang cả
  file Windows lẫn Android, và hai phía không đổi số cùng lúc.
- **So phiên bản theo từng số, không so chuỗi.** So chuỗi thì `0.1.10` đứng
  trước `0.1.9`, và tới bản thứ mười app lặng lẽ ngừng báo.
- **Dải báo nằm ở tầng app, không nằm trong trang phát.** Trang phát thoát sớm
  khi chưa có quyền đọc thông báo hoặc chưa có gì đang phát — đúng trạng thái
  của một máy vừa cài xong.

### Máy Samsung

Auto Blocker chặn **mọi** đường cài không đi qua Galaxy Store hay Play Store, kể
cả `PackageInstaller` do chính app gọi, nên trên máy bật nó thì cơ chế trên
không chạy — người dùng phải tắt nó, hoặc cài bằng `adb install`.

Đó là một trong **hai** lớp chặn, và lớp còn lại mới là lớp hay bị nhầm là lỗi
app. Xem [Vì sao máy cảnh báo "truy cập dữ liệu nhạy
cảm"](#vì-sao-máy-cảnh-báo-truy-cập-dữ-liệu-nhạy-cảm).

---

## Giao diện

Không thanh tiêu đề, không thanh điều hướng, không ngăn kéo. Toàn bộ chrome của
app là **một viên thuốc nổi** ở đáy màn hình — chạm để đổi trang, hoặc vuốt
ngang trên nội dung.

Lý do không phải để lạ: các trang đều là một dòng nội dung chạy theo bài hát.
Đặt một thanh cố định lên trên là cắt đôi dòng đó và che đúng phần đang xem. Nền
lấy màu từ ảnh bìa và chuyển màu từ từ khi đổi bài.

Trang phát dựng **sân khấu là một chỗ cắm, không phải khung ảnh**: hôm nay nó
dựng ảnh bìa vuông, mai có video thì cùng chỗ ấy đổi sang 16:9 và nhận một bề
mặt vẽ. Thanh tua là **cạnh dưới của sân khấu**, không phải một thanh trượt đặt
ở đâu đó.

---

## Đã làm gì cho nhanh

Chậm và giật là thứ dễ mất người dùng nhất.

| Chỗ | Trước | Sau |
|---|---|---|
| Tra lời | tối đa 9 lần gọi mạng nối tiếp | ba nguồn **song song**, chờ bằng lần chậm nhất |
| Nghe lại một bài | gọi mạng lại từ đầu | **hiện ngay** từ bộ nhớ đệm RAM + đĩa |
| Khung nổi | vẽ lại 10 lần/giây | chỉ vẽ khi **đổi dòng** |
| Trang lời | dựng lại danh sách 10 lần/giây | `derivedStateOf`, chỉ khi đổi dòng |
| Đồng hồ vị trí | chạy cả khi màn hình tắt | theo nhịp vẽ, **tự dừng** |
| Màu ảnh bìa | đọc ảnh gốc nghìn điểm ảnh | đọc ảnh thu nhỏ, trên luồng nền |

Chi tiết đáng nói nhất: bộ nhớ đệm trả về **trước khi** vào trạng thái "đang
tìm". Nhấp nháy một khung trống rồi mới hiện chữ là cảm giác chậm nhất, dù thật
ra chỉ tốn vài mili-giây.

---

## Khi lời không khớp tiếng hát

Tìm đúng **tên bài** không có nghĩa là đúng **bản thu**. Lời của bản thu phòng
đắp lên một bản hát live thì lệch từ đầu đến cuối.

**Không giả vờ khớp khi biết là không khớp.** Độ dài bài chênh quá 15 giây so
với bản tìm được thì mốc thời gian chắc chắn sai. Lúc đó lời hiện dạng chữ trơn
— không tô sáng, không tự cuộn — kèm một dòng nói rõ lý do. Tô sáng nhầm một
dòng suốt cả bài còn tệ hơn không tô gì.

**Chạm vào câu đang hát để căn lại**, ngay trên khung nổi. Một cú chạm khớp lại
cả bài, không phải rời app nhạc.

**Nhớ độ lệch theo từng bài.** Chỉnh một lần, lần sau nghe lại là đúng luôn.

---

## Quyền

| Quyền | Để làm gì | Bỏ qua được? |
|---|---|---|
| Đọc thông báo | Điều kiện của Android để biết app nào đang phát bài gì. Lyra **không** đọc nội dung thông báo | Không — thiếu là mất tính năng chính |
| Vẽ đè lên app khác | Dựng khung lời nổi | Được |
| Đọc nhạc trong máy | Thư viện nhạc. Xin đúng quyền **nhạc**, không đụng ảnh/video/tài liệu | Được |
| Thông báo | Thẻ điều khiển nhạc | Được |
| Cài đặt ứng dụng | Tự cài bản mới. **Chỉ bản `sideload`** | Được — thiếu thì chỉ mở trang phát hành |

Hai quyền đầu và quyền cài đặt không xin được bằng hộp thoại — Android chỉ cho
mở đúng trang cài đặt rồi chờ người dùng tự bật. Từ Android 8, quyền cài đặt
được cấp **theo từng app** chứ không còn là một công tắc chung của máy, nên Lyra
đưa thẳng người dùng tới trang của riêng nó.

Đó là toàn bộ danh sách. Đối chiếu được bằng
`aapt2 dump permissions <file.apk>`. Không có `QUERY_ALL_PACKAGES`, không danh
bạ, tin nhắn, vị trí, camera, micro, và không có dịch vụ trợ năng —
`READ_EXTERNAL_STORAGE` có mặt nhưng chặn ở `maxSdkVersion=32`, tức chết hẳn từ
Android 13 trở lên; nó chỉ để đọc nhạc trên máy đời cũ.

### Vì sao máy cảnh báo "truy cập dữ liệu nhạy cảm"

Thứ kích hoạt cảnh báo **không nằm trong danh sách quyền**. Nó là một khai báo
dịch vụ:

```xml
<service android:name="…LyraNotificationListener"
         android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
```

Android 13+ và các lớp bảo vệ của hãng soi đúng dòng này. **Và chúng nói đúng.**
`NotificationListenerService` không đọc riêng thông báo nhạc — nó mở ra *mọi*
thông báo trên máy, kể cả tin nhắn ngân hàng và mã OTP. Hệ thống không có cách
nào biết app định làm gì với nó, nên cảnh báo cho tất cả.

Lyra chỉ lấy tên bài, tên ca sĩ và vị trí phát rồi bỏ hết phần còn lại; không có
nội dung thông báo nào rời khỏi máy. Nhưng đó là lời của người viết app — thứ
kiểm chứng được là mã nguồn này và danh sách quyền ở trên.

Không có gì để cắt cho nhẹ đi. Bỏ `REQUEST_INSTALL_PACKAGES` cũng không hết cảnh
báo vì sai thủ phạm; bỏ dịch vụ đọc thông báo thì app không còn lý do tồn tại.
Nên cách duy nhất là đổi **kênh cài**, không phải đổi mã: `adb install` (đi qua
cơ chế cài theo phiên nên né được lớp *restricted settings* của Android 13+),
hoặc tắt Auto Blocker rồi dùng *Cho phép cài đặt bị hạn chế*, hoặc phát hành qua
Play.

---

## Cấu trúc

```
service/    Lyra                      trạng thái dùng chung, nối các mảnh
            LyraNotificationListener  neo giữ app sống
            LyraTileService           ô Quick Settings
media/      MediaSessionWatcher       bám các phiên media đang chạy
            NowPlaying                kiểu dữ liệu + bù vị trí phát
lyrics/     Identify                  ← chuyển từ identify.ts bản Windows
            LrcParser                 ← chuyển từ lrc.ts
            LyricsRepository          điều phối nguồn, chấm điểm khớp tên
sources/    Catalog                   gộp các nguồn, trộn kết quả
            LrclibClient
            ZingClient                ký HMAC-SHA512
            NctClient                 lời mã hoá RC4
            LocalLibrary              nhạc trong máy qua MediaStore
player/     LyraPlaybackService       Media3 MediaSessionService
            Playback                  hàng đợi, điều khiển
            StreamResolver            lấy đường phát ngay trước khi phát
            Artwork                   tải và thu nhỏ ảnh bìa
translate/  OnDeviceTranslator        ML Kit, chạy trên máy
            TranslationRepository     chỉ dịch khi thật sự cần
overlay/    OverlayView               View thuần, tự vẽ, có viền chữ
            OverlayHost               dựng cửa sổ, kéo thả, chạm xuyên qua
data/       LyricCache OffsetStore ManualLyricStore
            OverlayPrefs TranslatePrefs TranslationCache PlaylistStore
download/   Downloads                 mỗi biến thể một bản (xem trên)
ui/         MainActivity HomeScreen PlayerPane SearchPane
            Playlists LyricEditor LyraMark
```

---

## Bốn chỗ dễ sai, đã xử lý

**Vị trí phát là ảnh chụp, không phải hiện tại.** `PlaybackState.position` được
chụp tại `lastPositionUpdateTime`. Không bù phần đã trôi thì lời luôn chạy chậm
hơn nhạc vài giây. Và `lastPositionUpdateTime` dùng đồng hồ `elapsedRealtime`,
không phải `currentTimeMillis` — so nhầm hai thang này ra sai lệch hàng chục năm.

**Nhiều app cùng khai báo phiên một lúc.** Spotify tạm dừng còn YouTube đang
phát thì cả hai đều có phiên. Phải chọn phiên **đang phát**, không lấy phiên đầu
danh sách.

**Lyra đọc chính mình.** Khi Lyra ghi câu đang hát vào phiên media của nó để
hiện trên màn hình khoá, bộ theo dõi đọc lại và tưởng đó là bài mới — rồi đi tra
lời cho một câu hát. Nay bộ theo dõi bỏ qua gói của chính mình, và bài Lyra tự
phát đi một dòng riêng lấy thẳng từ bộ máy phát.

**Zing chỉ ký MỘT SỐ tham số.** `/search/multi` chỉ ký `ctime` và `version`, còn
`q` thì không. Ném cả `q` vào là Zing trả về "Incorect signature".

---

## Ghi chú

Lyra không lưu trữ và không phân phối nhạc. Lời bài hát thuộc về tác giả và đơn
vị nắm bản quyền. Phần lời và phần phát dựa vào API nội bộ của Zing MP3 và
NhacCuaTui — không có cam kết nào, và chúng có thể đổi bất cứ lúc nào.
