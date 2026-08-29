# Startup Manager - Portable <5MB

GUI siêu nhẹ quản lý app khởi động cùng Windows, chỉ cần 1 file `.exe` portable không cài đặt, không runtime, không .NET.

**Kích thước:** ~50-80 KB (chưa nén), ~30 KB khi nén UPX — vượt xa yêu cầu <5MB.

**Công nghệ:** C thuần + Win32 API (không framework, không Electron, không Python). Dùng Common Controls (ListView).

## Tính năng

| Chức năng | Mô tả |
|-----------|-------|
| **Theo dõi** | Quét toàn bộ vị trí khởi động: `HKCU\Run`, `HKLM\Run`, `RunOnce`, `Startup Folder (User/Common)`, và backup |
| **Tắt** | Backup giá trị registry/file vào `HKCU\Software\StartupManager_Disabled` rồi xóa gốc → có thể bật lại |
| **Bật lại** | Khôi phục từ backup |
| **Chạy ẩn** | Đổi lệnh `HKCU/HKLM\Run` sang `cmd /c start /min "" "app.exe"` (thu nhỏ) và ngược lại |
| **Xóa** | Xóa vĩnh viễn (không backup) |
| **Mở vị trí** | Mở thư mục chứa file khởi động |

## Build

### Windows (khuyến nghị)
```bat
build.bat
:: hoặc thủ công
gcc -Os -s -municode -mwindows -o StartupManager.exe main.c startup.c resource.rc -lcomctl32 -lshlwapi -lshell32
```

Yêu cầu: MinGW-w64 (https://www.mingw-w64.org/downloads/) hoặc `choco install mingw`

### Linux cross-compile
```bash
make
# 32-bit:
make 32
# nén thêm:
make upx
```

## Sử dụng
1. Chạy `StartupManager.exe` (không cần cài).
2. Nhấn `Làm mới` để quét.
3. Chọn 1 dòng → bấm `Tắt` / `Bật lại` / `Chạy ẩn/hiện` / `Xóa` / `Mở vị trí`.
4. Double-click dòng để xem chi tiết.
5. Với `HKLM\Run` cần **Run as Administrator** mới sửa được (sẽ báo lỗi nếu thiếu quyền).

## Lưu ý "Chạy ẩn"

- Registry `Run` không có flag ẩn sẵn. Tool wrap lệnh bằng `cmd /c start /min` để Windows khởi động ở chế độ minimized (không hiện cửa sổ). 
- Với file `.lnk` trong Startup Folder: chuột phải → Properties → Run: Minimized.
- Để ẩn hoàn toàn (không hiện taskbar), cần dùng Task Scheduler với flag Hidden — có thể thêm trong bản nâng cấp.

## Cấu trúc dự án
```
StartupManager/
├── main.c          # GUI Win32 ListView + Buttons
├── startup.c/h     # Logic quét Registry + Folder
├── resource.rc     # Manifest (DPI aware, Common Controls 6)
├── app.manifest
├── Makefile        # Linux cross-compile
└── build.bat       # Windows build
```

## So sánh kích thước

| Framework | Kích thước exe |
|-----------|---------------|
| **C + Win32 (tool này)** | **~60 KB** ✅ |
| Go + Walk | ~3 MB |
| Python + Tkinter + PyInstaller | ~15-30 MB ❌ |
| Electron | ~150 MB ❌ |
| .NET | cần runtime |

## Giấy phép
MIT — tự do dùng.
