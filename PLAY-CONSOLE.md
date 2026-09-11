# Nộp lên Google Play — phần khai báo

Chỗ khó nhất khi đưa AURA lên Play không phải là dựng gói, mà là **giải trình
quyền đọc thông báo**. Tệp này gom sẵn những gì Play sẽ hỏi, để lúc điền không
phải nghĩ lại từ đầu.

Không phải tài liệu kỹ thuật. Nó là bản nháp câu trả lời.

---

## 1. Quyền bị soi: `BIND_NOTIFICATION_LISTENER_SERVICE`

Play xếp quyền đọc thông báo vào nhóm nhạy cảm, và **mặc định là từ chối**. Đơn
được duyệt hay không phụ thuộc gần như hoàn toàn vào chỗ khai mục đích.

### Vì sao AURA cần nó

AURA hiện lời bài hát cho nhạc đang phát ở **app khác** — Zing MP3, NhacCuaTui,
YouTube, Spotify. Để biết bài nào đang phát, nó gọi
`MediaSessionManager.getActiveSessions()`, và Android **chỉ cho gọi hàm đó** khi
app có một `NotificationListenerService` đang được bật.

Nói cách khác: quyền này không dùng để đọc thông báo. Nó là **điều kiện duy nhất
Android đặt ra** để với tới được thẻ media của hệ thống. Không có nó thì tính
năng chính của app không tồn tại.

### Câu trả lời ngắn cho ô khai báo

> AURA là ứng dụng hiển thị lời bài hát. Tính năng chính của nó là hiện lời cho
> nhạc đang phát ở bất kỳ ứng dụng nào khác trên máy.
>
> Ứng dụng cần `NotificationListenerService` vì đó là điều kiện Android bắt buộc
> để gọi `MediaSessionManager.getActiveSessions()` — API duy nhất cho biết bài
> nào đang phát và ở vị trí nào. Ứng dụng **không đọc nội dung thông báo**: nó
> chỉ dùng phiên media (tên bài, tên ca sĩ, trạng thái phát, vị trí phát).
>
> Không có dữ liệu nào rời khỏi thiết bị. Không máy chủ, không tài khoản, không
> thu thập phân tích.

### Câu này kiểm được, không phải nói suông

`LyraNotificationListener` **không ghi đè `onNotificationPosted` lẫn
`onNotificationRemoved`** — đã tra lại mã nguồn. Lớp này chỉ tồn tại để Android
cho phép gọi `getActiveSessions()`. Nếu Play vặn lại, đó là câu trả lời, và nó
kiểm chứng được bằng chính gói đã nộp.

Nếu sau này có ai thêm một trong hai hàm đó vào, phần khai báo ở trên **thành
nói sai** và phải viết lại.

---

## 2. Khai báo an toàn dữ liệu (Data safety)

Phần này Play bắt điền và **đối chiếu với hành vi thật của gói**. Khai sai bị gỡ
ứng dụng, không phải nhắc nhở.

| Mục | Trả lời | Vì sao |
|---|---|---|
| Có thu thập dữ liệu không | **Không** | Không máy chủ, không phân tích, không quảng cáo |
| Có chia sẻ với bên thứ ba không | **Không** | |
| Dữ liệu có mã hoá khi truyền không | Có (HTTPS) | Chỉ khi tra lời ở LRCLIB / Zing / NCT |
| Người dùng xoá được dữ liệu không | **Có** | Xoá lịch sử ngay trong màn hình *Nghe gần đây*; gỡ app là mất sạch |

**Chỗ phải nói thật:** phần dịch lời dùng ML Kit của Google. Lần đầu bật dịch cho
một ngôn ngữ, thư viện ML Kit tải bộ máy dịch về và trong quá trình đó **tự tạo
và gửi đi một Firebase Installation ID**. Đó là thư viện của Google gửi, không
phải AURA — nhưng nó vẫn nằm trong gói, nên vẫn phải khai. Trang chính sách
riêng tư đã nói ra điều này; đừng để hai chỗ nói khác nhau.

---

## 3. Chính sách riêng tư

Play bắt buộc phải có một đường dẫn công khai:

    https://mittohoa.github.io/lyra-player/quyen-rieng-tu.html

Đã cập nhật ngày 12/9/2026 cho khớp với bản 0.3.34 — có mục riêng cho lịch sử
nghe, cho việc sao lưu, và cho nhật ký sự cố.

---

## 4. Những chỗ dễ bị vặn lại

**Quyền vẽ đè lên app khác** (`SYSTEM_ALERT_WINDOW`). Khung lời nổi cần nó. Play
không cấm, nhưng muốn thấy nó phục vụ đúng tính năng đã mô tả — và ở đây thì
đúng: nó chính là cái khung hiện lời.

**Bản `sideload` có tính năng tải nhạc về máy; bản `play` thì không.** Đã tách
sẵn bằng product flavor — kiểm lại rằng gói nộp lên là `playRelease` chứ không
phải `sideloadRelease`. Đây là chỗ sai một lần là gỡ ứng dụng.

**Ảnh chụp màn hình** phải là của bản thật, không dựng. Play đối chiếu.

---

## 5. Trước khi bấm nộp

- [ ] Gói là `bundlePlayRelease` (`app/build/outputs/bundle/playRelease/`)
- [ ] `versionCode` cao hơn mọi bản đã tải lên trước đó
- [ ] Đã thử gói `playRelease` trên máy thật, không phải chỉ `sideloadRelease`
- [ ] Trang chính sách riêng tư mở được bằng đường dẫn công khai ở trên
- [ ] Phần khai mục đích quyền đọc thông báo đã điền
