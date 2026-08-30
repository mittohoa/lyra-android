# Lyra cho Android

Lời bài hát hiện nổi trên màn hình, cho nhạc đang phát ở **app khác** — Spotify,
YouTube Music, Zing MP3, NhacCuaTui.

Lyra không tự phát nhạc. Nó đọc thứ đang phát qua `MediaSessionManager`, tự đi
dò lời, rồi vẽ lên một khung nổi đè trên app bạn đang dùng.

> Bản Windows: `d:\my_projects\media-player`.
> Kiến trúc đầy đủ: `docs/kien-truc-android.md` bên đó.

---

## Đang có gì

| Mốc | | |
|---|---|---|
| 1 | Đọc nhạc đang phát ở app khác | ✅ xong |
| 2 | Khung lời nổi, kéo thả được | ✅ xong |
| 3 | Dò lời từ LRCLIB | ✅ xong |
| 4 | Thêm nguồn Zing MP3 và NhacCuaTui | ✅ xong |
| 5 | Giao diện riêng, logo động, tối ưu tốc độ | ✅ xong |
| 5b | Bảng tinh chỉnh + ô Quick Settings | chưa |
| 6 | Căn lệch bằng một cú chạm, nhớ theo bài | ✅ xong |
| 7 | Dịch lời | chưa |

Kiểm chứng: **35 phép kiểm tra** cho phần logic thuần, đều đạt.
`Identify` và `LrcParser` chuyển từ bản Windows kèm nguyên bộ kiểm tra của
chúng — đạt hết ngay lần chạy đầu. Phần mã hoá đối chiếu với vector chuẩn của
RFC 4231 và tài liệu RC4, không tự chấm điểm mình.

Ba nguồn lời đã dò bằng API thật:

```
LRCLIB: 59 dòng, có mốc thời gian
Zing:  103 dòng, có mốc thời gian     ← chữ ký HMAC đúng
NCT:    59 dòng, có mốc thời gian     ← giải mã RC4 thành công
```

---

## Giao diện

Không thanh tiêu đề, không thanh điều hướng, không ngăn kéo. Toàn bộ chrome của
app là **một viên thuốc nổi** ở đáy màn hình — chạm để đổi trang, hoặc vuốt
ngang trên nội dung.

Lý do không phải để lạ: ba trang đều là một dòng nội dung chạy theo bài hát.
Đặt một thanh cố định lên trên là cắt đôi dòng đó và che đúng phần đang xem.
Nền lấy màu từ ảnh bìa và chuyển màu từ từ khi đổi bài.

Dấu hiệu Lyra thay chỗ cho thanh tiêu đề, và **cử động khi app đang tìm lời** —
cặp nốt lắc như bắt nhịp, thanh lời chạy từ trái sang, cùng chu kỳ 1,5 giây như
bản Windows. Không bận thì đứng yên hẳn.

---

## Sáu chỗ đã làm cho nhanh

Chậm và giật là thứ dễ mất người dùng nhất, nên đây là phần được chăm nhất.

| Chỗ | Trước | Sau |
|---|---|---|
| Tra lời | tối đa **9 lần gọi mạng nối tiếp** | ba nguồn **song song**; chờ bằng lần chậm nhất |
| Nghe lại một bài | gọi mạng lại từ đầu | **hiện ngay** từ bộ nhớ đệm RAM + đĩa |
| Khung nổi | vẽ lại **10 lần/giây** | chỉ vẽ khi **đổi dòng** |
| Trang lời | dựng lại danh sách 10 lần/giây | `derivedStateOf` — chỉ khi đổi dòng |
| Đồng hồ vị trí | bộ đếm chạy cả khi màn hình tắt | theo nhịp vẽ, **tự dừng** |
| Màu ảnh bìa | đọc ảnh gốc nghìn pixel | đọc ảnh thu nhỏ 24×24 |

Chi tiết đáng nói nhất: bộ nhớ đệm trả về **trước khi** vào trạng thái "đang
tìm". Nhấp nháy một khung trống rồi mới hiện chữ là cảm giác chậm nhất, dù thật
ra chỉ tốn vài mili-giây.

---

## Dựng và cài

```bash
./gradlew :app:assembleDebug          # ra app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest      # 35 phép kiểm tra
./gradlew :app:installDebug           # cần máy cắm vào hoặc máy ảo

# Dò ba nguồn lời bằng API thật (cần mạng, không nằm trong lần chạy thường)
LYRA_LIVE=1 ./gradlew :app:testDebugUnitTest --tests '*LiveSourcesProbe*' -i
```

Yêu cầu: JDK 17, Android SDK với platform 34. Máy này dùng JDK đi kèm Android
Studio ở `C:\Program Files\Android\Android Studio\jbr`.

`local.properties` trỏ tới SDK và **không vào git** — mỗi máy một khác. Nhớ
dùng dấu `/` chứ không dùng `\`: trong file `.properties`, `\` là ký tự thoát
nên `C:\Users` bị đọc thành `C:Users`.

Bản release (có rút gọn mã) đã dựng và kiểm: **1,1 MB** so với 10,9 MB bản
debug — R8 rút gọn 90%. Đã soát trong file dex để chắc `kotlinx.serialization`
không bị xoá nhầm: mọi tên trường JSON (`encodeId`, `keyDecryptLyric`,
`syncedLyrics`…) đều còn nguyên. Các lớp nội bộ bị đổi tên là đúng — chỉ
`LyraNotificationListener` giữ tên vì manifest gọi nó bằng tên.

---

## Khi lời không khớp tiếng hát

Tìm đúng **tên bài** không có nghĩa là đúng **bản thu**. Lời của bản thu phòng
đắp lên một bản hát live thì lệch từ đầu đến cuối — nhịp khác, dạo đầu khác, có
khi còn nói chuyện trước khi hát.

Ba lớp xử lý:

**Không giả vờ khớp khi biết là không khớp.** Độ dài bài chênh quá 15 giây so
với bản tìm được thì mốc thời gian chắc chắn sai. Lúc đó lời hiện dạng chữ trơn
— không tô sáng, không tự cuộn — kèm một dòng nói rõ lý do. Tô sáng nhầm một
dòng suốt cả bài còn tệ hơn không tô gì: người dùng tin vào nó rồi mới phát hiện
bị lừa.

**Chạm vào câu đang hát để căn lại.** Một cú chạm khớp lại cả bài, thay cho hàng
chục lần bấm +/- nửa giây như bản Windows.

**Nhớ độ lệch theo từng bài.** Chỉnh một lần, lần sau nghe lại là đúng luôn.

---

## Hai quyền phải tự bật

Cả hai đều không xin được bằng hộp thoại — chỉ mở được đúng trang cài đặt rồi
chờ người dùng bật. Màn hình chính có nút dẫn tới từng trang.

1. **Đọc thông báo** — không có thì không đọc được nhạc app khác.
   Lyra không đọc nội dung thông báo; quyền này chỉ là điều kiện Android đặt ra
   để cho gọi `getActiveSessions`.
2. **Vẽ đè lên app khác** — không có thì không hiện được lời.

---

## Cấu trúc

```
service/   LyraNotificationListener  neo giữ app sống
           Lyra                      trạng thái dùng chung, nối các mảnh
media/     MediaSessionWatcher       bám các phiên media đang chạy
           NowPlaying                kiểu dữ liệu + bù vị trí phát
lyrics/    Identify                  ← chuyển từ identify.ts
           LrcParser                 ← chuyển từ lrc.ts
           LyricsRepository          điều phối nguồn, chấm điểm khớp tên
sources/   LrclibClient
           ZingClient                ← chuyển từ zing.ts, ký HMAC-SHA512
           NctClient                 ← chuyển từ nct.ts, lời mã hoá RC4
           Crypto                    sha256 / hmac512 / rc4
overlay/   OverlayView               View thuần, tự vẽ, có viền chữ
           OverlayHost               dựng cửa sổ, kéo thả, chạm xuyên qua
ui/        MainActivity              Compose, đồng hồ vị trí theo nhịp vẽ
           HomeScreen                ba trang + viên thuốc nổi
           LyraMark                  logo, cử động khi đang tìm
           Artwork                   lấy màu chủ đạo của ảnh bìa
data/      LyricCache                nhớ lời đã tìm (RAM + đĩa)
```

---

## Ba chỗ dễ sai, đã xử lý

**Vị trí phát là ảnh chụp, không phải hiện tại.** `PlaybackState.position` được
chụp tại `lastPositionUpdateTime`. Không bù phần đã trôi thì lời luôn chạy chậm
hơn nhạc vài giây. Và `lastPositionUpdateTime` dùng đồng hồ `elapsedRealtime`,
không phải `currentTimeMillis` — so nhầm hai thang này ra sai lệch hàng chục năm.

**Nhiều app cùng khai báo phiên một lúc.** Spotify tạm dừng còn YouTube đang
phát thì cả hai đều có phiên. Phải chọn phiên **đang phát**, không lấy phiên đầu
danh sách.

**Zing chỉ ký MỘT SỐ tham số.** `/search/multi` chỉ ký `ctime` và `version`,
còn `q` thì không. Ném cả `q` vào là Zing trả về "Incorect signature". Bản
Windows đã mất khá nhiều thời gian cho chỗ này.

**Không dựng foreground service.** `NotificationListenerService` tự nó đã là chỗ
trú do hệ thống giữ sống, lại không phải cắm một thông báo thường trực vào máy
người dùng. Đổi lại mọi trạng thái phải nằm ở `object Lyra` bên ngoài service,
vì hệ thống giết và dựng lại service tuỳ ý.
