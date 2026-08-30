# Sec Auto Clear — Chrome Extension

Tiện ích **nhẹ & ổn định** tự động xoá:
- Cache, Lịch sử duyệt web, Lịch sử tải xuống, Form data, Local Storage / FileSystems / WebSQL / CacheStorage
- Tuỳ chọn thêm: Cookies, IndexedDB, Service Workers

Theo chu kỳ:
- **10 phút / 1 giờ / 1 ngày / Khi đóng Chrome** (cửa sổ cuối cùng)

> Ưu tiên nhẹ, MV3 chuẩn docs, tự lưu vào Chrome profile — **xóa file gốc vẫn chạy**.

---

## 🔒 Tự lưu vào Chrome — Xóa file vẫn không sao

**Đã cập nhật theo yêu cầu:** Extension tự lưu **cả code + cài đặt** vào Chrome profile, không phụ thuộc thư mục bạn tải về.

| Cái gì lưu | Lưu ở đâu | Xóa file gốc có mất? |
|------------|-----------|----------------------|
| **Code extension** | Sau khi Pack → Chrome copy vào `User Data/Default/Extensions/<id>/` (`background.js:1`) | **Không mất**. Chỉ khi bạn `Remove` trong `chrome://extensions` mới mất. |
| **Cài đặt** | `chrome.storage.sync` (theo tài khoản Google) + `chrome.storage.local` backup (`background.js:29-62`) | **Không mất**. Ngay cả khi xóa browsingData, storage vẫn giữ ([docs](https://developer.chrome.com/docs/extensions/develop/concepts/storage-and-cookies)). Sync còn theo tài khoản sang máy khác. |

### Cách cài để xóa file thoải mái (2 cách)

**Cách A — Dùng file .crx đã pack sẵn (khuyến nghị):**
1. Trong thư mục đã có sẵn `sec-auto-clear.crx` + `sec-auto-clear.pem` (do `pack.py` tạo)
2. Mở `chrome://extensions` → bật **Developer mode**
3. Kéo thả `sec-auto-clear.crx` vào trang đó → Chrome hỏi → **Add extension**
4. Xong! Giờ bạn **xóa cả thư mục `sec-auto-clear/` tải về cũng không sao**. Chrome đã copy vào profile.
5. **Giữ file `.pem`** để sau này update vẫn cùng ID (`nbpkmnfejelmcbdejpncbmeadohcomib`). Mất pem thì lần pack sau sẽ ra ID mới.

**Cách B — Tự Pack trên máy bạn (nếu kéo .crx bị chặn):**
1. `chrome://extensions` → **Pack extension**
2. `Extension root directory` → chọn thư mục `sec-auto-clear`
3. `Private key file` → để trống lần đầu, Chrome sẽ tạo `.pem` mới
4. Bấm **Pack extension** → ra `.crx` + `.pem` → kéo `.crx` vào như Cách A

> **Load unpacked** (cách cũ) thì **không** xóa file được — Chrome chỉ tham chiếu tới thư mục, xóa là lỗi `Manifest file is missing`. Phải dùng Pack/.crx mới tự lưu.

### Tự động Pack lại (dev)
```bash
python3 sec-auto-clear/pack.py
# ra sec-auto-clear.crx (ID: nbpkmnfejelmcbdejpncbmeadohcomib)
```

---

## Hooks đã đối chiếu docs

| Tính năng | Hook đúng | Docs |
|-----------|-----------|------|
| Định kỳ | `chrome.alarms.create({ periodInMinutes })` + `onAlarm` | [chrome.alarms](https://developer.chrome.com/docs/extensions/reference/api/alarms) — min 0.5 (Chrome 120+), không dùng `setInterval` |
| Xoá | `chrome.browsingData.remove({since:0, originTypes:{unprotectedWeb:true}}, DataTypeSet)` | [browsingData](https://developer.chrome.com/docs/extensions/reference/api/browsingData) |
| Lưu cấu hình | `chrome.storage.sync` + `chrome.storage.local` (dual) + `onChanged` sync | [storage](https://developer.chrome.com/docs/extensions/reference/api/storage) — `local` 10MB, `sync` 100KB, không bị xóa khi clear cache |
| Đóng Chrome | `windows.onRemoved` + `getAll()==0` + fallback `onStartup` | Không có `browser.onClose` đáng tin — async có thể bị kill, nên dọn bù lúc mở lại |
| Vòng đời SW | Listener top-level + `reconcileAlarm()` mỗi lần SW boot | [SW lifecycle](https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle) |

**"Khi đóng Chrome" không 100%:** Chrome tắt là kill process, `browsingData.remove` có thể tốn chục giây (docs) không kịp -> đã làm fallback `onStartup` sau 2s.

---

## Changelog

### v1.1.0
- **Fix:** popup không còn spam reload mỗi giây khi countdown về 0 (chỉ reload đúng 1 lần rồi dừng interval)
- **Fix:** "Lần tới" giờ khớp lịch alarm thật của Chrome — dọn tay không còn đẩy hạn dọn ra xa làm lệch countdown
- **Optimize:** `saveSettings` bỏ qua ghi storage nếu giá trị không đổi (tiết kiệm quota `storage.sync`, SW wake không còn ghi thừa)
- **Optimize:** `storage.onChanged` chỉ ghi chéo sync↔local khi giá trị thực sự khác nhau (tránh ping-pong ghi đè)
- **UI:** hiện "Đã tắt" thay vì countdown khi extension bị tắt; đóng modal Hướng dẫn bằng phím `Esc`; popup tự refresh chỉ chạy khi đang hiển thị
- **Pack:** `pack.py` loại trừ `__pycache__`/`*.pyc`/`.zip` khỏi CRX

## Cài đặt nhanh

1. Kéo `sec-auto-clear.crx` vào `chrome://extensions` (Developer mode ON)
2. Ghim icon 🛡️ → chọn tần suất → tick dữ liệu → `Dọn ngay` để test

---

## Sử dụng

- Switch bật/tắt trên cùng
- Tick `Cookies` sẽ đăng xuất — cân nhắc
- `since:0` = xóa toàn bộ, không phải "từ lần trước"

---

## Độ nhẹ

- 0 framework, 0 content_script, 0 host_permissions, ~300 dòng service worker
- `reconcileAlarm()` idempotent, `isClearing` guard tránh chạy chồng

```
sec-auto-clear/
  manifest.json
  background.js  # dual storage sync+local
  popup.html/js/css
  icons/
  sec-auto-clear.crx / .pem  # sau pack
  pack.py
```

License: MIT
